# 换电脑接续：自由炼丹V5

> 2026-10-29更新：以下03→04进度为历史快照。最新main已完成04–12，第13项正在验收；请以[最新制作清单](ALCHEMY_V5_CHECKLIST.md)和[第13项记录](../testing/ALCHEMY_V5_ACCEPTANCE_20261029.md)为准，不重复开发已接入项目。上传目标已改为hbyaing/Myriad-Ascension，见根AGENTS.md。

仓库：https://github.com/hbyaing/Myriad-Ascension.git，默认分支 main。先读根 AGENTS.md，检查分支/远程/未提交修改，fetch 后安全同步最新 main，最终合并上传main。不能直接覆盖已有工作；不要使用 reset --hard 或强推。

## 当前进度

- 第01项规范/基线完成；第02项实际原料观察面板完成，完整V5面板待后续。
- 第03项纯反应核心完成：提交 `2a11c5e`，PR47已合并，合并提交 `b65be8051d1f477ac64fae2565001bd2643f7463`。
- 第04项已实现真实输入、环境/异火轨迹、药材去品阶与创造模板规则、基础蕴月瓶；验证和限制见 docs/testing/ALCHEMY_V5_INPUTS_20261027.md。第05项进度见文末；第06项进度见文末；第07项进度见文末；08至11已完成各项切片；12继续统一迁移与全目录覆盖，13仍待制作。
- 第05项已将普通实体炉自由模式接入V5；固定丹方/天地为炉等待08统一。不要把工作台、纯核心或专服核心夹具当成实际炼丹接入。

最新设计：[DESIGN_V5_0_R1.txt](../specifications/alchemy-v5/DESIGN_V5_0_R1.txt)，包含原稿全文和本次追加修订，原始V5.0单独保留。SHA256（Git仓库LF字节，换机校验先统一LF） `8350e80da997b007f2c101adca774847ec43fbdd79b86c926c65afcea3e91e39`。
制作及新增子任务：[ALCHEMY_V5_CHECKLIST.md](ALCHEMY_V5_CHECKLIST.md)。用户2026-10-29已批准全部旧丹迁入V5、删除阶级并用一至九品；现行协议见 docs/testing/ALCHEMY_V5_MIGRATION_20261029.md，原始数据须保留备份。

## 本次新增设计要点

夜晚/月相分别影响连续环境参数和条件标签，同一来源不能重复加药力；天亮从实际变化步更新，不追溯取消已完成反应。连续窗口中断则重新计时，已吸收资源按真实规则保留/消耗，结构按维持条件演化。跨夜路线允许有代价温养，不保证所有阶段可暂停。

蕴月瓶存有限月华及来源，可白天供料，不能伪造当前夜晚/满月天象。参数/标签/响应与显示数据驱动，规则白名单执行。容量、速率、损耗、配方、美术未定；这些提议本次仅更新文档，未注册道具或改游戏代码。

## 接续入口和验证

- Java17、Minecraft1.20.1、Forge47.4.10、modId=wuxingxiantu；换机重新核对构建，不迁移加载器。
- 先读 accepted-decisions.json、accepted-plan.md、ALCHEMY_REWORK_PROPOSAL_20261027.md、docs/previews/alchemy-rework/README.md，以及核心规格 docs/testing/ALCHEMY_V5_CORE_20261027.md。
- 核心在 `src/main/java/cn/blockforge/generated/generatedmod/alchemy/simulation/`；正式 AlchemyService/炉体/天地为炉为后续接入点。参数观察层在 alchemy/observation 和 client/AlchemyObservationView。
- 上次 verifyFull 构建通过、GameTests编译通过且实际364项全通过，新增纯Java388项通过；网页10247项额外回归通过。历史通过不代表换机最新代码整体验收。
- 正常命令核对后用 `.\gradlew.bat verifyFull`；Python/Java本机路径按新电脑配置，不照搬旧绝对路径。完整专服验收不能启用 myriadExternalShaders。
- 网页通过仓库工作台总入口启动，不能复用旧机器127.0.0.1地址；根AGENTS指定路径不可用时核对仓库便携入口 docs/previews/启动全部工作台.py。网页不是游戏结算。
- 专服、JEI/光影、多人、自然生成/旧档等仍按各阶段真实验收；原恢复包独立克隆问题记录保留，不因源码可见就宣称恢复包已验收。

## 旧电脑磁盘状态（不是新电脑路径）

