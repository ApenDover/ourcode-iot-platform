package app

import (
	"context"
	"errors"
	"fmt"
	"go.opentelemetry.io/contrib/instrumentation/google.golang.org/grpc/otelgrpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"log/slog"
	"net"
	"net/http"
	routermanager "router-manager-service/internal/ports/genproto"
	"sync"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/redis/go-redis/v9"
	"google.golang.org/grpc"
	"router-manager-service/config"
	"router-manager-service/internal/adapters/innergrpc"
	"router-manager-service/internal/conf/util"
)

type Dependencies struct {
	DBPool      *pgxpool.Pool
	RedisClient *redis.Client
	Config      *config.Config
}

type App struct {
	ctx            context.Context
	cancel         context.CancelFunc
	wg             sync.WaitGroup
	grpcServer     *grpc.Server
	grpcListener   net.Listener
	metricsServer  *http.Server
	managerService *innergrpc.Server
	deps           *Dependencies
	shutdownOnce   sync.Once
}

func New(ctx context.Context, deps *Dependencies) *App {
	ctx, cancel := context.WithCancel(ctx)

	return &App{
		ctx:    ctx,
		cancel: cancel,
		deps:   deps,
	}
}

func (a *App) Start() error {

	if err := a.initGRPCServer(); err != nil {
		return fmt.Errorf("ошибка инициализации gRPC сервера: %w", err)
	}

	if err := a.initMetricsServer(); err != nil {
		return fmt.Errorf("ошибка инициализации metrics сервера: %w", err)
	}

	a.startBackgroundTasks()

	if err := a.startServers(); err != nil {
		return fmt.Errorf("ошибка старта серверов: %w", err)
	}

	return nil
}

func (a *App) initGRPCServer() error {

	a.managerService = innergrpc.NewServer(a.deps.DBPool, a.deps.RedisClient)

	port := a.deps.Config.GRPCPort

	listener, err := net.Listen("tcp", ":"+port)
	if err != nil {
		return fmt.Errorf("ошибка создания gRPC listener: %w", err)
	}
	a.grpcListener = listener
	a.grpcServer = a.createGRPCServer()
	routermanager.RegisterRouterManagerServiceServer(a.grpcServer, a.managerService)

	return nil
}

func (a *App) createGRPCServer() *grpc.Server {
	return grpc.NewServer(
		grpc.StatsHandler(otelgrpc.NewServerHandler()),
		grpc.ChainUnaryInterceptor(
			a.loggingInterceptor(),
			a.recoveryInterceptor(),
		),
	)
}

func (a *App) loggingInterceptor() grpc.UnaryServerInterceptor {
	return func(
		ctx context.Context,
		req interface{},
		info *grpc.UnaryServerInfo,
		handler grpc.UnaryHandler,
	) (interface{}, error) {
		log := util.GetLogger(ctx)
		startTime := time.Now()

		log.Debug("gRPC request started",
			slog.String("method", info.FullMethod),
			slog.String("start_time", startTime.Format(time.RFC3339)),
		)

		resp, err := handler(ctx, req)
		duration := time.Since(startTime)

		level := slog.LevelDebug
		if err != nil {
			level = slog.LevelError
		}

		log.Log(ctx, level, "gRPC request completed",
			slog.String("method", info.FullMethod),
			slog.String("duration", duration.String()),
			slog.String("error", fmt.Sprintf("%v", err)),
		)

		return resp, err
	}
}

func (a *App) recoveryInterceptor() grpc.UnaryServerInterceptor {
	return func(
		ctx context.Context,
		req interface{},
		info *grpc.UnaryServerInfo,
		handler grpc.UnaryHandler,
	) (resp interface{}, err error) {
		defer func() {
			if r := recover(); r != nil {
				log := util.GetLogger(ctx)
				log.Error("gRPC server panic recovered",
					slog.String("method", info.FullMethod),
					slog.Any("panic", r),
				)
				err = status.Errorf(codes.Internal, "internal server error")
			}
		}()

		return handler(ctx, req)
	}
}

func (a *App) initMetricsServer() error {
	port := a.deps.Config.MetricsPort
	if port == "" {
		port = "9090"
	}

	a.metricsServer = &http.Server{
		Addr:         fmt.Sprintf(":%s", port),
		Handler:      promhttp.Handler(),
		ReadTimeout:  5 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  15 * time.Second,
	}
	return nil
}

