package routes

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"router-manager-service/internal/adapters/db"
	"router-manager-service/internal/adapters/routes"
	"router-manager-service/internal/conf"
	"router-manager-service/internal/core/domainService"
	"router-manager-service/internal/core/service"
	"testing"
)

func TestSendPollAckCommandsWithPoolGrpc(t *testing.T) {
	// SETUP
	gin.SetMode(gin.TestMode)

	dbPath, pool, terminate, err := SetupPostgresContainerPool(t)
	require.NoError(t, err)
	defer terminate()

	go conf.InitGrpc(pool, *dbPath)

	commandRepo := db.NewPostgresCommandRepository(pool)
	routerRepo := db.NewPostgresRouterRepository(pool)
	cs := domainService.NewCommandService(commandRepo)
	rs := domainService.NewRouterService(routerRepo)

	svc := service.NewManagerService(cs, rs)
	engine := gin.New()
	routes.RegisterRoutes(engine, svc)

	// CreateCommand ---

	// GIVEN
	serial := uuid.New().String()
	payload := map[string]interface{}{"foo": "bar"}

	sendReqBody, _ := json.Marshal(map[string]interface{}{
		"router_serial": serial,
		"command_type":  "TEST_SEND",
		"payload":       payload,
	})

	// WHEN
	req := httptest.NewRequest(http.MethodPost, "/api/v1/send-command", bytes.NewBuffer(sendReqBody))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	engine.ServeHTTP(w, req)

	// THEN
	if w.Code != http.StatusOK {
		t.Fatalf("CreateCommand failed: %d, body: %s", w.Code, w.Body.String())
	}

	var sendResp map[string]interface{}
	err = json.Unmarshal(w.Body.Bytes(), &sendResp)
	require.NoError(t, err)
	assert.Equal(t, float64(1), sendResp["created"])

	// THEN CHECK DATABASE COMMAND
	sendCommand, err := commandRepo.GetAllByRouterSerial(serial)
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
	router, err := routerRepo.GetBySerial(serial)
	require.NoError(t, err)
	require.NotEmpty(t, router)
	fmt.Printf("%+v\n", router)

	assert.Equal(t, router.SerialNumber, serial)
	assert.Nil(t, router.LastSeenAt)
	assert.NotNil(t, router.CreatedAt)

	// PollCommands ---
	// GIVEN
	pollReqBody, _ := json.Marshal(map[string]interface{}{
		"router_serial": serial,
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
	pollCommand, err := commandRepo.GetAllByRouterSerial(serial)
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
	routerPoll, err := routerRepo.GetBySerial(serial)
	require.NoError(t, err)
	require.NotEmpty(t, routerPoll)
	fmt.Printf("%+v\n", routerPoll)

	assert.Equal(t, routerPoll.SerialNumber, serial)
	assert.NotNil(t, routerPoll.LastSeenAt)
	assert.NotNil(t, routerPoll.CreatedAt)

	// AckCommands

	// GIVEN
	ackReqBody, _ := json.Marshal(map[string]interface{}{
		"router_serial": serial,
		"command_id":    commandID,
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
	ackCommand, err := commandRepo.GetAllByRouterSerial(serial)
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
	routerAck, err := routerRepo.GetBySerial(serial)
	require.NoError(t, err)
	require.NotEmpty(t, routerAck)
	fmt.Printf("%+v\n", routerAck)

	assert.Equal(t, routerAck.SerialNumber, serial)
	assert.NotNil(t, routerAck.LastSeenAt)
	assert.NotNil(t, routerAck.CreatedAt)
	assert.NotEqual(t, routerPoll.LastSeenAt, routerAck.LastSeenAt)
}
