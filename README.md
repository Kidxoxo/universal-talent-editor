# Universal Talent Editor

面向 Minecraft 1.20.1 / Forge 的游戏内可视化通用天赋编辑器。

## 环境要求

- Minecraft `1.20.1`
- Forge `47.3.22–47.x`
- Java `17`
- 客户端和服务端均需安装同一版本 JAR
- 许可证：MIT

## 安装与入口

1. 将 `talent_editor-forge-1.20.1-1.0.0.jar` 放入客户端和服务端的 `mods` 目录。
2. 管理员进入世界后按 `F8`，或执行 `/talent_editor open` 打开编辑器。
3. 玩家按 `J`、右键“天赋册”或执行 `/talent_editor book` 打开天赋册。

`F8` 编辑器及修改类命令需要 2 级权限。快捷键可在控制设置中重绑。

## 核心功能

- 游戏内编辑天赋、稀有度、抽取池、计数器、书页布局和配置物品。
- 一个天赋可包含多条普通/动态属性、常驻效果、效果免疫、免费刷新、创造飞行及事件效果。
- 事件效果支持条件、概率、冷却、次数限制、主/失败动作，以及属性修改、伤害、治疗、反伤、状态效果和定向检索等动作。
- 可配置 1–5 选一、刷新次数、天赋上限。
- 支持自动或手动合成，以及个人/全服声音和字幕策略。
- 天赋册布局、分页、格位、背景和图标尺寸均可配置。
- 内置“天赋册”和“天赋点”可在物品配置中修改外观。
- 所有保存均由服务端校验，并提供 revision 冲突保护、事务历史、备份、撤销和失败回滚。

## 快速使用

1. 先按需创建稀有度、抽取池和计数器。
2. 点击“新建天赋”，填写 ID 和名称。
3. 在效果列表使用“添加效果 / 编辑所选效果 / 删除所选”。
4. 普通属性选择器可按注册 ID 或当前语言的本地化名称搜索。
5. 在“复杂属性”中添加动态属性、免费刷新、创造飞行和事件效果；每个事件触发器都会显示为独立可选条目。
6. 在“详细设置”中配置文本、类别、图标、条件、合成、抽取和获得表现。
7. 点击“仅验证”可检查配置，再点击“保存并启用”。


## 常用命令

```text
/talent_editor open
/talent_editor book
/talent_editor draw [抽取池ID]
/talent_editor grant <玩家> <天赋ID>
/talent_editor revoke <玩家> <天赋ID>
/talent_editor tokens add <玩家> <数量>
/talent_editor items list [搜索词]
/talent_editor items give <玩家> <物品ID> [数量]
/talent_editor inspect <玩家>
/talent_editor status
/talent_editor resources
/talent_editor validate
```

其他子命令可通过游戏内命令补全查看。

## 数据与资源位置

游戏内保存的定义位于：

```text
<世界>/datapacks/talent_editor_generated/
```

支持六类数据定义：`talents`、`rarities`、`pools`、`counters`、`layouts` 和 `items`。字段说明见 [Schema 2](docs/SCHEMA_V2.md)。

本地资源导入位于 `F8 → 本地资源`：

- 纹理接受 PNG/JPG/JPEG，导入后统一保存为 PNG。
- 纹理 ID 示例：`example:textures/talent_icons/1.png`。
- 实际文件：`resourcepacks/talent_editor_generated/assets/example/textures/talent_icons/1.png`。
- 音频仅接受有效 OGG；配置时填写声音事件 ID，例如 `example:talent.acquire`，而不是 `.ogg` 文件路径。
- 可连续导入多项，再执行一次“统一重载”。

联机服务器不会自动向远程客户端发送本地资源包；每个客户端仍需安装或导入相同资源。管理员可用 `/talent_editor resources` 检查资源状态。

## 导入、导出与新世界模板

“模组设置”可以：

- 将当前有效定义和本机资源包导出到 `talent_editor_exports/*.zip`；
- 将导出的 ZIP 安全导入当前世界；
- 将当前配置设为同一游戏目录中新世界的默认模板。

导入或替换前会创建时间戳备份；现有世界不会被全局模板自动覆盖。

## 构建

在源码仓库根目录使用 Java 17：

```powershell
$env:JAVA_HOME = '你的 JDK 17 目录'
.\gradlew.bat clean build
```

产物位于：

```text
build/libs/talent_editor-forge-1.20.1-1.0.0.jar
```
