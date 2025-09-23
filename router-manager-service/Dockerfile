FROM golang:1.25-alpine AS builder
WORKDIR /build
COPY go.mod go.sum ./
COPY cmd/ ./cmd/
COPY config/ ./config/
COPY internal/ ./internal/
COPY protobuf/ ./protobuf/
RUN go mod download && \
    CGO_ENABLED=0 GOOS=linux go build -o /build/router-manager ./cmd/app

FROM alpine:3.22
WORKDIR /app
COPY --from=builder /build/router-manager .
ENTRYPOINT ["./router-manager"]
