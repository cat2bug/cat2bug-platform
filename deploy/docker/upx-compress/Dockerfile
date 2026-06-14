# 最小镜像内执行 UPX（仅压缩工具；运行 Native 仍用 distroless/debian glibc）
FROM alpine:3.20
RUN apk add --no-cache upx
WORKDIR /work
ENTRYPOINT ["upx"]
