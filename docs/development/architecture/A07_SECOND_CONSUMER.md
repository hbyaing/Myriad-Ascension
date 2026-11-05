# A07：独立附属消费者验证

起点 `4bea077`。第二个需求：为管理员制作一个可选择加载的开炉诊断工具，复用已验证的结果接口；不能通过新增工具绕过主模组资格、重试保护和恢复机制。

实际实现：[admission-addon](../../../examples/admission-addon/README.md)。独立 Mod ID、独立 source set 与三个 Forge GameTest，主模组生产 Java 不增加工具命令。Gradle 开关 `-PadmissionExample` 明确启用。

验证从真实注册后的 Brigadier 命令树进入：权限拒绝、缺火拒绝、成功入场、只读检查、重复入场拒绝、位置关联故障后保留原身份，以及原宿主恢复。示例显示 reason/stage/id，拒绝自动重试；没有另建结果状态权威。

选择直接消费现有接口，没有为示例添加公共插件注册器、跨领域任务引擎或新的网络协议。增量为一个附属源类、一个 descriptor、一个测试类与可选构建块。原生产消费者不因本例改变；测试回归整个入口命名空间。

首次启动在 Gradle 配置阶段发现 gameTestServer 尚未创建就被访问，已将可选 Mod 接线移入现有 run 配置；失败日志 `build/architecture-a07-example.log` 保留。修正后，独立 Mod 与测试编译、Forge 注册/命令分发通过，**302/302 入口 GameTest 通过**（原 299 项与附属 3 项），退出码 0。重验日志 `build/architecture-a07-example-recheck.log`，核验 `build/architecture-a07-example-result.json`，日志 SHA-256 `02d281a4a6fab1de1241fe85e1e1bb0e5154ee8d1b540c3cafc531b0beaa3795`。服务器正常保存并退出。

独立开发 JAR 4,411 bytes，SHA-256 `d92999d056977f822b3ab4f85248393aa23425dc6b157e1dec294dea680bf612`，没有把它误装进正式实例。生产主 JAR 的账本工具新增对独立附属/客户端验收源集 class 名的交集检查，最终主包重建后才确认排除结果。

本次是源级/Forge 开发环境接入试验，开发 JAR不冒充经过重混淆与独立发行验证的发布包。后续主模组生产制品已重建且确认不含独立附属/测试类；真实客户端/网络与 xxcs 磁盘安装已完成，见 [最终验收](INTEGRATION_ACCEPTANCE_20261105.md)。
