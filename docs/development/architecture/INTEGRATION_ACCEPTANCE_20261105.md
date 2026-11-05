# 第一轮结构优化：集成验收

生产源码包括 `4bea077` 的 A02—A04 改造；A07 可选示例已由 `6fcdc17` / `f5f15ba` 保存。随后尚有验收夹具/文档变更，未修改生产 Java、资源、日志或网络协议。

## 当前结果

| 要求 | 当前证据与范围 | 状态 |
|---|---|---|
| 实际生产开炉结果与消费者 | BatchStartResult、tryStartV5Batch；AlchemyService 两条生产分支与 boolean 包装；A02/A04 故障、占用与恢复测试 | 服务端通过 |
| 保持目标规则、跨领域消费者 | A03 五项矩阵/实际法宝与阵法测试，无改变资格或新框架 | 通过 |
| 持久化副作用与 I/O | A04 三次真实生命周期 JFR，分离当前批次/历史档案；隔离保护与 buffered NBT 完整性检查 | 通过 |
| 实际生产包与资源账本 | 最终 reobf JAR，字节分类、CRC、fixture/独立附属/验收 class 排除 | 通过 |
| 最小自动检查 | Java 17 空 classpath 编译边界、反例和 Gradle check 集成 | 通过 |
| 第二个实际接入需求 | 独立 Forge Mod/source set/命令消费者，302/302 入口及附属测试，开发 JAR 限制明确 | 源级集成通过 |
| 正式集成构建与回归 | verifyFull 默认范围，536/536，31 个任务成功，包含 architectureBoundaryCheck | 通过 |
| 真实网络与客户端反馈 | 两次独立客户端/新建世界/现有鼠标防捕获，实际包发送与生产菜单/反馈/私有同步八项验证通过，HUD 截图已查看 | 通过 |
| xxcs 安装、哈希与备份 | 用户已指定 Desktop/.minecraft/versions/xxcs；最终包哈希与目标一致，仅一个 wuxingxiantu 包 | 磁盘安装通过；不等同该实例整套运行验收 |

## 完整默认验证

执行 Java 17、Gradle 8.8、Forge 47.4.10：

```text
gradlew.bat verifyFull -PpythonExecutable=<实际 Python 3.12> --console=plain -Dfile.encoding=UTF-8
```

未带 entryUiOnly、admissionExample、客户端/外部光影开关；任务真正执行 `build`、专项/资源检查、Java 回归、边界检查、GameTest 编译和完整默认 GameTest 服务器。**536/536 required tests 通过，退出码 0**，服务器保存后退出。默认命名空间由 gradle/verification.gradle 明列；这不声称所有可选专用测试或外部 Mod 组合都运行过。

日志 `build/architecture-final-verify-full.log`，SHA-256 `90ff8156c9f14de5e560c53c21d1e2a57abb37c4a833c10a9e8d103afe0db217`。可机读结果：[final-verify-full](measurements/final-verify-full-20261105.json)。没有删除失败测试或放宽预算；日志中的隔离/损坏文件异常来自有意故障测试，不以 ERROR 字样直接认定整套测试失败。

生产包 `build/libs/myriad-ascension-0.2.jar`，89,151,974 bytes；SHA-256 `a8c9f3eb18eaa0d7b5340316f4d14c26c4ac210835806813b064b3df1417049a`。相对 A05 初始包增加 48 bytes，不声称本轮已缩小包体。新增/修改的是结果语义、异常保护和读取缓冲；视觉资源未删改。[最终资源账本](measurements/final-jar-20261105.json) 排除实际编译出的非生产类名，未发现混入项。

客户端专用开关接线完成后，再以当前最终 Gradle 配置执行默认 `build`：24 个任务成功，资源/专项/回归/边界再次通过，重建 JAR 哈希仍与已安装包完全相同。日志 `build/architecture-final-config-build.log`，SHA-256 `9c9792b4c2d555922725d318a53726c67cc48611d3b9c1951bbacd2b71064353`。这次只涉及可选测试源集/文档，不改生产 Java 或资源；因此没有重复整个服务端长测来冒充新增覆盖。

## 客户端与安装

使用 `runClient -ParchitectureClient`，只编译专用验收类与复用的 PointerSafety/NoPointerGrabMixin。独立工作目录 `build/architecture-client`，新建世界，不接管已有存档。首次 Gradle run DSL、随后夹具缺失导入的失败记录保留在 `build/architecture-client-acceptance.log` / `architecture-client-acceptance-recheck.log`；修正后的成功日志 `build/architecture-client-acceptance-fixed.log` SHA-256 `4afafa3200b096ff18398a06f5120c0e346bc1b75d2ced711a39478aeeef3e6d`。

