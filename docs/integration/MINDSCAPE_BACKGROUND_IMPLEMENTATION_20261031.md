# 心境识海背景实施说明（2026-10-31）

用户已明确确认侵染与击败后两态配色，并授权实装。本轮从 `37d70c4` 的隔离工作树实现，并合并保留同期主线化虹提交 `bf07974`。这份说明记录本专项，不代表整个Mod已完成整体验收。

## 批准配色

侵染态保持原批准的 `mindscape-palette.json`，资源为 `src/main/resources/assets/wuxingxiantu/mindscape/palette-corrupted.json`，SHA-256为：

```text
75bdd745aedec420fab9406e425e6399f3fea95d7c39af933cf860fc3f8ea8fb
```

侵染态全部值保持不变：`goldBase=[1.55,0.72,0.06]`，`goldHighlight=[1.9,0.95,0.12]`，`redBase=[4.92,0.02350261112501315,0.02350261112501315]`，灵力底色与发光均为`[0,0,0]`，`spiritStrength=1`，`exposure=1`。

击败后采用用户最新银白底、墨黑卷纹与少量金丝参考，以及当前工作台导出的 `mindscape-palette-purified (1).json`，替代旧的大面积黄色外观。对应资源为 `src/main/resources/assets/wuxingxiantu/mindscape/palette-purified.json`，SHA-256为：

```text
1cd903fbce6c1a50032607f0ed35a4b8369a54e2e99a16d1c3760332f3126c8d
```

完整配色对象如下，保留16的HDR色值及0.1的整体亮度；字段名沿用v1格式，不表示金色应占主导：

```json
{
  "goldBase": [16, 16, 16],
  "goldHighlight": [16, 6.859047945822506, 0.28227127181414524],
  "redBase": [0.37, 0.008, 0.06],
  "spiritBase": [0, 0, 0],
  "spiritEmission": [0, 0, 0],
  "spiritStrength": 4,
  "exposure": 0.1
}
```

两态现在使用不同文件。零灵力颜色是明确数据，不能补蓝或擅改强度；同源材质的内部暗槽保留。旧击败后批准文件在工作台 `comparisons/before-silver-ink-purified-adoption-20261031/palettes/approved-purified.json` 中保留可恢复副本。

用户提供的完整FBM shader保留原算序；源作者、链接及许可证未提供，不能将该核心署名为助手原创。背景是有限曲面的程序材质，有真实相机视差，不是完整三维流体求解器。

## 生命周期与网络

自然击败绑定Boss后，服务端先防重结算，进入`COMPLETED`，清理Boss与残余攻击，但保留实例租约及外物快照。前48tick连续净化，随后24tick保持胜利外观，总计72tick后释放租约并归返恢复。只接受服务端有效胜利；最终心魔一阶段不会触发净化。

`complete(player,true)`管理完成与`force_restore`仍即时恢复。失败、Boss被移除、中止、死亡、断线及重连沿现有恢复路径处理；未胜利不授予净化，已经结算的胜利不重复奖励，也不会因展示被中断而重新施加入侵。

`MindscapeStatePacket`追加可空session UUID与slot，协议版本为109；remaining按打坐或胜利窗口分别表示剩余tick。客户端`MindscapeClient`提供`state()`、`challenge()`、`sessionId()`、`origin()`与`purification(partialTick)`。origin在当前维度、会话或状态不匹配时为null；坐标纯客户端计算，不加载服务端管理类。

快速连续重入可能在`addFreshEntity`成功后遇到Boss尚未加入可查询实体表的窗口。初次跟踪前最多等待5个服务端tick，并保留生成实例以立即识别真实死亡、显式移除和错绑定；合法区块卸载可由同UUID的新实例接管。首次可见后恢复原严格检查，超时仍失败并清理Boss、租约和外界快照；完成、退出及服务端停止均清理暂存引用。奖励、15秒打坐及72tick胜利窗口不变。

## 渲染接入

为保持已批准配色与程序材质的外观，心境采用独立视觉域：Oculus/Iris存在时，仅为`wuxingxiantu:mindscape`选择`VanillaRenderingPipeline`，由本Mod的着色器绘制背景；其他维度继续调用Iris原管线工厂，保留当前光影包选择与配置。工厂包装沿用原管理器设置当前管线及缓存，真实维度切换仍由Oculus执行`destroyPipeline`并重建，离开心境后恢复外界光影。这是实现批准视觉的兼容取舍，并非用户指定的技术方案，也不表示外部光影包会在心境内继续参与着色。

背景在`AFTER_SKY`阶段绘制，沿当前实际投影、视图和相机重建本地射线。有限背景壳写自身世界深度，虚拟远景膜片交点只作为材质坐标，不冒充真实世界几何。背景壳范围包含第三人称相机；实际地面、角色、透明层与粒子随后按原管线绘制。当前实现保存并恢复GL/FBO/MRT状态，资源重载可重新加载着色器与批准配色。

本轮修复全向背景漏洞：主材质层闭合且不透明，主层不再受后方有限范围或掠射遮罩切开；两个副层仍保留有限轮廓，并在主层交界前3格渐隐。投影改用单一有界平滑域，分母为`1+sqrt(q.z*q.z+0.04)`，不再依赖立体投影后极点切口。仅当虚拟材质眼点在主椭球内的归一化半径大于0.9时，才作C1连续压缩并渐近0.95；真实相机射线与物理深度壳不变。用户完整FBM内核与材质公式保留，背景备用色和薄雾取自对应批准配色，避免露出固定纯白背景。

第三方光影使用不同的材质缓冲和调色流程；独立管线用于保持已批准外观，避免逐包改写颜色与材质契约。

