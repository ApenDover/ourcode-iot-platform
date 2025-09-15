package routes

import (
	"net/http"
	"router-manager-service/internal/core/service"
	"router-manager-service/internal/domain"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type SendCommandRequest struct {
	SerialNumber string                 `json:"router_serial"`
	CommandType  string                 `json:"command_type" binding:"required"`
	Payload      map[string]interface{} `json:"payload"`
}

func RegisterRoutes(engine *gin.Engine, service *service.ManagerService) {
	api := engine.Group("/api/v1")

	api.POST("/send-command", func(c *gin.Context) {
		var request SendCommandRequest
		if err := c.ShouldBindJSON(&request); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		var commands []domain.CommandOut

		if request.SerialNumber != "" {
			var cmd domain.CommandOut
			cmd = service.CreateCommand(c, request.SerialNumber, request.CommandType, request.Payload)
			commands = append(commands, cmd)
		} else {
			commands = service.CreateCommandForAll(c, request.CommandType, request.Payload)
		}

		c.JSON(http.StatusOK, gin.H{
			"created": len(commands),
			"ids":     getCommandIDs(commands),
		})
	})

	type PollCommandRequest struct {
		SerialNumber string `json:"router_serial" binding:"required"`
	}

	api.POST("/commands/poll", func(c *gin.Context) {
		var request PollCommandRequest
		if err := c.ShouldBindJSON(&request); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"err": err.Error()})
			return
		}

		commands := service.GetPendingCommandsAndMarkItSent(c, request.SerialNumber)

		if len(commands) == 0 {
			c.JSON(http.StatusOK, []gin.H{})
			return
		}

		now := time.Now()
		for i := range commands {
			commands[i].Status = "SENT"
			commands[i].SentAt = &now
		}

		response := make([]gin.H, 0, len(commands))
		for _, cmd := range commands {
			response = append(response, gin.H{
				"id":         cmd.ID,
				"status":     cmd.Status,
				"created_at": cmd.CreatedAt,
			})
		}
		c.JSON(http.StatusOK, response)
	})

	type AckCommandRequest struct {
		SerialNumber string    `json:"router_serial" binding:"required"`
		CommandID    uuid.UUID `json:"command_id" binding:"required"`
	}

	api.POST("/commands/ack", func(c *gin.Context) {
		var request AckCommandRequest
		if err := c.ShouldBindJSON(&request); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"err": err.Error()})
			return
		}

		service.AckCommand(c, request.SerialNumber, request.CommandID)

		c.JSON(http.StatusOK, gin.H{
			"status":     "ACKED",
			"command_id": request.CommandID,
		})
	})
}

func getCommandIDs(commands []domain.CommandOut) []uuid.UUID {
	ids := make([]uuid.UUID, len(commands))
	for i, cmd := range commands {
		ids[i] = cmd.ID
	}
	return ids
}
