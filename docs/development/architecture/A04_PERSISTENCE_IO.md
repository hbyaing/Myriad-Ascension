# A04：持久化边界、重试保护与 I/O

调查基线 `c9fb7d2`，本地先交付。测量与改造状态逐项记录，不声称多文件事务或全服性能保证。

## 当前实际责任

| 阶段 | 实际顺序与权威 | 失败后的允许动作 |
|---|---|---|
| 准备 | 校验资格、复制材料、构造批次身份与规则快照 | 未调用日志时可纠正条件后重新操作 |
| 写批次 | validate → certification.reserve → 强制写盘并原子替换批次文件 → certification.record → ResearchPractice.record → 调整 active cache | write 未返回不能断言无副作用；查原身份，不能直接重新制造 |
| 发布到炉 | 位置关联文件 → 应用托管库存/燃料 → 同步 → chunkSource.save(true) | 已有日志仍是依据；位置关联、区块保存和日志不是一个原子事务 |
| 自然运行 | 每 20 世界 tick 尝试推进，先写新日志，再应用和同步；异常暂停 | 保留日志，核查原因后从原身份恢复 |
| READY 领取 | 先持久化玩家待领索引，再写 DELIVERING → 放背包回执 → PlayerList.save → 读取 player.dat 验证回执 → 写 CLAIMED → 清待领索引 | 回执未确认则保留意图；重复领取/恢复检查同一身份，不另发结果 |
| 领取恢复 | 登录最高优先级与每 20 玩家 tick 检查待领索引 | 已 CLAIMED 的遗留索引可清理；DELIVERING 重走回执核验 |

日志 `writeFile` 为临时文件写完后 force(true)，再原子替换；遇 Windows AccessDenied 最多三次等待共 30ms，只重试同一个临时文件，不回退非原子覆盖。读取保留 MAX_BYTES、NBT 计量和尾随数据拒绝。

认证 reserve 可能先将抽样种子/材料批次及 PENDING 试次写入认证账本，再写批次。终态 record 可能进一步更新认证；研究实践记录进入 PlayerCrafts SavedData 并 setDirty，并不等同独立强制写盘。ACTIVE 时后二者通常直接返回，但不能把通用 write 视为单一文件操作。认证有 reconcile，且满额协议评估能将没有批次提交的遗留 PENDING 标为 MISSING_COMMIT；不应主动删种子记录或把重试伪造成独立样本。

终态研究记录的持久化晚于批次日志，不能宣称任何崩溃点都已与终态原子保存；后续同身份 write 可依批次去重补记。本轮没有重建整个研究协调器。炼器仍使用 CraftingJobs/方块/玩家资源的既有边界，本结论没有将它们伪装成同一 V5 事务。

## 已发现并修复的重复开炉窗口

A02 结果已经保留不确定身份，但原 catch 只在已经 applyV5、炉体有 v5Ref 时暂停。日志调用或位置关联失败而尚未 apply 时，调用者能收到 RECOVERY_REQUIRED，炉体却仍可再次接收开炉。

本批在发生过日志尝试的异常中，将原 batchId 放入既有私有 V5Ref 并设置 v5Fault/setChanged，复用已有占用和隔离机制；不修改日志协议，不扣取尚未发布的输入。保存的现有 V5Ref 让方块实体重载后也保持占用。位置关联修复后仍用原身份 restoreV5，成功恢复才解除暂停。

这是原炉的重复入场保护，不是任意位置、拆炉或断电情形的全局精确重试协议。若根本没有可确认批次，仍需操作者核对原始文件后处理，不能通过 UI 连点绕过。setChanged 也不额外保证此故障路径已经强制写盘。

故障测试同时验证材料/燃料未改、原身份保留、重复点击拒绝、方块实体序列化重载后仍拒绝，以及修复关联后按原身份恢复。此时整个 NBT 不再逐字相等，因为增加私有隔离引用是刻意的修复；材料槽 NBT 与燃料仍逐项检查。

## 实测方法与范围

`ArchitectureIoGameTests.completedFurnaceAndReceiptIoLedger` 在独立 batch 中构造正常四份灵芽草丹炉，保持多方块角区块加载，执行真实开炉、470 世界 tick 的自然运行和领取。要求 21 个反应步、FINISHED、一个回执、CLAIMED 与空待领索引。JFR 仅在 GameTest 源集启用 FileRead/FileWrite，阈值 0，带栈；生产无计时开关。

