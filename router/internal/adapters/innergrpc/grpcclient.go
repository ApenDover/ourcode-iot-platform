package innergrpc

import (
	"context"
	"fmt"
	"log/slog"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	"router-manager-service/internal/conf/util"
	genproto "router-manager-service/internal/ports/genproto"
)

type Client struct {
	conn    *grpc.ClientConn
	client  genproto.RouterManagerServiceClient
	address string
}

func New(ctx context.Context, address string) (*Client, error) {
	conn, err := grpc.DialContext(ctx, address,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
		grpc.WithTimeout(10*time.Second),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to gRPC server: %w", err)
	}

	return &Client{
		conn:    conn,
		client:  genproto.NewRouterManagerServiceClient(conn),
		address: address,
	}, nil
}

func (c *Client) PollCommands(ctx context.Context, routerSerial string) (*genproto.PollCommandsResponse, error) {
	log := util.GetLogger(ctx)

	req := &genproto.PollCommandsRequest{
		RouterSerial: routerSerial,
	}

	resp, err := c.client.PollCommands(ctx, req)
	if err != nil {
		log.Error("Failed to poll commands",
			slog.String("router_serial", routerSerial),
			slog.String("error", err.Error()),
		)
		return nil, fmt.Errorf("poll commands failed: %w", err)
	}

	log.Debug("Successfully polled commands",
		slog.String("router_serial", routerSerial),
		slog.Int("commands_count", len(resp.Commands)),
	)

	return resp, nil
}

func (c *Client) AckCommand(ctx context.Context, routerSerial, commandID string) (*genproto.AckCommandResponse, error) {
	log := util.GetLogger(ctx)

	req := &genproto.AckCommandRequest{
		RouterSerial: routerSerial,
		CommandId:    commandID,
	}

	resp, err := c.client.AckCommand(ctx, req)
	if err != nil {
		log.Error("Failed to ack command",
			slog.String("router_serial", routerSerial),
			slog.String("command_id", commandID),
			slog.String("error", err.Error()),
		)
		return nil, fmt.Errorf("ack command failed: %w", err)
	}

	log.Debug("Successfully acknowledged command",
		slog.String("router_serial", routerSerial),
		slog.String("command_id", commandID),
		slog.String("status", resp.Status),
	)

	return resp, nil
}

func (c *Client) Close() error {
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}
