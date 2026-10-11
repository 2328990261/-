# AI 荐书：人设 / 模型等配置从 application.yml 移到后台数据库

「AI 荐书」助手的人设、推荐规则与模型连接参数（模型名、接口地址、API Key、温度）过去散落在 `application.yml` / 环境变量里，改配置要改文件、重启服务。为让管理员能在后台实时调整，决定把这些配置存进数据库单行表 `ai_assistant_config`（id=1），后台「AI 助手」页可编辑；字段为空时回退到 `application.yml`/环境变量兜底。模型、接口、密钥在每次调用时读取快照并重建 `OpenAiApi`，从而无需重启即可生效。

**Status**: accepted

**Considered Options**
- 后台数据库配置：管理员可自助调整，改动即时生效；代价是需要引入 API Key 的 AES-GCM 加密存储，并把运行参数从代码配置改为运行期数据。
- 继续用 application.yml/环境变量：密钥不落库、变更可走配置文件管理；但无法在后台运行时切换，达不到「后台选模型」的需求。
- 仅模型名进库、连接信息仍走环境变量：改造最小且密钥不落库；但管理员换接口/密钥仍需改文件，体验割裂，故未采用。

**Consequences**
- `AI 荐书` 的模型/人设/推荐规则可在后台即时修改并「对话测试」，无需重启或改配置文件。
- API Key 以 `AES-256-GCM` 加密后存于 `ai_assistant_config`，读取时解密使用、后台展示脱敏；加密密钥来自配置 `ai.api-key-secret`（可用环境变量 `AI_API_KEY_SECRET` 覆盖）。
- 运行期配置变更要求每次调用按最新快照重建 `OpenAiApi`（不再复用单例缓存），带来轻微构建开销。
- 现有 `ai_assistant_config` 表此前只有 mapper/pojo 引用而无建表脚本，本次补齐 `sql/ai_assistant_config.sql`。
