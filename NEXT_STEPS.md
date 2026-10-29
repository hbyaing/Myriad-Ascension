> 2026-10-29最新接续：仓库为 https://github.com/hbyaing/Myriad-Ascension ，目标main。炼丹V5第04–12项已接入，第13项首轮验收完成，整体仍为进行中；下一步独立专服双客户端与更多热重载/故障注入。先读[制作清单](docs/integration/ALCHEMY_V5_CHECKLIST.md)、[第13项验收记录](docs/testing/ALCHEMY_V5_ACCEPTANCE_20261029.md)及根AGENTS.md。以下“下一项04”等为历史记录，不能用作最新进度。

> 2026-10-27接续历史：当时第03项纯核心完成，下一项04；对应[换电脑接续说明](docs/integration/ALCHEMY_V5_HANDOFF_20261027.md)和[V5.0-R1修订TXT](docs/specifications/alchemy-v5/DESIGN_V5_0_R1.txt)保留供追溯。

当前按用户确认的[16项工作清单](docs/integration/CHECKLIST-16.md)执行；数据与迁移边界见[第3项核对](docs/integration/DATA_AND_SAVE_AUDIT.md)。

> 2026-10-21继续：项目仍为开发进度快照。当前功能分支为codex/myriad-integration-foundation-20261021；恢复问题、实际克隆证据及本轮验收进展见docs/integration/RECOVERY.md与ACCEPTANCE.md。以下2026-10-20交接记录保留，不作为最新整体验收通过结论。

# 万界仙途 · Myriad Ascension 进度交接（2026-10-20）

用户要求因额度不足快速收尾，停止扩展。本提交是开发进度快照，**不是已经完整验收的发布版**。

- 基于五行仙途框架，吸收 hbyaing/xiantu r115 内容，modId 保持 wuxingxiantu 以兼容存档。
- 交付路线：`codex/myriad-ascension-delivery-old-ui` 保留老UI；新UI独立暂存在 `codex/myriad-ascension-new-ui`，不得直接合入其未验收草药界面草稿。
- D01–D30 用户已确认取舍，详见 docs/integration/accepted-decisions.json 与 SOURCE_CONTENT_COVERAGE.md。
- 已接入储物袋、剑阵、黑野猪、药浴与人偶、四级交易、法宝与辅材、护器玉符、神识探矿、灵脉罗盘等；D28 蒲团小游戏、D29 元婴自动保命、D30 副药组合已保存源码。
- D30 普通炉使用闲置材料槽的最多三类副药，每类一株；天地为炉通过旧界面明确选择，不扫描消费背包额外草药。原丹药的使用和境界门槛继续有效。
- 网络协议已升至95，客户端与服务端须使用同版本。

## 验证边界

- 旧阶段2已完成255项必需GameTest；历史结果不代表本次快照全部通过。
- 9项无外部光影客户端截图曾通过，且没有真实鼠标捕获；它早于后续 D28–D30 和罗盘布局修复。
- 本轮 compileJava / compileGameTestJava 已通过；最后药龄主方校验调整须随最终编译复核。
- 后续完整服务端测试尚未通过：先前测试混入客户端Oculus依赖导致启动失败，不能把Gradle退出0当作通过。
- Complementary原ZIP存在Java ZIPFS不接受的非UTF8附加条目；本地已无损提取 shaders/ 与License为目录，尚未完成实际光影验收。第三方运行依赖不上传。
- 自然世界矿脉/草药/生物及保存重载验收夹具已准备，尚未执行完成。

## 下一步（按顺序）

1. 在本分支运行 `verifyFull --offline -Dnet.minecraftforge.gradle.check.certs=false`，**不要带 myriadExternalShaders**。必须看到本次日志 `GAME TESTS COMPLETE` 与全部必需测试通过；修复 D28/D29/D30 真实入口回归。
2. 单独运行 opt-in 客户端：`runClient -PmyriadCapture -PmyriadExternalShaders -PmyriadExpectedShaderPack=ComplementaryReimagined_r5.9.3-unpacked`，配置本地原生库路径；检查10项截图，尤其蒲团小游戏、罗盘与副药按钮。禁止抓取真实鼠标。
3. `runClient -PmyriadCapture -PmyriadWorldgen` 单独执行自然生成与存档重载验收，依据 worldgen-acceptance.json 核验，不以注册存在代替自然生成证据。
4. 确认副药费用、失败消耗、取消退还、药龄与冰火毒附击后再产出正式JAR；排除QA代码/鼠标保护/测试依赖，不覆盖已验证阶段2备份。
5. 继续对照 SOURCE_CONTENT_COVERAGE.md 收尾源内容，真实重复再向用户询问，禁止另建重复的修为、灵气或主人账户。

本机构建需JDK17、Gradle8.8和专用qa-python；本机完整日志在上一级 work/myriad-wrapup-compile.log。依赖保持离线，不提交构建缓存、存档和第三方光影包。
