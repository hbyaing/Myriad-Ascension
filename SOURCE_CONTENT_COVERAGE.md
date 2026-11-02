> 2026-11-02：当前覆盖清单已移除退役的装饰/特殊丹条目；旧档退役映射与历史验收证据保留，详见 docs/integration/REMOVED_DOLLS_20261027.md。

> 2026-10-21：本轮状态以 docs/integration/acceptance-baseline.json 和 ACCEPTANCE.md 为准。下方保留此前阶段账本；“本轮/未提交/正在实装”等为2026-10-20历史语境，不是当前源码缺失证明。新一轮完整验收正在执行，不引用旧255项结果作为当前通过证据。

# 仙途 r115 内容融合覆盖账本（持续维护，非完成证明）

来源固定 ff253fa45ec919dc4fe9cc1d7587971d1f70a14c；目标框架为五行仙途 v0.2.1。已验证基线为0e48c62，255项必测通过。当前未提交新增模块尚无完整总验证结果：上次298项测试运行在第三批中断，不能计为通过；正在补齐遗漏并做客户端验证。

| 来源活跃入口 | 当前目标映射 | 状态/证据边界 |
| --- | --- | --- |
| ModItems/ModBlocks四档灵石、SpiritVeinFeature、SpiritAura | economy/SpiritStoneGrade、world/连片矿脉、探测罗盘、veincompass | D01/D02已接；手持方向/距离/浓度读取五行唯一灵脉数据，不另叠修炼倍率；新区块分布及HUD待实测 |
| HerbType、WildHerbFeature、HerbItem药龄 | plants/ImportedHerbs、HerbAge | 18物种和药龄已接；自然生成实玩待验收 |
| PillType、InheritanceScroll、Codex | 目标alchemy、knowledge | D06–D16/D22已接，255测试基线；新版控火按用户要求暂存 |
| BagData/Inventory/Paging/Filter/BagMenu/BagMagnetEvents | storage/完整模块 | 本轮已接线，待总验证与原生客户端实看 |
| SwordArrayServer、SwordChartItem、四剑、client/formation | formation/完整模块 | 本轮已接线；源仅有阵景与收费，无伤害/实际防御；模型/光影待客户端验收 |
| BlackBoarEntity/BeastRegistration、BeastBlood/Hide | importedcreatures | 本轮已接；掉落实际辅材用途接普通丹炉/天地为炉/炼器；待总验证 |
| SpiritBathBlock/BE | importedcontent | 目标已默认炼气，无来源凡人层；保留四草药浴补修为并服从目标修为门槛，禁止直升筑基 |
| ForgingManager/BlankType.activeTypes、ArtifactGrowthEvents | artifacts | 六类法宝已注册，铜/铁（兼容玄铁）/紫水晶/下界合金四类基材，六配方进入原炼器任务，1..3模型档从五行精炼/成色推导；温养/本命/唯一主人沿用目标；待统一验证 |
| AuxMaterial/ArtifactAffix | artifacts | 15辅材原生存配方；D23/D24统一锋锐/坚固候选，经原强化入口采用；独有雷击/汲血/护盾及单次护甲韧性保留，不另叠攻击/最大耐久树；待验证 |
| RefinementRules.consumeCosts(policy2)、refine_seal配方 | EquipmentCrafting action22 | D26确认：玉符可替代额外一晶，优先兼容旧晶支付，不改变五行精炼树；待验证 |
| ForgingAnvilBlockEntity coolant kind2、quench_essence配方 | 原器台索引3淬炼与CraftingJobs escrow | 首次淬灵＋3导灵，同任务持久材料与结果托管；中断退款、成功不退；待生命周期测试 |
| ArtifactNameSubmitPacket→finishNaming | 原版铁砧＋ArtifactNaming | D27明确沿用铁砧命名/上限/费用，专用赐名界面暂存；主人与成长NBT保留，阻止异主合并/洗主人；待真实AnvilMenu测试 |
| ActionPacket ACTION_SENSE→DivineSense.toggle | divinesurvey＋五行识海入口 | D25确认：化神且开识海，8×8区块异步矿物/结构探查；统一神识账户，启动/复查8点及每秒1点，可选Xaero默认关；不移植持续穿墙敌人高亮；待验证 |
| ClientSetup L键→ACTION_QTE_START→QteManager | 五行CultivationLogic/打坐/心魔门槛 | D28已确认保留，接入五行统一修为结算及心魔门槛；服务端验证游标时序与会话，正在实装 |
| CommonEvents→NascentSoul.tryEscape/applyMeleePressure | soulpreservation＋目标伤害规则 | D29已确认：加入25分钟冷却的元婴致命遁逃；伤害/破盾沿用五行；正在实装，不能绕过强制死亡/虚空/心魔/渡劫结算 |
| 目标原MerchantOffer付款 | economy/SpiritTradePayments及mixin | 本轮统一四档货币填付款栏、原价与库存规则、找零；待总验证 |

## 来源停用与遗留资源

- RiftScrollItem明确“Save compatibility only…cannot teach or cast”，当前use不存在；一指裂地不是r115活跃法术，不重启其历史巨伤/破地逻辑。
- BlankType.BANNER/SEAL/CAULDRON在active()返回false；不能将九种登记误称九种可制作。
- source ClientSetup artifact_rank只返回1..3；rank4..7孤立资源不等于源活跃七档成长。
- 新UI与四阶段控火暂存，不因全量内容融合目标绕过用户“交付先用老UI”的明确要求。

## 完成仍欠的证据

当前新增模块总构建/GameTests、真实客户端模型与阵法含/不含外部光影、自然矿脉/草药分布和旧存档/多人实玩仍待完成。D23–D27已明确取舍；现有通过数不能证明尚未覆盖的运行场景完成。
