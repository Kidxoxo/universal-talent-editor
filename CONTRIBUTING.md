# Contributing / 参与贡献

Thank you for contributing. 欢迎提交问题与改进。

1. Use Java 17 and keep changes compatible with Minecraft 1.20.1 / Forge 47.x.
2. Create a focused branch and avoid committing `build`, `.gradle`, `run`, logs, or game files.
3. Run `./gradlew test` for logic changes and `./gradlew build` before opening a pull request.
4. Describe behavior changes, compatibility impact, and manual verification in the pull request.
5. Keep client input untrusted: definitions and player mutations must remain server-authoritative and validated.

请使用 Java 17，保持 Forge 1.20.1 兼容；不要提交构建缓存、运行目录、日志或游戏文件。逻辑修改至少运行测试，提交 PR 前运行完整构建，并说明兼容性与手动验证情况。
