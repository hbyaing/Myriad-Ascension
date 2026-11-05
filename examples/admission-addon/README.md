# 开炉接口的独立附属接入示例

本例是独立 Forge 模组 `myriad_admission_example`，不是万界仙途生产代码。用途是验证第二个实际消费者：管理员工具按原炉的规则请求开炉，并完整显示返回的原因、提交阶段和批次身份。不增加另一套材料、批次、认证或恢复记录。

## 运行

从仓库根目录执行，使用 Java 17：

```text
gradlew.bat runGameTestServer admissionExampleJar -PadmissionExample -PentryUiOnly
```

`-PadmissionExample` 才启用本目录的源码、模组注册与三个附属集成测试；普通 build、游戏启动和生产 JAR 不包含它。测试同时回归完整 `wuxingxiantu_entry` 命名空间，不是只运行示例。

开发输出 `build/libs/myriad-admission-example-0.1.jar` 来自独立 source set。它是本工程映射环境中的开发 JAR；本例不将未经过独立发行重混淆/二进制兼容验证的开发 JAR当成正式附属发行包。实际验证是 Forge 开发运行中的独立 Mod 注册、真实命令分发及主模组调用，不声称已验证所有外部分发环境。

## 消费者约定

命令要求权限 2，位置必须已加载；start 要求真实玩家，后续资格、距离、材料、占用和提交均由 AlchemyFurnaceBlockEntity 决定。

```text
/admission_probe inspect x y z
/admission_probe start x y z
```

inspect 不变更炉体，只显示占用、已发布批次状态和身份，不返回 seed、材料与完整服务端快照。尚未发布的隔离批次可能只能从原 start 返回的诊断/服务端日志找到身份，inspect 不是完整恢复控制台。

start 正常完成返回 1；其他返回 0 **并不表示没有提交**。操作者必须看 stage 和 batch；RECOVERY_REQUIRED 明确提示查询/修复原批次，不能自动重复请求。示例不会尝试补写位置关联或强制清除隔离；这些动作属于掌握日志与实际炉体依据的宿主。

## 扩展代价与边界

新增一个源码类、一份附属 mods.toml、一个测试类及可选构建接线；无需改变主模组生产 Java、注册入口、网络 ID、批次格式或恢复协议。Command source 的权限与用户输入解析归附属，规则和状态变更归原炉；这就是本例验证的分工，不是未来所有附属都必须遵循的统一插件框架。

主模组 boolean 兼容入口及 AlchemyService 仍按原路径运行，A02—A04 的故障和回归测试同时执行。若未来需要替换炼丹算法或无人值守跨距离开炉，本接口并不自动授予这些能力，须另行设计权限与状态责任。
