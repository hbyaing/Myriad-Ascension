# 16项融合工作清单（2026-10-21）

本表是当前执行入口，保留用户的顺序。以五行仙途为框架，仙途r115填充内容；当前为开发进度快照。代码存在、自动测试通过、客户端验收是三个独立状态。下列文件均在当前源码树实际存在；文件存在不等于功能已完整验收。

源码前缀：`src/main/java/cn/blockforge/generated/generatedmod/`；测试前缀：`src/gametest/java/cn/blockforge/generated/generatedmod/`。

| 顺序 | 工作项目 | 实际源码/测试入口 | 状态与剩余验收 |
|---|---|---|---|
| 1 | 整理融合源码与分支 | RECOVERY.md、verify_recovery_bundle.py | 已完成恢复验证；原恢复包缺失父提交问题保留，老UI/新UI/早期融合分支独立保留；误推main已通过cfe6fee恢复为41ae51d同一文件树 |
| 2 | 统一融合进度表 | 本表、accepted-decisions.json、SOURCE_CONTENT_COVERAGE.md | 本轮建立16项执行映射；D01–D30是已确认决策，历史“已接入”不当作全部验收 |
| 3 | 统一底层数据与存档 | CultivationDataImpl.java、event/PlayerLifecycleEvents.java、progression/PlayerCrafts.java、crafting/EquipmentBond.java；IntegrationDataGameTests.java | 已核对唯一数据入口及兼容边界，新增4项测试通过；真实磁盘重载/旧存档仍归第15项，见DATA_AND_SAVE_AUDIT.md |
| 4 | 统一灵石与经济 | economy/SpiritCurrency.java、SpiritTradePayments.java、crafting/EquipmentCrafting.java、formation/SwordArrayUpkeep.java；MyriadFoundationGameTests.java、SpiritTradeCurrencyGameTests.java | 四档与原子找零已接入；主要扣费/兑换/交易入口已核对，见ECONOMY_AUDIT.md；新增兑换与混合付款测试，实机/完整退款仍待验收；剑阵5极品等值结算是已确认规则 |
| 5 | 完善灵脉与草药 | world/SpiritVeinFeature.java、SpiritVeinData.java、plants/ImportedHerbs.java、HerbCultivationData.java、divinesurvey/DivineSurvey.java；MyriadFoundationGameTests.java、VeinCompassGameTests.java | 18来源草药资源链已逐项核对；已修复新矿脉跨入旧邻区块的写入边界，新增4项回归通过；自然分布/原档边界实测待第15项，见HERBS_VEINS_AUDIT.md |
| 6 | 完善炼丹融合 | alchemy/FormulaKnowledge.java、alchemy/auxiliary/AuxiliaryAlchemy.java、network/service/AlchemyService.java；AuxiliaryAlchemyGameTests.java、AlchemyMaterialTransferGameTests.java | 本轮核对140条丹方/25条分阶配方、成色/知识/传承与D30；新增3项真实施法/满包退款/失败结算回归通过，交互实机待做，见ALCHEMY_AUDIT.md |
| 7 | 完善炼器与法宝 | crafting/RefiningOperation.java、EquipmentBond.java、artifacts/ArtifactAux.java、ArtifactInfusion.java；EquipmentCraftingGameTests.java、BoundEquipmentCraftingGameTests.java、ArtifactNamingGameTests.java | 本轮核对15器方/6器型/15辅材、主人/成长/赐名/托管，新增3项边界回归通过；实机待做，见REFINING_ARTIFACT_AUDIT.md |
| 8 | 完善修炼与保命机制 | meditationqte/MeditationQte.java、soulpreservation/SoulPreservation.java；meditationqte/QteGameTests.java、SoulPreservationGameTests.java | D28–D29开发核对及13项专项通过；真实蒲团界面隔离截图通过；完整特殊死亡/重连实机仍待第15项，见CULTIVATION_SURVIVAL_AUDIT.md |
| 9 | 完善术法融合 | arts/SustainedSwordArts.java、ArtCatalog.java；SustainedSwordArtsGameTests.java | 补齐每5tick五把追踪剑并保留范围伤害，新增2项回归；学习/槽/敌我/供灵共用主框架，见SWORD_ARTS_AUDIT.md；客户端光影留第14项 |
| 10 | 完善诛仙剑阵 | formation/SwordArrayServer.java、SwordArrayUpkeep.java、formation/client/SwordArrayClient.java；formation/SwordArrayGameTests.java | 固定阵位/半径15/四剑到剑幕时序/旋转/等值付费/清理已核对，6项专项通过；实际防御与伤害仍独立设计，见FORMATION_AUDIT.md |
| 11 | 完善储物袋与内容物品 | storage/StorageBagItem.java、BagMagnetRules.java、importedcontent/SpiritBathBlockEntity.java；StorageBagGameTests.java、ImportedCreaturesGameTests.java、ImportedContentGameTests.java | 袋/磁铁/妖兽/药浴已核对；旧阶段专项数量为历史证据，退役内容不列为现有功能；实际袋/筛选/内容模型截图通过，真实重启多人仍待第15项，见STORAGE_CONTENT_AUDIT.md |
| 12 | 统一老UI与说明 | knowledge/KnowledgeItems.java、client/HerbProcessingTableScreen.java、client/EquipmentWorkstationScreen.java；EntryUiGameTests.java、KnowledgeIntegrationGameTests.java | 老UI布局保留；补齐JEI五丹阶×四品质，隔离客户端10张截图通过；JEI双缩放专项结果见UI_INTEGRATION_AUDIT.md与本轮验证报告；非全部客户端状态验收 |
| 13 | 完整服务端回归 | verifyFull、tools/verify_gametest_log.py | 最终339/339必需测试通过，28任务全部重跑；119/119警告、6项日志门禁测试及154非生产类型排除检查通过；见VALIDATION-STEPS-8-13-20261021.md |
| 14 | 客户端与光影验收 | ACCEPTANCE.md | 尚未执行本轮无/有光影模型、护罩、遮挡与渲染状态恢复验收 |
| 15 | 自然生成、存档与多人验收 | DATA_AND_SAVE_AUDIT.md、ACCEPTANCE.md | 尚未执行真实自然生成、磁盘重载、旧档、多人全矩阵；NBT往返不等于此项通过 |
| 16 | 正式打包与发布 | build.gradle、jar-audit-20261021.json | 基线QA排除检查通过；只有恢复快照，无正式完成包；后续改动后重新核验客户端/服务端版本 |

执行方式：第3–12项各自保留“源码核对、自动测试、实机、待决重叠”证据，不把第13项全绿反填为所有功能完成。第14–15项作为集中实机验收。发现未确认重复项先询问用户，不自行取舍。

本轮没有新增融合取舍。旧仙途命名空间迁移未实现，应先取得独立备份旧档与明确字段映射，再作为迁移任务实施；不得直接用融合包打开唯一原档。
