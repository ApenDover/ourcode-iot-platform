package main

import (
	"router-mananger-service/internal/conf"
	"router-mananger-service/internal/util"
)

func main() {
	log := util.GetLogger()
	log.Info("Запуск приложения..")
	//_ = conf.InitHttp()
	_ = conf.InitGrpc()
}
