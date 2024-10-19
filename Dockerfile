####################################################################################################
## Builder
####################################################################################################
FROM rust:alpine AS builder

RUN apk add --no-cache musl-dev

WORKDIR /robot-insprill

COPY . .

# Set environment variables so the build has git info
RUN cargo build --release

####################################################################################################
## Final image
####################################################################################################
FROM alpine:latest

WORKDIR /opt/robot-insprill
COPY --from=builder /robot-insprill/target/release/robot-insprill .

RUN adduser --home /nonexistent --no-create-home --disabled-password robot-insprill
USER robot-insprill

CMD ["./robot-insprill"]
