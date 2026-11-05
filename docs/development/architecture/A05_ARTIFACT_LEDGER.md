# A05：生产制品与资源账本

生产输入版本 `1016720`（含 A01—A03），Java 17 / Gradle 8.8 / Forge 47.4.10。新账本工具为本次未提交工具变更，不参与生产源码。先本地交付。

后续更新：以下为初始账本。包含 A04 改造的最终包为 89,151,974 bytes，默认完整 verifyFull 536/536 通过；已按用户后续指定安装至 Desktop/.minecraft/versions/xxcs，哈希核对通过。原 E 盘路径缺失不再阻止安装，见 [最终验收](INTEGRATION_ACCEPTANCE_20261105.md) 与 [最终账本](measurements/final-jar-20261105.json)。

## 构建与复核

`gradlew.bat build -PpythonExecutable=<实际 Python 3.12> --console=plain -Dfile.encoding=UTF-8` 退出码 0，BUILD SUCCESSFUL，23 个任务。资源 15 项、11 组专项检查、regressionTest、balanceReport、GameTest 编译及 reobfJar 执行通过。此前独立入口 GameTest 297/297 通过；本次 build 不执行服务端 GameTest，不能称为完整 verifyFull。

日志 `build/architecture-production-build.log`，SHA-256 `eb03cf9da7d80867a2450d9b2c1326119e2a1471e926ac6171ed930c7747f5b5`。初始工作树为稀疏检出；按实际脚本补齐 heart_sword.bbmodel、retired-weapon-flame、weapon-flame-modules 和 weapon-flames-0.2 参考目录后运行，不绕过专项检查。

制品：`build/libs/myriad-ascension-0.2.jar`，SHA-256 `25fd539822214cd344964c66878ed5791825413db434218b35bd6358d79304e0`。只读统计工具：`tools/architecture_jar_ledger.py`；可重跑：

```text
python tools/architecture_jar_ledger.py build/libs/myriad-ascension-0.2.jar --report build/architecture-jar-ledger.json
```

完整结果：[JSON 账本](measurements/jar-baseline-20261105.json)。它记录生产 HEAD、工作树是否有变更、制品哈希、CRC、分类与重复资源路径。字节单位为 byte，不混用 MB 与 MiB。

## 实际体积

| 内容 | 未压缩字节 | ZIP 压缩载荷字节 |
|---|---:|---:|
| 全部条目，共 4,372 项 | 170,203,635 | 见 JSON（文件头等开销另计） |
| 纹理，1,200 项 | 74,364,160 | 70,483,514 |
| Java class，1,482 项 | 9,351,932 | 3,851,337 |
| mesh，6 项 | 34,036,361 | 2,967,345 |
| models，638 项 | 25,988,890 | 3,166,119 |
| animations，10 项 | 8,472,466 | 254,320 |
| data，643 项 | 884,176 | 238,181 |

JAR 文件大小 **89,151,926 bytes**。几何和动画文本压缩率高，纹理多数已压缩，不能从源码文件大小直接推断 JAR 贡献。最大的若干单项包括 pack.png、丹炉 material_1.png、logo、重铸/铭刻窗口及背景纹理。模组图标仍由 mods.toml 引用，不当成垃圾文件。

运行包不含 `source-assets/`、`reference_assets/`、`software/bernie/` 类型桩、`/qa/myriad/` 或 `*GameTests.class`；另将本地实际编译出的所有 GameTest class 名与 JAR 条目取交集，确认夹具及其内部类未混入。检出的 `src/main/resources` 为 2,889 文件、160,851,627 bytes；其中 mods.toml 经构建变量展开，因此不能简单用文件哈希逐项等同打包资源。源资产与参考目录属于作者输入，不由本包统计推断其仓库总体积，也没有声称已完整检出全部源素材。

## 重复与取舍

发现 61 组资源字节完全相同，按每组保留一份计算，额外未压缩字节合计 1,056,719。这是候选账面值，不是已经节省的包体或内存。

最大一组是 spirit_devourer/0.png 和 1.png。实际 SpiritDevourerRenderer 同时通过默认纹理及模型部件纹理索引形成路径；直接删一张可能破坏模型或资源包覆盖。异火重复纹理也可能为不同资源位置与着色规则服务。若后续合并，须先检查模型、材质、动画及外部资源包约定，再做图像与客户端对照。本批未删改任何素材。

当前先治理真实大项：后续可对大型 GUI/图标做像素不变的编码比较，或用批准的视觉对照调整分辨率；不能凭字节统计擅自降低画质。整理 Java 目录不会直接消除上述纹理贡献，架构优化与制品治理分别验证。

ZIP 大小不是 GPU 纹理占用、加载耗时、堆占用或 FPS；本批没有这些测量，不以包体下降冒充运行性能改善。

## 安装与未覆盖

现行授权实例 `E:/我的世界/PCL 正式版 2.10.9/.minecraft/versions/1.20.1-Forge_47.4.26/mods` 当前不存在，本机也没有 E 盘；旧 AXO 路径不可用。未擅自改装到 Desktop 的其他 Forge/xxcs 实例，未关闭用户游戏。客户端及最终安装仍需单独完成；本批生产构建、服务端测试与账本证据有效，不代表客户端已加载本包。
