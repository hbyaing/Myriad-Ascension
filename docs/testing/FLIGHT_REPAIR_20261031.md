# 御剑高度、尾迹与化虹修复（2026-10-31）

接续“修复”聊天：御剑在特定高度无法召唤或自动退出，剑柄与尾迹连接抽动，化虹前端抽动，化虹耗灵过高。

## 修复内容

- 原版 `Entity.checkBelowWorld()` 在实体低于维度最低高度减 64 格时删除普通实体。仙界最低高度为 0，因此 Y=-64 以下的飞剑会消失。`ControlledSwordEntity` 仅对附近存活主人仍控制的御剑保留实体，继续由既有御剑生命周期检查管理；普通攻击剑、无主人飞剑仍清理。玩家虚空伤害和死亡逻辑没有改动。
- 修正完全位于建筑高度外时的碰撞预判空区间。建筑高度外的空气不会被误认为碰撞块。
- 世界尾迹保留固定 tick 历史，渲染端在当前挂点前按弧长连续插值衔接，避免极短末段和整点剔除带来的切线跳动。螺旋在真实剑尾处从零半径展开，使用剑自身标架。
- 按用户追加的近景反馈，将剑柄后宽度展开距离由 1.4 格延长为 4.5 格，根部宽度系数从 10% 调至 8%，继续使用平滑曲线渐进展开，消除细线后突然变粗的短促扩张。
- 所有化虹局部头部共用历史路径；默认虹流也沿同一路径弯曲。截面采用从载体标架运输的方向，移除竖直飞行附近的 world-up 阈值切换。
- 入虹消耗由最大灵力 1% 降为 0.25%，每秒维持费由 2% 降为 0.5%，两者均降低 75%。独立遁虚费用及原有飞行权限保持既有规则，提示同步更新。

## 验证方式

```powershell
.\gradlew.bat verifyFull --offline --no-daemon --console=plain
.\gradlew.bat runGameTestServer -PflightXianjieCompatibility --offline --console=plain
.\gradlew.bat runClient -PmyriadCapture -PhuahongCapture -PhuahongSkins -PhuahongMotion -PhuahongOutput=reports/flight-repair-20261031/native-final --offline --console=plain
.\gradlew.bat runClient -PmyriadCapture -PswordTailCapture -PswordTailOutput=reports/flight-repair-20261031/sword-taper-native --offline --console=plain
.\gradlew.bat runClient -PmyriadCapture -PswordTailCapture -PswordTailFullSpeed -PswordTailOutput=reports/flight-repair-20261031/sword-taper-fullspeed-native --offline --console=plain
```

仙界专项仅在显式启用时加载 `build/flight-compat-libs/wuxingchuangshi-1.0.jar`，需自行提供创世仙途正式包；不上传、打包或修改该依赖。此次使用 r248-cloud-sampling，SHA-256 为 `10df7b1a487f8ca448c7e3f45dcea4ced0cae4b67dd4a9263e727b0f97e05813`。

外部光影客户端增加 `-PmyriadExternalShaders`，并在独立测试目录启用 Solas Shader V3.7b。测试客户端包含禁止鼠标捕获保护，结束时正常保存退出，测试源码和保护不进入正式 JAR。

## 已确认与边界

- 同一创世仙途正式包的真实仙界服务端测试：修复前 Y=-80 召剑后消失；修复后 27 个高度（Y=-80 到 Y=260）全部通过召剑、乘骑和持续升降。测试仅在独立生成存档中清出空井，并给测试玩家补充生命以持续检查虚空线下的载体行为。
- 云层内外的正高度空井测试未复现自动退出；不能据此认定用户整套 Mod、任意存档地形或所有高度触发条件都已解决。确认修复的是原版底层清理与此次发现的衔接问题。
- 路径生产实现经过 8,666 项检查，覆盖竖直朝向、反向、载体滚转、连续采样边界。
- `verifyFull` 重跑通过，包含全部 486 项必需服务端 GameTests、回归检查及构建。首轮失败的高度用例碰到了平坦测试世界基岩；修正测试位置后再运行的结果才作为通过依据。
- 无光影与 Solas 的八种化虹实机捕获均无失败、指针违规为 0，包含转弯、俯仰、原生/后绘制、遮挡、退虹和关闭特效。原生额外保留 123 帧、Solas 保留 54 帧连续画面。已查看关键帧及连续帧联系表。
- 化虹及御剑连续转弯捕获使用测试专用 0.1 倍速度；御剑另用 `swordTailFullSpeed` 补拍正式 1 倍速度的直飞近景。服务端测试覆盖正式速度和两倍化虹倍率；这些不是全速性能报告。
- 渐进宽度最终无光影慢速捕获通过 591 帧挂点检查，Solas 慢速通过 351 帧；正式速度直飞无光影通过 165 帧、Solas 通过 100 帧，挂点误差均为 0。各轮指针违规均为 0。
- 修正御剑测试脚本：取消反复瞬移回起点，截图前要求存在实际尾迹几何，记录御剑顶点数及客户端位置、朝向。早先部分截图只有静止剑体，不能凭整轮曾出现顶点或挂点误差为 0 就认定该截图通过；以 `sword-taper-*` 新捕获替代。

本机日志与原始画面保留在 `build/flight-*.log`、`reports/flight-repair-20261031/`，不随源码提交。安装旧包备份和 SHA-256 由 `tools/install_test_instance.py` 记录。

最终正式包已安装到经 instanceId 核对的 AXO“测试 (1)”实例，PCL 历史路径在本机不存在。正式包 SHA-256：`e4f5bfb1872ee4b400bdb3c6f50257260cafe23d11ed03c3bf6503a9b4c811c1`。安装脚本确认仅一份本模组 JAR，旧包可恢复，正式包不包含测试用例及鼠标保护。运行中的用户客户端未关闭，磁盘更新需要下次启动才加载；助手启动的验收客户端均已正常退出。
