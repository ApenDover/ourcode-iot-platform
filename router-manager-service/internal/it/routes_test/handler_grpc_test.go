package routes

import (
	"context"
	"fmt"
	"github.com/redis/go-redis/v9"
	"google.golang.org/protobuf/types/known/structpb"
	"router-manager-service/internal/conf/util"
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
	"router-manager-service/config"
	"router-manager-service/internal/adapters/db"
	"router-manager-service/internal/conf"
	routermanager "router-manager-service/internal/ports/genproto"
)

func TestSendPollAckCommandsWithPoolGrpc(t *testing.T) {
	// SETUP
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	_, pool, terminate, err := SetupPostgresContainerPool(t)
	require.NoError(t, err)
	defer terminate()

	cfg := config.LoadConfig()

	rc := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.RedisUrl, cfg.RedisPort),
		Password: cfg.RedisPassword,
		DB:       0,
	})
	defer rc.Close()

	grpcServer := conf.InitGrpc(pool, rc)
	defer grpcServer.GracefulStop()

	conn, err := grpc.NewClient("localhost:9092",
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	require.NoError(t, err)
	defer conn.Close()

	client := routermanager.NewRouterManagerServiceClient(conn)

	commandRepo := db.NewPostgresCommandsAdapter(pool)
	routerRepo := db.NewPostgresRouterAdapter(pool)

	// CreateCommand ---

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

	// THEN CHECK DATABASE COMMAND
	sendCommand, err := commandRepo.GetAll(ctx)
	require.NoError(t, err)
	require.NotEmpty(t, sendCommand)

	firstSendCommand := sendCommand[0]
	fmt.Printf("%+v\n", firstSendCommand)
	assert.Equal(t, "PENDING", string(firstSendCommand.Status))
	assert.Equal(t, "TEST_SEND", firstSendCommand.CommandType)
	assert.Nil(t, firstSendCommand.SentAt)
	assert.NotNil(t, firstSendCommand.Payload)
	assert.NotNil(t, firstSendCommand.RouterID)
	assert.NotNil(t, firstSendCommand.ID)

	// THEN CHECK DATABASE ROUTER
	router, err := routerRepo.GetBySerial(ctx, serial)
	require.NoError(t, err)
	require.NotNil(t, router)
	fmt.Printf("%+v\n", router)

	assert.Equal(t, router.SerialNumber, serial)
	assert.Nil(t, router.LastSeenAt)
	assert.NotNil(t, router.CreatedAt)

	// PollCommands ---
	// GIVEN

	// WHEN
	pollResp, err := client.PollCommands(ctx, &routermanager.PollCommandsRequest{
		RouterSerial: serial,
	})

	// THEN
	require.NoError(t, err)
	require.Equal(t, 1, len(pollResp.Commands))
	assert.Equal(t, "SENT", pollResp.Commands[0].Status)

	commandIDStr := pollResp.Commands[0].Id
	commandID, err := uuid.Parse(commandIDStr)
	require.NoError(t, err)

	// THEN CHECK DATABASE
	pollCommand, err := commandRepo.GetAll(ctx)
	require.NoError(t, err)
	require.NotEmpty(t, pollCommand)

	firstPollCommand := pollCommand[0]
	fmt.Printf("%+v\n", firstPollCommand)
	assert.Equal(t, "SENT", string(firstPollCommand.Status))
	assert.Equal(t, "TEST_SEND", firstPollCommand.CommandType)
	assert.NotNil(t, firstPollCommand.SentAt)
	assert.Nil(t, firstPollCommand.AckedAt)
	assert.NotNil(t, firstPollCommand.Payload)
	assert.NotNil(t, firstPollCommand.RouterID)
	assert.NotNil(t, firstPollCommand.ID)

	// THEN CHECK DATABASE ROUTER
	routerPoll, err := routerRepo.GetBySerial(ctx, serial)
	require.NoError(t, err)
	require.NotNil(t, routerPoll)
	fmt.Printf("%+v\n", routerPoll)

	assert.Equal(t, routerPoll.SerialNumber, serial)
	assert.NotNil(t, routerPoll.LastSeenAt)
	assert.NotNil(t, routerPoll.CreatedAt)

	// AckCommands

	// GIVEN

	// WHEN
	ackResp, err := client.AckCommand(ctx, &routermanager.AckCommandRequest{
		RouterSerial: serial,
		CommandId:    commandID.String(),
	})

	// THEN
	require.NoError(t, err)
	assert.Equal(t, "ACKED", ackResp.Status)

	// THEN CHECK DATABASE
	ackCommand, err := commandRepo.GetAll(ctx)
	require.NoError(t, err)
	require.NotEmpty(t, ackCommand)

	firstAckCommand := ackCommand[0]
	fmt.Printf("%+v\n", firstAckCommand)
	assert.Equal(t, "ACKED", string(firstAckCommand.Status))
	assert.Equal(t, "TEST_SEND", firstAckCommand.CommandType)
	assert.NotNil(t, firstPollCommand.SentAt)
	assert.NotNil(t, firstAckCommand.AckedAt)
	assert.NotNil(t, firstAckCommand.Payload)
	assert.NotNil(t, firstAckCommand.RouterID)
	assert.NotNil(t, firstAckCommand.ID)

	// THEN CHECK DATABASE ROUTER
	routerAck, err := routerRepo.GetBySerial(ctx, serial)
	require.NoError(t, err)
	require.NotNil(t, routerAck)
	fmt.Printf("%+v\n", routerAck)

	assert.Equal(t, routerAck.SerialNumber, serial)
	assert.NotNil(t, routerAck.LastSeenAt)
	assert.NotNil(t, routerAck.CreatedAt)
	assert.NotEqual(t, routerPoll.LastSeenAt, routerAck.LastSeenAt)
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

	host, _ := pgContainer.Host(ctx)
	port, _ := pgContainer.MappedPort(ctx, "5432")

	dsn := fmt.Sprintf("postgres://test:test@%s:%s/testdb?sslmode=disable", host, port.Port())
	pool, err := pgxpool.New(ctx, dsn)
	require.NoError(t, err)

	for i := 0; i < 30; i++ {
		if err := pool.Ping(ctx); err == nil {
			break
		}
		time.Sleep(time.Second)
	}

	conf.RunMigrations(dsn, util.MigrationsPath(config.LoadConfig().MigrationPath))

	terminate := func() {
		pool.Close()
		_ = pgContainer.Terminate(ctx)
	}

	return &dsn, pool, terminate, nil
}
