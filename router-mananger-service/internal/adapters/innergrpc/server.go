package innergrpc

import (
	"context"
	"log"
	"net"
	"router-mananger-service/config"
	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/core/domainService"
	"router-mananger-service/internal/core/service"
	"router-mananger-service/internal/domain"
	"router-mananger-service/internal/util"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"

	routermanager "router-mananger-service/internal/ports/genproto"
)

type Server struct {
	routermanager.UnimplementedRouterManagerServiceServer
	CommandService *domainService.CommandService
	RouterService  *domainService.RouterService
	ManagerService *service.ManagerService
}

func NewServer(pool *pgxpool.Pool) *Server {
	repoCommand := db.NewPostgresCommandRepository(pool)
	repoRouter := db.NewPostgresRouterRepository(pool)
	commandService := domainService.NewCommandService(repoCommand)
	routerService := domainService.NewRouterService(repoRouter)
	managerService := service.NewManagerService(commandService, routerService)

	// Запускаем периодическое обновление статусов SENT -> ERROR
	go func() {
		ticker := time.NewTicker(config.LoadConfig().TimeExpired)
		defer ticker.Stop()
		for range ticker.C {
			managerService.MarkExpiredAsError()
		}
	}()

	return &Server{
		CommandService: commandService,
		RouterService:  routerService,
		ManagerService: managerService,
	}
}

// mustEmbedUnimplementedRouterManagerServiceServer реализует требование интерфейса
func (s *Server) mustEmbedUnimplementedRouterManagerServiceServer() {}

func (s *Server) Start(port string) error {
	lis, err := net.Listen("tcp", ":"+port)
	if err != nil {
		return err
	}

	grpcServer := grpc.NewServer()
	routermanager.RegisterRouterManagerServiceServer(grpcServer, s)

	log.Printf("gRPC server starting on port %s", port)
	return grpcServer.Serve(lis)
}

// SendCommand - адаптер для создания команды
func (s *Server) SendCommand(_ context.Context, req *routermanager.SendCommandRequest) (*routermanager.SendCommandResponse, error) {
	util.GetLogger().Info("Получил запрос SendCommand")
	payloadMap := req.Payload.AsMap()

	if req.RouterId != "" {
		routerID, err := uuid.Parse(req.RouterId)
		if err != nil {
			return nil, err
		}
		_ = s.CommandService.CreateCommand(routerID, req.CommandType, payloadMap)
		return &routermanager.SendCommandResponse{Created: 1}, nil
	}

	commandAll := s.ManagerService.CreateCommandForAll(req.CommandType, payloadMap)
	return &routermanager.SendCommandResponse{Created: int32(len(commandAll))}, nil
}

// PollCommands - адаптер для получения команд роутера
func (s *Server) PollCommands(ctx context.Context, req *routermanager.PollCommandsRequest) (*routermanager.PollCommandsResponse, error) {
	util.GetLogger().Info("Получил запрос PollCommands")
	routerID, err := uuid.Parse(req.RouterId)
	if err != nil {
		return nil, err
	}

	commands := s.CommandService.GetPendingCommands(routerID)
	var pbCommands []*routermanager.Command
	for _, cmd := range commands {
		pbCommand, err := s.commandToProto(cmd)
		if err != nil {
			return nil, err
		}
		pbCommands = append(pbCommands, pbCommand)
	}

	return &routermanager.PollCommandsResponse{Commands: pbCommands}, nil
}

// AckCommand - адаптер для подтверждения команды
func (s *Server) AckCommand(ctx context.Context, req *routermanager.AckCommandRequest) (*routermanager.AckCommandResponse, error) {
	routerID, err := uuid.Parse(req.RouterId)
	if err != nil {
		return nil, err
	}

	commandID, err := uuid.Parse(req.CommandId)
	if err != nil {
		return nil, err
	}

	s.CommandService.AckCommand(commandID, routerID)
	return &routermanager.AckCommandResponse{Status: "ACKED"}, nil
}

// commandToProto - преобразование доменной команды в protobuf
func (s *Server) commandToProto(cmd domain.Command) (*routermanager.Command, error) {
	payload, err := structpb.NewStruct(cmd.Payload)
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
		Id:          cmd.ID.String(),
		RouterId:    cmd.RouterID.String(),
		CommandType: cmd.CommandType,
		Payload:     payload,
		Status:      string(cmd.Status),
		CreatedAt:   timestamppb.New(cmd.CreatedAt),
		SentAt:      sentAt,
		AckedAt:     ackedAt,
	}, nil
}
