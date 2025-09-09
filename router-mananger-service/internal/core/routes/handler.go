package routes

import (
	"net/http"
	"router-mananger-service/internal/core/service"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type SendCommandRequest struct {
	RouterID    uuid.UUID              `json:"router_id"`
	CommandType string                 `json:"command_type" binding:"required"`
	Payload     map[string]interface{} `json:"payload"`
}

func RegisterRoutes(engine *gin.Engine, service *service.CommandService) {
	api := engine.Group("/api/v1")
	api.POST("/send-command", func(c *gin.Context) {
		var request SendCommandRequest
		if error := c.ShouldBindJSON(&request); error != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": error.Error()})
			return
		}

		cmd, err := service.SendCommand(request.RouterID, request.CommandType, request.Payload)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, gin.H{
			"id":         cmd.ID,
			"status":     cmd.Status,
			"created_at": cmd.CreatedAt,
		})
	})
}
