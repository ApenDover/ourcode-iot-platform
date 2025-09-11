package routes

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/core/routes"
	"router-mananger-service/internal/util"
	"testing"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	tc "github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"

	_ "github.com/lib/pq"
	_ "router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/core/service"
	_ "router-mananger-service/internal/domain"
)

func TestSendPollAckCommandsWithPool(t *testing.T) {
	// SETUP
	gin.SetMode(gin.TestMode)
	ctx := context.Background()

	pool, terminate, err := setupPostgresContainerPool(t)
	require.NoError(t, err)
	defer terminate()

	repo := db.NewPostgresCommandRepository(pool)
	svc := service.NewCommandService(repo)
	engine := gin.New()
	routes.RegisterRoutes(engine, svc)

	// --- 1. SendCommand ---

	// GIVEN
	routerID := uuid.New()
	payload := map[string]interface{}{"foo": "bar"}

	_, err = pool.Exec(ctx,
		`INSERT INTO routers (id, serial_number, ip_address, created_at) 
			VALUES ($1, $2, $3, $4)`, routerID, routerID, "192.168.0.1", time.Now(),
	)
	require.NoError(t, err)

	sendReqBody, _ := json.Marshal(map[string]interface{}{
		"router_id":    routerID,
		"command_type": "TEST_SEND",
		"payload":      payload,
	})

	// WHEN
	req := httptest.NewRequest(http.MethodPost, "/api/v1/send-command", bytes.NewBuffer(sendReqBody))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	engine.ServeHTTP(w, req)

	// THEN
	if w.Code != http.StatusOK {
		t.Fatalf("SendCommand failed: %d, body: %s", w.Code, w.Body.String())
	}

	var sendResp map[string]interface{}
	err = json.Unmarshal(w.Body.Bytes(), &sendResp)
	require.NoError(t, err)
	assert.Equal(t, float64(1), sendResp["created"])

	// THEN CHECK DATABASE
	sendCommand, err := repo.GetAllByRouterId(routerID)
	require.NoError(t, err)
	require.NotEmpty(t, sendCommand)

	firstSendCommand := sendCommand[0]
	fmt.Printf("%+v\n", firstSendCommand)
	assert.Equal(t, "PENDING", firstSendCommand.Status)
	assert.Equal(t, "TEST_SEND", firstSendCommand.CommandType)
	assert.Nil(t, firstSendCommand.SentAt)
	assert.NotNil(t, firstSendCommand.Payload)
	assert.NotNil(t, firstSendCommand.RouterID)
	assert.NotNil(t, firstSendCommand.ID)

	// --- 2. PollCommands ---
	// GIVEN
	pollReqBody, _ := json.Marshal(map[string]interface{}{
		"router_id": routerID,
	})

	// WHEN
	req = httptest.NewRequest(http.MethodPost, "/api/v1/commands/poll", bytes.NewBuffer(pollReqBody))
	req.Header.Set("Content-Type", "application/json")
	w = httptest.NewRecorder()
	engine.ServeHTTP(w, req)

	// THEN
	if w.Code != http.StatusOK {
		t.Fatalf("PollCommands failed: %d, body: %s", w.Code, w.Body.String())
	}

	var pollResp []map[string]interface{}
	err = json.Unmarshal(w.Body.Bytes(), &pollResp)
	require.NoError(t, err)
	require.Equal(t, 1, len(pollResp))
	assert.Equal(t, "SENT", pollResp[0]["status"])

	commandIDStr := pollResp[0]["id"].(string)
	commandID, err := uuid.Parse(commandIDStr)
	require.NoError(t, err)

	// THEN CHECK DATABASE
	pollCommand, err := repo.GetAllByRouterId(routerID)
	require.NoError(t, err)
	require.NotEmpty(t, pollCommand)

	firstPollCommand := pollCommand[0]
	fmt.Printf("%+v\n", firstPollCommand)
	assert.Equal(t, "SENT", firstPollCommand.Status)
	assert.Equal(t, "TEST_SEND", firstPollCommand.CommandType)
	assert.NotNil(t, firstPollCommand.SentAt)
	assert.Nil(t, firstPollCommand.AckedAt)
	assert.NotNil(t, firstPollCommand.Payload)
	assert.NotNil(t, firstPollCommand.RouterID)
	assert.NotNil(t, firstPollCommand.ID)

	// --- 3. AckCommands ---

	// GIVEN
	ackReqBody, _ := json.Marshal(map[string]interface{}{
		"router_id":  routerID,
		"command_id": commandID,
	})

	// WHEN
	req = httptest.NewRequest(http.MethodPost, "/api/v1/commands/ack", bytes.NewBuffer(ackReqBody))
	req.Header.Set("Content-Type", "application/json")
	w = httptest.NewRecorder()
	engine.ServeHTTP(w, req)

	// THEN
	if w.Code != http.StatusOK {
		t.Fatalf("AckCommands failed: %d, body: %s", w.Code, w.Body.String())
	}

	var ackResp map[string]interface{}
	err = json.Unmarshal(w.Body.Bytes(), &ackResp)
	require.NoError(t, err)
	assert.Equal(t, "ACKED", ackResp["status"])

	// THEN CHECK DATABASE
	ackCommand, err := repo.GetAllByRouterId(routerID)
	require.NoError(t, err)
	require.NotEmpty(t, ackCommand)

	firstAckCommand := ackCommand[0]
	fmt.Printf("%+v\n", firstAckCommand)
	assert.Equal(t, "ACKED", firstAckCommand.Status)
	assert.Equal(t, "TEST_SEND", firstAckCommand.CommandType)
	assert.NotNil(t, firstPollCommand.SentAt)
	assert.NotNil(t, firstAckCommand.AckedAt)
	assert.NotNil(t, firstAckCommand.Payload)
	assert.NotNil(t, firstAckCommand.RouterID)
	assert.NotNil(t, firstAckCommand.ID)
}

func setupPostgresContainerPool(t *testing.T) (*pgxpool.Pool, func(), error) {
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
	if err != nil {
		return nil, nil, fmt.Errorf("failed to start container: %w", err)
	}

	host, err := pgContainer.Host(ctx)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get container host: %w", err)
	}
	port, err := pgContainer.MappedPort(ctx, "5432")
	if err != nil {
		return nil, nil, fmt.Errorf("failed to get mapped port: %w", err)
	}

	dsn := fmt.Sprintf("postgres://test:test@%s:%s/testdb?sslmode=disable", host, port.Port())
	pool, err := pgxpool.New(ctx, dsn)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create pool: %w", err)
	}

	for i := 0; i < 30; i++ {
		if err := pool.Ping(ctx); err == nil {
			break
		}
		time.Sleep(time.Second)
	}

	terminate := func() {
		pool.Close()
		if err := pgContainer.Terminate(ctx); err != nil {
			t.Logf("failed to terminate container: %v", err)
		}
	}

	err = db.RunMigrations(dsn, util.MigrationsPath())
	if err != nil {
		terminate()
		return nil, nil, fmt.Errorf("failed to run migrations: %w", err)
	}

	return pool, terminate, nil
}
