package metrics

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var (
	CommandsSent = promauto.NewCounterVec(prometheus.CounterOpts{
		Name: "router_commands_sent_total",
		Help: "Total number of commands sent",
	}, []string{"command_type"})

	CommandsPolled = promauto.NewCounter(prometheus.CounterOpts{
		Name: "router_commands_polled_total",
		Help: "Total number of commands polled",
	})

	CommandsAcked = promauto.NewCounter(prometheus.CounterOpts{
		Name: "router_commands_acked_total",
		Help: "Total number of commands acknowledged",
	})

	MethodDuration = promauto.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "method_duration_seconds",
		Help:    "Time spent processing commands",
		Buckets: []float64{.001, .005, .01, .025, .05, .1, .25, .5, 1},
	}, []string{"method"})

	DatabaseDuration = promauto.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "database_duration_seconds",
		Help:    "Time spent processing database query",
		Buckets: []float64{.001, .005, .01, .025, .05, .1, .25, .5, 1},
	}, []string{"query_type"})

	CommandErrors = promauto.NewCounterVec(prometheus.CounterOpts{
		Name: "router_command_errors_total",
		Help: "Total number of command errors",
	}, []string{"method", "error_type"})
)
