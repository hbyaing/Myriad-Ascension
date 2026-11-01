# 化虹着色器启动崩溃修复（2026-11-02）

用户提供错误报告：`错误报告-11-02-2026_00.14.53.zip`。仅读取崩溃和日志，不执行压缩包内启动脚本，不上传整包或启动凭据。

根因：Intel UHD Graphics（32.0.101.7088）在 Forge 47.4.23 的 RegisterShadersEvent 中编译 huahong_volume.fsh 第25行失败：`overloaded functions must have the same return type`。本模组定义的 `float noise3(vec3)` 与 GLSL 内置同签名、返回 vec3 的函数冲突。错误导致启动资源加载失败，不涉及存档或丹道知识。

改为 `huahongValueNoise3`，同步所有调用点。源文本反向替换后与基线124f68b完全相同，算法、参数、画面公式均不变。没有吞掉异常或禁用化虹。构建新增 check_shader_builtins 检查，避免再次定义 noise1–noise4；这是源代码审计，不冒充实际GLSL编译。

正式构建已通过；独立后台客户端实际启动与资源重载均通过，生产化虹 shader ready。另一个后台客户端在 Oculus 1.8.0 / Embeddium 0.3.31 下进入真实世界，Iris API 确认 ComplementaryReimagined_r5.9.3 启用，OpenGL错误0，鼠标捕获违规0，测试客户端均正常退出。此次外部光影验证使用现有炼丹控件QA场景，证明资源加载和场景管线通过，不声称检查了全部化虹外观；Intel原环境未实机复测。正式JAR已核对新函数名和QA代码不入包。当前机器的 GPU/Forge 不一定与错误报告一致，需要在报告中分别记录，不能宣称已在该Intel驱动复测。无游戏规则/schema/协议改动。本轮没有重新跑完整服务端GameTests；此前全量飞行测试失败记录不因本次构建而清除。

已按既有授权更新本机 D:/我的世界/.minecraft/versions/1.20.1-Forge_47.4.26/mods，旧包校验备份至 `deliverables\mod-backups\2026-11-02\万界仙途自动安装-012421-1894f6db`，SHA256 `5239b2c4216cb73362af2d0b4a011053364bcb8086a7c8e4e89ab0c09b325ebe`。未关闭用户游戏，需重启加载。错误报告中的 C:/Users/*****/Desktop/.minecraft/versions/Rapid Optimization 是另一环境，未冒称已安装到该被脱敏路径。可转交 deliverables/20261102-shader-startup-fix 中的正式JAR及校验文件。
