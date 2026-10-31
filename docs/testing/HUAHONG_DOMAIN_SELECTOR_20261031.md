# 化虹皮肤选择改用领域同款按钮（2026-10-31）

按用户明确指定，化虹皮肤触发按钮直接采用内视领域选择的现有完整控件：普通 `CultivationUi.Action`、高度 20、`chrome=1`，当前名称与领域页相同的内联展开标记共用居中排版。删除此前自定义 `renderWidget` 和右侧独立箭头；没有新增、修改图标贴图，也未改领域页自身。

领域原消费者位于 `CultivationVisionPage.init()` 与 `selectedDomain()`。普通、悬停、焦点、按下和禁用均由同一个 `CultivationUi.Action` / `referenceButton` 消费，使用 `wuxia/button/reference_atlas.png` 的领域样式。化虹既有八皮肤网格、自动露出完整网格、选择后收起以及 Enter / Esc 行为保留。

本次仅改 UI 控件，复用独立后台 `HuahongCapture -PhuahongUiOnly` 入口，无光影与 Solas 分别完成 44 张截图，覆盖 1920×1080 / 2560×1440、GUI 3 / 4、普通 / 悬停 / 焦点 / 禁用 / 展开及真实鼠标坐标和键盘逻辑。两端均无失败、GL 错误为 0、指针捕获违规为 0。QA 对真实活动控件增加了领域样式和 20 高度核对，继续验证八项同屏且不重叠。

实机原图与独立完整按钮裁图在 `reports/huahong-domain-selector-20261031/`，已实际打开两端状态总览、完整按钮和整窗。此前细箭头候选已撤回，仅保留本机候选记录，未安装或提交。工作台使用本次最终 UI 图，既有已验证飞行图沿用上一轮，不能将本次 UI-only 记录称为重新完成飞行或全部服务端测试。

本轮重新执行 `build` 和项目回归检查；正式包仍排除 QA 与鼠标保护。工作台上一版本的 122 个文件已备份至 `E:/垃圾/2026-10-31/化虹箭头融合/previous-workbench-c94d679`。安装备份和最终 SHA-256 由 `tools/install_test_instance.py` 记录于本机清单；磁盘安装与正在运行的 JVM 加载状态分开。

最终正式包 SHA-256：`e55c52de2e5fadff4e18f178d01fcf2cc91682d1eddd15fbece75663f3b8cdb2`，已安装到核对 instanceId 的 AXO“测试 (1)”实例，目标哈希一致、仅一个本模组 JAR，其他 64 个 Mod JAR 的哈希不变。旧包可从 `E:/垃圾/2026-10-31/万界仙途自动安装-173448-6d8138b7` 恢复。清单保存在 `reports/huahong-domain-selector-20261031/installation.json`。
