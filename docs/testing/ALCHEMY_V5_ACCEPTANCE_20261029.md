# 第13项验收记录（进行中）

基线 `5ebf576`，仓库 hbyaing/Myriad-Ascension，Minecraft1.20.1 / Forge47.4.10 / Java17。仅以实际执行的场景判定；历史通过不替代本轮结果。

## 验收矩阵

|项目|方法|状态|
|---|---|---|
|完整服务端回归|verifyFull，实际GameTest结束计数|最终verifyFull构建成功，实际481项必需测试通过，另388项纯核心检查通过|
|双玩家权限/重复领取|两个独立ServerPlayer及实际炉体/日志|通过；不冒称两个网络客户端|
|换维度/死亡/旁观|实际炉体会话与领取入口|通过；跨维度同坐标被拒绝|
|日志损坏隔离|截断本次测试批次文件，检查锁料与保留证据，恢复原字节|通过；不测试或承诺任意损坏/断电恢复|
|自然日出|真实世界时间逐tick推进，实际蕴月瓶ticker|独立namespace专项实际运行1项通过，未手动推进检查期间的时间|
|八种月相/瓶重载|真实服务端采样、库存持久化与无补发|通过|
|无JEI/无光影客户端|隔离后台世界及第二JVM|首轮与保存重进通过，GL0/鼠标捕获违规0|
|有JEI客户端|实际可选模组、目录/界面及原流程|两次独立JVM通过；复验要求炼丹界面中物品列表实际可见|
|外部光影|Oculus1.8.0+Embeddium0.3.31，核对实际活动shaderpack|ComplementaryReimagined_r5.9.3-unpacked启用，首次与第二JVM通过，GL0/捕获违规0；不代表其他光影包|
|研究工作台UI/知识裁剪|JEI+实际光影，候选/谱系/对照/认证、禁用/焦点/悬停/缩放/原生点击，两JVM|本轮32个截图检查通过，GL0/捕获违规0；不是所有可能研究数据的穷举|
|两个真实网络客户端|两个独立JVM，本机LAN/Forge TCP；独立专服测试另列|本机LAN通过：越权/领取重放/断线重连/死亡重生/换维度；不冒称独立DedicatedServer双客户端|
|热重载/故障组合|冻结版本、未领取/输出满、重启恢复与损坏边界|已有回归加本轮专项；任意跨文件回滚不在保证范围|

## 本轮发现与处理

1. 炉体stillValid只检查原世界的方块与坐标距离，未显式检查玩家维度。加入player.level()==level，避免另一维度同坐标被当成有效会话；新增回归。
2. JEI目录仍展示已废弃的五丹阶/四成色及旧百分比药效，改为V5一至九品与实际导入模板预览。旧物品变体只保留为既有配方查找键，创造取物仍走真实服务端导入，不在客户端伪造可服用凭证。妖兽内丹保持独立材料分类。
3. QA增加独立gameDir/output选项及实际JEI/光影探针，均在opt-in source set，禁止进入正式包。每次运行使用自有世界、隐藏窗口与鼠标保护，不操作用户客户端。

一次481项全量回归中两项既有御剑夹具失败（inertiaBrakeRollAndSourceRemoval、wallStopsChargeAndProtectsTarget），不删除失败记录、不放宽断言。新增测试改用独立GameTest批次，保留旧默认批次布局后最终481项全部通过；未修改原御剑断言。

## 保证边界

本轮未设计新的UI美术或改动已批准布局。核心/日志/物品/研究的服务端真值不向客户端直接复制。月华来源不替代当前天象。任何仍待执行的矩阵项必须保留，不能将本项标为整体完成或正式发布版。


## 实际执行与证据

- `verifyFull -PpythonExecutable=<本机QA Python> --max-workers=1 --offline --console=plain`：最终构建成功、GameTests编译成功且481项实际运行全通过。正式验收不带myriadExternalShaders。
- `runGameTestServer -Pv5DawnOnly --max-workers=1 --offline --console=plain`：独立namespace仅1项自然日出测试通过；所用空结构在GameTest资源，不进入正式包。
- `runClient -Pv5BatchCapture -Pv5MigrationCapture -Pv5CompatibilityCapture`，分别无JEI、加`-PjeiCapture`、再加`-PmyriadExternalShaders`；每种第二次加`-Pv5BatchResume`。三个隔离目录六个JVM，45张流程截图，均passed、鼠标捕获违规0；包括23类旧丹导入、实际服用、箱子取物、真实控温九品与凭证重载。
- opt-in `v5GameDir`、`v5Output`、`myriadNativePath`用于独立目录和当前Windows运行库，绝不指向用户存档。第三方JEI使用本机缓存15.59.0.212；光影依赖从已授权用户实例复制到QA目录，不修改或上传第三方JAR。原shader ZIP额外字段不能保证Java ZIPFS读取，使用保留许可证的解压目录。
- [证据目录](../integration/evidence/alchemy-final-acceptance-20261029/verification.json)含各次原始JSON和代表截图。JEI目录的before-gutter图片是发现问题时的旧布局，after-gutter及shaders目录是复验后的布局，不把旧图称作修复后通过。

