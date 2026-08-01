# 本地 Jenkins + KubePi + kind 部署指南

## 连接原理

```
Jenkins 容器
└── 连接 Docker ──→ /var/run/docker.sock（挂载宿主机 Docker socket）
    ├── docker build / docker push → 构建镜像并推送到本地 registry (localhost:5000)
    └── kubectl（~/.kube/config → host.docker.internal:API端口）
        └── kubectl apply / rollout → 把应用部署到 kind 集群

宿主机本地 registry (registry:2, localhost:5000)
└── kind 节点通过 containerd mirror 从宿主机拉取镜像（host.docker.internal:5000）

KubePi Pod（运行在 kind 集群内）
└── 通过 Service Account 自动获得集群访问权限
    └── 调用 K8s API 管理集群资源（含应用 Pod/Deployment/Service/Ingress）
```

- Jenkins 构建镜像 → 推送本地 registry → kubectl 部署到 kind 集群
- KubePi 通过 K8s API 管理集群（应用也在集群内，KubePi 统一可见）
- 应用入口：Ingress (nginx) → http://localhost:80 按路径路由（/ 前端，/api、/ws、/images 后端）

---

## 方案 A：Docker Desktop 内置 K8s（推荐）

无需安装额外工具，Docker Desktop 自带一切。

### 1. 开启 K8s

Docker Desktop → Settings → Kubernetes → Enable Kubernetes → Apply & Restart

等待左下角 K8s 图标变绿。

### 2. 启动 Jenkins

```bash
docker compose -f deploy/jenkins/docker-compose.yaml up -d --build
```

> 首次需要 `--build`，因为自定义了 Jenkins 镜像（内置 kubectl）。

获取初始密码：
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

浏览器打开 http://localhost:8888 ，安装推荐插件，创建管理员账号。

验证 Jenkins 能访问 Docker：
```bash
docker exec jenkins docker ps
```

> 如果报权限错误 `permission denied`，在宿主机执行 `stat -c '%g' /var/run/docker.sock` 查看 Docker socket 的 GID，然后修改 `deploy/jenkins/docker-compose.yaml` 中 `group_add` 的值。

### 3. 部署 KubePi

```bash
kubectl apply -f deploy/kubernetes/kubepi.yaml
```

等待就绪：
```bash
kubectl wait --for=condition=available deployment/kubepi -n kubepi-system --timeout=120s
```

浏览器打开 http://localhost:30080 ，账号 `admin` / `kubepi`。

### 4. 在 KubePi 中接入集群

进入 KubePi → 集群管理 → 导入集群，kubeconfig 内容获取方式：

```bash
cat ~/.kube/config
```

---

## 方案 B：kind 集群（当前项目使用）

适合需要多集群或更灵活 K8s 环境的场景。需先安装 kind：

```bash
winget install Kubernetes.kind
```

### 1. 创建集群

```bash
kind create cluster --config deploy/kubernetes/kind-config.yaml
```

kind-config 已配置：80/443 端口映射（Ingress 入口）、30080（KubePi）、containerd mirror（镜像从宿主机本地 registry 拉取）。

验证：
```bash
kubectl cluster-info
kubectl get nodes
```

### 2. 启动本地镜像仓库（registry:2）

应用镜像先推送到宿主机本地仓库，kind 节点通过 containerd mirror 拉取：

```bash
docker run -d --name local-registry --restart=unless-stopped -p 5000:5000 registry:2
```

### 3. 安装 Ingress Controller

```bash
curl -sSL https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml -o ingress-nginx-kind.yaml
# 去掉镜像 digest（本地单平台镜像与多平台 digest 不一致会导致拉取失败）
sed -E 's|@sha256:[a-f0-9]{64}||g' ingress-nginx-kind.yaml | kubectl apply -f -
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=180s
```

> 镜像拉取问题：kind 节点直连 registry.k8s.io 不稳定。做法：宿主机 `docker pull registry.k8s.io/ingress-nginx/controller:v1.15.1` 后 `docker tag ... localhost:5000/...` 并 push，kind 通过 mirror 从宿主机拉。
>
> 注意：如果 kind 节点报 `proxyconnect tcp: dial tcp 127.0.0.1:7890: connection refused`，是节点继承了宿主机代理环境变量导致。解决：进入节点 `docker exec -it local-dev-control-plane bash`，写 systemd drop-in 清掉 containerd 的代理并重启：
> ```bash
> mkdir -p /etc/systemd/system/containerd.service.d
> printf '[Service]\nEnvironment="HTTP_PROXY=" "HTTPS_PROXY=" "ALL_PROXY=" "http_proxy=" "https_proxy=" "all_proxy=" "NO_PROXY=*"\n' > /etc/systemd/system/containerd.service.d/proxy-off.conf
> systemctl daemon-reload && systemctl restart containerd
> ```

### 4. 启动 Jenkins

```bash
docker compose -f deploy/jenkins/docker-compose.yaml up -d --build
```

获取初始密码：
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

给 Jenkins 容器配置访问 kind 集群的 kubeconfig（server 用 host.docker.internal，跳过 TLS 校验——证书 SAN 不含该域名）：

```bash
PORT=$(kind get kubeconfig --name local-dev | grep -oE 'https://127.0.0.1:[0-9]+' | grep -oE '[0-9]+$')
kind get kubeconfig --name local-dev | sed -E "s|server: https://127.0.0.1:[0-9]+|server: https://host.docker.internal:${PORT}|; s|    certificate-authority-data:.*|    insecure-skip-tls-verify: true|" > jenkins-kubeconfig.yaml
docker exec jenkins mkdir -p /var/jenkins_home/.kube
docker cp jenkins-kubeconfig.yaml jenkins:/var/jenkins_home/.kube/config
```

