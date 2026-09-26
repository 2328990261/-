# Redis 与线程池优化开发文档

> 状态：草案，待确认后实施
> 范围：Spring Boot 后端  
> 原则：MySQL 仍是唯一权威数据源；Redis 只承担缓存、验证码和临时状态；线程池按业务隔离，避免不同类型任务互相拖垮。

## 1. 背景

项目当前已经引入 Redis 依赖并新增线程池配置，但两者尚未真正接入业务。

### 1.1 Redis 现状

- 已在 `springboot/pom.xml` 引入 `spring-boot-starter-data-redis`。
- `application.yml` 中的 Redis 配置位于 `spring.cache.redis` 下。
- Spring Boot 3.2 的 Redis 连接配置应使用 `spring.data.redis`。
- `spring.cache.redis` 只用于配置 Spring Cache 的 Redis 缓存参数，不用于配置 Redis 连接。
- 业务代码中尚未使用 `RedisTemplate`、`StringRedisTemplate` 或 `@Cacheable`。

### 1.2 线程池现状

- `ThreadPool` 类没有 `@Configuration`，因此 `@Bean` 不会被 Spring 扫描注册。
- 当前方法返回裸 `ThreadPoolExecutor`，会绕开 Spring 对 `ThreadPoolTaskExecutor` 的生命周期管理。
- 当前参数为 `core=3 / max=5 / queue=10`：
  - 队列满之前不会创建额外线程。
  - 3 个核心线程忙时，后续任务先进入 10 个队列位置。
  - 队列满后才扩展到最大线程数。
  - 队列和线程都满后会触发默认 `AbortPolicy`。
- 未配置优雅停机、拒绝处理和监控。

## 2. 目标

1. 修正 Redis 与线程池基础配置，使两者真正可用。
2. 将线程池按业务类型拆分，降低互相影响。
3. 为推荐结果、用户标签权重和热点内容增加 Redis 缓存。
4. 将 EPUB 导入、推荐并行取数、热度重算接入合适的线程池。
5. Redis 异常时核心读接口可以降级访问 MySQL。
6. 提供清晰的实施顺序、测试方案和回滚方案。

## 3. 非目标

第一期不包含以下内容：

- 不修改推荐算法本身的业务规则。
- 不引入消息队列。
- 不将前端上传改造成完整异步任务中心。
- 不缓存大体积章节正文。
- 不默认启用 Java 21 虚拟线程，先用可控的平台线程池。
- 不把所有 SQL 都加缓存，只选择读多写少且计算成本较高的数据。

## 4. 总体设计

```text
前端请求
   |
   v
Controller / Service
   |
   +-- Redis 命中
   |     |-- 推荐结果
   |     |-- 用户标签权重
   |     |-- 热门列表 / 轮播图 / 详情
   |     `-- 标签热度
   |
   v Redis 未命中
Service + MySQL
   |
   |-- recommendExecutor：推荐并行取数
   |-- uploadExecutor：EPUB 解析和分卷导入
   `-- statsExecutor：标签热度重算、缓存预热
```

## 5. Redis 基础方案

### 5.1 配置修正

目标配置：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2s
      connect-timeout: 2s
