# 心境识海背景实施说明（2026-10-31）

用户已明确确认侵染与击败后两态配色，并授权实装。本轮从 `37d70c4` 的隔离工作树实现，并合并保留同期主线化虹提交 `bf07974`。这份说明记录本专项，不代表整个Mod已完成整体验收。

## 批准配色

两态均使用用户明确指定的 `mindscape-palette.json`。对应资源为 `src/main/resources/assets/wuxingxiantu/mindscape/palette-corrupted.json` 和 `palette-purified.json`，SHA-256均为：

```text
75bdd745aedec420fab9406e425e6399f3fea95d7c39af933cf860fc3f8ea8fb
```

两份数据目前完全相同：`goldBase=[1.55,0.72,0.06]`，`goldHighlight=[1.9,0.95,0.12]`，`redBase=[4.92,0.02350261112501315,0.02350261112501315]`，`spiritBase`与`spiritEmission`均为`[0,0,0]`，`spiritStrength=1`，`exposure=1`。零值是用户明确数据，不能补回蓝色或将强度擅改为0。原材质的内部槽线仍保留，两态由既有红/金材质分支显示不同外观。

用户提供的完整FBM shader保留原算序；源作者、链接及许可证未提供，不能将该核心署名为助手原创。背景是有限曲面的程序材质，有真实相机视差，不是完整三维流体求解器。

## 生命周期与网络

自然击败绑定Boss后，服务端先防重结算，进入`COMPLETED`，清理Boss与残余攻击，但保留实例租约及外物快照。前48tick连续净化，随后24tick保持胜利外观，总计72tick后释放租约并归返恢复。只接受服务端有效胜利；最终心魔一阶段不会触发净化。

`complete(player,true)`管理完成与`force_restore`仍即时恢复。失败、Boss被移除、中止、死亡、断线及重连沿现有恢复路径处理；未胜利不授予净化，已经结算的胜利不重复奖励，也不会因展示被中断而重新施加入侵。

`MindscapeStatePacket`追加可空session UUID与slot，协议版本为109；remaining按打坐或胜利窗口分别表示剩余tick。客户端`MindscapeClient`提供`state()`、`challenge()`、`sessionId()`、`origin()`与`purification(partialTick)`。origin在当前维度、会话或状态不匹配时为null；坐标纯客户端计算，不加载服务端管理类。

## 渲染接入

为保持已批准配色与程序材质的外观，心境采用独立视觉域：Oculus/Iris存在时，仅为`wuxingxiantu:mindscape`选择`VanillaRenderingPipeline`，由本Mod的着色器绘制背景；其他维度继续调用Iris原管线工厂，保留当前光影包选择与配置。工厂包装沿用原管理器设置当前管线及缓存，真实维度切换仍由Oculus执行`destroyPipeline`并重建，离开心境后恢复外界光影。这是实现批准视觉的兼容取舍，并非用户指定的技术方案，也不表示外部光影包会在心境内继续参与着色。

背景在`AFTER_SKY`阶段绘制，沿当前实际投影、视图和相机重建本地射线。有限背景壳写自身世界深度，虚拟远景膜片交点只作为材质坐标，不冒充真实世界几何。背景壳范围包含第三人称相机；实际地面、角色、透明层与粒子随后按原管线绘制。当前实现保存并恢复GL/FBO/MRT状态，资源重载可重新加载着色器与批准配色。

第三方光影使用不同的材质缓冲和调色流程；独立管线用于保持已批准外观，避免逐包改写颜色与材质契约。

不修改其他Mod、原场地、五座锚点、碰撞范围或心境进度账户。独立管线的静态接入本身不等于实际光影切换已验收，须以对应客户端报告与图片为准。

## 已完成验证与剩余范围

本机主验证证据（构建产物和本地QA目录，不保证收录到源码发布包）：

- `reports/mindscape-merged-build-server.log`及`build/gametest-run/logs/latest.log`记录合并后496项required GameTests全部通过，正式`build`也成功。两条自然心魔击杀测试改为验证胜利窗口结束后归返；管理即时恢复语义保留，并新增胜利登出恢复及packet往返验证。合并前首轮为488项。
- `reports/mindscape/native/mindscape/verification.json`为无外部光影独立客户端首轮报告，`complete=true`、`failures=[]`，包含17个命名截图场景，实际窗口为1600×900。记录真实入境、自然胜利过渡/停留/归返、最终心魔一二阶段、失败中止和管理即时归返；指针保护记录无捕获违规。

合并化虹前的独立管线矩阵四组均完成20个截图场景，报告`complete=true`、`failures=[]`，共80图；汇总见`reports/mindscape/client-final-summary.json`：

| 报告目录（`reports/mindscape/`下） | 实际环境 |
| --- | --- |
| `native-owned/mindscape` | 未安装Oculus；可选Mixin缺依赖启动正常 |
| `oculus-disabled/mindscape` | Oculus/Embeddium已安装，Iris API确认光影关闭 |
| `solas-owned/mindscape` | 外界Solas 3.7b，心境独立原生管线，归返恢复Solas |
| `bliss-owned/mindscape` | 外界Bliss 2.1.2，心境独立原生管线，归返恢复Bliss |

每组包含真实服务端入境、自然胜利的48tick净化及24tick停留、最终心魔二阶段门槛、中止/管理归返、实际资源重载、转头与FOV、近墙、第三人称越界相机、玻璃和原生透明粒子。报告逐图记录实际pipeline类型；光影启用组核实外界`IrisRenderingPipeline`、心境`VanillaRenderingPipeline`、归返恢复Iris，扩展顶点格式也恢复。关键红/金图同时检查画面色度，防止只统计draw次数而把无背景画面误判通过；透明遮挡和接缝另作目视复核。

客户端固定MC 1.20.1、Forge 47.4.10、Java 17、1600×900、RTX 4060 Ti；光影轮使用Oculus 1.8.0及Embeddium 0.3.31。测试只操作新建独立存档与隐藏客户端，所有组均记录鼠标捕获违规为0并正常保存退出。保护/夹具仅在测试source set，不进入正式JAR。

合并同期主线后再次完成Solas的全部20图（`reports/mindscape/solas-merged/mindscape/verification.json`），核实公共着色器注册、新测试注入、资源重载、心境画面及外界管线恢复，报告无失败。其余三组保留上述合并前基线标识，不冒称所有组合均在合并后重新执行。未将1600×900验证扩展为其他GPU或所有分辨率的验收结论。

正式包CRC、两份配色字节哈希及测试类排除检查均通过，见`reports/mindscape/production-jar-verification.json`。批准配色目录使用固定LF，避免Windows检出改变字节哈希。

正式包已按约定安装到AXO「测试（1）」实例，旧版本已备份，mods内仅保留一个本Mod有效JAR。安装包SHA-256为`d01278dce5f1142e940c31da6ac417a584b44e77a29432c633cc64d506a70e7b`，安装记录见`reports/last-test-instance-installation.json`。未结束用户游戏；磁盘更新不表示正在运行的JVM已加载新版。