通过八项真实链路验证：无菜单请求不扣料、生产菜单两端打开、实际 AlchemyPacket 编解码与处理完成托管、中文成功反馈收到、私有 V5Ref 不到客户端、重复请求反馈忙碌且身份保持。没有用直调服务端接口代替客户端发包。鼠标防捕获统计：1,665 帧、阻止 3 次捕获、违规 0；独立客户端与集成服务器均正常保存退出。[首轮客户端结果](measurements/client-network-20261105.json)。

首轮截图为真实炉面板正常显示，但面板遮挡 HUD，不能用截图声称动作栏文字当时可见。第二次运行增加正常关闭/重开菜单，取得 `success-hud.png` 与 `busy-hud.png`，人工查看确认中文成功/忙碌文字完整显示；没有隐藏界面绘制或伪造提示。第二次八项检查仍全过，1,602 帧、阻止 4 次捕获、违规 0，正常退出。日志 `build/architecture-client-feedback-hud.log` SHA-256 `2537d0dba93ed4e3ce40dcdf7e42009a856a84d82bbd6cdf338070191826156d`，结果 [HUD 复验](measurements/client-feedback-hud-20261105.json)。截图交付位置 `C:/Users/33140/Documents/ChatGPT/主要的/outputs/architecture-final-acceptance-20261105/`，未塞入生产资源。

此轮没有重做 UI，也没有声称外部光影/JEI 或 xxcs 全 Mod 组合已验证。完整目标要求的相关客户端/网络路径已经完成；这些额外组合不作为已通过结论，也不把范围扩大成所有未来客户端场景保证。

用户明确选择的目录为 `C:/Users/33140/Desktop/.minecraft/versions/xxcs`，实际 Forge 版本 47.4.28；独立开发验收使用 47.4.10。安装使用现有 `tools/install_test_instance.py --game-dir ... --backup-root ...`，生产目标哈希与上述最终 JAR 一致。该实例原先没有 modId=wuxingxiantu 的旧包，未改动其他模组，最终仅一个有效本模组 JAR。[安装清单](measurements/xxcs-installation-20261105.json)。

E 盘不存在，故安装清单/备份位置为 `C:/Users/33140/Documents/ChatGPT/主要的/outputs/architecture-test-install/backups/2026-11-05/万界仙途自动安装-175512-6d9fb394`。没有谎称备份了不存在的旧包，没有删除原有其他包、操作用户存档或强制关闭进程。磁盘安装不是正在运行的 xxcs 已加载；独立测试客户端退出后，用户可自行启动 xxcs 验收其完整模组组合。

## 交付边界

本轮仍先本地交付，未推送。最终验收夹具与截图不进入生产包。没有修改玩家培养/灵根路线、炼丹配方规则、目标筛选、批次数据格式或网络 ID；旧 boolean 入口仍可调用。异常操作的隔离引用属于既有 V5Ref，操作者仍须查原批次，不能把撤销代码当成恢复全部存档的办法。

## 完成条件复核

目标的六项有限交付分别由上表对应：实际生产接口与旧消费者保持、跨领域真实行为、持久化/恢复与三次 I/O、真实制品及资源账本、自动边界和独立附属接入、默认完整构建/回归/相关客户端网络/安装与交接。A01 原失败、A02/A03/A07 配置/编译失败均保留，没有用后续成功覆盖失败记录。

本轮结论限定为第一轮本地可验证结构优化完成，待用户体验验收与明确上传决定。没有发布到远端，没有自动改变其他 AI 的框架选择或给玩法开发设置等待锁。每个后续领域可独立演进，复用本轮边界前仍应检查实际需求。

收尾核对 [completion-audit](measurements/completion-audit-20261105.json) 的 56 项当前事实：三组真实测试结果/日志哈希、两次客户端报告、I/O 字节与次数、生产/安装/账本哈希、唯一有效本模组包、备份清单、CRC、实际编译的非生产类排除、生产输入未变化及文档相对链接。相对链接初查发现稀疏检出尚未落下已有 V0.3/准备文档，补齐原跟踪文件后通过，未伪造或改写缺失引用。机器审计不替代本表逐项的范围判断；相关 HUD 截图亦已实际查看。最终包检查时没有遗留助手启动的 Java 进程。

回退可按各本地提交撤销代码、测试和工具；既有炉体日志和 V5Ref 恢复义务仍须保留，不承诺 git revert 撤销任何已发生的游戏操作。图像资源、灵根玩法、存档协议及目标规则没有被本轮删除或替换。永久最佳架构、全服性能提升、任意时刻断电原子性及附属发行二进制兼容都不在本轮证据中。
