# 1.3.0 开发基础与代码梳理

## 确定 1.2.0 基线

本工作区同时保留根目录早期实现、`talent-editor`、`universal-talent-editor-1.1.0` 和 `github-publish-1.2.0-20260930`。目录名不能代表实际版本：`talent-editor` 是旧 1.0.0，而名为 1.1.0 的开发目录实际已经演进至 1.2.0。

本次以 `github-publish-1.2.0-20260930` 的发布提交 `8e10f98`（Release Universal Talent Editor 1.2.0）为基础。开发前逐文件比较其 `src` 与旧开发目录，内容一致。完整复制发布源码至独立的 `universal-talent-editor-1.3.0`，原发布目录、旧代码和已有世界未做迁移覆盖。

核对资料包括发布 README、1.2.0 版本资料、SCHEMA_V2、既有开发知识库及实现报告，并以实际代码为准解决历史文档差异。例如 1.2.0 网络协议实际已是 42；布局默认值实际为左侧 60%、2 列 3 行、格宽 120 / 高 56、图标 48。此次同步修正新源码目录中的相关文档。

## 1.2.0 已实现内容

| 部分 | 已有能力 | 本次处理 |
|---|---|---|
| 定义与编译 | Schema 2 的天赋、稀有度、池、计数器、布局、配置物品；严格字段/引用/前置检查 | 扩展布局，复用原编译管线 |
| 天赋运行时 | 属性、动态公式、条件、事件触发、生命周期与获取/移除动作 | 保留 |
| 抽取与合成 | 服务端会话、候选、刷新、幸运/保底、代币、重复规则、前置与消耗 | 保留业务规则，新增界面入口映射 |
| 计数器 | 自动事件累计、重置、可见性、类别、条件和公式引用、玩家同步 | 自定义书页继续展示 |
| 玩家表现 | HUD、文字/音效/粒子、玩家模型透明度与缩放、获取/合成时间轴和预览 | 保留 |
| 配置物品 | 属性列表、多装备槽、右键/触发动作、图标和 Curios 集成 | 保留 |
| 游戏内管理 | 定义编辑、引用查询、删除保护、数据包导入导出、历史与撤销 | 新 UI 草稿仍由原布局提交入口保存 |
| 客户端资源 | PNG/JPG/JPEG 导入、PNG 资源引用、本地资源包重载与导出 | 复用选择器，并纳入 UI 正常/悬停图片 |
| 旧天赋册布局 | 行列、自由格位、固定天赋前置页、分页背景 | 未启用新 UI 时保留旧渲染；启用后保留数据分页关系 |

## 数据与请求流

管理员编辑 → `LayoutDraft / LayoutEditScreen` 本地草稿 → 原 `SubmitLayoutPacket` → 权限和 revision 检查 → `DefinitionService` 完整编译 → `EditorPackStorage` 备份并原子替换世界数据包 → `RuntimeSnapshot` 发布 → 玩家状态/抽取会话携带活动布局 → 客户端渲染。

这条链决定了 UI 应当成为布局定义的一部分。单独保存成客户端配置会让多人界面与服务端选定布局脱节，也无法自然继承已有导入导出、权限和冲突检测。

`TalentBookPagination` 负责固定目标、空位与溢出页；1.3.0 只用新的格位索引映射其结果。`TalentDrawScreen` 使用服务端返回的候选；按钮发送原请求，不根据预览数据自行发放天赋。

## 1.3.0 新增代码分层

| 文件/类 | 职责 |
|---|---|
| `domain/UiDocument` | 不可变画布与模块；锚点几何、等比缩放和逆坐标 |
| `compile/UiDocumentCodec` | 编译器、网络和编辑器共同使用的有界严格解码器 |
| `TalentBookLayoutDefinition` | 两个可选文档 `bookUi` / `drawUi`；旧构造与数据兼容 |
| `client/UiScene` | 正式页面和预览共用的绘制、裁剪、纹理透明、点击和正文滚动 |
| `client/UiTemplates` | 将默认界面或现有第一页/自由格位转换为模块，保留原版按钮风格 |
| `client/UiCanvasEditorScreen` | 选择、拖曳、尺寸调整、视野缩放/平移、常驻图层、吸附、历史与键盘操作 |
| `client/UiElementPropertiesScreen` | 位置/外观/内容分组属性、图片/池选择、锚点切换、颜色与数字校验 |
| `client/LayoutPageEditorScreen / LayoutBindingScreen` | 页面顺序、背景与天赋绑定；不再重复编辑模块几何 |
| `client/LayoutContentSlots` | 为新增内容格位分配无重叠的旧版兼容坐标，保留既有格位 |
| `client/UiEditHistory` | 64 步不可变草稿历史，完整拖动作为一步 |
| `client/UiPreviewContext / UiLayoutPreviewScreen` | 本地示例/已同步数据预览，不发送抽取或合成请求 |
| `client/UiCards` | 将现有天赋/计数器展示数据转成可编辑卡片内容 |
| `PlayerTalentStatePacket / DrawSessionPacket` | 同步 UI；直接打开抽取时也发送活动布局 |
| `DefinitionReferenceIndex / ClientResourceRequirementCatalog` | 池引用保护与资源需求目录 |

原 `LayoutSlotEditorScreen` 和 `LayoutPreviewScreen` 已移除，布局主表单不再暴露重复的行列/尺寸字段；旧 JSON 几何继续完整保留并通过原严格编译器校验。

Schema 保持 2；协议从 42 升级为 45，防止旧客户端把新增数据错误解码。没有新增运行前置模组。

## 参考项目与采用方式

- [FancyMenu 指定 Forge 1.20.1 分支](https://github.com/Keksuccino/FancyMenu/tree/v2-1.20.1-forge) 与 [官方说明](https://docs.fancymenu.net/docs/en-US/home)：参考模块、布局、定位和编辑/保存工作流。指定分支许可证对源码/JAR 集成有限制；本实现未复制其代码或捆绑其 JAR。
- [Drippy Loading Screen 官方 Wiki](https://github.com/Keksuccino/Drippy-Loading-Screen/wiki) 与 [FancyMenu 加载页说明](https://docs.fancymenu.net/docs/en-US/loading-screen)：参考“在可交互替代画面上编辑与预览”的思路。用户提供的 Drippy 指定分支页面未成功获取，未把它作为已审查源码。

本次独立实现适配本模组数据与权限的编辑器。模块几何与业务操作分离，同一渲染器用于正式界面与预览，降低两种显示方式不一致的风险。

## 明确的产品边界

支持编辑本模组天赋册与抽取页；不修改 Minecraft 启动加载页。不是 FancyMenu 布局格式兼容层。每个活动布局有一份跨页复用的书页 UI 和一份抽取 UI，未增加逐池 UI、逐页独立文档、组合/解组、脚本、网页、视频或 GIF。PNG 图片当前按矩形铺满，不做九宫格切片。

玩家端需要相同客户端资源包，布局网络同步不等于资源文件分发。候选索引保持 0–4，运行时隐藏未使用的卡片，不会自动重排已设计位置。操作和配置说明见 [UI 指南](UI_EDITOR_1.3.0.md)，验证边界见 [验证记录](../BUILD_VERIFICATION.json)。

当前代码逻辑修复与验证见 [LOGIC_FIXES_1.3.0.md](LOGIC_FIXES_1.3.0.md)。
