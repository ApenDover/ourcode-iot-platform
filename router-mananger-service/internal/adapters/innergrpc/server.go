package innergrpc

import (
	"context"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"
	"log"
	"net"
	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/core/domainService"
	"router-mananger-service/internal/core/service"
	"router-mananger-service/internal/domain"
	"router-mananger-service/internal/util"

	routermanager "router-mananger-service/internal/ports/genproto"
)

type Server struct {
	routermanager.UnimplementedRouterManagerServiceServer
	ManagerService *service.ManagerService
}

func NewServer(pool *pgxpool.Pool) *Server {
	repoCommand := db.NewPostgresCommandRepository(pool)
	repoRouter := db.NewPostgresRouterRepository(pool)
	commandService := domainService.NewCommandService(repoCommand)
	routerService := domainService.NewRouterService(repoRouter)
	managerService := service.NewManagerService(commandService, routerService)

	return &Server{
		ManagerService: managerService,
	}
}

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

func (s *Server) Stop() {
	s.Stop()
}

// SendCommand - адаптер для создания команды
func (s *Server) SendCommand(_ context.Context, req *routermanager.SendCommandRequest) (*routermanager.SendCommandResponse, error) {
	util.GetLogger().Info("Получил запрос SendCommand")
	payloadMap := req.Payload.AsMap()

	if req.RouterSerial != "" {
		_ = s.ManagerService.CreateCommand(req.RouterSerial, req.CommandType, payloadMap)
		return &routermanager.SendCommandResponse{Created: 1}, nil
	}

	commandAll := s.ManagerService.CreateCommandForAll(req.CommandType, payloadMap)
	return &routermanager.SendCommandResponse{Created: int32(len(commandAll))}, nil
}

func (s *Server) PollCommands(ctx context.Context, req *routermanager.PollCommandsRequest) (*routermanager.PollCommandsResponse, error) {
	util.GetLogger().Info("Получил запрос PollCommands")

	commands := s.ManagerService.GetPendingCommandsAndMarkItSent(req.RouterSerial)
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

	commandID, err := uuid.Parse(req.CommandId)
	if err != nil {
		return nil, err
	}

	s.ManagerService.AckCommand(req.RouterSerial, commandID)
	return &routermanager.AckCommandResponse{Status: "ACKED"}, nil
}

// commandToProto - преобразование доменной команды в protobuf
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