JFR 文件事件没有单独覆盖 force 耗时；开炉和领取的墙钟时间包含原同步保存调用。GameTest 不以正常 20TPS 节拍运行，470 tick 不表示 23.5 秒真实时间。已有测试世界含历史批次和区块，整个 archive 的读数不能说成当前一炉的必需次数。

`tools/ArchitectureIoSummary.java` 用 JDK 独立读取 JFR，按原批次 UUID 分离 measured-batch 与 other-batches。示例：

```text
java tools/ArchitectureIoSummary.java recording.jfr batch-uuid output.json
```

两次未缓冲对照中，**当前批次均为 5,561 次 FileRead、282,768 bytes；24 次 FileWrite、1,094,898 bytes**。其他批次分别 108,128/124,764 次读，体现旧测试档案随运行增长。第一次开炉 4.416s、领取 149.181ms，第二次开炉 7.023s、领取 184.514ms；这不是可直接归因缓冲的配对延迟对照，也不是正常玩家服务器的延迟预算。

定位 `BatchJournal.readFile` 直接用 DataInputStream 包装 Files.newInputStream，NBT primitive/字节读取会形成大量底层读取。本批加入 BufferedInputStream；保留 byte budget、NBT 计量、尾随校验、写盘和恢复顺序。必须以相同批次载荷的分类读数和完整性测试验证，不能凭修改一行宣布性能提升。

原始测量：`build/entry-ui-gametest/architecture-io/ordinary-furnace-*.jfr/.json`。第一份 JFR SHA-256 `8090c6f8958108846c6618d7788b92888f7822420a4eeb8f00b1f991b3eff250`，日志 `build/architecture-a04-entry.log`，298/298 通过，日志 SHA-256 `58eab1c4831a68747f0106be047f0f044fbd48c01576f7e1d6a2de8f2fbef36d`。这次只测基线，未包含后来增加的隔离/缓冲修复。

## 后测

| 当前批次，真实 470 tick / 21 反应步 | 基线未缓冲 | 隔离修复、未缓冲 | 隔离修复、已缓冲 |
|---|---:|---:|---:|
| FileRead 事件数 | 5,561 | 5,561 | 30 |
| FileRead 字节 | 282,768 | 282,768 | 282,768 |
| FileWrite 事件数 | 24 | 24 | 24 |
| FileWrite 字节 | 1,094,898 | 1,094,898 | 1,094,898 |
| FileRead 事件总耗时 | 23.799ms | 42.645ms | 0.799ms |
| 开炉墙钟时间 | 4,416.018ms | 7,022.685ms | 2,959.423ms |
| 领取墙钟时间 | 149.181ms | 184.514ms | 160.619ms |

同一夹具和相同批次读写字节量下，缓冲显著减少被观测的 Java 文件读取事件；写入频率和字节量未降低。开炉包含冷归档索引/区块保存且历史档案持续增长，系统调度与 JFR 成本也变化，不能把墙钟差值全部归因于缓冲，更不能声称领取必然变快。当前证据支持保留局部读取缓冲，不支持删除强制写盘或直接异步化。

逐次数据与归档活动分离结果已保留在 [measurements](measurements/) 的 `io-*.json`，原 JFR 的哈希与缓冲运行前相关源码指纹另存。未缓冲基线的生产版本为 `c9fb7d2`；第二次包含隔离修复，第三次再包含缓冲和损坏读取测试；每次均使用相同测量测试。JFR 未收入 Git，不将 25MB 观测记录塞入源码或生产包。

隔离修复的独立运行 298/298 通过，日志 `build/architecture-a04-quarantine-entry.log`，SHA-256 `fc086aa45be44f3eaf7b581b9687400f9a4b7b22b1848ce0a654b6052a53355c`。

缓冲后的完整入口 **299/299 GameTest 通过**，Java/GameTest 编译通过，Gradle 退出码 0，服务端保存并正常退出。新增的读取故障测试实际向原批次文件追加尾随字节、截断和写入超过预算的文件，确认均拒绝；finally 恢复原字节后内容一致。日志 `build/architecture-a04-buffered-entry.log`，结果 `build/architecture-a04-buffered-result.json`，SHA-256 `4eeb50bd56a90f867c4d5450e8cad3b06208e7fba21d3bd2aa691db4c484345c`。

本批不涉及格式迁移；旧数据仍经同一个 NBT 解析与校验。未模拟进程断电或任意多炉并发，未完成客户端和最终发布构建。代码回退可撤销读取缓冲；对已经隔离的故障炉仍应先核查原批次，不把解除保护当作通用恢复手段。
