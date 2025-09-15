package innergrpc

import (
	"context"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"google.golang.org/grpc"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"
	"log/slog"
	"net"
	"router-manager-service/internal/adapters/db"
	"router-manager-service/internal/core/domainService"
	"router-manager-service/internal/core/service"
	"router-manager-service/internal/domain"
	"router-manager-service/internal/util"

	routermanager "router-manager-service/internal/ports/genproto"
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
	log := util.GetLogger()
	lis, err := net.Listen("tcp", ":"+port)
	if err != nil {
		return err
	}

	loggingInterceptor := func(
		ctx context.Context,
		req interface{},
		info *grpc.UnaryServerInfo,
		handler grpc.UnaryHandler,
	) (interface{}, error) {
		log.Info("Incoming gRPC request",
			slog.String("method", info.FullMethod),
			slog.String("request", fmt.Sprintf("%+v", req)),
		)

		resp, err := handler(ctx, req)

		log.Info("Outgoing gRPC response",
			slog.String("method", info.FullMethod),
			slog.String("response", fmt.Sprintf("%+v", resp)),
			slog.String("error", fmt.Sprintf("%v", err)),
		)
		return resp, err
	}

	grpcServer := grpc.NewServer(
		grpc.UnaryInterceptor(loggingInterceptor),
	)

	routermanager.RegisterRouterManagerServiceServer(grpcServer, s)
	return grpcServer.Serve(lis)
}

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

func (s *Server) AckCommand(ctx context.Context, req *routermanager.AckCommandRequest) (*routermanager.AckCommandResponse, error) {

	commandID, err := uuid.Parse(req.CommandId)
	if err != nil {
		return nil, err
	}

	s.ManagerService.AckCommand(req.RouterSerial, commandID)
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
