# A06：最小结果边界检查

A02 已将开炉结果作为纯 Java 值交给生产调用者，A03 说明不能将整个目标规则统一。本批只检查已验证的 BatchStartResult 边界，不强迫其他领域采用相同架构。

`tools/check_architecture_boundary.py` 将实际 BatchStartResult.java 复制到空临时目录，以 Java 17、空依赖 classpath/sourcepath 独立编译。若结果开始依赖 Minecraft、Forge、NBT、客户端或其他项目类，检查失败；不能因完整工程 classpath 恰好具备该类而漏掉。JDK 类型仍可使用。

Gradle `architectureBoundaryCheck` 使用已配置的 Java 17 compiler 和 pythonExecutable，加入现有 check 依赖，普通 build/verifyFull 自动包含。不新增全仓包引用禁令，不自动决定哪些业务状态应当共享。

工具验证：实际结果通过；构造没有 import、直接在字段中写 net.minecraft.world.level.Level 的反例，检查正确失败。`tools/test_architecture_boundary.py` 两项通过。该反例没有改动生产文件。

Gradle 接线已实际运行通过：`runGameTestServer -PentryUiOnly architectureBoundaryCheck -PpythonExecutable=<实际 Python>`，298/298 服务端测试通过且 boundary 任务成功；日志 `build/architecture-a04-quarantine-entry.log`。结果值未因后续读取缓冲修改而变化。

这只证明静态编译依赖边界，不证明操作原子性、业务合法性、运行时反射行为或 AI 改动无语义冲突。实际失败/恢复由 GameTest 承担，包体/测试夹具分离由制品账本承担。最终默认完整 build/verifyFull 已包含本任务并通过，536/536 GameTest，见 [最终验收](INTEGRATION_ACCEPTANCE_20261105.md)。
