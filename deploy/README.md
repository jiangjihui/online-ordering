# 本地 Jenkins + KubePi 部署指南

## 连接原理

```
Jenkins 容器
└── 连接 Docker ──→ /var/run/docker.sock（挂载宿主机 Docker socket）
    └── Jenkinsfile 中的 docker build / docker compose 命令
        └── 通过宿主机 Docker 直接执行

KubePi Pod（运行在 K8s 集群内）
└── 通过 Service Account 自动获得集群访问权限
    └── /var/run/secrets/kubernetes.io/serviceaccount/ (K8s 自动注入)
        └── 调用 K8s API 管理集群资源
```

- Jenkins 通过 Docker 部署应用（构建镜像、启动容器）
- KubePi 通过 K8s API 管理集群（查看状态、日志、资源）
- 两者各管各的，没有直接依赖

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

## 方案 B：kind 集群

适合需要多集群或更灵活 K8s 环境的场景。需先安装 kind：

```bash
winget install Kubernetes.kind
```

### 1. 创建集群

```bash
kind create cluster --config deploy/kubernetes/kind-config.yaml
```

验证：
```bash
kubectl cluster-info
kubectl get nodes
```

### 2. 启动 Jenkins

```bash
docker compose -f deploy/jenkins/docker-compose.yaml up -d --build
```

获取初始密码：
```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 3. 部署 KubePi

```bash
kubectl apply -f deploy/kubernetes/kubepi.yaml
```

浏览器打开 http://localhost:30080 ，账号 `admin` / `kubepi`。

### 4. 在 KubePi 中接入集群

获取 kind 集群的 kubeconfig，**并将 API Server 地址改为集群内部地址**：
```bash
kind get kubeconfig --name local-dev | sed 's|server: https://127.0.0.1:[0-9]*|server: https://kubernetes.default.svc|'
```

> **关键说明**：`kind get kubeconfig` 默认输出的 server 是 `https://127.0.0.1:<随机端口>`，这是宿主机上映射给 kind 容器的端口。但 KubePi 的 Pod 运行在集群内部，Pod 里的 `127.0.0.1` 指向 Pod 自身，连不到 API Server，导入会报 `dial tcp 127.0.0.1:xxxxx: connect: connection refused`。
>
> 因此必须把 server 改成集群内部 DNS 地址 `https://kubernetes.default.svc`（kind/kubeadm 的 API Server 证书 SAN 已包含该域名，CA 与客户端证书无需改动）。kind 分配的端口是随机的，上面 sed 用 `[0-9]*` 通配，可适配任意端口。

将上面命令的输出内容粘贴到 KubePi 的集群导入页面即可。

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

### 3. 全局工具配置

Manage Jenkins → Global Tool Configuration：

| 工具名 | 类型 | 版本 |
|--------|------|------|
| JDK17 | JDK | Java 17 |
| Maven | Maven | 3.9+ |
| NodeJS18 | NodeJS | 18.x |

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
