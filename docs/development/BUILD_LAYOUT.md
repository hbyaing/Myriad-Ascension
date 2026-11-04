# 构建与验证布局

`build.gradle` 保留发布构建、依赖、资源处理和各专项客户端捕获的装配入口。

`gradle/verification.gradle` 负责所有随 `check` 与 `verifyFull` 执行的通用验证：资源检查、专项 Python 审计、回归运行、GameTest 源集及 GameTest 服务端配置。

现有命令和属性不变：

- `gradlew check` 仍运行资源、专项、回归和 GameTest 编译检查。
- `gradlew verifyFull` 仍在 `build` 后执行 Forge GameTest 服务端。
- GameTest 的 `-P...Only` 与兼容性属性仍由同一验证脚本解释。

新增通用验证任务时，应优先放在 `gradle/verification.gradle`；仅由一个视觉捕获入口使用的客户端配置，继续与该专项入口放在一起。这样不会把通用发布配置、服务端验证和临时客户端验收堆到同一段构建脚本中。
