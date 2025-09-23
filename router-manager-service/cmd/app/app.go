package app

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"sync"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/redis/go-redis/v9"
	"google.golang.org/grpc"
	"router-manager-service/config"
	"router-manager-service/internal/conf/util"
)

type Dependencies struct {
	DBPool      *pgxpool.Pool
	RedisClient *redis.Client
	Config      *config.Config
}

type App struct {
	ctx           context.Context
	cancel        context.CancelFunc
	wg            sync.WaitGroup
	grpcServer    *grpc.Server
	metricsServer *http.Server
	grpcListener  net.Listener
	deps          *Dependencies
}

func New(ctx context.Context, deps *Dependencies) (*App, error) {
	ctx, cancel := context.WithCancel(ctx)

	app := &App{
		ctx:    ctx,
		cancel: cancel,
		deps:   deps,
	}

	grpcServer, err := initGRPCServer(ctx, deps.DBPool, deps.RedisClient)
	if err != nil {
		cancel()
		return nil, fmt.Errorf("failed to init gRPC server: %w", err)
	}
	app.grpcServer = grpcServer

	app.metricsServer = &http.Server{
		Addr:    fmt.Sprintf(":%s", deps.Config.MetricsPort),
		Handler: promhttp.Handler(),
	}

	return app, nil
}

func (a *App) Start() error {
	log := util.GetLogger(a.ctx)

	if err := a.startGRPC(); err != nil {
		return fmt.Errorf("failed to start gRPC server: %w", err)
	}
	log.Info("gRPC сервер запущен", slog.String("addr", a.GetGRPCAddress()))

	if err := a.startMetrics(); err != nil {
		return fmt.Errorf("failed to start metrics server: %w", err)
	}
	log.Info("Сервер метрик запущен", slog.String("addr", a.deps.Config.MetricsPort))

	return nil
}

func (a *App) startGRPC() error {
	port := a.deps.Config.GRPCPort
	if port == "" {
		port = "50051"
	}

	lis, err := net.Listen("tcp", ":"+port)
	if err != nil {
		return fmt.Errorf("failed to listen: %w", err)
	}
	a.grpcListener = lis

	a.wg.Add(1)
	go func() {
		defer a.wg.Done()
		if err := a.grpcServer.Serve(lis); err != nil && !errors.Is(err, grpc.ErrServerStopped) {
			slog.Error("gRPC server error", slog.String("error", err.Error()))
		}
	}()

	return nil
}

func (a *App) startMetrics() error {
	a.wg.Add(1)
	go func() {
		defer a.wg.Done()
		if err := a.metricsServer.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			slog.Error("Metrics server error", slog.String("error", err.Error()))
		}
	}()

	return nil
}

func (a *App) GetGRPCAddress() string {
	if a.grpcListener == nil {
		return ""
	}
	return a.grpcListener.Addr().String()
}

func (a *App) Stop() {
	log := util.GetLogger(a.ctx)

	log.Info("Остановка приложения...")
	a.cancel()

	// Остановка gRPC сервера
	if a.grpcServer != nil {
		log.Info("Остановка gRPC сервера...")
		grpcCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()

		stopped := make(chan struct{})
		go func() {
			a.grpcServer.GracefulStop()
			close(stopped)
		}()

		select {
		case <-stopped:
			log.Info("gRPC сервер остановлен")
		case <-grpcCtx.Done():
			log.Warn("Таймаут остановки gRPC, принудительная остановка")
			a.grpcServer.Stop()
		}
	}

	if a.metricsServer != nil {
		log.Info("Остановка сервера метрик...")
		metricsCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		if err := a.metricsServer.Shutdown(metricsCtx); err != nil {
			log.Error("Ошибка остановки метрик", slog.String("error", err.Error()))
		} else {
			log.Info("Сервер метрик остановлен")
		}
	}

	done := make(chan struct{})
	go func() {
		a.wg.Wait()
		close(done)
	}()

	select {
	case <-done:
		log.Info("Все горутины завершены")
	case <-time.After(15 * time.Second):
		log.Warn("Таймаут graceful shutdown")
	}

	log.Info("Приложение остановлено")
}

func initGRPCServer(ctx context.Context, pool *pgxpool.Pool, rc *redis.Client) (*grpc.Server, error) {
	var wg sync.WaitGroup
	return InitGrpc(ctx, pool, rc, &wg), nil
}
