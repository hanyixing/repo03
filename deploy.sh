#!/bin/bash
# =============================================================================
# VBlog 统一部署脚本
# 用法: ./deploy.sh [命令]
# 命令:
#   build       - 构建前后端项目
#   docker-up   - 启动 Docker 容器（全部服务）
#   docker-down - 停止 Docker 容器
#   docker-log  - 查看 Docker 容器日志
#   backend     - 仅构建后端
#   frontend    - 仅构建前端
#   clean       - 清理构建产物
#   status      - 查看服务状态
# =============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/blogserver"
FRONTEND_DIR="${PROJECT_ROOT}/vueblog"

# 日志函数
log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC}  $1"; }

# 检查依赖
check_dependencies() {
    log_step "检查项目依赖..."

    # 检查 Java
    if ! command -v java &> /dev/null; then
        log_error "Java 未安装，请安装 JDK 8"
        exit 1
    fi
    log_info "Java: $(java -version 2>&1 | head -1)"

    # 检查 Maven（使用 wrapper）
    if [ ! -f "${BACKEND_DIR}/mvnw" ]; then
        log_error "Maven Wrapper 未找到: ${BACKEND_DIR}/mvnw"
        exit 1
    fi
    log_info "Maven Wrapper: 已就绪"

    # 检查 Node.js
    if ! command -v node &> /dev/null; then
        log_error "Node.js 未安装，请安装 Node.js 10+"
        exit 1
    fi
    log_info "Node.js: $(node -v)"

    # 检查 npm
    if ! command -v npm &> /dev/null; then
        log_error "npm 未安装"
        exit 1
    fi
    log_info "npm: $(npm -v)"

    # 检查 Docker（如果执行 docker 相关命令）
    if [ "$1" = "docker" ]; then
        if ! command -v docker &> /dev/null; then
            log_error "Docker 未安装"
            exit 1
        fi
        log_info "Docker: $(docker --version)"

        if ! command -v docker-compose &> /dev/null; then
            log_error "docker-compose 未安装"
            exit 1
        fi
        log_info "docker-compose: $(docker-compose --version)"
    fi

    log_info "依赖检查通过"
}

# 构建后端
build_backend() {
    log_step "构建后端服务..."
    cd "${BACKEND_DIR}"

    chmod +x mvnw
    ./mvnw clean package -Pprod -DskipTests -B

    if [ $? -eq 0 ]; then
        log_info "后端构建成功"
        ls -lh target/blogserver-*.jar
    else
        log_error "后端构建失败"
        exit 1
    fi
}

# 构建前端
build_frontend() {
    log_step "构建前端项目..."
    cd "${FRONTEND_DIR}"

    # 安装依赖
    if [ ! -d "node_modules" ]; then
        log_step "安装前端依赖..."
        npm install --registry=https://registry.npm.taobao.org
    fi

    # 构建
    npm run build

    if [ $? -eq 0 ]; then
        log_info "前端构建成功"
        ls -lh dist/
    else
        log_error "前端构建失败"
        exit 1
    fi
}

# 构建前后端
build_all() {
    log_step "开始构建前后端项目..."
    check_dependencies

    build_backend
    build_frontend

    log_info "===== 构建完成 ====="
}

# Docker 启动
docker_up() {
    log_step "启动 Docker 容器..."
    check_dependencies "docker"
    cd "${PROJECT_ROOT}"

    # 检查 .env 文件
    if [ ! -f ".env" ]; then
        log_warn ".env 文件不存在，使用 .env.example 创建..."
        if [ -f ".env.example" ]; then
            cp .env.example .env
            log_warn "请编辑 .env 文件设置正确的数据库密码等敏感信息"
        fi
    fi

    docker-compose up -d --build

    log_info "===== Docker 服务启动完成 ====="
    log_info "前端地址: http://localhost:80"
    log_info "后端地址: http://localhost:8081"
    log_info "数据库端口: 3306"
    echo ""
    log_step "查看日志: ./deploy.sh docker-log"
    log_step "停止服务: ./deploy.sh docker-down"
}

# Docker 停止
docker_down() {
    log_step "停止 Docker 容器..."
    cd "${PROJECT_ROOT}"
    docker-compose down
    log_info "Docker 服务已停止"
}

# Docker 日志
docker_log() {
    cd "${PROJECT_ROOT}"
    docker-compose logs -f --tail=100
}

# 服务状态
service_status() {
    log_step "检查服务状态..."
    cd "${PROJECT_ROOT}"

    echo ""
    log_info "=== Docker 容器状态 ==="
    docker-compose ps 2>/dev/null || log_warn "Docker 容器未运行"

    echo ""
    log_info "=== 后端服务 ==="
    if [ -f "${BACKEND_DIR}/target/blogserver-0.0.1-SNAPSHOT.jar" ]; then
        log_info "JAR 文件存在: $(ls -lh ${BACKEND_DIR}/target/blogserver-*.jar 2>/dev/null)"
    else
        log_warn "后端 JAR 文件未找到，请先执行构建"
    fi

    echo ""
    log_info "=== 前端构建 ==="
    if [ -d "${FRONTEND_DIR}/dist" ]; then
        log_info "dist 目录存在"
        ls "${FRONTEND_DIR}/dist/" 2>/dev/null
    else
        log_warn "前端 dist 目录未找到，请先执行构建"
    fi
}

# 清理构建产物
clean_all() {
    log_step "清理构建产物..."

    # 清理后端
    cd "${BACKEND_DIR}"
    ./mvnw clean -B
    log_info "后端清理完成"

    # 清理前端
    cd "${FRONTEND_DIR}"
    rm -rf dist/
    log_info "前端清理完成"

    log_info "===== 清理完成 ====="
}

# 显示帮助
show_help() {
    echo "VBlog 统一部署脚本"
    echo ""
    echo "用法: ./deploy.sh [命令]"
    echo ""
    echo "可用命令:"
    echo "  build       构建前后端项目（默认）"
    echo "  docker-up   启动 Docker 容器（全部服务）"
    echo "  docker-down 停止 Docker 容器"
    echo "  docker-log  查看 Docker 容器日志"
    echo "  backend     仅构建后端"
    echo "  frontend    仅构建前端"
    echo "  clean       清理构建产物"
    echo "  status      查看服务状态"
    echo "  help        显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  ./deploy.sh build        # 构建前后端"
    echo "  ./deploy.sh docker-up    # Docker 启动全部服务"
    echo "  ./deploy.sh backend      # 仅构建后端"
}

# 主入口
case "${1:-build}" in
    build)
        build_all
        ;;
    docker-up)
        docker_up
        ;;
    docker-down)
        docker_down
        ;;
    docker-log)
        docker_log
        ;;
    backend)
        check_dependencies
        build_backend
        ;;
    frontend)
        check_dependencies
        build_frontend
        ;;
    clean)
        clean_all
        ;;
    status)
        service_status
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "未知命令: $1"
        show_help
        exit 1
        ;;
esac
