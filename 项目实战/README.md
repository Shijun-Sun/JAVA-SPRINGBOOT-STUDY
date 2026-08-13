# 项目实战 · 真实电商系统源码与生产部署

> 定位：理论学习每完成一个模块，就到这里找**真实大型项目**的对应实现来对照，
> 最终把它完整部署成你自己的"生产环境"。
>
> 理论负责"为什么这么设计"，这里负责"真实项目实际长什么样、上线还要处理什么"。

---

## 一、选型结论

**主线项目：[macrozheng/mall](https://github.com/macrozheng/mall)**（Apache License 2.0，可自由部署使用）

选它的理由：

1. **业务完整**：前台商城（首页门户、搜索、购物车、订单流程、会员中心）+ 后台管理
   （商品、订单、会员、促销、运营、内容、统计、权限）—— 是真的电商，不是 CRUD demo
2. **技术栈几乎覆盖我们全部 11 个模块**：MySQL、Redis、Elasticsearch、RabbitMQ、MongoDB、
   Nginx、Docker、Jenkins、ELK、Spring Security + JWT、SpringDoc、MinIO
3. **中文文档成套**：[macrozheng.com](https://www.macrozheng.com) 有完整教程，
   Windows 部署 / Docker 部署 / Docker Compose 部署 / Jenkins 自动化部署都有专篇 —— 这对"部署成生产环境"这个目标极其关键
4. **模块划分清晰**，读起来不会迷路：

```
mall
├── mall-common     工具类与通用代码（统一响应、错误码在这里）
├── mall-mbg        MyBatis Generator 生成的数据库操作代码
├── mall-security   Spring Security 封装模块
├── mall-admin      后台管理接口
├── mall-search     基于 Elasticsearch 的商品搜索
├── mall-portal     前台商城接口
└── mall-demo       框架搭建时的测试代码
```

5. **有微服务版本作为进阶**：[mall-swarm](https://github.com/macrozheng/mall-swarm)
   —— Spring Cloud Alibaba + Sa-Token + K8s。M08 学微服务时直接切过去，业务不变、架构升级，
   这是最理想的对照实验。

分支要选对：`master` 是 **Spring Boot 3.5 + JDK 17**；`dev-v2` 是 Boot 2.7 + JDK 8（别碰）。

### 备选（暂不使用，登记备查）

| 项目 | 特点 | 什么时候考虑 |
|------|------|-------------|
| [mall-swarm](https://github.com/macrozheng/mall-swarm) | mall 的微服务版，Spring Cloud Alibaba + Boot 3.2 + Sa-Token + K8s | M08 微服务模块，作为对照 |
| 芋道 ruoyi-vue-pro / yudao-cloud | 迭代非常勤，含商城模块，代码规范度高 | 想看另一种工程风格时 |

---

## 二、它和我们理论主线的差异（这些差异是资产，不是问题）

| 维度 | mall | 我们的理论主线 | 怎么用这个差异 |
|------|------|--------------|--------------|
| Spring Boot | 3.5 | 4.0 | 做一次真实的**版本迁移分析**（Boot 3.5 开源版已于 2026-06 EOL，这是真实生产诉求） |
| 持久层 | MyBatis + MyBatisGenerator | MyBatis-Plus | 对比代码生成 vs 通用 Mapper 的取舍 |
| 消息队列 | RabbitMQ | RocketMQ（主）/ Kafka | M06 对比三者的定位与选型依据 |
| MySQL | 5.7 | 8.4 LTS | 5.7 已 EOL，正好练升级与兼容性排查 |
| Elasticsearch | 7.17 | 8.x | 7 → 8 的破坏性变更（安全默认开启、type 移除历史） |
| 分层 | Entity / Param / VO（有偷懒处） | 严格边界 | Review 真实项目的妥协点，理解"为什么工程上会妥协" |

**架构能力的核心就是判断力**：知道每个选择的代价，而不是背哪个"更好"。这张表往后会一直补充。

---

## 三、四阶段推进路线

### 阶段 1 · 跑起来（配合理论 M01~M03）
- 本机用 Docker Compose 起 MySQL + Redis，只启 `mall-admin` 模块（这两个中间件就够）
- 导入数据库脚本，后台管理系统能登录、能看到商品列表
- 前端 `mall-admin-web` 跑起来（你的前端经验在这里是优势）
- **产出**：一份《本机启动踩坑记录》，遇到的每个报错都记下来 —— 这是最真实的运维素材

### 阶段 2 · 读源码（配合理论 M01~M07）
每学完一个理论模块，做一次定向源码阅读，回答固定三问：
**它怎么实现的 / 为什么这么实现 / 我会怎么改**

| 理论模块 | 定向阅读目标 |
|---------|------------|
| M01 | `mall-common` 的 `CommonResult`、全局异常处理、各模块包结构划分 |
| M02 | `mall-mbg` 生成代码、订单/商品/SKU 表结构与索引设计、Druid 连接池配置 |
| M03 | Redis 的实际用法：验证码、购物车、热门商品缓存，key 设计与过期策略 |
| M04 | `mall-security` 的过滤器链、JWT 实现、动态权限（`@PreAuthorize` 用没用） |
| M05 | `document/` 里的 nginx 配置，动静分离与前端资源部署 |
| M06 | RabbitMQ 实现的订单超时取消（延迟队列怎么做的） |
| M07 | `mall-search` 的 mapping 设计、数据同步方式、搜索排序逻辑 |

### 阶段 3 · 改造（配合理论 M08~M11）
只读不改学不到东西。计划中的改造项：
- 把某个模块的 MyBatis 换成 MyBatis-Plus，对比代码量与可控性
- 加一个原项目没有的**秒杀模块**（M11 的最终产出，这是最能体现架构能力的部分）
- 把 RabbitMQ 的延迟关单改成 RocketMQ 定时消息实现，压测对比
- 引入 Actuator + Prometheus + Grafana，做出监控大盘
- 按 mall-swarm 的思路，把单体拆出一个服务，跑通网关与分布式事务

### 阶段 4 · 部署成生产环境（配合理论 M05 / M09 / M10）
这是终极目标里"部署当生产环境"的落点。**已定方案：全部在本机模拟生产，不用云主机**
（详见 `环境/01-环境清单与部署拓扑.md`）。按真实生产标准来：

- 一套独立的 `compose.prod.yml`，和开发环境彻底分开（不是同一份配置改参数）
- Nginx 反向代理 + **负载均衡到 3 个应用实例** + 动静分离 + 限流 + 真实 IP 透传
- HTTPS：mkcert 本地 CA + 本地域名（`mall.local`）
- 本机 `registry:2` 当镜像仓库，走通"构建产物与运行分离"
- 中间件不暴露端口到宿主机，只在容器内网；数据卷持久化 + **备份与恢复演练**
  （备份没演练过等于没有备份）
- K8s：本机 k3d 多节点集群，跑 Deployment / Service / Ingress / 探针 / HPA
- CI/CD：workflow **代码照写、我照 review**，但不接真实 GitHub 算力
  （触发条件只留 `workflow_dispatch`，可选用 `act` 本机验证）
- 安全基线：改默认口令、中间件端口收口、Actuator 收口、密钥不进仓库

> 本机模拟对**原理、配置能力、故障排查**这三样一点都不打折。
> 学不到的部分（DNS、ACME 签发、备案、真实网络延迟、云安全组）已列成明确的缺口清单，
> 见环境文档第五节 —— 知道自己缺什么，比假装全学到了重要。
> 其中"真实网络延迟与故障"可以用 `tc netem` 在本机注入延迟丢包来补，M10 会做。

---

## 四、仓库与命名空间规划（已确认：走 fork）

### 4.1 为什么用 GitHub Organization 而不是个人账号

你要求"最大限度模拟实战"，那就按真实公司的方式组织。建一个 **Organization** 当命名空间，
比在个人账号下堆一堆 `mall-xxx` 仓库更接近实战，理由是几条能力只在 org 层才有：

- **Secrets 分层**：org 级 secrets（服务器 SSH key、镜像仓库凭证）多个仓库共享，不用每个仓库配一遍
- **Environments 与审批**：可以定义 `staging` / `production` 环境，生产部署要人工审批 —— 真实发布流程
- **权限模型**：team、分支保护、Code Owner、PR 必须 review 才能合 —— 这些都是实战流程的一部分
- **多仓库协同**：后端、前端、基础设施分仓，用 workflow 互相触发

建议 org 名：`<你的前缀>-mall-lab`（例如 `ssj-mall-lab`）。

**仓库全部设为 public**：mall 是 Apache 2.0，fork 出来本身就是 public；
另外 Actions 对 public 仓库免费不限分钟数，将来真要跑 CI 时不受额度限制。

> ⚠️ 代价必须清楚：public 仓库里**任何密码、密钥、云主机 IP、数据库连接串都不能出现**。
> 所有敏感信息走 GitHub Secrets + 服务器上的 `.env` 文件（不进仓库）。
> 这不是学习洁癖，公开仓库泄露的 AK/SK 被扫描器捡走是分钟级的事。
> 这条纪律本身就是 M04 安全模块的实战内容。

### 4.2 仓库划分（三个）

| 仓库 | 来源 | 内容 |
|------|------|------|
| `mall` | fork `macrozheng/mall` | 后端源码，阶段 3 的改造都提在这里 |
| `mall-admin-web` | fork `macrozheng/mall-admin-web` | 后台管理前端（你的前端经验用得上） |
| `mall-infra` | **自建** | 基础设施即代码：docker-compose、nginx.conf、Dockerfile、K8s manifest、部署脚本、GitHub Actions workflow |

`mall-infra` 单独一个仓库是刻意设计：**应用代码和基础设施代码分离**是真实项目的普遍做法，
运维配置的变更节奏和业务代码完全不同，混在一起会互相干扰发布。

### 4.3 分支策略（模拟真实协作）

```
main       ← 保护分支，只接受 PR 合并，对应生产环境
develop    ← 日常开发集成
feature/*  ← 每个改造项一个分支，例如 feature/seckill、feature/mybatis-plus
```

规则：
- `main` 开启分支保护：禁止直接 push，PR 必须通过 CI 检查
- 每个改造项走完整流程：建分支 → 提交 → 开 PR → **我在 PR 里做 code review** → 合并 → 本机部署验证
- 保留上游连接：`git remote add upstream https://github.com/macrozheng/mall.git`，
  以后能拉取原作者更新，练习**处理上游合并冲突** —— 这是维护 fork 的真实技能

> ⚠️ **fork 的一个特性，这次正好帮我们**：GitHub 默认**禁用 fork 仓库里的 Actions**。
> 因为我们只写 workflow 代码不真实运行，**不去手动启用就是我们想要的状态**。
> （反过来说：将来真要跑 CI 时，workflow 不触发就是这个原因，去 Actions 标签页启用即可 ——
> 很多人卡在这里。）
>
> **fork 和 "clone 后推到自己仓库" 的区别**（你提到两种都行，说明下差异）：
> fork 会保留与上游的关联，能直接向原项目提 PR、能一键同步上游更新、贡献图也能体现；
> clone 后手动推只是一个孤立仓库，虽然也能手动加 upstream remote 拉更新，但少了这层关系。
> **推荐 fork**，成本一样，能力更多。

### 4.4 本地目录规划

```
java-springboot-study/            ← 学习仓库（笔记、讲义、作业代码），建议也建成 git 仓库并推到你 GitHub
├── 理论学习/
├── 项目实战/
│   ├── README.md                 本文件
│   ├── mall/                     ← clone 你的 fork（加入 .gitignore，不纳入学习仓库）
│   ├── mall-admin-web/           ← 同上
│   ├── mall-infra/               ← 同上
│   └── 记录/
│       ├── 启动踩坑记录.md
│       ├── 源码阅读笔记.md
│       └── 部署记录.md
```

三个实战仓库放在 `项目实战/` 下（工作区统一，方便对照），但通过 `.gitignore` 排除，
各自是独立仓库、独立提交历史。已经帮你写好 `.gitignore`。

---

## 五、运行环境方案（已定：全部本机）

> 完整的环境实测数据、资源预算、拓扑图、能力缺口清单，都在
> **`环境/01-环境清单与部署拓扑.md`** —— 那份是唯一事实来源，这里只讲结论。

### 5.1 决策

**全部在本机运行。CI/CD 的代码照写、我照 review，但不接真实算力、不做真实远程部署。**

原因：没有合适的公网机器（Oracle Always Free 注册受阻；内网机无公网 IP，且是公司在用的机器，不能碰）。
本机是 Mac mini，10 核 / 24GB / 可用 87GB —— 比原计划的云主机强，全套架构跑得下。

### 5.2 保留的核心原则：构建与运行分离

即便全在一台机器上，这个流程也要走全，因为它是架构层面的东西，不是资源限制的妥协：

```
源码 → docker build（多阶段构建）→ 推本机 registry:2 → prod compose 从 registry 拉起
```

刻意绕这一道，而不是直接 `docker compose up --build`。理由：
**镜像是不可变的交付物**，"构建一次、到处运行"是容器化的核心价值。
直接 build-and-run 会让你错过版本管理、镜像标签、回滚这些真正重要的部分。

### 5.3 dev 与 prod 两套配置，不共用

| | 开发环境 | 模拟生产 |
|---|---------|---------|
| 编排 | `compose.dev.yml` | `compose.prod.yml` |
| 应用 | IDEA 里跑，热重载 | 容器里跑，**3 个实例** |
| 中间件端口 | 暴露到宿主机，方便连工具 | **不暴露**，只在容器内网 |
| 配置来源 | yml 里明文 | 环境变量 / `.env`（不进仓库） |
| 日志 | DEBUG，控制台 | INFO，JSON 结构化 + 轮转 |
| 数据库 schema | 可用 `ddl-auto` 图快 | 只走 Flyway 迁移 |
| 入口 | 直连应用端口 | 必须过 Nginx（HTTPS + 负载均衡） |

**多实例是这套方案里最有价值的一环**。单实例跑起来的应用，一堆问题永远不会暴露：
本地缓存不一致、Session 存内存、定时任务重复执行、文件传到本地磁盘。
起三个实例、前面挡一个 Nginx，这些坑当场现形 —— 这比学 Nginx 配置语法重要得多。

### 5.4 安全基线（本机也照做，养习惯）

- 改所有默认口令，密码走 `.env`，`.env` 进 `.gitignore`
- 中间件端口在 prod 配置里不映射到宿主机
- Actuator 只暴露必要端点，敏感端点加认证
- 容器设 `mem_limit` + 日志轮转（`max-size` / `max-file`）

> 这些在本机做起来"没必要"，但正是这些习惯决定了将来上真实公网时会不会出事。
> 公网上没设密码的 Redis 被挖矿劫持是最高频的入侵案例之一，扫描器找上来是小时级的。

### 5.5 部署拓扑目标（阶段 4 完成态，全部在本机）

```
浏览器 → https://mall.local:8443   （/etc/hosts + mkcert 本地 CA 证书）
              │
           Nginx 容器（HTTPS 终结 / 动静分离 / limit_req 限流 / 负载均衡）
              ├── /            → 静态资源（前端构建产物）
              ├── /api/        → upstream: portal-1 / portal-2 / portal-3
              └── /admin-api/  → upstream: admin-1 / admin-2
                                       │
                          ┌────────────┴────────────┐
                       MySQL 容器                Redis 容器
                    （不暴露端口 / 数据卷持久化 / 备份恢复演练）

镜像来源：本机 registry:2（localhost:5000），不直接用本地镜像缓存
K8s 版本：同一套服务改用 k3d 集群跑，练 Deployment / Service / Ingress / 探针 / HPA
```

### 5.6 CI/CD：代码保留，不真实运行

| 产出 | 写 | 跑 |
|------|----|----|
| Dockerfile（多阶段）、compose、nginx.conf、K8s manifest | ✅ | ✅ 本机真跑 |
| `.github/workflows/*.yml`、远程部署脚本 | ✅ 写并 review | ❌ 不接 GitHub 算力 |

两道保险防止 workflow 被意外触发：触发条件只留 `workflow_dispatch`（`push` 注释掉但保留，
说明设计意图）；fork 仓库的 Actions 本身默认就是禁用的，不去启用即可。

可选：装 `act` 在本机用 Docker 跑 workflow，验证 YAML 语法和 job 编排。不装我也会做静态 review。

---

## 六、下一步动作

### 需要你做的

1. **启动 Docker Desktop**，并在 Settings → Resources 把内存调到 12~16GB、CPU 给 6~8 核
   （默认分配通常不够，M07 起 ES 就会不够用）
2. **建 Organization**：GitHub 右上角 `+` → New organization → Free 套餐。
   或者直接用你已有的 `Shijun-Sun` —— 告诉我用哪个
3. **fork 两个仓库**：网页打开 `macrozheng/mall`、`macrozheng/mall-admin-web` 点 Fork，
   Owner 选你的 org。fork 是服务端操作，不下载东西，和网速无关。
   `mall-infra` 在 org 里 New repository 建空仓库

### 我拿到 org 名后做的

- 浅克隆三个仓库到 `项目实战/`，配好 upstream remote
- 搭 `mall-infra` 骨架：`compose.dev.yml`、`compose.prod.yml`、`nginx.conf`、
  多阶段 Dockerfile、`Makefile`、`.env.example`、workflow（不启用触发）
- 把学习仓库初始化成 git 仓库并推到你 GitHub

### 可选：GitHub CLI

`gh` 不是必需品 —— fork、开 PR、配 Secrets 网页上全都能做。装了的好处是这些操作我能直接代劳，
不用你切浏览器。国内网络下 `brew install gh` 很慢，可以先换镜像：

```bash
export HOMEBREW_NO_AUTO_UPDATE=1
export HOMEBREW_API_DOMAIN="https://mirrors.ustc.edu.cn/homebrew-bottles/api"
export HOMEBREW_BOTTLE_DOMAIN="https://mirrors.ustc.edu.cn/homebrew-bottles"
brew install gh && gh auth login
```

> GitHub Desktop **不包含** `gh`，两者是独立产品，装 Desktop 解决不了这个问题。

### clone 的网络优化

mall 仓库带大量文档图片，全量历史很大。用浅克隆：

```bash
git clone --depth 1 https://github.com/<你的org>/mall.git
```

需要完整历史时再 `git fetch --unshallow`。
GitHub Actions 的 checkout 默认也是浅克隆，同一个道理 —— CI 不需要历史。

### 我拿到上面信息后做的

- clone 三个仓库到 `项目实战/`（浅克隆），配好 upstream remote
- 建 `mall-infra` 仓库骨架：本地开发用 compose（MySQL + Redis）+ 生产用 compose + Nginx 配置模板
- 写 GitHub Actions workflow：构建 → 测试 → 推 GHCR → SSH 部署
- 把学习仓库初始化并推到你 GitHub（笔记和作业代码也该有版本历史）

> 时间上不用急着全做完：**阶段 1 只需要 MySQL + Redis + mall-admin 能跑起来**，
> CI/CD 和 Nginx 是 M05/M09 的内容。先把 M01 的讲义和作业推进，实战环境同步铺。