不修改其他Mod、原场地、五座锚点、碰撞范围或心境进度账户。独立管线的静态接入本身不等于实际光影切换已验收，须以对应客户端报告与图片为准。

## 已完成验证与剩余范围

本机验证证据（构建产物和本地QA目录，不保证收录到源码发布包）分为当前修复与历史发布版本。

当前全覆盖与银白墨纹配色修复：

- `reports/mindscape-full-coverage-build.log`记录本轮`build`成功；最终交付包仍须在剩余核验后重新构建。
- `reports/mindscape/full-coverage-solas-wide/mindscape/verification.json`记录Solas独立客户端32图完成，`complete=true`、`failures=[]`，实际分辨率2560×1080。包含两态前后左右、正上正下、近边界与第三人称相机，以及资源重载、真实玻璃/透明粒子、胜利归返及外界管线恢复；额外击败后视角分别由真实主人击杀进入原72tick窗口，不直接灌客户端状态。
- 其中27个心境场景对材质场与实际`AFTER_SKY`目标进行192×108采样、8×6分块核对，均为`minimumAlpha=1`、`uncoveredAlphaRatio=0`、`divergentTiles=0`。纹理变化及目标一致性同时受检，避免仅把alpha写为1或填充常色而误判覆盖。银白材质本身允许近白区域，近白连通区只作诊断，不能单凭其存在判定背景漏洞。报告记录鼠标捕获违规为0，未使用真实鼠标输入。
- `reports/mindscape/full-coverage-disabled-verified/mindscape/verification.json`记录当前安装Oculus/Embeddium但禁用光影的1600×900复验，32图完成且无失败；27次完整背景采样最低覆盖alpha为1、未覆盖样本为0，实际目标无分块偏差超限，快速连续重入也通过。前期失败的QA目录不计入通过数。
- `reports/mindscape-full-coverage-final-build-server.log`记录本轮构建和500项服务端GameTests通过，包含4项首次实体跟踪/重载/显式移除/超时清理回归。它对应后续主线并发合并前的本轮实现，合并后的最终结果另行记录。

历史已发布版本`0e4a5e0`及其前序验证：

- `reports/mindscape-merged-build-server.log`及当轮`build/gametest-run/logs/latest.log`记录合并后496项required GameTests全部通过，正式`build`也成功。两条自然心魔击杀测试改为验证胜利窗口结束后归返；管理即时恢复语义保留，并新增胜利登出恢复及packet往返验证。合并前首轮为488项。这是旧发布版本的服务端证据，不替代当前记录。
- `reports/mindscape/native/mindscape/verification.json`为无外部光影独立客户端首轮报告，`complete=true`、`failures=[]`，包含17个命名截图场景，实际窗口为1600×900。记录真实入境、自然胜利过渡/停留/归返、最终心魔一二阶段、失败中止和管理即时归返；指针保护记录无捕获违规。

合并化虹前的历史独立管线矩阵四组均完成20个截图场景，报告`complete=true`、`failures=[]`，共80图；汇总见`reports/mindscape/client-final-summary.json`：

| 报告目录（`reports/mindscape/`下） | 实际环境 |
| --- | --- |
| `native-owned/mindscape` | 未安装Oculus；可选Mixin缺依赖启动正常 |
| `oculus-disabled/mindscape` | Oculus/Embeddium已安装，Iris API确认光影关闭 |
| `solas-owned/mindscape` | 外界Solas 3.7b，心境独立原生管线，归返恢复Solas |
| `bliss-owned/mindscape` | 外界Bliss 2.1.2，心境独立原生管线，归返恢复Bliss |

每组包含真实服务端入境、自然胜利的48tick净化及24tick停留、最终心魔二阶段门槛、中止/管理归返、实际资源重载、转头与FOV、近墙、第三人称越界相机、玻璃和原生透明粒子。报告逐图记录实际pipeline类型；光影启用组核实外界`IrisRenderingPipeline`、心境`VanillaRenderingPipeline`、归返恢复Iris，扩展顶点格式也恢复。关键红/金图同时检查画面色度，防止只统计draw次数而把无背景画面误判通过；透明遮挡和接缝另作目视复核。

客户端固定MC 1.20.1、Forge 47.4.10、Java 17、1600×900、RTX 4060 Ti；光影轮使用Oculus 1.8.0及Embeddium 0.3.31。测试只操作新建独立存档与隐藏客户端，所有组均记录鼠标捕获违规为0并正常保存退出。保护/夹具仅在测试source set，不进入正式JAR。

合并同期主线后再次完成Solas的全部20图（`reports/mindscape/solas-merged/mindscape/verification.json`），核实公共着色器注册、新测试注入、资源重载、心境画面及外界管线恢复，报告无失败。以上80图与合并后20图均为本轮全覆盖和新配色之前的历史证据，不作为此次修复的复验；也不扩展为其他GPU或所有分辨率的验收结论。

历史正式包CRC、当时两份配色字节哈希及测试类排除检查通过，见`reports/mindscape/production-jar-verification.json`。本轮终包仍须重新核对新配色、CRC及测试类排除。批准配色目录使用固定LF，避免Windows检出改变字节哈希。

历史发布包已安装到AXO「测试（1）」实例，SHA-256为`d01278dce5f1142e940c31da6ac417a584b44e77a29432c633cc64d506a70e7b`；它不包含本轮全覆盖与新配色修复。本轮正式安装包、提交及远端发布结果待主任务最终填写，不沿用旧JAR或旧安装记录。磁盘更新与正在运行的JVM是否加载新版仍分别记录，不结束用户游戏。
