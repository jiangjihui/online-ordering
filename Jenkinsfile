pipeline {
    agent any

    environment {
        // 项目配置
        PROJECT_NAME    = 'online-ordering'
        // 镜像仓库：本地 dev 用 localhost:5000（宿主机本地 registry:2），
        // Jenkins 推送后，kind 节点通过 containerd mirror 从宿主机拉取。
        // 正式环境改为实际地址如 ghcr.io/myorg。
        IMAGE_REGISTRY  = 'localhost:5000'
        // environment 块的值必须是引号字符串或函数调用，三元表达式需整体包在双引号里
        IMAGE_PREFIX    = "${IMAGE_REGISTRY ? IMAGE_REGISTRY + '/' + PROJECT_NAME : PROJECT_NAME}"

        // 镜像 Tag：使用构建号 + Git 短哈希
        IMAGE_TAG       = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"

        // 部署目录（本地测试时改为工作空间下的相对路径）
        DEPLOY_DIR      = "${WORKSPACE}/deploy-target"

        // Maven 配置（按实际环境修改）
        MAVEN_HOME      = tool name: 'Maven', type: 'maven'
        // 不再要求 JDK17 工具：容器自带 Temurin JDK 21，已能编译 java.version=17 项目。
        // 编译出的字节码版本 61 (Java 17)，可直接运行在任何 JDK 17+ 的运行时上。
        // JAVA_HOME 使用容器自带的环境变量（/opt/java/openjdk）

        // Node.js 配置
        // Vite 8 / rolldown 要求 Node >=20.19；容器内已安装 Node 22 LTS 到 /opt/node22（见 Frontend Build stage）
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
                script {
                    // 使用容器自带的 JDK（Temurin 21 at $JAVA_HOME），编译目标为 Java 17 字节码
                    def jdkHome = env.JAVA_HOME ?: '/opt/java/openjdk'
                    def mvnHome = tool name: 'Maven', type: 'maven'
                    dir('backend') {
                        sh """
                            export JAVA_HOME="${jdkHome}"
                            export PATH="${jdkHome}/bin:${mvnHome}/bin:\$PATH"

                            echo "===== Java 版本 ====="
                            java -version

                            echo "===== Maven 构建 ====="
                            mvn clean package ${params.SKIP_TESTS ? '-DskipTests' : ''} \
                                -Dmaven.test.failure.ignore=false \
                                -B -V
                        """
                    }
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
                script {
                    // Vite 8 要求 Node >=20.19；NodeJS18 工具太旧，改用容器内安装的 Node 22 LTS（/opt/node22）
                    def nodeHome = '/opt/node22'
                    dir('frontend') {
                        sh """
                            export PATH="${nodeHome}/bin:\$PATH"

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

        // ==================== 部署前备份 ====================
        stage('Backup Database') {
            when {
                expression { params.DB_BACKUP && params.DEPLOY_ENV == 'prod' }
            }
            steps {
                sh """
                    echo "===== 备份 SQLite 数据库（K8s 环境）====="
                    echo "生产环境请用以下命令从集群备份："
                    echo "  kubectl exec -n online-ordering deploy/backend -- sh -c 'cat /app/data/ordering.db' > ordering_backup_\$(date +%Y%m%d_%H%M%S).db"
                """
            }
        }

        // ==================== 部署（K8s / kind 集群） ====================
        stage('Deploy') {
            steps {
                sh """
                    echo "===== 部署到 ${params.DEPLOY_ENV} 环境 (kind K8s) ====="

                    # 1. 推送镜像到本地 registry（kind 节点通过 containerd mirror 从宿主机拉取）
                    echo "----- 推送镜像到 ${IMAGE_PREFIX} -----"
                    docker push ${IMAGE_PREFIX}/backend:${IMAGE_TAG}
                    docker push ${IMAGE_PREFIX}/frontend:${IMAGE_TAG}
                    docker push ${IMAGE_PREFIX}/backend:latest
                    docker push ${IMAGE_PREFIX}/frontend:latest

                    # 2. 应用 K8s 清单（幂等；首次部署会创建 namespace / PVC / Ingress）
                    echo "----- apply K8s 清单 -----"
                    kubectl apply -f deploy/kubernetes/app-namespace.yaml
                    kubectl apply -f deploy/kubernetes/app-backend.yaml
                    kubectl apply -f deploy/kubernetes/app-frontend.yaml
                    kubectl apply -f deploy/kubernetes/app-ingress.yaml

                    # 3. 更新镜像 tag（滚动更新，build 号保证每次都是新镜像）
                    echo "----- 滚动更新镜像 -----"
                    kubectl set image deployment/backend backend=${IMAGE_PREFIX}/backend:${IMAGE_TAG} -n online-ordering
                    kubectl set image deployment/frontend frontend=${IMAGE_PREFIX}/frontend:${IMAGE_TAG} -n online-ordering

                    # 4. 等待滚动完成
                    echo "----- 等待 Deployment 就绪 -----"
                    kubectl rollout status deployment/backend -n online-ordering --timeout=180s
                    kubectl rollout status deployment/frontend -n online-ordering --timeout=120s

                    echo "===== 部署完成 ====="
                    kubectl get pods -n online-ordering
                """
            }
        }

        // ==================== 健康检查（K8s） ====================
        stage('Health Check') {
            steps {
                script {
                    def maxRetries = 6
                    def waitSeconds = 10
                    def podsOk = false
                    def ingressOk = false

                    for (int i = 1; i <= maxRetries; i++) {
                        echo "健康检查第 ${i}/${maxRetries} 次..."

                        // 检查 Pod 全部 Running/Ready（主检查，走 kubectl 不依赖网络路径）
                        def podCheck = sh(
                            script: 'kubectl get pods -n online-ordering --no-headers 2>&1 | grep -vE "Running|Completed" | wc -l',
                            returnStdout: true
                        ).trim()
                        if (podCheck == '0') {
                            podsOk = true
                            echo "Pod 检查通过（全部 Running）"
                        } else {
                            echo "尚有非 Running Pod 数量: ${podCheck}"
                        }

                        // 检查 Ingress 入口（用 Docker 网关地址访问宿主机 80，避免 host.docker.internal IPv6 问题）
                        def ingressCheck = sh(
                            script: '''
                                GW=$(ip route 2>/dev/null | awk '/^default/{print $3; exit}')
                                [ -z "$GW" ] && GW="172.21.0.1"
                                FRONT=$(curl -s -o /dev/null -w "%{http_code}" --max-time 6 "http://$GW:80/" || echo "000")
                                BACK=$(curl -s -o /dev/null -w "%{http_code}" --max-time 6 "http://$GW:80/doc.html" || echo "000")
                                echo "$FRONT-$BACK"
                            ''',
                            returnStdout: true
                        ).trim()
                        if (ingressCheck == '200-200' || ingressCheck == '200-401') {
                            ingressOk = true
                            echo "Ingress 检查通过 (frontend/backend: ${ingressCheck})"
                        } else {
                            echo "Ingress 检查未通过 (frontend-backend: ${ingressCheck})"
                        }

                        if (podsOk && ingressOk) {
                            break
                        }

                        if (i < maxRetries) {
                            echo "等待 ${waitSeconds} 秒后重试..."
                            sleep waitSeconds
                        }
                    }

                    if (!podsOk) {
                        error 'Pod 健康检查失败！请检查: kubectl get pods -n online-ordering'
                    }
                    if (!ingressOk) {
                        error 'Ingress 健康检查失败！请检查: kubectl get ingress -n online-ordering; kubectl logs -n ingress-nginx deploy/ingress-nginx-controller'
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
            部署成功！(K8s / kind 集群)
            ========================================
            环境:     ${params.DEPLOY_ENV}
            镜像Tag:  ${IMAGE_TAG}
            前端地址:  http://localhost:80
            后端API:   http://localhost:80/api
            API文档:   http://localhost:80/doc.html
            KubePi:    http://localhost:30080
            ========================================
            """
        }
        failure {
            echo """
            ========================================
            部署失败！(K8s / kind 集群)
            ========================================
            排查步骤:
            1. 查看上方构建日志定位错误
            2. 检查 Pod 状态: kubectl get pods -n online-ordering
            3. 查看后端日志: kubectl logs -n online-ordering deploy/backend
            4. 查看前端日志: kubectl logs -n online-ordering deploy/frontend
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