```

说明：

- 密码建议通过环境变量传入，不提交明文。
- 如果使用 Spring Cache 注解，需要添加 `@EnableCaching`。
- 如果直接使用 `StringRedisTemplate`，可以不启用 Spring Cache 注解。
- 第一期建议以 `StringRedisTemplate + JSON` 为主，key 和 TTL 更清晰。

### 5.2 基础封装

建议新增 `RedisSupportService`，统一处理：

- key 前缀，例如 `novel:`。
- TTL 常量。
- JSON 序列化和反序列化。
- Redis 异常捕获和 warning 日志。
- 缓存开关，例如 `app.cache.enabled`。
- 缓存 miss 时的加载逻辑。

建议避免默认 JDK 序列化，统一使用字符串 key 和 JSON value。

## 6. Redis 使用点



### 6.1 推荐结果缓存

推荐接口计算链路较长，会读取偏好、配置、收藏、完读、评论、阅读历史、协同数据等，是 Redis 的核心收益点。

Key 设计：

```text
novel:rec:result:{userId}:{sortType}:{version}
novel:rec:version:{userId}
```

规则：

1. 推荐结果 TTL 建议 3 到 5 分钟。
2. 用户行为变化时递增用户版本号。
3. 版本号变化后，旧 key 自然失效。
4. Redis 异常时直接走 MySQL 原推荐逻辑。
5. 缓存结果需要标记 `cachedAt`，避免 debug 信息被误认为实时计算。

需要递增版本号的行为：

- 收藏新增或取消。
- 阅读历史新增或更新。
- 标记完读或取消完读。
- 评论新增、更新、删除。
- 不感兴趣新增、更新、删除。
- 用户偏好标签更新。
- 用户推荐 profile 更新。

### 6.2 用户标签权重缓存

`effectiveWeightsByTagName` 会读取多个用户行为表和全量小说数据，适合缓存。

Key 设计：

```text
novel:rec:weights:{userId}:{version}
```

规则：

1. TTL 与推荐结果一致。
2. 与推荐结果共用用户版本号。
3. 用户行为变化后统一失效。
4. Redis 异常时回退原计算逻辑。

### 6.3 热点公共内容缓存

适合缓存：

| 数据 | Key | TTL |
|---|---|---:|
| 轮播图 | `novel:public:carousel` | 5 分钟 |
| 分类小说列表 | `novel:public:label:{label}` | 5 分钟 |
| 小说详情 | `novel:public:book:{id}` | 5 分钟 |
| 推荐主配置 | `novel:config:recommend-main` | 10 分钟 |
| 评论推荐配置 | `novel:config:recommend-comment` | 10 分钟 |

清理策略：

- 第一期先用 TTL 过期，降低实现复杂度。
- 第二期再在管理端新增、修改、删除数据时主动清理。
- 管理端更新小说、轮播图、推荐配置时，应删除对应缓存。

不缓存章节正文：

- 单章内容可能较大，容易占用 Redis 内存。
- 先缓存列表、详情、配置。

### 6.5 标签热度

当前 `SiteTagHeatServiceImpl` 使用本地内存缓存，并在请求线程中同步重算。

目标设计：

1. 请求线程读取 Redis 热度结果。
2. Redis miss 时提交重算任务到 `statsExecutor`。
3. 重算完成后写回 Redis，TTL 建议 2 到 5 分钟。
4. 有旧值时可以先返回旧值。
5. 无旧值时可执行一次同步兜底计算。

### 6.6 阅读量计数缓冲

当前每次阅读都会直接更新 MySQL。

可选优化：

```text
Redis INCR novel:read:count:{novelId}
定时任务批量写回 MySQL
```

该方案涉及定时任务、原子读取、失败补偿和多实例锁，建议放到第二期。

第一期保留现有直接更新 MySQL 的方式。

## 7. 线程池方案

### 7.1 线程池拆分

| Bean 名称 | 用途 | 初始参数 | 拒绝策略 |
|---|---|---:|---|
| `recommendExecutor` | 推荐流程并行查库 | `core=6 / max=10 / queue=200 / keepAlive=60s` | `AbortPolicy` + 业务降级 |
| `uploadExecutor` | EPUB 解析、分卷导入 | `core=2 / max=4 / queue=20 / keepAlive=60s` | `AbortPolicy` + 返回系统繁忙 |
| `statsExecutor` | 标签热度重算、缓存预热 | `core=1 / max=2 / queue=10 / keepAlive=60s` | `CallerRunsPolicy` |

参数是初始值，后续需要根据机器核数、数据库连接池和压测结果调整。

### 7.2 目标代码结构

新增 `ExecutorConfig`，替代当前 `ThreadPool`：

```java
@Configuration(proxyBeanMethods = false)
public class ExecutorConfig {