func (a *App) startBackgroundTasks() {
	a.wg.Add(1)
	go a.runExpiredCommandsChecker()
}

func (a *App) runExpiredCommandsChecker() {
	defer a.wg.Done()

	log := util.GetLogger(a.ctx)
	period := a.deps.Config.CheckExpiredInterval
	expiredTime := a.deps.Config.TimeExpired

	if period <= 0 {
		log.Info("Отключена проверка просроченных команд")
		return
	}

	log.Info("Запускаю scheduller проверки просроченных команд",
		slog.String("period", period.String()),
		slog.String("expired_time", expiredTime.String()),
	)

	ticker := time.NewTicker(period)
	defer ticker.Stop()

	for {
		select {
		case <-a.ctx.Done():
			log.Info("Останавливаю scheduller проверки просроченных команд")
			return
		case <-ticker.C:
			a.checkExpiredCommands(period, expiredTime, log)
		}
	}
}

func (a *App) checkExpiredCommands(period, expiredTime time.Duration, log *slog.Logger) {
	ctx, cancel := context.WithTimeout(a.ctx, period/2)
	defer cancel()

	startTime := time.Now()
	err := a.managerService.ManagerService.MarkExpiredAsError(ctx, expiredTime)
	duration := time.Since(startTime)

	if err != nil {
		if errors.Is(err, context.Canceled) || errors.Is(err, context.DeadlineExceeded) {
			log.Debug("ошибка при проверке просроченных SENT записей", slog.String("reason", err.Error()))
		} else {
			log.Error("ошибка при простаноке ERROR статуса просроченным командам", slog.String("error", err.Error()))
		}
	} else {
		log.Debug("проверка просроченных SENT записей завершена ", slog.Duration("duration", duration))
	}
}

func (a *App) startServers() error {
	a.wg.Add(1)
	go func() {
		defer a.wg.Done()
		log := util.GetLogger(a.ctx)

		log.Info("Запускаю gRPC сервер", slog.String("info", a.grpcListener.Addr().String()))

		if err := a.grpcServer.Serve(a.grpcListener); err != nil && !errors.Is(err, grpc.ErrServerStopped) {
			log.Error("ошибка gRPC сервера", slog.String("error", err.Error()))
		}
	}()
	a.wg.Add(1)
	go func() {
		defer a.wg.Done()
		log := util.GetLogger(a.ctx)

		log.Info("Запускаю metrics сервер", slog.String("info", a.metricsServer.Addr))

		if err := a.metricsServer.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Error("ошибка metrics сервара", slog.String("error", err.Error()))
		}
	}()

	return nil
}

func (a *App) Stop() {
	a.shutdownOnce.Do(a.gracefulShutdown)
}

func (a *App) gracefulShutdown() {
	log := util.GetLogger(a.ctx)
	log.Info("Начинаю graceful shutdown")

	a.cancel()
	a.stopServers(log)
	a.waitForShutdown(log)

	log.Info("Graceful shutdown завершен")
}

func (a *App) stopServers(log *slog.Logger) {
	if a.grpcServer != nil {
		log.Info("Stopping gRPC server")
		a.grpcServer.GracefulStop()
		log.Info("gRPC server stopped")
	}

	if a.metricsServer != nil {
		log.Info("останавливаю metrics сервер")
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		if err := a.metricsServer.Shutdown(ctx); err != nil {
			log.Error("не получилось остановить metrics сервер", slog.String("error", err.Error()))
		} else {
			log.Info("metrics сервер остановлен")
		}
	}
}

func (a *App) waitForShutdown(log *slog.Logger) {
	shutdownChan := make(chan struct{})

	go func() {
		a.wg.Wait()
		close(shutdownChan)
	}()

	select {
	case <-shutdownChan:
		log.Info("все горутинки завершены")
	case <-time.After(30 * time.Second):
		log.Warn("аккуратно завершиться не удалось, таймаут истек")
	}
}

func (a *App) GetGRPCAddress() string {
	if a.grpcListener == nil {
		return ""
	}
	return a.grpcListener.Addr().String()
}
