package innergrpc

import (
	"context"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
	"go.opentelemetry.io/contrib/instrumentation/google.golang.org/grpc/otelgrpc"
	"go.opentelemetry.io/otel/trace"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"
	"log/slog"
	"net"
	"router-manager-service/internal/adapters/cache"
	"router-manager-service/internal/adapters/db"
	"router-manager-service/internal/conf/util"
	"router-manager-service/internal/core/domain"
	"router-manager-service/internal/core/service"
	routermanager "router-manager-service/internal/ports/genproto"
)

type Server struct {
	routermanager.UnimplementedRouterManagerServiceServer
	ManagerService *service.ManagerService
}

func NewServer(pool *pgxpool.Pool, redisClient *redis.Client) *Server {
	pgc := db.NewPostgresCommonAdapter(pool)
	pra := db.NewPostgresRouterAdapter(pool)
	ca := cache.NewRedisClient(redisClient)
	managerService := service.NewManagerService(pgc, pra, ca)
	return &Server{
		ManagerService: managerService,
	}
}

func (s *Server) mustEmbedUnimplementedRouterManagerServiceServer() {}

func (s *Server) Start(port string) (*grpc.Server, error) {
	lis, err := net.Listen("tcp", ":"+port)
	if err != nil {
		return nil, fmt.Errorf("не могу прослушат порт %s: %w", port, err)
	}

	loggingInterceptor := func(
		ctx context.Context,
		req interface{},
		info *grpc.UnaryServerInfo,
		handler grpc.UnaryHandler,
	) (interface{}, error) {
		log := util.GetLogger(ctx)

		span := trace.SpanFromContext(ctx)
		sc := span.SpanContext()
		log.Info(">>>> gRPC request",
			slog.String("method", info.FullMethod),
			slog.String("request", fmt.Sprintf("%+v", req)),
			slog.String("trace_id", sc.TraceID().String()),
			slog.String("span_id", sc.SpanID().String()),
		)

		resp, err := handler(ctx, req)

		log.Info("<<<< gRPC response",
			slog.String("method", info.FullMethod),
			slog.String("response", fmt.Sprintf("%+v", resp)),
			slog.String("error", fmt.Sprintf("%v", err)),
			slog.String("trace_id", sc.TraceID().String()),
			slog.String("span_id", sc.SpanID().String()),
		)
		return resp, err
	}

	util.GetLogger(context.Background()).Info("Запуск gRPC сервера на порту " + port)

	grpcServer := grpc.NewServer(
		grpc.StatsHandler(otelgrpc.NewServerHandler()),
		grpc.UnaryInterceptor(loggingInterceptor),
	)

	routermanager.RegisterRouterManagerServiceServer(grpcServer, s)

	go func() {
		if serveErr := grpcServer.Serve(lis); serveErr != nil {
			util.GetLogger(context.Background()).Error("gRPC server failed", slog.String("error", serveErr.Error()))
		}
	}()

	return grpcServer, nil
}

func (s *Server) SendCommand(ctx context.Context, req *routermanager.SendCommandRequest) (*routermanager.SendCommandResponse, error) {
	log := util.GetLogger(ctx)
	payloadMap := req.Payload.AsMap()

	if req.RouterSerial != "" {
		_, err := s.ManagerService.CreateCommand(ctx, req.RouterSerial, req.CommandType, payloadMap)
		if err != nil {
			log.Error("Ошибка SendCommand", slog.String("error", err.Error()))
			return nil, status.Error(codes.Internal, "не удалось сохранить команды")
		}
		return &routermanager.SendCommandResponse{Created: 1}, nil
	}

	commandAll, err := s.ManagerService.CreateCommandForAll(ctx, req.CommandType, payloadMap)
	if err != nil {
		log.Error("Ошибка SendCommand", slog.String("error", err.Error()))
		return nil, status.Error(codes.Internal, "не удалось сохранить команды")
	}
	return &routermanager.SendCommandResponse{Created: int32(len(commandAll))}, nil
}

func (s *Server) PollCommands(ctx context.Context, req *routermanager.PollCommandsRequest) (*routermanager.PollCommandsResponse, error) {
	log := util.GetLogger(ctx)
	commands, errPoll := s.ManagerService.GetPendingCommandsAndMarkItSent(ctx, req.RouterSerial)
	if errPoll != nil {
		log.Error("Ошибка PollCommands", slog.String("error", errPoll.Error()))
		return nil, status.Error(codes.Internal, "не удалось получить команды")
	}
	var pbCommands []*routermanager.Command
	for _, cmd := range commands {
		pbCommand, err := s.commandToProto(cmd)
		if err != nil {
			log.Error("Ошибка десерилизации", slog.String("error", err.Error()))
			return nil, status.Error(codes.Internal, "не удалось получить команды")
		}
		pbCommands = append(pbCommands, pbCommand)
	}

	return &routermanager.PollCommandsResponse{Commands: pbCommands}, nil
}

func (s *Server) AckCommand(ctx context.Context, req *routermanager.AckCommandRequest) (*routermanager.AckCommandResponse, error) {
	log := util.GetLogger(ctx)
	commandID, err := uuid.Parse(req.CommandId)
	if err != nil {
		log.Error("req.CommandId не UUID", slog.String("error", err.Error()))
		return nil, status.Error(codes.Internal, "command_id должен быть UUID")
	}

	errAck := s.ManagerService.AckCommand(ctx, req.RouterSerial, commandID)
	if err != nil {
		log.Error("Ошибка AckCommand", slog.String("error", errAck.Error()))
		return nil, status.Error(codes.Internal, "не удалось подтвердить команды")
	}
	return &routermanager.AckCommandResponse{Status: "ACKED"}, nil
}

func (s *Server) commandToProto(cmd domain.CommandOut) (*routermanager.Command, error) {
	payload, err := structpb.NewStruct(*cmd.Payload)
	if err != nil {
		return nil, err
	}

	var sentAt, ackedAt *timestamppb.Timestamp
	if cmd.SentAt != nil {
		sentAt = timestamppb.New(*cmd.SentAt)
	}
	if cmd.AckedAt != nil {
		ackedAt = timestamppb.New(*cmd.AckedAt)
	}

	return &routermanager.Command{
		Id:           cmd.ID.String(),
		RouterSerial: cmd.SerialNumber,
		CommandType:  cmd.CommandType,
		Payload:      payload,
		Status:       string(cmd.Status),
		CreatedAt:    timestamppb.New(cmd.CreatedAt),
		SentAt:       sentAt,
		AckedAt:      ackedAt,
	}, nil
}
