# Redis 与线程池优化 — 运行时验证记录

> 补充完成 `openspec/changes/optimize-redis-thread-pools/tasks.md` 中 5.3 与 5.7 的运行时验证。
> 环境：本地 MySQL（3306）+ Redis 8.8.1（6379，无密码），Spring Boot 3.2.5 / Java 24，应用端口 8081。

## 5.3 推荐与有效权重缓存 TTL 过期（短 TTL 运行时测试）

启动参数：`CACHE_RECOMMEND_TTL=10s`，其余使用默认配置。测试用户：`userId=1`（admin），`sortType=custom`。

| 步骤 | 结果 |
|---|---|
| 清理 `novel:rec:*` 后首次请求 | `cacheStatus=MISS`，`elapsedMs=376` |
| 写入 key 与 TTL | `novel:rec:result:1:custom:1`、`novel:rec:weights:1:1`，两者 `TTL=9s` |
| 立即再次请求 | `cacheStatus=HIT`，`elapsedMs=14`，`cachedAt` 与首次写入一致（08:30:08.957） |
| 等待 13s（> 10s TTL） | 两个 key 均已过期清空 |
| 过期后再次请求 | `cacheStatus=MISS`（重新计算），两个 key 以新 `TTL=9s` 重新写入 |

结论：推荐结果与有效标签权重均按配置 TTL 过期，过期后由 MySQL 重新计算并以新 TTL 写回；两者共用用户推荐版本号 `novel:rec:version:1`，缓存命中时 `cachedAt` 元数据保持写入时间。

## 5.7 有界队列拒绝与优雅关闭（运行时冒烟测试）

以 `AnnotationConfigApplicationContext` 加载真实 `ExecutorConfig` Bean 冒烟测试：

```
recommendExecutor: core=6, max=10, queue=200, prefix=novel-recommend-, keepAlive=60s, gracefulWait=true, await=30s
uploadExecutor:    core=2, max=4,  queue=20,  prefix=novel-upload-,    keepAlive=60s, gracefulWait=true, await=30s
statsExecutor:     core=1, max=2,  queue=10,  prefix=novel-stats-,     keepAlive=60s, gracefulWait=true, await=30s
```

- 饱和拒绝：向 `uploadExecutor` 提交 `4(max)+20(queue)=24` 个阻塞任务后再提交 5 个任务，`accepted=24, rejected=5`，全部拒绝均打印
  `WARN ... Executor rejected task workload=upload pool=4 active=4 queue=20 thread=main`（AbortPolicy + 拒绝日志）。
- 优雅关闭：`statsExecutor` 中运行 2.5s 任务后调用 `shutdown()`，`elapsedMs=2503` 且任务完成标记为 true，验证 `waitForTasksToCompleteOnShutdown=true / awaitTerminationSeconds=30` 生效，在途任务可在关闭时完成。
- 冒烟测试退出码 0（`SMOKE_TEST_PASS`）。

结论：三个业务池均有界队列、显式拒绝策略且拒绝被记录；关闭时等待在途任务（上限 30s）。