# 背包旧版布局恢复与窗口适配隔离

恢复本地旧版 f823139 的 InventoryResolutionFit.apply 缩放算法，保留当前功能和既有贴图。背包内嵌 CultivationPage 不再进入顶层 ScreenResolutionEvents 初始化：其宽高应来自宿主内容区，不能被全屏GUI宽高覆盖，也不能改变窗口缩放。炼丹/研析台继续使用现有顶层适配。

客户端使用本机实际窗口和GUI选项独立计算，未增加服务端共享布局或硬编码用户身份。1920×1080和2560×1440均保留旧版背包策略；没有退回旧版炼丹功能。

本机 verifyFull 构建成功，482项必需GameTests通过。独立后台客户端分别在无JEI/无光影、JEI 15.74/Oculus/Embeddium且Bliss实际启用条件下各验证48组：1920×1080 GUI3/4、2560×1440 GUI2/3/4、1280×900 GUI4，逐项打开背包、内视、功法、灵剑、百艺、识海、成就、设置。验证宿主画布、外框边界、分页宽度、控件横向边界及原生导航点击前后缩放不变。截图在渲染结束后采集并拒绝全黑画面。两轮failures均为空，鼠标捕获违规0，测试客户端正常退出。

最终本机证据：reports/backpack-layout-native-final-20261030/verification.json、reports/backpack-layout-jei-bliss-final-20261030/verification.json；相邻目录各48张截图。早期tick阶段截图含黑帧，未作为最终视觉证据。未替换UI美术，不适用新设计审批流程；未新建或修改工作台。

此轮针对背包及其分页，不代表全部工作台或全部模组兼容矩阵已重新验收。正式安装由tools/install_test_instance.py备份并校验；实际安装清单见reports/backpack-install-20261030.json。当前机器授权AXO实例存在且instanceId匹配，使用测试(1)目录；未发现规则中另一台机器的PCL路径。
