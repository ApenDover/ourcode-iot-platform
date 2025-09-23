package routes

import (
	"context"
	"fmt"
	"github.com/redis/go-redis/v9"
	"router-manager-service/cmd/app"
	"router-manager-service/internal/database"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	tc "github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/protobuf/types/known/structpb"

	"router-manager-service/config"
	"router-manager-service/internal/adapters/db"
	routermanager "router-manager-service/internal/ports/genproto"
)

func TestSendPollAckCommandsWithPoolGrpc(t *testing.T) {
	// SETUP
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second) // увеличиваем таймаут
	defer cancel()

	// Поднимаем тестовые контейнеры
	_, pool, terminate, err := SetupPostgresContainerPool(t)
	require.NoError(t, err)
	defer terminate()

	cfg := config.LoadConfig()

	// Используем тестовый Redis (можно также поднять через testcontainers)
	rc := createTestRedisClient(cfg)
	defer rc.Close()

	// Создаем зависимости приложения
	deps := &app.Dependencies{
		DBPool:      pool,
		RedisClient: rc,
		Config: &config.Config{
			GRPCPort:      "0",
			MetricsPort:   "0",
			RedisUrl:      cfg.RedisUrl,
			RedisPort:     cfg.RedisPort,
			RedisPassword: cfg.RedisPassword,
		},
	}

	application := app.New(ctx, deps)
	require.NoError(t, err)

	err = application.Start()
	require.NoError(t, err)
	defer application.Stop()

	// Даем серверу время запуститься
	time.Sleep(500 * time.Millisecond)

	// Создаем gRPC клиент
	conn, err := grpc.DialContext(
		ctx,
		application.GetGRPCAddress(),
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithBlock(),
		grpc.WithTimeout(5*time.Second),
	)

	require.NoError(t, err)
	defer conn.Close()

	client := routermanager.NewRouterManagerServiceClient(conn)

	// Репозитории для проверки БД
	commandRepo := db.NewPostgresCommandsAdapter(pool)
	routerRepo := db.NewPostgresRouterAdapter(pool)

	// TEST: SendCommand
	t.Run("SendCommand", func(t *testing.T) {
		// GIVEN
		serial := uuid.New().String()
		payload := map[string]interface{}{"foo": "bar"}

		pbPayload, err := structpb.NewStruct(payload)
		require.NoError(t, err)

		// WHEN
		sendResp, err := client.SendCommand(ctx, &routermanager.SendCommandRequest{
			RouterSerial: serial,
			CommandType:  "TEST_SEND",
			Payload:      pbPayload,
		})

		// THEN
		require.NoError(t, err)
		assert.Equal(t, int32(1), sendResp.Created)

		// Проверяем команду в БД
		sendCommand, err := commandRepo.GetAll(ctx)
		require.NoError(t, err)
		require.Len(t, sendCommand, 1)

		firstSendCommand := sendCommand[0]
		t.Logf("Send Command: %+v", firstSendCommand)
		assert.Equal(t, "PENDING", string(firstSendCommand.Status))
		assert.Equal(t, "TEST_SEND", firstSendCommand.CommandType)
		assert.Nil(t, firstSendCommand.SentAt)
		assert.NotNil(t, firstSendCommand.Payload)
		assert.NotNil(t, firstSendCommand.RouterID)
		assert.NotNil(t, firstSendCommand.ID)

		// Проверяем роутер в БД
		router, err := routerRepo.GetBySerial(ctx, serial)
		require.NoError(t, err)
		require.NotNil(t, router)
		t.Logf("Router: %+v", router)

		assert.Equal(t, serial, router.SerialNumber)
		assert.Nil(t, router.LastSeenAt)
		assert.NotNil(t, router.CreatedAt)

		// TEST: PollCommands
		t.Run("PollCommands", func(t *testing.T) {
			// WHEN
			pollResp, err := client.PollCommands(ctx, &routermanager.PollCommandsRequest{
				RouterSerial: serial,
			})

			// THEN
			require.NoError(t, err)
			require.Len(t, pollResp.Commands, 1)
			assert.Equal(t, "SENT", pollResp.Commands[0].Status)

			commandIDStr := pollResp.Commands[0].Id
			commandID, err := uuid.Parse(commandIDStr)
			require.NoError(t, err)

			// Проверяем команду в БД после poll
			pollCommand, err := commandRepo.GetAll(ctx)
			require.NoError(t, err)
			require.Len(t, pollCommand, 1)

			firstPollCommand := pollCommand[0]
			t.Logf("Poll Command: %+v", firstPollCommand)
			assert.Equal(t, "SENT", string(firstPollCommand.Status))
			assert.Equal(t, "TEST_SEND", firstPollCommand.CommandType)
			assert.NotNil(t, firstPollCommand.SentAt)
			assert.Nil(t, firstPollCommand.AckedAt)
			assert.NotNil(t, firstPollCommand.Payload)
			assert.NotNil(t, firstPollCommand.RouterID)
			assert.Equal(t, commandID, firstPollCommand.ID)

			// Проверяем роутер в БД после poll
			routerPoll, err := routerRepo.GetBySerial(ctx, serial)
			require.NoError(t, err)
			require.NotNil(t, routerPoll)
			t.Logf("Router after poll: %+v", routerPoll)

			assert.Equal(t, serial, routerPoll.SerialNumber)
			assert.NotNil(t, routerPoll.LastSeenAt)
			assert.NotNil(t, routerPoll.CreatedAt)

			// TEST: AckCommand
			t.Run("AckCommand", func(t *testing.T) {
				// WHEN
				ackResp, err := client.AckCommand(ctx, &routermanager.AckCommandRequest{
					RouterSerial: serial,
					CommandId:    commandID.String(),
				})

				// THEN
				require.NoError(t, err)
				assert.Equal(t, "ACKED", ackResp.Status)

				// Проверяем команду в БД после ack
				ackCommand, err := commandRepo.GetAll(ctx)
				require.NoError(t, err)
				require.Len(t, ackCommand, 1)

				firstAckCommand := ackCommand[0]
				t.Logf("Ack Command: %+v", firstAckCommand)
				assert.Equal(t, "ACKED", string(firstAckCommand.Status))
				assert.Equal(t, "TEST_SEND", firstAckCommand.CommandType)
				assert.NotNil(t, firstAckCommand.SentAt)
				assert.NotNil(t, firstAckCommand.AckedAt)
				assert.NotNil(t, firstAckCommand.Payload)
				assert.NotNil(t, firstAckCommand.RouterID)
				assert.Equal(t, commandID, firstAckCommand.ID)

				// Проверяем роутер в БД после ack
				routerAck, err := routerRepo.GetBySerial(ctx, serial)
				require.NoError(t, err)
				require.NotNil(t, routerAck)
				t.Logf("Router after ack: %+v", routerAck)

				assert.Equal(t, serial, routerAck.SerialNumber)
				assert.NotNil(t, routerAck.LastSeenAt)
				assert.NotNil(t, routerAck.CreatedAt)

				// LastSeenAt должен обновиться после ack
				assert.True(t, routerAck.LastSeenAt.After(*routerPoll.LastSeenAt),
					"LastSeenAt should be updated after ack")
			})
		})
	})
}

