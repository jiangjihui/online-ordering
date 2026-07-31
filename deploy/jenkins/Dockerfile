FROM jenkins/jenkins:lts

USER root

# 安装 Docker CLI（用于通过 socket 调用宿主机 Docker）
# jenkins/jenkins:lts 基础镜像不含 gpg，直接下载 .asc 格式 key（apt 原生支持）
# 移除 debian-security 源（构建环境内该源经常超时，且 docker-ce-cli 不依赖它）
RUN curl -fsSL https://download.docker.com/linux/debian/gpg -o /usr/share/keyrings/docker-archive-keyring.asc \
    && chmod a+r /usr/share/keyrings/docker-archive-keyring.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list \
    && rm -f /etc/apt/sources.list.d/debian.sources /etc/apt/sources.list \
    && echo "deb http://deb.debian.org/debian $(. /etc/os-release && echo "$VERSION_CODENAME") main" > /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli \
    && rm -rf /var/lib/apt/lists/*

# 安装 kubectl（可选，启用 K8s 连接时需要）
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
    && chmod +x kubectl \
    && mv kubectl /usr/local/bin/

# 将 jenkins 用户加入 docker 组（解决 socket 权限问题）
# 容器内 docker 组的 GID 需要与宿主机一致，运行时通过 --group-add 解决
USER jenkins
