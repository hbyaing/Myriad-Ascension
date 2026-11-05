# A02：普通丹炉开炉结果边界

起点：本地 `98c9b8b`，其 A01 基线入口 GameTest 287/287 通过。本批先本地交付。

后续集成更新：A04 补齐未发布异常的原身份隔离与重载保护；本接口、兼容入口及生产调用者在默认完整 536 项测试和真实客户端网络/提示中通过，正式包已安装 xxcs。本文以下保留 A02 当时的结果与限制，最新证据见 [最终验收](INTEGRATION_ACCEPTANCE_20261105.md)。

## 实际改造

新增 `alchemy/batch/BatchStartResult`，保留原 `startV5Batch(player[,recipe])` boolean 方法，委托 `tryStartV5Batch(player,recipe)`。生产 AlchemyService 的自由炼丹和配方炼丹两条调用改用结果接口，并由 `reportAlchemyStart` 处理动作栏提示。

结果包括拒绝/结果原因、写入阶段、原批次 ID 和异常详情。纯结果类型没有依赖 Forge、NBT 或客户端类型，没有额外持有库存和存档。

| 阶段 | 本次调用已知事实 | 后续含义 |
|---|---|---|
| NOT_ATTEMPTED | 尚未调用批次日志写入 | 资格或准备拒绝；没有为本次返回已提交批次 ID |
| UNCONFIRMED | 已调用日志写入，但没有正常返回 | 可能已有日志或认证副作用；保留本次批次 ID，不能当作未执行重试 |
| JOURNAL_CONFIRMED | 日志 write 已返回，后续关联/发布失败 | 查询原批次记录并修复位置关联，不新建一份批次替代 |
| WORLD_APPLIED | 方块实体已应用托管状态，后续同步或保存路径异常 | 保持原恢复机制，核查原批次和方块状态 |
| COMPLETED | 原开炉流程及保存调用正常返回 | 可以通知开始；不额外承诺断电耐久或多文件原子事务 |

只有 STARTED 对应 COMPLETED；中途提交阶段异常返回 RECOVERY_REQUIRED，其余资格与准备失败在 NOT_ATTEMPTED。日志 write 本身包括认证 reserve 等操作，因此 UNCONFIRMED 不进一步假装知道磁盘上是否完全没有结果。

通知从开炉流程移到实际调用者/兼容包装层，通知异常只记录，不将已完成的开炉改报为未提交。成功文案沿用既有含义；新增拒绝提示具有中英文资源。没有更改网络消息字段、日志格式、批次计算、材料分配或领取协议。

## 调用者如何使用

```java
BatchStartResult result = furnace.tryStartV5Batch(player, recipeOrNull);
if (result.started()) {
    // 本次开炉流程完成；不要再次提交同一动作。
} else if (result.needsRecovery()) {
    // 记录 result.batchId()/stage()，交给掌握日志与位置关联的宿主核查。
    // 不能把它折算为“材料没扣，直接再来一次”。
} else {
    // 按 reason 解释资格、材料或准备错误，纠正条件后才重新操作。
}
AlchemyService.reportAlchemyStart(player, result);
```

旧 boolean 接口继续可用，但无法表达完整失败信息；新调用者应采用结果接口。原有入场占用与恢复逻辑继续有效，本次并未实现任意时刻重放旧请求的精确幂等，也未引入新的自动恢复协调器。

## 验证设计与状态

新增 V5AdmissionGameTests，按真实炉体入口检查：

1. 缺少异火的稳定原因与不变更。
2. 不受支持材料在准备阶段失败，无提交身份且不扣料。
3. 成功返回实际日志身份；重复开炉不改变该批次。
4. 以该玩家独占的认证文件路径故障，验证日志调用未确认时不谎报普通拒绝。
5. 以该炉位置关联路径故障，验证日志已写入与库存未发布能被区分；解除故障后用原批次 ID 重新关联与恢复，重复恢复不重复扣料。

故障夹具仅操作测试自己的位置/玩家路径，在 finally 中清理或恢复；没有向生产添加故障开关。文件故障不等于进程强退，后续 I/O 与完整崩溃窗口仍归 A04 调查。

资源检查 15/15 通过；Java 与 GameTest 编译通过。入口命名空间 GameTest **292/292 通过**（原 287 项加新增 5 项），Gradle 退出码 0，正常保存并关闭服务器。日志 `build/architecture-a02-entry.log`，核验结果 `build/architecture-a02-entry-result.json`，日志 SHA-256：`78349cddd8ad1b87817889eac652440bd12e4c73374f8ef143e1623461827aa8`。运行使用 Java 17 与 `-PentryUiOnly`，没有将此范围报告为全模组测试。

测试通过覆盖真实服务端炉体、原 boolean 调用者与本次结果接口；AlchemyService 两条生产调用已编译接入，但未单独模拟客户端发包。尚未验证客户端动作栏实显、完整 verifyFull 或新生产 JAR 安装。

## 取舍

选择领域内结果与兼容包装，保留现有实际执行者。相对仅给 boolean 附加日志，它让生产调用者可直接采取不同动作；相对建立通用制造引擎，它避免重复库存、日志和恢复权威。此处收益是结果语义和责任可见性，尚无性能改善的实测主张。
