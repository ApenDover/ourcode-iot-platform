package routes

import (
	"github.com/gin-gonic/gin"
	"net/http"
)

type Login struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

func Route() {
	enging := gin.Default()
	enging.GET("/ping", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "pong",
		})
	})

	enging.POST("/login", func(c *gin.Context) {
		var json Login
		// проверяем корректность JSON
		if err := c.ShouldBindJSON(&json); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// здесь можно добавить проверку username/password
		c.JSON(http.StatusOK, gin.H{
			"status":   "ok",
			"username": json.Username,
		})
	})

	err := enging.Run()
	if err != nil {
		return
	}
}
