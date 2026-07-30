pipeline {
    agent any

    environment {
        // 项目配置
        PROJECT_NAME    = 'online-ordering'
        // 镜像仓库，本地测试设为空或不推送；正式环境改为实际地址
        IMAGE_REGISTRY  = 'registry.example.com'
        IMAGE_PREFIX    = "${IMAGE_REGISTRY}/${PROJECT_NAME}"

        // 镜像 Tag：使用构建号 + Git 短哈希
        IMAGE_TAG       = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"

        // 部署目录（本地测试时改为工作空间下的相对路径）
        DEPLOY_DIR      = "${WORKSPACE}/deploy-target"

        // Maven 配置（按实际环境修改）
        MAVEN_HOME      = tool name: 'Maven', type: 'maven'
        JAVA_HOME       = tool name: 'JDK17', type: 'jdk'

        // Node.js 配置
        NODEJS_HOME     = tool name: 'NodeJS18', type: 'nodejs'
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging', 'prod'], description: '部署环境')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false, description: '是否推送镜像到仓库（本地测试不需要）')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: '是否跳过后端单元测试')
        booleanParam(name: 'DB_BACKUP', defaultValue: true, description: '部署前是否备份 SQLite 数据库')
    }

    options {
        // 保留最近 10 次构建
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // 构建超时 30 分钟
        timeout(time: 30, unit: 'MINUTES')
        // 不允许并发构建
        disableConcurrentBuilds()
        // 时间戳
        timestamps()
    }

    stages {

        // ==================== 代码检出 ====================
        stage('Checkout') {
            steps {
                checkout scm
                echo "Git 分支: ${env.GIT_BRANCH}"
                echo "Git 提交: ${env.GIT_COMMIT}"
                echo "镜像 Tag: ${env.IMAGE_TAG}"
            }
        }

        // ==================== 后端构建 ====================
        stage('Backend Build') {
            steps {
                dir('backend') {
                    sh """
                        export JAVA_HOME="${JAVA_HOME}"
                        export PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:\$PATH"

                        echo "===== Java 版本 ====="
                        java -version

                        echo "===== Maven 构建 ====="
                        mvn clean package ${params.SKIP_TESTS ? '-DskipTests' : ''} \
                            -Dmaven.test.failure.ignore=false \
                            -B -V
                    """
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
                failure {
                    error '后端构建失败，请检查 Maven 输出'
                }
            }
        }

        // ==================== 前端构建 ====================
        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    sh """
                        export PATH="${NODEJS_HOME}/bin:\$PATH"

                        echo "===== Node 版本 ====="
                        node -v
                        npm -v

                        echo "===== 安装依赖 ====="
                        npm ci --registry=https://registry.npmmirror.com

                        echo "===== 构建前端 ====="
                        npm run build
                    """
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'frontend/dist/**', fingerprint: true, allowEmptyArchive: true
                }
                failure {
                    error '前端构建失败，请检查 npm 输出'
                }
            }
        }

        // ==================== 构建镜像 ====================
        stage('Build Docker Images') {
            parallel {
                stage('Backend Image') {
                    steps {
                        sh """
                            echo "===== 构建后端 Docker 镜像 ====="
                            docker build \
                                -t ${IMAGE_PREFIX}/backend:${IMAGE_TAG} \
                                -t ${IMAGE_PREFIX}/backend:latest \
                                -f backend/Dockerfile \
                                backend/
                        """
                    }
                }
                stage('Frontend Image') {
                    steps {
                        sh """
                            echo "===== 构建前端 Docker 镜像 ====="
                            docker build \
                                -t ${IMAGE_PREFIX}/frontend:${IMAGE_TAG} \
                                -t ${IMAGE_PREFIX}/frontend:latest \
                                -f frontend/Dockerfile \
                                frontend/
                        """
                    }
                }
            }
        }

        // ==================== 推送镜像 ====================
        stage('Push Images') {
            when {
                expression { params.PUSH_IMAGE }
            }
            steps {
                echo "===== 推送镜像到仓库 ====="
                sh """
                    docker push ${IMAGE_PREFIX}/backend:${IMAGE_TAG}
                    docker push ${IMAGE_PREFIX}/backend:latest
                    docker push ${IMAGE_PREFIX}/frontend:${IMAGE_TAG}
                    docker push ${IMAGE_PREFIX}/frontend:latest
                """
            }
        }

        // ==================== 部署前备份 ====================
        stage('Backup Database') {
            when {
                expression { params.DB_BACKUP && params.DEPLOY_ENV == 'prod' }
            }
            steps {
                sh """
                    echo "===== 备份 SQLite 数据库 ====="
                    BACKUP_FILE="ordering_backup_\$(date +%Y%m%d_%H%M%S).db"

                    # 如果部署目录存在数据库文件，则备份
                    if [ -f "${DEPLOY_DIR}/data/ordering.db" ]; then
                        cp ${DEPLOY_DIR}/data/ordering.db ${DEPLOY_DIR}/data/\${BACKUP_FILE}
                        echo "数据库已备份: \${BACKUP_FILE}"

                        # 保留最近 5 个备份
                        ls -t ${DEPLOY_DIR}/data/ordering_backup_*.db | tail -n +6 | xargs -r rm --
                    else
                        echo "未找到数据库文件，跳过备份"
                    fi
                """
            }
        }

        // ==================== 部署 ====================
        stage('Deploy') {
            steps {
                sh """
                    echo "===== 部署到 ${params.DEPLOY_ENV} 环境 ====="

                    # 生成 docker-compose 部署文件
                    mkdir -p ${DEPLOY_DIR}
                    cat > ${DEPLOY_DIR}/docker-compose.deploy.yml << 'EOF'
version: '3.8'
services:
  backend:
    image: ${IMAGE_PREFIX}/backend:${IMAGE_TAG}
    ports:
      - "8080:8080"
    volumes:
      - backend-data:/app/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "wget --spider -S http://localhost:8080/ 2>&1 | grep -q 'HTTP/'"]
      interval: 10s
      timeout: 5s
      retries: 5

  frontend:
    image: ${IMAGE_PREFIX}/frontend:${IMAGE_TAG}
    ports:
      - "80:80"
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped

volumes:
  backend-data:
EOF

                    # 停止旧服务
                    docker compose -f ${DEPLOY_DIR}/docker-compose.deploy.yml down || true

                    # 启动新服务
                    docker compose -f ${DEPLOY_DIR}/docker-compose.deploy.yml up -d

                    echo "===== 等待服务启动 ====="
                    sleep 10
                """
            }
        }

        // ==================== 健康检查 ====================
        stage('Health Check') {
            steps {
                script {
                    def maxRetries = 6
                    def waitSeconds = 10
                    def backendOk = false
                    def frontendOk = false

                    for (int i = 1; i <= maxRetries; i++) {
                        echo "健康检查第 ${i}/${maxRetries} 次..."

                        // 检查后端
                        def backendStatus = sh(
                            script: 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/doc.html || echo "000"',
                            returnStdout: true
                        ).trim()
                        if (backendStatus == '200' || backendStatus == '401') {
                            backendOk = true
                            echo "后端健康检查通过 (HTTP ${backendStatus})"
                        }

                        // 检查前端
                        def frontendStatus = sh(
                            script: 'curl -s -o /dev/null -w "%{http_code}" http://localhost:80/ || echo "000"',
                            returnStdout: true
                        ).trim()
                        if (frontendStatus == '200') {
                            frontendOk = true
                            echo "前端健康检查通过 (HTTP ${frontendStatus})"
                        }

                        if (backendOk && frontendOk) {
                            break
                        }

                        if (i < maxRetries) {
                            echo "等待 ${waitSeconds} 秒后重试..."
                            sleep waitSeconds
                        }
                    }

                    if (!backendOk) {
                        error '后端健康检查失败！请检查日志: docker logs online-ordering-backend-1'
                    }
                    if (!frontendOk) {
                        error '前端健康检查失败！请检查日志: docker logs online-ordering-frontend-1'
                    }
                }
            }
        }
    }

    // ==================== 后处理 ====================
    post {
        success {
            echo """
            ========================================
            部署成功！
            ========================================
            环境:     ${params.DEPLOY_ENV}
            镜像Tag:  ${IMAGE_TAG}
            前端地址:  http://localhost:80
            后端地址:  http://localhost:8080
            API文档:   http://localhost:8080/doc.html
            ========================================
            """
        }
        failure {
            echo """
            ========================================
            部署失败！
            ========================================
            排查步骤:
            1. 查看上方构建日志定位错误
            2. 检查容器状态: docker ps -a
            3. 查看后端日志: docker logs <backend-container>
            4. 查看前端日志: docker logs <frontend-container>
            ========================================
            """
            // 部署失败时，如果部署了新版本，尝试回滚
            script {
                if (params.DEPLOY_ENV == 'prod') {
                    echo '生产环境部署失败，请手动检查并回滚'
                }
            }
        }
        always {
            // 清理工作空间中的构建产物
            dir('backend') {
                sh 'mvn clean -q || true'
            }
            // 清理悬空镜像
            sh 'docker image prune -f -q || true'
        }
    }
}