> kind API Server 端口是动态的，集群重建后需重新执行上面命令。

### 5. 部署 KubePi

先把镜像推送到本地 registry（kind 通过 mirror 拉取）：
```bash
docker pull m.daocloud.io/1panel/kubepi:v2.0.0
docker tag m.daocloud.io/1panel/kubepi:v2.0.0 localhost:5000/1panel/kubepi:v2.0.0
docker push localhost:5000/1panel/kubepi:v2.0.0
```

然后：
```bash
kubectl apply -f deploy/kubernetes/kubepi.yaml
kubectl wait --for=condition=available deployment/kubepi -n kubepi-system --timeout=120s
```

浏览器打开 http://localhost:30080 ，账号 `admin` / `kubepi`。

### 6. 在 KubePi 中接入集群

获取 kind 集群的 kubeconfig，**并将 API Server 地址改为集群内部地址**：
```bash
kind get kubeconfig --name local-dev | sed 's|server: https://127.0.0.1:[0-9]*|server: https://kubernetes.default.svc|'
```

> **关键说明**：`kind get kubeconfig` 默认输出的 server 是 `https://127.0.0.1:<随机端口>`，这是宿主机上映射给 kind 容器的端口。但 KubePi 的 Pod 运行在集群内部，Pod 里的 `127.0.0.1` 指向 Pod 自身，连不到 API Server，导入会报 `dial tcp 127.0.0.1:xxxxx: connect: connection refused`。
>
> 因此必须把 server 改成集群内部 DNS 地址 `https://kubernetes.default.svc`（kind/kubeadm 的 API Server 证书 SAN 已包含该域名，CA 与客户端证书无需改动）。kind 分配的端口是随机的，上面 sed 用 `[0-9]*` 通配，可适配任意端口。

将上面命令的输出内容粘贴到 KubePi 的集群导入页面即可。

### 7. 部署应用（首次手动，之后由 Jenkins 完成）

```bash
# 推送镜像（或让 Jenkins 构建）
docker tag registry.example.com/online-ordering/backend:latest localhost:5000/online-ordering/backend:latest  # 示例：把已有镜像 tag 成仓库格式
docker push localhost:5000/online-ordering/backend:latest

# 应用 K8s 清单
kubectl apply -f deploy/kubernetes/app-namespace.yaml
kubectl apply -f deploy/kubernetes/app-backend.yaml
kubectl apply -f deploy/kubernetes/app-frontend.yaml
kubectl apply -f deploy/kubernetes/app-ingress.yaml
kubectl rollout status deployment/backend deployment/frontend -n online-ordering
```

访问：http://localhost:80（前端）/ http://localhost:80/doc.html（API 文档），应用可在 KubePi 中管理。

---

## 配置 Jenkins 流水线

### 1. 添加 GitHub 凭据

Manage Jenkins → Credentials → System → Global credentials → Add Credentials：

| 字段 | 值 |
|------|-----|
| Kind | Username with password |
| Username | GitHub 用户名 |
| Password | GitHub Personal Access Token（需勾选 `repo` 权限） |
| ID | `github-credentials` |

> 生成 Token：GitHub → Settings → Developer settings → Personal access tokens → Generate new token (classic)，勾选 `repo`

### 2. 创建 Pipeline 任务

1. Jenkins 首页 → New Item → 输入名称 → 选择 Pipeline
2. 勾选 **Pipeline script from SCM**
3. SCM 选 Git
4. Repository URL: `https://github.com/jiangjihui/online-ordering`
5. Credentials: 选择 `github-credentials`
6. Script Path 保持默认 `Jenkinsfile`
7. 保存后点击 Build Now

### 3. 全局工具配置（说明）

| 工具名 | 是否必需 | 说明 |
|--------|---------|------|
| Maven | 是 | Maven 3.9+，自动安装（Jenkinsfile 中 `tool name:'Maven'` 引用） |
| JDK17 / NodeJS18 | **否** | Jenkinsfile 已改为使用容器自带的 JDK21（`/opt/java/openjdk`）和 Node22（`/opt/node22`），无需再配置 JDK/Node 工具 |

> 前端构建要求 Node ≥20.19（Vite 8），容器内置 Node 22 LTS 到 `/opt/node22`；JDK 用容器自带 Temurin 21（`-release 17` 编译出 Java 17 字节码）。

### 4. 部署到 K8s（Jenkinsfile 说明）

Deploy 阶段流程：`docker push` 镜像到本地 registry → `kubectl apply` 应用清单 → `kubectl set image` 滚动更新 → `rollout status` 等待 → Health Check（kubectl 检查 Pod + curl Ingress 入口）。

Jenkins 容器需要 `~/.kube/config`（见方案 B 第 4 步），镜像仓库地址为 `localhost:5000`（宿主机 registry:2）。

---

## 清理

### 方案 A

```bash
# 停止 Jenkins
docker compose -f deploy/jenkins/docker-compose.yaml down -v

# 删除 KubePi
kubectl delete -f deploy/kubernetes/kubepi.yaml

# 关闭 K8s: Docker Desktop → Settings → Kubernetes → Disable
```

### 方案 B

```bash
# 停止 Jenkins
docker compose -f deploy/jenkins/docker-compose.yaml down -v

# 删除 kind 集群（一键清理所有 K8s 资源）
kind delete cluster --name local-dev
```
