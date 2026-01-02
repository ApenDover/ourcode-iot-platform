package innergrpc

import (
	"context"
	"fmt"
	"log/slog"
	"router-manager-service/internal/adapters/cache"
	"router-manager-service/internal/adapters/db"
	"router-manager-service/internal/conf/util"
	"router-manager-service/internal/core/domain"
	"router-manager-service/internal/core/service"
	genproto "router-manager-service/internal/ports/genproto"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"
)

type Server struct {
	genproto.UnimplementedRouterManagerServiceServer
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

func (s *Server) SendCommand(ctx context.Context, req *genproto.SendCommandRequest) (*genproto.SendCommandResponse, error) {
	log := util.GetLogger(ctx)

	payloadMap := req.Payload.AsMap()
	var created int

	if req.RouterSerial != "" {
		_, err := s.ManagerService.CreateCommand(ctx, req.RouterSerial, req.CommandType, payloadMap)
		if err != nil {
			log.Error("Failed to create command", slog.String("error", err.Error()))
			return nil, status.Error(codes.Internal, "failed to create command")
		}
		created = 1
	} else {
		commands, err := s.ManagerService.CreateCommandForAll(ctx, req.CommandType, payloadMap)
		if err != nil {
			log.Error("Failed to create commands for all routers", slog.String("error", err.Error()))
			return nil, status.Error(codes.Internal, "failed to create commands")
		}
		created = len(commands)
	}

	log.Info("Commands created successfully", slog.Int("count", created))
	return &genproto.SendCommandResponse{Created: int32(created)}, nil
}

func (s *Server) PollCommands(
	ctx context.Context,
	req *genproto.PollCommandsRequest) (*genproto.PollCommandsResponse, error) {
	log := util.GetLogger(ctx)

	commands, err := s.ManagerService.GetPendingCommandsAndMarkItSent(ctx, req.RouterSerial)
	if err != nil {
		log.Error("Failed to poll commands", slog.String("error", err.Error()))
		return nil, status.Error(codes.Internal, "failed to get commands")
	}

	pbCommands := make([]*genproto.Command, 0, len(commands))
	for _, cmd := range commands {
		pbCommand, err := s.commandToProto(cmd)
		if err != nil {
			log.Error("Failed to convert command to genproto", slog.String("error", err.Error()))
			continue
		}
		pbCommands = append(pbCommands, pbCommand)
	}

	log.Info("Polled commands", slog.Int("count", len(pbCommands)))
	return &genproto.PollCommandsResponse{Commands: pbCommands}, nil
}

func (s *Server) AckCommand(ctx context.Context, req *genproto.AckCommandRequest) (*genproto.AckCommandResponse, error) {
	log := util.GetLogger(ctx)

	commandID, err := uuid.Parse(req.CommandId)
	if err != nil {
		log.Error("Invalid command ID", slog.String("command_id", req.CommandId))
		return nil, status.Error(codes.InvalidArgument, "invalid command ID")
	}

	if err := s.ManagerService.AckCommand(ctx, req.RouterSerial, commandID); err != nil {
		log.Error("Failed to acknowledge command",
			slog.String("command_id", req.CommandId),
			slog.String("error", err.Error()),
		)
		return nil, status.Error(codes.Internal, "failed to acknowledge command")
	}

	log.Info("Command acknowledged", slog.String("command_id", req.CommandId))
	return &genproto.AckCommandResponse{Status: "ACKED"}, nil
}

func (s *Server) commandToProto(cmd domain.CommandOut) (*genproto.Command, error) {
	payload, err := structpb.NewStruct(cmd.Payload)
	if err != nil {
		return nil, fmt.Errorf("failed to create struct: %w", err)
	}

	var sentAt, ackedAt *timestamppb.Timestamp
	if cmd.SentAt != nil {
		sentAt = timestamppb.New(*cmd.SentAt)
	}
	if cmd.AckedAt != nil {
		ackedAt = timestamppb.New(*cmd.AckedAt)
	}

	return &genproto.Command{
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