正式包 Myriad-Ascension-2a11c5e.jar 已安装到用户指定 D:/我的世界/.minecraft/versions/1.20.1-Forge_47.4.26/mods；SHA256 `5550bf6f3791e4a13a423f9dba79ebdc70652c4efe368ccd8ffdefee5bebe695`。旧包已备份，其余18个Mod哈希未变；运行中的JVM须重启加载。此次文档修订没有新JAR，也不执行安装。

本地 deliverables、reports、Gradle缓存和工作台服务器进程并非全部跟随Git上传。需要新包时在新机器构建；不要声称这些本地文件已同步。

## 2026-10-27 第04项本机接续

本次功能分支 codex/alchemy-v5-inputs-20261027，基线main 3c571f7。第04项实现和限制详见 docs/testing/ALCHEMY_V5_INPUTS_20261027.md；最新 clean verifyFull 实际374项必需服务端测试全通过，警告119/119。原AXO实例发现失败后，用户指定PCL目录；已安装到 E:/我的世界/PCL 正式版 2.10.9/.minecraft/versions/1.20.1-Forge_47.4.26/mods，正式JAR SHA256见本项规格。实际PCL客户端20张创造页+取物NBT验收通过，测试进程正常保存退出，临时保护包已移除，保留GeckoLib和Moonlight依赖。下一项05开始实际炉体事务，继续保护旧物品数据，不能误以为当前炉体已调用V5。

## 2026-10-27 第05项接续

分支 codex/alchemy-v5-batches-20261027，基于第04项分支（PR49）。普通炉自由炼制已接V5批次；当前封存药液不可服用。读 docs/testing/ALCHEMY_V5_BATCHES_20261027.md，下一项06事故与副产物；不要把第05项当作九品/能力或全部入口完成。用户要求后续自行搭建独立后台客户端，不捕获真实鼠标。本项可选 -Pv5BatchCapture / -Pv5BatchResume 两阶段测试使用仓库外 work/v5-independent-client，正常保存退出；QA不进入正式JAR。磁盘安装仍使用用户指定PCL目录，后台验收不启动用户PCL。

## 2026-10-28 第06项接续

分支 codex/alchemy-v5-incidents-20261028，基于第05项提交4f2afe6（PR50，仍与04叠加）。普通炉新批次已接事故/警兆、付费控制、丹灰/废丹/药渣、设备磨损/修复。默认炉内事故，不新增世界破坏。旧批次规则保持，网络协议98。读 docs/testing/ALCHEMY_V5_INCIDENTS_20261028.md；下一项07能力编译和九品，固定丹方/天地为炉仍归08。

独立后台实机：`gradlew runClient -Pv5BatchCapture -Pv5IncidentCapture --offline`，同一隔离profile创建自己的新世界，真实C2S四组场景与GUI2/3画面，NoGrab保护；测试角色启用飞行保持，不操作用户鼠标。正常退出并保存；QA不得打包。专服长温养600逻辑步不是自然夜晚10分钟实机验收，后者和多人/光影/任意强杀继续按13。

## 2026-10-28 第07项接续

分支 codex/alchemy-v5-effects-20261028，基于第06项e2e3511（PR51）。新V5普通炉批次已实际编译九种有限模块与一至九品，统一物品v5_pill及真实服药/恢复路径完成；网络协议99。旧批次未绑定EffectPolicy继续原结果，不默改旧丹或授予职业称号。详细数据/预算/持久化边界见 docs/testing/ALCHEMY_V5_EFFECTS_20261028.md。下一项08多入口统一，随后09–11知识/研究/认证、12迁移/覆盖/平衡、13完整验收。

能力定义在alchemy/effects.json；批次绑定策略、反应完成快照ReactionCore和公开PillResult，升级编译算法必须保留旧策略解释器或显式迁移，不能同版本改旧结果。服务端DoseIntent+玩家消费凭证确认后才提交DosesUsed；物品公开NBT不是权威。后台实机用 -Pv5BatchCapture -Pv5EffectsCapture，两次JVM第二次加-Pv5BatchResume；真实进食用本测试客户端内部按键状态，不捕获真实鼠标。

## 2026-10-28 第08项接续