### 保留的失败与修正过程

首轮御剑夹具两项失败已保留说明。首次自然日出用例与会改世界时间的其他测试共享运行且对白天判定时机做了过早假设；改为独立namespace，以真实日出后的服务端采样判定。检查前释放瓶内部分库存，确保有剩余容量，排除满瓶导致“停止采集”的假阳性。

外部光影首次被QA隐藏窗口的Redirect与Embeddium同位置Redirect冲突阻止；改用不占用调用的ModifyArg，在调用前设置隐藏/不聚焦提示，保留Embeddium窗口创建逻辑。中间尝试的构造函数Inject不被当前Mixin版本支持，未保留该失败实现。最终光影启动和重进通过，保护继续有效；全部仅QA源码，不进入生产。

研究工作台原QA要求“设置GUI4后实际比例必须等于4”，与本次有JEI时的自适应缩放不一致。保留失败JSON，并改为核对当前面板尺寸、真实窗口和JEI侧边预算算出的准确比例，继续验证原生点击位置；不放宽真实操作断言。


## 双客户端实际网络结果

两个独立游戏JVM，账号仅为隔离测试用V5Host/V5Peer，以本机loopback TCP连接真实Forge LAN服务。主端创建自有世界并发布临时端口；远端经ConnectScreen真实握手登录。主端菜单通过生产openAlchemy/NetworkHooks打开，后续开始/封炉/领取均通过真实客户端菜单按钮或正式AlchemyPacket。远端右键炉体并尝试开始、控火、封炉、三次领取，不能修改他人所有权或取得结果；主端重复领取后仅一份有效凭证。

远端断线后重新连接、死亡后发送真实重生请求、收到实际下界换维度同步后重放炼丹包，主端凭证与所有权不变。双方JSON passed=true、捕获违规0，并正常退出。第一个连通尝试超时在主端菜单打开前，没有被计为通过，失败JSON保留；修正夹具时机后复验。没有修改生产权限规则来放行测试。

为了避免两个Gradle进程争用同一工程的执行锁，主端由Gradle启动，远端通过 tools/run_v5_network_peer.py 复用**实际测试主端**已解析的启动命令，替换测试角色和用户名，并使用独立目录。只捕获V5Host BootstrapLauncher命令到本地临时JSON，不读取用户客户端命令或继承环境的快照；只在本地构造MOD_CLASSES。初次尝试过早导出Gradle启动元数据，尚未解析classpath，未保留该无效导出代码。

示例主端：`runClient -Pv5BatchCapture -Pv5NetworkRole=host -Pv5Username=V5Host -Pv5GameDir=build/v5-final-acceptance/network-host -Pv5NetworkOutput=<新的隔离输出目录>`。主端起来后仅筛选其V5Host BootstrapLauncher进程，保存ProcessId/CommandLine为本地JSON；再用工具的--host-command-json、--game-dir、--log启动远端。输出目录必须每轮独立，拒绝复用旧port.json；两个result.json都通过才算完成，JVM退出0不等于验收通过。

## 当前结论与剩余边界

第13项**首轮验收已完成，整体保持进行中**。本轮涵盖481项完整专服GameTests、1项独立自然日出、六次迁移/服药/重启客户端、两次研究/认证客户端、两个真实网络客户端，以及3处生产修复（跨维度会话、JEI旧分级说明、JEI侧栏空间）。不升级加载器、引擎/规则/schema/随机算法或协议，不执行新的迁移。

剩余需要单独开展：独立DedicatedServer部署下的两个真实客户端、更多规则/资源热重载和故障注入时点、其他JEI/光影版本组合及长时间压力。本机LAN不能替代独立专服双客户端部署；475/481历史或本轮测试都不代表任意断电/损坏/跨文件回滚保证。项目仍为开发进度快照，不作为正式完整发布版。


## 正式包与本机安装

最后普通build成功，CRC、生产入口及QA/JEI本体未打入正式JAR的检查通过。测试代码、隐藏窗口/鼠标保护、网络夹具仅在opt-in source set；本轮生产包不含这些类。

测试包 Myriad-Ascension-1.20.1-v5-13-first.jar，SHA256 `561c08db7bef42fe360da811fac76f5110f13f4a1205a27d97c7cd2b4d8163f8`。它是第13项首轮开发快照，不称正式完整发布版。

本机仓库记录的E盘PCL安装路径不存在，使用用户在本对话此前明确指定且核实有效的 D:/我的世界/.minecraft/versions/1.20.1-Forge_47.4.26/mods。安装工具先按modId识别并备份旧2a11c5e包，再原子替换并核对SHA；其余18个Mod前后哈希一致。E盘垃圾目录不可用，旧包保存在工程deliverables/mod-backups/2026-10-29/万界仙途自动安装-200434-7da05520。安装摘要见证据目录；完整包/备份/原始本机日志不随Git上传。没有关闭用户进程；新JVM启动才加载磁盘包，已运行游戏需要重启。
