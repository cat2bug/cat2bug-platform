# Spring Native / UPX 最小 glibc 运行时
# 二进制依赖: ld-linux, libc.so.6, libz.so.1（GraalVM Native 默认动态链接 zlib）
ARG BASE_IMAGE=gcr.io/distroless/cc-debian12:latest
ARG GNU_ARCH=aarch64

FROM debian:bookworm-slim AS libs
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates zlib1g \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /export /app/logs /app/data /app/upload /app/config/install /tmp \
    && cp -a /lib/*-linux-gnu/libz.so.1* /export/ \
    && chmod 1777 /tmp \
    && chown -R 65532:65532 /app

FROM ${BASE_IMAGE}
ARG GNU_ARCH
COPY --from=libs /export/ /lib/${GNU_ARCH}-linux-gnu/
COPY --from=libs /app /app
COPY cat2bug-admin /cat2bug-admin
WORKDIR /app
EXPOSE 2020
USER 65532:65532
ENTRYPOINT ["/cat2bug-admin"]