新仓库 https://github.com/hbyaing/Myriad-Ascension 的 main 是统一上传目标，完成并验证后合并推送main。第08项将普通炉固定丹方和天地为炉/副药适配到V5Batch，保留旧任务兼容及天地炼器。读 docs/testing/ALCHEMY_V5_ENTRIES_20261028.md；新入口私有元数据Entry/Recipe，天地玩家托管见PortableBatch和BatchJournal.portable，玩家付款凭证确认后才推进；协议100。下一项09丹方/知识/技艺，不把旧方例输出当作新物理成丹承诺。后台验收参数 -Pv5BatchCapture -Pv5EntriesCapture，第二JVM加-Pv5BatchResume，禁止捕获真实鼠标。

## 丹道研析台UI接续（2026-10-28用户批准）

V2已获明确批准并实装新工作方块/菜单/六页界面，真实基础公开样本观察与去重个人记录可用。见docs/testing/RESEARCH_TABLE_UI_20261028.md；协议101，正式研究/知识树/反推/谱系/认证仍按09–11继续，不能把UI交付算作全研究系统完成。原有统一工作台新增research.html。后台参数 -Pv5BatchCapture -Pv5ResearchCapture，第二JVM加-Pv5BatchResume；复用NoGrab，不移动真实鼠标。

## 2026-10-28 第09项基础链路接续

阅读docs/testing/ALCHEMY_V5_KNOWLEDGE_20261028.md。ResearchCurriculum为服务端受控教学定义，FormulaKnowledge.understanding存既有PlayerPersisted理论与冻结模板，ResearchPractice在BatchJournal终态提交后向既有PlayerCrafts追加前128批展示摘要；不改物理、不授实践XP或认证。三组教学模板不是全部丹方完成映射。协议102；同一研析台后续10反推/自创/谱系、11独立认证，12全量覆盖。后台QA仍用-Pv5BatchCapture -Pv5ResearchCapture及第二JVM-Pv5BatchResume，创建research-knowledge-*独立世界，research09-qa.json控制文件及research-knowledge-client-validation输出；禁止鼠标捕获。

## 2026-10-28 第10项接续

读docs/testing/ALCHEMY_V5_INFERENCE_20261028.md。ResearchEvidence读取本人前128条实践索引与权威批次，只有新ResearchStart完整批次可作严格对照。ResearchMethods绑定公开规则内容，ResearchInference只用授权观测/公开目标，ResearchDifficulty将歧义与操作窗口分开，ResearchLineage在既有PlayerCrafts保存32个以内不可变版本/联合点。协议103，菜单文本每栏640字。不要把观测支持当独立认证/连续有效域，也不要把20%便捷假设步长变成通用降级保证。下一项11留出集/全失败认证账本。后台-Pv5BatchCapture -Pv5InferenceCapture，第二JVM加-Pv5BatchResume；独立research-inference-*存档/research10-qa.json，禁止鼠标捕获。


## 2026-10-29 第11项接续

当前唯一上传目标 https://github.com/hbyaing/Myriad-Ascension 的main，按AGENTS安全同步后直接提交主线。第11项及炉体跨区块形状越界修复见 docs/testing/ALCHEMY_V5_CERTIFICATION_20261029.md。认证使用既有PlayerCrafts账户和同一BatchJournal权威凭证扩展，网络104。不要把旧Rank/XP折算成新认证，不要给旧药材补造新来源。12迁移全量覆盖、13完整兼容/故障验收仍待做。最终测试、客户端重启与正式包校验见该规格和对应evidence目录。


## 2026-10-29 第12项完成接续

旧丹统一V5一至九品，不再保留并行旧丹结算。读 docs/testing/ALCHEMY_V5_MIGRATION_20261029.md 及 evidence/alchemy-migration-20261029/validation.json。网络105；旧V5效果策略1和输入schema1继续按冻结定义读取，新规则不回灌旧成丹。全目录与680映射、475项完整专服、12项隔离迁移专项、后台独立客户端重启与实际消费通过，捕获违规0。

生产包SHA256 `5cda7c09908d8ff75f9f888d72342708ec898752bee35567cc36fcdb69da3274`，已安装到 E:/我的世界/PCL 正式版 2.10.9/.minecraft/versions/1.20.1-Forge_47.4.26/mods；其他7个模组JAR哈希未变，需重启加载。QA类与迁移测试资源均不在正式包内。主线仍为 hbyaing/Myriad-Ascension 的main；39fbddf是本项提前上传的开发检查点，最终状态见包含本记录的提交。下一项13尚未开始。
