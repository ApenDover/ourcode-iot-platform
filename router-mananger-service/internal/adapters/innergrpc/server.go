package innergrpc

import (
	"context"
	"log"
	"net"
	"router-mananger-service/config"
	"router-mananger-service/internal/core/service"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"

	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/core/domainService"
	"router-mananger-service/internal/domain"
	routermanager "router-mananger-service/internal/ports/genproto"
)

type Server struct {
	routermanager.UnimplementedRouterManagerServiceServer
	commandService *domainService.CommandService
	routerService  *domainService.RouterService
	managerService *service.ManagerService
}

func NewServer(pool *pgxpool.Pool) *Server {
	repoCommand := db.NewPostgresCommandRepository(pool)
	repoRouter := db.NewPostgresRouterRepository(pool)
	commandService := domainService.NewCommandService(repoCommand)
	routerService := domainService.NewRouterService(repoRouter)

	managerService := service.NewManagerService(commandService, routerService)

	go func() {
		ticker := time.NewTicker(config.LoadConfig().TimeExpired)
		defer ticker.Stop()
		for range ticker.C {
			managerService.MarkExpiredAsError()
		}
	}()

	return &Server{
		commandService: commandService,
		routerService:  routerService,
		managerService: managerService,
	}
}

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
	// ШЕЛУХА: Валидация и преобразование gRPC запроса
	payloadMap := req.Payload.AsMap()

	if req.RouterId == "" {
		routerID, err := uuid.Parse(req.RouterId)
		if err != nil {
			return nil, err
		}

		// ВЫЗОВ БИЗНЕС-ЛОГИКИ: Передаем команду в сервисный слой
		_ = s.commandService.CreateCommand(routerID, req.CommandType, payloadMap)
		if err != nil {
			return nil, err
		}
		return &routermanager.SendCommandResponse{
			Created: 1,
		}, nil
	}

	commandAll := s.managerService.CreateCommandForAll(req.CommandType, payloadMap)

	// ШЕЛУХА: Преобразуем результат в gRPC ответ
	return &routermanager.SendCommandResponse{
		Created: int32(len(commandAll)),
	}, nil
}

// PollCommands - адаптер для получения команд роутера
func (s *Server) PollCommands(ctx context.Context, req *routermanager.PollCommandsRequest) (*routermanager.PollCommandsResponse, error) {
	// ШЕЛУХА: Валидация и преобразование gRPC запроса
	routerID, err := uuid.Parse(req.RouterId)
	if err != nil {
		return nil, err
	}

	// ВЫЗОВ БИЗНЕС-ЛОГИКИ: Получаем команды из сервисного слоя
	commands := s.commandService.GetPendingCommands(routerID)
	if err != nil {
		return nil, err
	}

	// ШЕЛУХА: Преобразуем доменные команды в gRPC ответ
	var pbCommands []*routermanager.Command
	for _, cmd := range commands {
		pbCommand, err := s.commandToProto(cmd)
		if err != nil {
			return nil, err
		}
		pbCommands = append(pbCommands, pbCommand)
	}

	return &routermanager.PollCommandsResponse{
		Commands: pbCommands,
	}, nil
}

// AckCommand - адаптер для подтверждения команды
func (s *Server) AckCommand(ctx context.Context, req *routermanager.AckCommandRequest) (*routermanager.AckCommandResponse, error) {
	// ШЕЛУХА: Валидация и преобразование gRPC запроса
	routerID, err := uuid.Parse(req.RouterId)
	if err != nil {
		return nil, err
	}

	commandID, err := uuid.Parse(req.CommandId)
	if err != nil {
		return nil, err
	}

	// ВЫЗОВ БИЗНЕС-ЛОГИКИ: Подтверждаем команду через сервисный слой
	s.commandService.AckCommand(commandID, routerID)

	// ШЕЛУХА: Преобразуем результат в gRPC ответ
	return &routermanager.AckCommandResponse{
		Status: "ACKED",
	}, nil
}

// commandToProto - вспомогательный метод для преобразования доменной команды в protobuf
func (s *Server) commandToProto(cmd domain.Command) (*routermanager.Command, error) {
	// Преобразуем map в protobuf Struct
	payload, err := structpb.NewStruct(cmd.Payload)
	if err != nil {
		return nil, err
	}

	// Создаем gRPC сообщение из доменной модели
	return &routermanager.Command{
		Id:          cmd.ID.String(),
		RouterId:    cmd.RouterID.String(),
		CommandType: cmd.CommandType,
		Payload:     payload,
		Status:      string(cmd.Status),
		CreatedAt:   timestamppb.New(cmd.CreatedAt),
		SentAt:      timestamppb.New(*cmd.SentAt),
		AckedAt:     timestamppb.New(*cmd.AckedAt),
	}, nil

}
