# 源码导航

当前状态唯一入口：[CURRENT_STATUS.md](CURRENT_STATUS.md)。最新工作区清理与恢复方法：[docs/cleanup/20261004-workspace-cleanup.md](docs/cleanup/20261004-workspace-cleanup.md)。历史记录：[docs/archive/repair-history-20260927-pre-balance.md](docs/archive/repair-history-20260927-pre-balance.md)。本文件仅列入口。

平台：Minecraft 1.20.1 / Forge 47.4.10 / Java 17 / GeckoLib 4.8.4。
Java 根目录：`src/main/java/cn/blockforge/generated/generatedmod/`。

结构优化准备入口：[V5 现状、责任与首批改造边界](docs/development/STRUCTURE_OPTIMIZATION_PREPARATION_20261105.md)。该导航静态核对基线为 `05fd2f9`，不代表本次实机验收；长期方向见 [V0.3 参考稿](docs/development/ARCHITECTURE_EVOLUTION_REFERENCE_V0.3.md)，采用以实际工程为准。

| 模块 | Java 入口 |
|---|---|
| 噬灵虫与植物参考资源 | `entity/SpiritDevourerEntity`、`SpiritDevourerSpawns`、`client/SpiritDevourerRenderer`；`tools/import_spirit_devourer.py`、`import_oct07_pixel_references.py`；[整合记录](docs/audits/current/20261007_SPIRIT_DEVOURER_FLIGHT_PLANTS.md) |
| 注册与生命周期 | `GeneratedMod`、`registry/`、`event/` |
| 数值与服务端配置 | `balance/RealmBalance`、`DivineArtCosts`、`HeartDemonBalance`、`HeartMethodBalance`、`HeartMethodEffects`、`TribulationBalance`、`ChestBalance`、`GameplayConfig`；[平衡整改](docs/audits/current/20261005_BALANCE_FOLLOWUP.md) |
| 元神出窍距离耗灵 | `SoulAnchor` 保存精确本体坐标；`balance/DivineArtCosts.maintenance` 计算三维距离倍率；`DivineArtLogic` 每秒扣费；`DivineArtCatalog` 与语言资源同步说明 |
| 玩家能力与同步 | `CultivationDataImpl`、`network/CultivationSync`、`network/packet/cultivation/SyncPacket`、`CultivationVitalsPacket` |
| 日月停轮与旧存档迁移 | `DivineArtCatalog`、`WuXingItems`、`TimeStopManager`、`LegacyDivineMigration` |
| 功法几何判定与粒子接入 | `ArtCatalog`、`network/packet/cultivation/ArtReleasePacket`、`arts/ArtVfxCatalog`、`arts/ArtParticleLibrary`、`client/arts/ArtVisualEffects` |
| 三个被动功法槽 | `CultivationData`、`CultivationDataImpl`、`event/ArtPassiveTicker`、`network/packet/cultivation/EquipPassiveArtPacket`、`client/CultivationArtsPage` |
| 功法心法钓鱼与灵修者交易 | `acquisition/CultivationBookSources`、`event/CultivationBookEvents`；26 本缺漏书目共用价格/权重/来源定义，交互前按等级补充旧商人；[获取记录](docs/audits/current/20261005_CULTIVATION_BOOK_ACQUISITION.md) |
| 物品介绍、名称与品阶配色 | `event/TooltipEvents`、`text/ItemTextPalette`、`ItemTextCatalog`、`ItemGradeResolver`；`assets/wuxingxiantu/item_text_catalog.json`；导入器 `tools/import_item_text_spec.py`；[文案记录](docs/audits/current/20261005_ITEM_TEXT_PALETTE.md) |
| 获取来源审计 | `AcquisitionCatalog`、`WuXingItemSources`；`acquisition/SourceEvidence`、`AcquisitionSourcesEvent`、`EvidenceTrade`；首批提供者 `SpiritTradeEvents`、`event/WorldEvents`、`event/ServerTickEvents`、`HeartSwordSystem` |
| 灵修者空岛与道士职业 | `SpiritShrineManager`、`SpiritShrineLocations`、`TaoistEvents`；`data/wuxingxiantu/structures/spirit_shrine_island.nbt`；[职业修复](docs/audits/current/20261005_SHRINE_TAOIST_PROFESSION.md) |
| 炼丹炼器持久任务 | `network/service/CraftingJobs`（位置主键、owner 索引、到期队列）、`AlchemyService`、`CraftingRecoveryCommands`、`AlchemyFurnaceBlockEntity`、`RefiningForgeBlockEntity` |
| V5 炼丹批次与领取权威 | `AlchemyService.handleAlchemyPacket` → `AlchemyFurnaceBlockEntity.startV5Batch` → `alchemy/batch/V5Batch`、`BatchJournal`；领取 `BatchDelivery`；旧 `CraftingJobs` 与 V5 并存，不是 V5 的唯一记录 |
| V5 规则内核与恢复验证 | `alchemy/simulation/AlchemySimulation`、`SimulationRules`、`SimulationRuleLoader`、`SimulationSnapshotCodec`；测试 `src/gametest/java/cn/blockforge/generated/generatedmod/V5BatchGameTests.java`；模拟快照为服务端私有数据，不可直接作为客户端 DTO |
| 服务端交互校验 | `network/service/CompassService`、`AlchemyService`、`TalismanService` |
| 心剑与心铸 | `HeartSwordSystem`、`combat/HeartSwordCombatProfile`、`HeartForgeContracts`、`client/heartsword/` |
| Boss 与心境 | `boss/combat/`、`HeartDemonEntity`、`mindscape/MindscapeArenas`、`MindscapeProgression` |
| Boss 动作与普攻 | `boss/combat/BossMotionCatalog`、`BossBasicAttacks`；`data/wuxingxiantu/boss_motions.json`；`tools/author_boss_motions.py`、`check_boss_motions.py` |
| 十 Boss 七十二变技能 | `boss/combat/BossPlayerSkills`、`client/BossTransformSkillScreen`、`client/boss/BossTransformMotions`；试炼奖励 `MindscapeManager.complete`，创造丹 `CreationPillItem` |
| 技能粒子制作库（独立素材，未接入游戏） | Boss V3：`tools/vfx_library/build_v3.py`、`source-assets/skill-particle-library-v3/preview/index.html`；阵法与基础粒子扩展：`tools/vfx_library/formation_library_v1.py`、`source-assets/formation-particle-library-v1/preview/index.html`、交接 `docs/formation-particle-library-v1.md`；其余技能继续用V2，V1/V2保留对比 |
| Boss 与人物 HUD | `HudConfig`、`client/BossBarHud`、`CultivationSettingsPage` |
| 内视与心法界面 | `client/CultivationVisionPage`、`CultivationArtsPage`、`CultivationLayout` |
| 异火与剑身效果 | `fire/`、`client/weaponflame/`、`client/ExoticBurnRenderer` |
| 点燃原点定位与独立材质 | `client/ExoticBurnRenderer`、`ExoticBurnRenderType`；`coremods/exotic_burn_anchor.js`；`docs/audits/current/20260928_BURN_ANCHOR_RETIRE_WRAP.md` |
| 未来武器外观入口（当前无缠绕/拖尾/碎焰） | `client/weaponflame/WeaponFlameRenderer` 保留心剑桥接；`WeaponFlameHitEffects` 保留协议接收；`WeaponFlameResources` 保留配色/锚点 |
| 已退役外观与原始模块素材 | `source-assets/retired-weapon-flame-20260928/`（不编译/打包）；`source-assets/weapon-flame-modules-v1/README.md`；`reference_assets/weapon-flames-photon-r2/` |
| 0.2 参考图材质与验证 | `tools/extract_weapon_flame_reference.py`；`reference_assets/weapon-flames-0.2/`；`reports/weapon-flame-0.2/`；`docs/audits/current/20260928_VERSION_02_HANDOFF.md` |
| 炼制特效 | `OfferingItemEntity`、`client/OfferingItemRenderer`、`client/AlchemyFurnaceEffects` |
| 御剑 | `yujian/`、`YujianController` |
| 境界御空特殊/原版模式 | `realmflight/`、`client/realmflight/RealmFlightVisuals`、`MeditationModels`、`CultivationSettingsPage`；[实装记录](docs/audits/current/20261007_REALM_FLIGHT.md) |
| 御剑无固定速度封顶 | `yujian/YujianTuning`（档位速度与合法向量）、`YujianController`、`FlightCollision`（高速整段检测）、`FlyingSwordConfig`；[最新速度记录](docs/audits/current/20261005_FLIGHT_SPEED_UNCAPPED.md) |
| 御剑输入与本轮背包/轮盘修复 | `client/YujianClientController`、`yujian/YujianTuning`、`client/CultivationMainScreen`、`ArtRadialScreen`；[本轮记录](docs/audits/current/20261005_FLIGHT_UI_NURTURE.md) |
| 剑蕴灵力突破与空槽图案 | `yujian/SwordNurture`、`network/packet/yujian/SwordNurtureActionPacket`、`client/CultivationSwordPage`、`robe/EquipmentSlots`、`tools/build_equipment_slot_icons.py` |
| 渡劫 | `TribulationManager`、`TribulationState`（试炼和坑洞 SavedData、玩家检查点）、`event/PlayerLifecycleEvents`、`balance/TribulationBalance` |

