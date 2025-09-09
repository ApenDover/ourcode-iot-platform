package main

import (
	"fmt"
	"log/slog"
	"os"
)

func main() {
	s := "gopher"
	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))
	logger.Info("Hello and welcome", slog.String("user", s))

	for i := 1; i <= 5; i++ {
		fmt.Println("i =", 100/i)
	}
}
