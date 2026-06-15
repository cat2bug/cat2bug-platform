# 最小镜像内执行 UPX（官方静态二进制，支持 amd64/arm64；Debian apt 无 arm64 upx-ucl）
ARG UPX_VERSION=4.2.4
FROM debian:bookworm-slim
ARG UPX_VERSION
ARG TARGETARCH
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates wget xz-utils \
    && rm -rf /var/lib/apt/lists/* \
    && case "$TARGETARCH" in \
         amd64) UPX_ARCH=amd64 ;; \
         arm64) UPX_ARCH=arm64 ;; \
         *) echo "unsupported TARGETARCH: $TARGETARCH" >&2; exit 1 ;; \
       esac \
    && wget -q "https://github.com/upx/upx/releases/download/v${UPX_VERSION}/upx-${UPX_VERSION}-${UPX_ARCH}_linux.tar.xz" -O /tmp/upx.tar.xz \
    && tar -xJf /tmp/upx.tar.xz -C /tmp \
    && mv "/tmp/upx-${UPX_VERSION}-${UPX_ARCH}_linux/upx" /usr/local/bin/upx \
    && chmod +x /usr/local/bin/upx \
    && rm -rf /tmp/upx.tar.xz "/tmp/upx-${UPX_VERSION}-${UPX_ARCH}_linux" \
    && upx --version | head -1
WORKDIR /work
ENTRYPOINT ["upx"]
