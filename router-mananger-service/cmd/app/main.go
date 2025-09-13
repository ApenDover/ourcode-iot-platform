package main

import (
	"router-mananger-service/config"
	"router-mananger-service/internal/conf"
	"router-mananger-service/internal/util"
)

func main() {
	log := util.SetupLogger(config.LoadConfig().Profile)
	log.Info("Запуск приложения..")
	//_ = conf.InitHttp()
	_ = conf.InitGrpc()
}
