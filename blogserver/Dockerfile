# 后端服务 Dockerfile
FROM openjdk:8-jdk-alpine AS builder

WORKDIR /app

# 复制 Maven wrapper 和 pom.xml 先下载依赖
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 给 mvnw 执行权限
RUN chmod +x mvnw

# 下载依赖（利用 Docker 缓存层）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src src

# 构建项目
RUN ./mvnw clean package -Pprod -DskipTests -B

# 运行阶段 - 使用更小的 JRE 镜像
FROM openjdk:8-jre-alpine

LABEL maintainer="vblog-team"
LABEL description="VBlog Blog Server"

# 创建非 root 用户
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/blogserver-*.jar app.jar

# 修改文件属主
RUN chown -R app:app /app

# 切换到非 root 用户
USER app

# 暴露端口
EXPOSE 8081

# JVM 参数优化
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8081/ || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
