package main

import (
	"router-mananger-service/config"
	"router-mananger-service/internal/conf"
	"router-mananger-service/internal/util"
)

func main() {
	util.SetupLogger(config.LoadConfig().Profile)
	conf.InitGrpc()
}
