package util

import (
	"context"
	"strconv"
)

func StringToInt(str string) int32 {
	res, err := strconv.Atoi(str)
	if err != nil {
		GetLogger(context.Background()).Error("error converting string to int", err)
		return 1
	}
	return int32(res)
}
