# 前端 Nginx Dockerfile
FROM node:10-alpine AS builder

WORKDIR /app

# 复制 package.json 先安装依赖
COPY package.json package-lock.json ./

# 安装依赖
RUN npm install --registry=https://registry.npm.taobao.org

# 复制源代码
COPY . .

# 构建前端
RUN npm run build

# 运行阶段 - 使用 Nginx 提供静态文件服务
FROM nginx:1.19-alpine

LABEL maintainer="vblog-team"
LABEL description="VBlog Frontend"

# 复制自定义 Nginx 配置
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制 dist 文件
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -q --spider http://localhost/ || exit 1
