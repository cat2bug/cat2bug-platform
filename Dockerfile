# 基础镜像
FROM openjdk
# author
MAINTAINER yuzhantao
# 指定路径
WORKDIR /home
# 复制jar文件到路径
COPY ./cat2bug-platform-admin/target/cat2bug-admin.jar cat2bug-admin.jar
# 启动认证服务
ENTRYPOINT ["java","-jar","cat2bug-admin.jar"]