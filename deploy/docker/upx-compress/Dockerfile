# 最小镜像内执行 UPX（debian 基础层，避免 alpine 镜像源不稳定）
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends upx-ucl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /work
ENTRYPOINT ["upx"]