    @Bean("uploadExecutor")
    public ThreadPoolTaskExecutor uploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("novel-upload-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }
}
```

`recommendExecutor` 和 `statsExecutor` 采用同样结构定义。

统一要求：

- 返回 `ThreadPoolTaskExecutor`，不要 `getThreadPoolExecutor()` 解包。
- 不手动调用 `initialize()`，交给 Spring 管理。
- 每个线程池有独立线程名前缀。
- 队列必须有界。
- 显式设置拒绝策略。
- 开启优雅停机。
- 所有异步异常必须记录日志。

### 7.3 调用方式

推荐显式注入 executor：

```java
CompletableFuture.supplyAsync(() -> queryData(), recommendExecutor);
```

不建议：

```java
CompletableFuture.supplyAsync(() -> queryData());
```

后者会使用公共 `ForkJoinPool`，无法被业务线程池管理。

如果使用 `@Async`：

- 启动类需要 `@EnableAsync`。
- 必须指定 executor 名称。
- 不建议同类内部自调用。
- 事务不会自动传播到异步线程。

第一期建议直接注入 executor，不使用 `@Async`，调用链更清晰。

## 8. 线程池业务接入点

### 8.1 EPUB 分卷导入

当前 `uploadVolumes` 在请求线程中串行解析多个 EPUB。

第一期建议：

- 保持接口同步返回，不改前端交互。
- 使用 `uploadExecutor` 并行处理多个 EPUB 文件。
- 限制同时解析的文件数量。
- 单个文件失败时记录文件名和异常，不影响其他文件。

注意事项：

- 不要在异步线程中直接读取 `MultipartFile` 的流。
- 先保存为临时文件，或先读取为字节数组，再提交任务。
- 请求结束后 multipart 流可能失效。

第二期可选：

- 上传后立即返回 `taskId`。
- 后台继续解析。
- 前端轮询任务状态和进度。

第二期需要前端和管理端交互改造，确认后再实施。

### 8.2 推荐并行取数

推荐流程中相互独立的数据可以并行读取：

- 推荐主配置。
- 评论行为配置。
- 用户 profile。
- 用户偏好标签。
- 用户有效标签权重。
- 黑名单和不感兴趣数据。
- 收藏、完读、评论、阅读历史。
- 全量小说基础数据。

建议：

1. 使用 `recommendExecutor` 并行提交独立查询。
2. 使用 `CompletableFuture` 聚合结果。
3. 设置总超时，建议 3 秒。
4. 单个查询失败时记录 warning，并尝试串行兜底。
5. 不建议把所有小 SQL 都并行化，优先并行耗时明显且相互独立的查询。

### 8.3 标签热度重算

将热度重算从请求线程迁移到 `statsExecutor`：

1. 请求先查 Redis。
2. 缓存命中直接返回。
3. 缓存过期时提交后台重算任务。
4. 后台任务完成后更新 Redis。
5. 后台任务异常时记录日志，并保留旧缓存。

## 9. 实施阶段

### 阶段一：基础配置修正

- [ ] 修正 `application.yml` 的 Redis 配置。
- [ ] 新增 `ExecutorConfig`。
- [ ] 删除或废弃原 `ThreadPool`。
- [ ] 配置三个业务线程池。
- [ ] 验证应用可正常启动。

### 阶段二：Redis 基础能力

- [ ] 新增 `RedisSupportService`。
- [ ] 统一 JSON 序列化。
- [ ] 统一 key 前缀和 TTL 常量。
- [ ] 增加缓存开关。
- [ ] 增加 Redis 异常降级日志。


### 阶段三：推荐缓存

- [ ] 新增用户推荐版本 key。
- [ ] 缓存推荐结果。
- [ ] 缓存用户有效标签权重。
- [ ] 在用户行为写操作后递增版本号。
- [ ] Redis 异常时回退原推荐逻辑。

### 阶段四：热点内容缓存

- [ ] 缓存轮播图。
- [ ] 缓存分类小说列表。
- [ ] 缓存小说详情。
- [ ] 缓存推荐配置。
- [ ] 管理端更新后清理对应缓存。

### 阶段五：线程池业务接入

- [ ] EPUB 上传接入 `uploadExecutor`。
- [ ] 推荐独立查询接入 `recommendExecutor`。
- [ ] 标签热度重算接入 `statsExecutor`。
- [ ] 增加任务耗时和失败日志。

## 10. 测试方案

### 10.1 编译和启动

```powershell
cd springboot
.\mvnw.cmd test
```

如项目暂无测试，则至少执行：

```powershell
cd springboot
.\mvnw.cmd -DskipTests package
```

### 10.2 推荐缓存测试

1. 首次请求推荐，记录响应时间。
2. 第二次请求同一用户同一 `sortType`，应命中缓存。
3. 更新用户偏好或阅读历史后，应生成新版本。
4. 新版本下不应读取旧推荐结果。
5. 停止 Redis 后，推荐接口应仍能从 MySQL 计算并返回。

### 10.3 热点内容缓存测试

1. 首次访问轮播图、分类列表和小说详情。
2. 第二次访问应命中缓存。
3. 管理端更新对应数据后，应清理或更新缓存。
4. 缓存 TTL 到期后，应重新从 MySQL 加载。

### 10.4 线程池测试

1. 并发上传多个 EPUB，观察 `novel-upload-` 线程执行。
2. 并发请求推荐，观察 `novel-recommend-` 线程执行。
3. 触发标签热度重算，观察 `novel-stats-` 线程执行。
4. 填满上传队列，验证拒绝策略和用户提示。
5. 停止应用，验证线程池等待任务完成。

### 10.5 降级测试

1. 停止 Redis 服务。
2. 请求推荐、小说列表、详情等核心读接口。
3. 核心读接口应降级访问 MySQL。
4. 后端日志应出现 Redis warning，不应出现未处理异常堆栈。
5. 验证码相关接口应返回明确错误，而不是服务不可用。

## 11. 验收标准

1. Redis 配置使用 `spring.data.redis`，应用启动无配置绑定错误。
2. 三个线程池均由 Spring 管理，线程名清晰可识别。
3. 推荐结果可以命中 Redis 缓存，并在用户行为变化后失效。
4. 用户有效标签权重可以命中 Redis 缓存，并与推荐结果使用同一版本。
5. 轮播图、分类列表、小说详情和推荐配置可以命中 Redis 缓存。
6. Redis 不可用时，核心读接口仍可访问 MySQL。
7. EPUB 导入、推荐查询、热度重算使用不同线程池。
8. 线程池队列有界，拒绝策略明确。
9. 后端编译和主要接口手工测试通过。

## 12. 回滚方案

1. 设置 `app.cache.enabled=false`，缓存逻辑直接回退 MySQL。
2. Redis 异常时自动降级到 MySQL。
3. 保留原推荐计算代码，不将算法逻辑迁移到 Redis。
4. 保留同步上传实现，异步上传仅作为后续可选能力。
5. 如线程池配置引发问题，可将业务调用临时改回同步执行。
6. 如线程池 Bean 引发启动失败，可临时恢复原同步调用并禁用对应配置类。

## 13. 风险与注意事项

| 风险 | 影响 | 处理方式 |
|---|---|---|
| Redis 不可用 | 缓存和验证码异常 | 核心读接口降级 MySQL，验证码返回明确错误 |
| 缓存不一致 | 用户看到旧推荐或旧内容 | 使用版本号、TTL 和管理端主动清理 |
| 线程池过载 | 任务被拒绝或响应变慢 | 有界队列、独立线程池、拒绝日志、压测调参 |
| 异步上下文丢失 | 异步线程读取不到请求信息 | 显式传递 userId、bookId 等参数 |
| 事务不传播 | 异步写库缺少事务 | 异步任务调用独立事务方法 |
| Redis 内存占用过大 | 服务不稳定 | 控制 TTL，避免缓存章节大文本 |
| 并行查库过多 | 数据库连接被占满 | 限制并行度，并让线程池参数小于数据库连接池上限 |

## 14. 监控与日志

建议至少记录以下信息：

- Redis 缓存命中和未命中次数。
- Redis 异常类型和发生时间。
- 推荐接口总耗时和缓存命中状态。
- 线程池活跃线程数、队列长度和拒绝次数。
- EPUB 解析任务耗时、成功数和失败数。
- 标签热度重算耗时和完成时间。

如果后续引入 Spring Boot Actuator 和 Micrometer，可进一步暴露：

- `executor.active`
- `executor.queued`
- `executor.pool.size`
- `cache.gets`
- `cache.puts`
- Redis 连接状态。

## 15. 待确认事项

1. Redis 环境地址、端口和密码是否已经确定？
2. 第一期是否同时实现推荐缓存和热点内容缓存，还是先只做验证码？
3. EPUB 上传第一期保持同步接口，还是直接改造为异步任务？
4. 阅读量 Redis 计数缓冲是否放到第二期？
5. 是否需要引入 Spring Boot Actuator 暴露线程池和 Redis 指标？
6. 线程池初始参数是否按本方案执行，还是需要根据部署机器配置调整？

确认以上事项后，再开始修改代码。