func SetupPostgresContainerPool(t *testing.T) (*string, *pgxpool.Pool, func(), error) {
	ctx := context.Background()
	req := tc.ContainerRequest{
		Image:        "postgres:16",
		ExposedPorts: []string{"5432/tcp"},
		Env: map[string]string{
			"POSTGRES_USER":     "test",
			"POSTGRES_PASSWORD": "test",
			"POSTGRES_DB":       "testdb",
		},
		WaitingFor: wait.ForListeningPort("5432/tcp"),
	}

	pgContainer, err := tc.GenericContainer(ctx, tc.GenericContainerRequest{
		ContainerRequest: req,
		Started:          true,
	})
	require.NoError(t, err)

	host, err := pgContainer.Host(ctx)
	require.NoError(t, err)

	port, err := pgContainer.MappedPort(ctx, "5432")
	require.NoError(t, err)

	dsn := fmt.Sprintf("postgres://test:test@%s:%s/testdb?sslmode=disable", host, port.Port())

	// Ждем пока БД будет готова принимать соединения
	var pool *pgxpool.Pool
	for i := 0; i < 30; i++ {
		pool, err = pgxpool.New(ctx, dsn)
		if err == nil {
			if err := pool.Ping(ctx); err == nil {
				break
			}
		}
		if i == 29 {
			require.NoError(t, err, "Failed to connect to database after 30 attempts")
		}
		time.Sleep(time.Second)
	}

	// Выполняем миграции используя наш новый migrator
	migrator, err := database.NewMigrator(dsn)
	require.NoError(t, err)
	defer migrator.Close()

	err = migrator.Up()
	require.NoError(t, err)

	terminate := func() {
		if pool != nil {
			pool.Close()
		}
		if pgContainer != nil {
			_ = pgContainer.Terminate(ctx)
		}
	}

	return &dsn, pool, terminate, nil
}

func createTestRedisClient(cfg *config.Config) *redis.Client {
	return redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.RedisUrl, cfg.RedisPort),
		Password: cfg.RedisPassword,
		DB:       1, // используем БД 1 для тестов чтобы не мешать основным данным
	})
}