审计交接：[AUDIT_FOLLOWUP_01_03.md](docs/audits/current/AUDIT_FOLLOWUP_01_03.md)。同步优化入口：`CultivationDataImpl` 的两类版本号、`network/CultivationSync`、`network/SyncMetrics`。2026-10-05 最新完整构建，全量 GameTest 137/137 通过；最新产物与验证边界见 CURRENT_STATUS。

心剑固定输入：`source-assets/contracts/heart_sword_v2_import.json`；隔离检查 `tools/check_source_package.py`（只复制必需输入，不带 reports/build）；Java 仅语法检查工具 `tools/CheckJavaSyntax.java`；源码打包 `tools/package_source.ps1`（不传 JarPath 即只打包源码，不触发构建）。

资源：`src/main/resources/assets/wuxingxiantu/`、`data/wuxingxiantu/`。原始资产：`source-assets/`、`reference_assets/`。

验证与报告入口：`build.gradle` 应用的 `gradle/verification.gradle` 定义 `specialistChecks`、`balanceReport`、`verifyFull` 和 GameTest 服务端运行约定；客户端专项配置仍见 `build.gradle`。网络 ID 表 `tools/network-message-ids.json`；Java 回归与报表 `src/regression/java/`；GameTest `src/gametest/java/`；构建警告预算 `tools/check_warning_budget.py`。以上是入口说明，历史通过率不代表当前版本验收结果。

