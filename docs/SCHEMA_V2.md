# Schema 2 当前实现

本页描述 Universal Talent Editor 1.0.0 已经能够编译的字段，不代表最终完整模式。

## 天赋册布局

文件：

    data/<namespace>/talent_editor/layouts/<id>.json

示例：

    {
      "schema": 2,
      "enabled": true,
      "priority": 10,
      "background": "example:textures/gui/talent_book.png",
      "panelPercent": 48,
      "columns": 3,
      "rows": 5,
      "slotWidth": 42,
      "slotHeight": 30,
      "iconSize": 16,
      "slots": [
        {"id": "featured", "x": 0, "y": 0, "width": 84, "height": 42, "iconSize": 24},
        {"id": "compact", "x": 90, "y": 8, "width": 52, "height": 28, "iconSize": 16}
      ],
      "pages": [
        {
          "id": "opening",
          "background": "example:textures/gui/opening.png",
          "slots": [
            {"id": "hero", "x": 12, "y": 8, "width": 110, "height": 48, "iconSize": 28}
          ]
        }
      ],
      "showGeneratedEffects": true
    }

| 字段 | 必需 | 说明 |
|---|---|---|
| schema | 是 | 必须为 2 |
| enabled | 否 | 默认 true；禁用的布局不参与活动布局选择 |
| priority | 否 | -100000–100000，数值更高者优先；相同时按布局 ID 排序 |
| background | 否 | 客户端完整纹理 ID；缺失资源时回退默认背景 |
| panelPercent | 否 | 左侧格位区域占屏幕宽度百分比，25–70，默认 42 |
| columns | 否 | 期望列数，1–8，默认 2；窗口放不下时自动减少 |
| rows | 否 | 期望行数，1–10，默认 4；窗口放不下时自动减少 |
| slotWidth | 否 | 格位宽度，24–128 像素，默认 58 |
| slotHeight | 否 | 格位高度，22–96 像素，默认 30 |
| iconSize | 否 | 图标显示尺寸，8–32 像素，且必须比格位高度至少小 4 像素 |
| slots | 否 | 自由格位模板；存在时替代行列网格并决定每页容量，最多 256 项 |
| pages | 否 | 按顺序使用的前置页面模板，最多 64 页；每页必须拥有 ID 和至少一个自由格位 |
| showGeneratedEffects | 否 | 布局级实际加成总开关；还会与单个天赋的同名开关共同判断 |

`slots` 中每项包含稳定 `id`、逻辑坐标 `x/y`、`width/height` 与独立 `iconSize`。ID 必须匹配
`[a-z0-9_.-]{1,64}` 且不得重复；坐标和矩形必须落在 2048×2048 逻辑画布内，格位彼此不得重叠。
宽度范围为 24–256，高度为 22–128，图标为 8–64 且必须保留至少 4 像素的纵向内边距。
天赋按数组顺序填入格位，此模板在每一页重复使用；客户端会等比缩小整块逻辑画布以适应当前窗口，但不会放大。
省略 `slots` 时继续使用 `columns/rows/slotWidth/slotHeight/iconSize` 规则网格，因此已有布局无需迁移。

`pages` 中每项包含唯一页面 `id`、可选 `background` 和独立 `slots`。页面背景留空时继承根布局背景。
未设置固定绑定时，天赋按顺序流入这些前置页面，之后继续使用根级 `slots` 或规则网格生成任意数量的回退页，因此定义增加天赋时不会被静默截断。
根模板与全部前置页面合计最多 4096 个自由格位；每页内部仍执行相同的 ID、尺寸、越界和重叠校验。
页面 ID 只用于稳定识别和编辑，不要求与 ResourceLocation 相同，格式为 `[a-z0-9_.-]{1,64}`。

阶段 27 支持在前置页面的格位内添加可选 `talent`，将其固定绑定到一个已定义天赋：

    {
      "id": "hero",
      "x": 12, "y": 8, "width": 110, "height": 48, "iconSize": 28,
      "talent": "example:vitality"
    }

`talent` 必须是存在的完整天赋 ID；同一布局的全部前置页面内，一个天赋只能绑定一次。
根级 `slots` 是重复使用的回退模板，只允许自动填入，不能设置 `talent`。
未设置或清除绑定时省略该字段，不写空字符串。

展示先预留全部页面的固定目标，再按服务端展示列表顺序将其余天赋填入自动格位，余量进入回退页。
例如服务端列表为 A/B/C，第二页绑定 A，则第一页的自动格位从 B 开始。
固定目标未出现在当前“已拥有”或“可合成”列表时，显示灰色预留空位；绑定本身不会授予天赋或开启合成。
自动格位不足时继续分页，固定格位不重复消耗自动流入容量；计数器页仍使用原有列表。
固定绑定会产生反向引用，禁用布局的引用也保留；删除对应天赋前需要先清除绑定。

操作：`F8 → 书页布局 → 逐页布局 → 编辑本页格位`，选中格位后点击“固定天赋…”检索选择；
“清除绑定”恢复自动填入。移动或缩放格位保留绑定；“从网格生成”会替换当前画布的所有格位及绑定。
完成后依次应用格位、应用逐页布局，最后在布局主编辑页保存。预览用金色顶边标出固定格位，悬浮可查看绑定 ID。
当前网络协议为 27，联机两端需安装同一新版 JAR；未配置绑定的旧 JSON 无需迁移。

模组自带 `talent_editor:default` 作为默认布局。服务端严格编译所有布局，并把选中的活动布局随玩家天赋状态发送给客户端；专用服务器上的客户端不需要直接读取服务器数据包。背景图片仍属于客户端资源，文件位置为
`assets/<namespace>/textures/...`，不会由世界数据包自动传输。

管理员可通过 `F8 → 书页布局` 新建和编辑上述字段。“自由格位”页面可从当前网格生成根模板，随后左键拖动格位、右键拖动或拖动右下角调整尺寸，并可新增/删除格位；重叠会标红且不能应用。“逐页布局”页面可新增、删除和调整前置页面顺序，编辑页面 ID/背景，并为每页打开相同的格位画布。布局目录会标出当前活动项，编辑页的“预览”可依次切换所有前置页面和后续回退页，并在背景纹理未安装于当前客户端时显示警告。布局保存、删除和撤销使用与天赋定义相同的服务端权限、revision 与持久事务保护。

## 本地化文本

名称和描述可直接写字符串，也可使用结构化文本：

    {
      "literal": "无翻译时显示的文字",
      "translation": "talent.example.vitality.name",
      "color": "#55AAFF",
      "bold": true,
      "italic": false
    }

`literal` 与 `translation` 至少填写一个。两者同时存在时优先显示 `literal`；只填写 `translation` 时，
天赋册与抽取界面会由每个客户端按自己的语言资源解析翻译键。`color` 可留空，或使用
#RRGGBB/#AARRGGBB。游戏内“文本样式”页面支持天赋名称和每段描述的新增、删除、预览及全部上述字段。

## 配置物品

文件：

    data/<namespace>/talent_editor/items/<id>.json

文件路径同时决定逻辑物品 ID。例如
`data/example/talent_editor/items/talent_shard.json` 定义 `example:talent_shard`：

    {
      "schema": 2,
      "enabled": true,
      "name": {
        "literal": "天赋碎片",
        "color": "#A855F7",
        "bold": true
      },
      "description": [
        "用于后续天赋合成。",
        {"translation": "item.example.talent_shard.hint", "italic": true}
      ],
      "customModelData": 1001,
      "glint": true,
      "attributeModifiers": [
        {"attribute": "minecraft:generic.attack_damage", "amount": 2,
         "operation": "addition", "slot": "mainhand", "modifierKey": "shard_attack"}
      ],
      "use": {
        "enabled": true,
        "cooldownTicks": 40,
        "consumeCount": 0,
        "actions": [
          {"type": "add_effect", "effect": "minecraft:speed", "durationTicks": 100, "target": "self"}
        ]
      },
      "loot": [
        {
          "tableMatch": "exact",
          "table": "minecraft:chests/simple_dungeon",
          "chance": 0.10,
          "chancePerLuck": 0.01,
          "minCount": 1,
          "maxCount": 2,
          "dimensions": ["minecraft:overworld"]
        }
      ],
      "requiredMods": []
    }

| 字段 | 必需 | 说明 |
|---|---|---|
| schema | 是 | 必须为 2 |
| enabled | 否 | 默认 true；false 时定义保留但管理员命令不会发放 |
| name | 是 | 字符串或结构化本地化文本 |
| description | 否 | 字符串、结构化文本，或由两者组成的数组；写入物品 Lore |
| customModelData | 否 | 0–16777215，默认 0；0 使用载体默认模型 |
| glint | 否 | 默认 false；是否强制显示附魔光效 |
| attributeModifiers | 否 | 装备槽属性修饰器数组，最多 256 条 |
| use | 否 | 右键动作、按逻辑 ID 隔离的冷却和可选消耗 |
| loot | 否 | 精确、前缀或正则战利品表与可选上下文条件的注入规则数组，最多 256 条 |
| requiredMods | 否 | 模组 ID 数组；缺少时产生警告并将定义标为不可用 |

Forge 注册表会在启动阶段冻结，因此数据包重载不能安全增加任意真正的 Item registry ID。
当前实现预注册 `talent_editor:configured_item` 作为通用载体，同时预注册固定功能的
`talent_editor:talent_book` 和 `talent_editor:talent_point`。普通逻辑物品把定义 ID 写入
`TalentEditorDefinition` NBT，并把名称、Lore、`CustomModelData` 和光效标记写入物品栈。
这样可以热重载定义，也能让已经生成的物品在定义暂时缺失时继续保留其显示与身份。
两个系统物品也从同名定义读取上述显示信息和装备属性，但其右键功能固定为打开天赋册、增加一次抽取，
不会执行定义中的通用 `use` 动作。

管理员命令：

    /talent_editor items list [搜索词]
    /talent_editor items give <玩家> <逻辑物品ID> [数量]

`give` 数量范围为 1–2304，自动拆为每组最多 64 个；背包放不下的剩余物品掉落在玩家处。
禁用或缺少前置模组的定义不可发放。

Minecraft 1.20.1 客户端资源包可按 `customModelData` 为载体选择模型。资源包中的
`assets/talent_editor/models/item/configured_item.json` 可写为：

    {
      "parent": "minecraft:item/generated",
      "textures": {"layer0": "minecraft:item/paper"},
      "overrides": [
        {
          "predicate": {"custom_model_data": 1001},
          "model": "example:item/talent_shard"
        }
      ]
    }

目标模型 `assets/example/models/item/talent_shard.json`：

    {
      "parent": "minecraft:item/generated",
      "textures": {"layer0": "example:item/talent_shard"}
    }

对应 PNG 为 `assets/example/textures/item/talent_shard.png`。普通配置物品共用一个载体模型，
所以其 `overrides` 必须集中列出全部 `customModelData`，普通定义之间应使用不同数值。
天赋册和天赋点分别使用自己的 item model，可以彼此以及与普通载体复用同一数值。
物品定义 JSON 属于服务端数据包，模型/PNG 属于客户端资源包，专用服务器不会自动把它们发送给玩家。
管理员可在 `F8 → 配置物品` 中检索、新建、编辑、仅验证、保存、查看引用和安全删除；写入仍使用统一 revision、事务历史、失败回滚和撤销流程。

逻辑物品 ID 可用于 `has_item`、`event_item`、自动计数器 `items` 和 `synthesis.consumeItems`。运行时遇到带 `TalentEditorDefinition` 的载体时只按逻辑 ID 匹配，不会同时按 `talent_editor:configured_item` 重复匹配；没有逻辑 ID 的普通载体仍按其注册 ID 处理。为避免歧义，配置物品 ID 与已注册真实物品 ID 冲突会以 `CONFIGURED_ITEM_ID_COLLISION` 拒绝整批定义，只有上述两个同名系统定义例外。

`attributeModifiers` 每条包含已注册 `attribute`、有限 `amount`、`operation`、`slot` 与物品内唯一的
`modifierKey`。`operation` 可为 `addition`、`multiply_base`或 `multiply_total`；`slot` 可为
`mainhand`、`offhand`、`head`、`chest`、`legs`或 `feet`。修饰器 UUID 由逻辑物品 ID 和键稳定生成，
同一定义内不允许重复键。属性数据写入载体 NBT，既有背包物品在服务端 tick 中按快照 revision 刷新；定义删除后清除旧属性。可视化入口为 `F8 → 配置物品 → 属性效果`。

`use` 包含 `enabled`（默认 true）、`cooldownTicks`（0–1200000）、`consumeCount`（0–64）和非空
`actions`。当前允许 `add_effect`、`remove_effect`、`play_sound` 和 `show_message`；效果动作只允许
`target: "self"`。冷却按玩家＋逻辑物品 ID 隔离，不使用所有载体共享的原版 Item 冷却。只有至少一个动作实际生效时才记录冷却和消耗；创造模式不消耗。非创造玩家持有数量不足 `consumeCount` 时不执行任何动作。
游戏内入口为 `F8 → 配置物品 → 右键动作`。动作列表可新增、删除和上下排序；效果和声音可从当前注册表检索。`show_message` 界面支持 `chat`、`action_bar`、`title`、副标题、本地化文本、颜色、粗斜体和标题时长。若 `use` 包含未知字段、未知动作或非 `self` 效果目标，界面会保持整段高级 JSON 只读；普通表单保存仍原样保留它。

`loot` 中每条规则格式：

    {
      "tableMatch": "prefix",
      "table": "minecraft:chests/",
      "chance": 0.10,
      "chancePerLuck": 0.01,
      "minCount": 1,
      "maxCount": 2,
      "dimensions": ["minecraft:overworld"],
      "entityTarget": "this_entity",
      "entityTypes": ["minecraft:zombie"],
      "entityTags": ["minecraft:undead"]
    }

`tableMatch` 可为 `exact`（默认）、`prefix` 或 `regex`。`exact` 的 `table` 必须是带命名空间的完整 ResourceLocation；`prefix` 使用同样的小写 ID 字符并按完整字符串开头匹配，例如 `minecraft:chests/` 可覆盖原版所有箱子表，`example:` 可覆盖整个命名空间；`regex` 使用 Java 正则并对完整的 `namespace:path` 调用 `matches()`。表达式长度为 1–256；正则在定义编译时预编译，拒绝语法错误、反向引用、前后查找/内嵌标志，以及包含内部量词或分支的再次量化分组，以降低高频掉落路径中的灾难性回溯风险。Minecraft 1.20.1 没有可由 Forge 统一查询的战利品表注册表标签，因此“整类表”使用前缀或受限正则表达，不把方块/实体标签误当成表标签。

定义不会因为当前尚未加载到某张精确表而被拒绝。`chance` 默认 1，范围 0–1；`chancePerLuck` 默认 0，范围 -1–1。运行时有效概率为 `clamp(chance + LootContext.getLuck() × chancePerLuck, 0, 1)`；基础概率和系数同时为 0 时规则保留但产生永不投放警告。宝箱等上下文的幸运值通常为 0，实际值由调用该战利品表的系统提供。`minCount` 默认 1，`maxCount` 默认等于最小值，必须满足 `1 <= minCount <= maxCount <= 64`。

`dimensions` 是可选维度 ID 数组，数组内任一匹配即可。`entityTarget` 可为 `this_entity`（默认）或 `killer_entity`；只有存在实体筛选时才影响匹配。`entityTypes` 与 `entityTags` 合并为一个 OR 组，维度组和实体组之间为 AND。实体类型会对当前 Forge 注册表校验；标签和维度只严格校验 ID 语法，以允许数据包动态内容。若 LootContext 不提供所选实体而规则存在实体筛选，则匹配失败；实体战利品表中的 `this_entity` 通常是被击杀实体，`killer_entity` 是直接击杀者，宝箱上下文通常两者都不存在。

同一配置物品可为同一张表配置多个不同匹配范围；表匹配模式、表达式、规范化后的维度集合、实体目标及实体类型/标签集合均相同时视为重复。没有实体筛选时，单独改变 `entityTarget` 不会形成新范围。不同配置物品命中同一张表的规则按稳定顺序独立判定，因此精确、前缀和正则规则可有意叠加。游戏内“战利品规则”页可切换三种表匹配方式；“高级条件”继续编辑维度、实体和幸运条件，并从当前注册表或服务端标签目录选择实体类型和标签。

模组内置一个无条件 Forge Global Loot Modifier，并在每次生成时按 `LootContext.getQueriedLootTableId()` 查询当前不可变运行时索引。精确规则保留 O(1) 表 ID 索引，只有前缀/正则规则进入模式列表；所有配置物品合计最多 4096 条模式规则。只有配置物品本身启用且前置可用时规则才进入索引；保存/数据包重载后即时更新，不需要重启。若更高优先级数据包用 `replace: true` 覆盖 `data/forge/loot_modifiers/global_loot_modifiers.json` 并移除了 `talent_editor:configured_items`，则注入不会运行。游戏内入口为 `F8 → 配置物品 → 战利品规则`。

核心运行时不依赖 LootJS。1.20.1 分支 LootJS 2.13.1 面向 KubeJS 2001 服务端脚本并维护自己的掉落修改流程；将相同规则同时注册到 LootJS 与本模组 GLM 会造成重复投放。因此可选兼容层未来只暴露配置物品/天赋点发放 API 给脚本，不复制本节规则到第二条掉落管线。

## 稀有度

文件：

    data/<namespace>/talent_editor/rarities/<id>.json

示例：

    {
      "schema": 2,
      "enabled": true,
      "name": {
        "literal": "普通",
        "translation": "rarity.example.common",
        "color": "#A0A0A0"
      },
      "color": "#A0A0A0",
      "order": 0,
      "offerable": true,
      "baseWeight": 1.0
    }

name 可直接写字符串，也可写对象。color 支持 #RRGGBB、#AARRGGBB 或整数。

当前字段：

| 字段 | 必需 | 说明 |
|---|---|---|
| schema | 是 | 必须为 2 |
| enabled | 否 | 默认 true；不再参与普通抽取目录 |
| name | 是 | 字符串或 LocalizedText 对象 |
| color | 是 | #RRGGBB、#AARRGGBB 或整数 |
| order | 否 | 编辑器和展示顺序，-100000–100000，默认 0 |
| offerable | 否 | 是否允许普通抽取，默认 true |
| baseWeight | 否 | 池没有覆盖时的稀有度权重，0–1e9，默认 1 |

游戏内首页的“稀有度”入口可以新建或编辑全部字段，并提供颜色预览。修改会连同现有池和天赋
整体重新编译；因此错误颜色、越界数值或被破坏的引用不会写入活动快照。

## 天赋

文件：

    data/<namespace>/talent_editor/talents/<id>.json

当前最小示例：

    {
      "schema": 2,
      "text": {
        "name": "活力",
        "description": ["最大生命值 +4。"],
        "showGeneratedEffects": true
      },
      "rarity": "talent_editor:common",
      "activeEffects": [
        {
          "type": "attribute_modifier",
          "attribute": "minecraft:generic.max_health",
          "amount": 4,
          "operation": "addition"
        }
      ]
    }

当前字段：

| 字段 | 必需 | 说明 |
|---|---|---|
| schema | 是 | 必须为 2 |
| enabled | 否 | 默认 true |
| text | 是 | 名称、描述、自动效果说明开关 |
| rarity | 是 | 必须引用存在的稀有度 |
| categories | 否 | ResourceLocation 数组 |
| icon | 否 | 完整 ResourceLocation |
| offer | 否 | enabled、weight、pools |
| capacityCost | 否 | 默认 1，范围 0–10000 |
| tags | 否 | ResourceLocation 数组 |
| requiredMods | 否 | 前置模组 ID；缺失时定义保留但不激活 |
| acquireCondition | 否 | 获取条件；默认总是允许 |
| synthesis | 否 | 进阶/合成入口与成功后的消耗项 |
| activeEffects | 否 | 持续效果动作 |
| triggers | 否 | 事件、复合条件与动作 |
| onAcquire | 否 | 任意首次获得成功后执行的表现动作 |
| onSynthesize | 否 | 合成成功且执行完 onAcquire 后追加的表现动作 |
| onRemove | 否 | 移除时动作预留字段；当前没有内置可执行动作 |

玩家拥有的天赋默认保存在服务端 capability/NBT 中，死亡克隆、重新登录与切换维度均会保留；天赋不是会在死亡时掉落的背包物品。只有单独配置了重置策略的计数器会按其规则清零。

游戏内“基础设置”当前可编辑 `enabled`、`text.description`、`text.showGeneratedEffects`、
`rarity`、`categories`、`icon` 和 `capacityCost`。稀有度选项来自服务器当前已经成功编译的稀有度目录。
描述输入使用可见的 `\\n` 分隔段落；未修改描述时，原 JSON 中的本地化对象和颜色样式保持不变。
需要配置单独段落的翻译键或字体样式时，应使用“文本样式”页面。

图标建议使用完整纹理 ID：

    "icon": "example:textures/talent_icons/vitality.png"

对应客户端资源文件为：

    assets/example/textures/talent_icons/vitality.png

当前建议使用 16×16 PNG。图标会显示在天赋册与抽取候选卡，但必须由客户端已有模组或资源包提供；
普通世界数据包不会同步客户端纹理。

`offer` 示例：

    {
      "enabled": true,
      "weight": 2.5,
      "pools": ["example:main"]
    }

`pools` 留空表示可进入任意抽取池；填写后只进入列出的池。所有池 ID 都必须引用存在的抽取池。

## 获得与合成表现动作

`onAcquire` 在玩家第一次获得该天赋后执行，当前覆盖抽取成功、管理员 `grant` 和合成成功。重复天赋返还或转化计数器不算首次获得，不执行表现。合成成功会先按数组顺序执行 `onAcquire`，再执行 `onSynthesize`，因此后者适合只在合成时追加的音效或文字。

    "onAcquire": [
      {
        "type": "play_sound",
        "sound": "example:talent.acquire",
        "source": "players",
        "volume": 1.0,
        "pitch": 1.0
      },
      {
        "type": "show_message",
        "mode": "title",
        "text": {"literal": "天赋觉醒", "color": "#FFD966", "bold": true},
        "subtitle": {"translation": "talent.example.awakened.subtitle", "color": "#FFFFFF"},
        "fadeInTicks": 10,
        "stayTicks": 70,
        "fadeOutTicks": 20
      }
    ],
    "onSynthesize": [
      {
        "type": "show_message",
        "mode": "action_bar",
        "text": {"literal": "合成完成", "color": "#55FF55"}
      }
    ]

`play_sound.sound` 是声音事件 ResourceLocation。当前服务端已注册的声音可以在可视化选择器中检索；也允许手填只由客户端资源包定义的声音 ID，此时编译产生 `UNREGISTERED_SOUND_EVENT` 警告但不会拒绝定义，运行时会把该 ID 作为直接声音事件发给获得者。`source` 可选 `master`、`music`、`records`、`weather`、`blocks`、`hostile`、`neutral`、`players`、`ambient` 或 `voice`，默认 `players`；`volume` 范围 0–16，`pitch` 范围 0.01–4。

自定义资源包音效必须使用 OGG。例如动作中的 `example:talent.acquire` 可对应：

    assets/example/sounds/talent/acquire.ogg

以及资源包中的 `assets/example/sounds.json`：

    {
      "talent.acquire": {
        "sounds": [{"name": "example:talent/acquire", "stream": false}]
      }
    }

客户端需要安装该资源包；普通世界数据包不会自动发送声音文件。服务端无法验证远程客户端文件是否存在，但编辑器的声音页会检查当前客户端是否已从 `sounds.json` 加载该事件；“本地预览”会实际试听已加载的事件，缺失时保留文字预览并显示警告。短音效使用 `stream:false`；较长音乐片段可改为 `stream:true`，并酌情把动作的 `source` 设为 `music` 或 `records`。文件需为 OGG Vorbis。

管理员可在 `F8 → 本地资源` 填写 OGG 或 MP3 源文件和声音事件 ID。受控导入器会将事件路径中的点转换为资源目录斜杠、维护 `sounds.json`，并把规范化后的 OGG 大小和 SHA-256 写入 `resourcepacks/talent_editor_generated/talent_editor.manifest.json`。Minecraft 1.20.1 不直接读取 MP3：导入器会调用 `config/talent_editor/tools/ffmpeg(.exe)`、系统 `PATH` 中的 FFmpeg，或 JVM 参数 `-Dtalent_editor.ffmpeg=<完整路径>` 指定的程序，将 MP3 转为 OGG Vorbis；转换器缺失或失败时不登记资源。

纹理源可为 PNG、JPG 或 JPEG，但目标 ID 始终必须是 `namespace:textures/...png`，导入器会实际解码、检查尺寸并规范化写为 PNG。每项导入只更新受控资源包和会话级待重载集合；管理员可连续导入纹理/音频，最后点击一次“统一重载”。天赋图标、布局根背景、逐页背景和获得表现音效的选择器会合并当前客户端已加载资源与本地导入清单，并支持按 ID 搜索；尚未统一重载的清单项标记为等待重载。此机制不是多人文件分发，其他客户端仍需取得并安装相同资源。

`show_message.mode` 可选 `chat`、`action_bar` 或 `title`。`text` 使用与天赋名称/描述相同的结构化文字格式，支持直接文字、客户端翻译键、`#RRGGBB`/`#AARRGGBB` 颜色、粗体和斜体。可视化直接文字输入框把可见的 `\n` 解码为真正换行；多行播报建议使用 `chat`。动作栏和原版标题本身是单行展示区域，不保证多行排版。`subtitle` 仅允许用于 `title`；标题的淡入、停留、淡出范围分别为 0–1200、0–12000、0–1200 tick。聊天栏和动作栏会忽略标题时间字段。

游戏内入口为“天赋编辑 → 获得表现”。每个阶段的简单表单可组合最多一个音效和一个文字动作，并可在主文字/副标题之间切换样式。“本地预览”只模拟当前阶段，不保存草稿、不授予天赋且不发送网络请求；退出时停止本轮试听。含其他动作、重复同类动作或额外字段的高级数组会在该阶段只读保留，另一阶段仍可独立编辑。

## 抽取池

文件：

    data/<namespace>/talent_editor/pools/<id>.json

示例：

    {
      "schema": 2,
      "enabled": true,
      "name": "主天赋池",
      "choices": 3,
      "refreshes": 1,
      "maxOwned": 24,
      "excludeOwned": true,
      "duplicatePolicy": "exclude",
      "luckAttribute": "minecraft:generic.luck",
      "luckFactors": {
        "example:rare": 0.05,
        "example:epic": 0.10
      },
      "pityRarity": "example:rare",
      "pityAfter": 10,
      "rarityWeights": {
        "talent_editor:common": 70,
        "example:rare": 25,
        "example:epic": 5
      }
    }

| 字段 | 必需 | 说明 |
|---|---|---|
| schema | 是 | 必须为 2 |
| enabled | 否 | 默认 true |
| name | 是 | 字符串或 LocalizedText 对象 |
| choices | 否 | 每次候选数，1–5，默认 3 |
| refreshes | 否 | 每个抽取会话的免费刷新次数，0–100，默认 1 |
| maxOwned | 否 | 玩家可拥有的天赋数量上限，1–10000，默认 24 |
| excludeOwned | 否 | 兼容字段；应与 duplicatePolicy 是否为 exclude 保持一致 |
| rarityWeights | 否 | 覆盖指定稀有度权重；未列出的稀有度使用其 baseWeight |
| luckAttribute | 否 | 用于计算幸运修正的已注册属性 ID；留空禁用幸运修正 |
| luckFactors | 否 | 稀有度 ID 到线性系数的对象，单项 -1000–1000 |
| pityRarity | 否 | 保底目标稀有度；该稀有度及更高 order 均视为满足保底 |
| pityAfter | 否 | 第几次确认抽取保证出现目标稀有度候选，0–1,000,000；0 禁用 |
| duplicatePolicy | 否 | exclude、refund_token 或 add_counter，默认 exclude |
| duplicateCounter | 否 | add_counter 策略写入的玩家计数器 ID |
| duplicateCounterAmount | 否 | 每次重复转化数量，1–1e12，默认 1 |

抽取使用两阶段权重。第一阶段只在启用、可抽取且仍有有效候选的稀有度间按池权重选择；
第二阶段在该稀有度内按天赋 `offer.weight` 选择。一个稀有度下增加天赋数量不会改变该稀有度的总权重。
同一批候选不重复，且会排除玩家已经拥有的天赋。

幸运值先限制在 -1024–1024，再对每个稀有度计算：

    有效权重 = 基础/池覆盖权重 × clamp(1 + 幸运值 × luckFactor, 0, 1,000,000)

随后只在当前玩家确实存在候选的稀有度之间归一化。未在 `luckFactors` 中出现的稀有度系数为 0。
编辑器高级设置页可以输入仅用于预览的幸运值，并即时显示定义级归一化概率；实际抽取始终重新读取服务端玩家属性。
幸运属性、`luckFactors` 的稀有度键、保底稀有度和重复奖励计数器均可从当前目录检索选择，选择器返回时保留其他尚未保存的输入。

保底进度按“玩家 + 抽取池 ID”持久化。若 `pityAfter=10`，连续 9 次确认结果未达到目标稀有度，
第 10 次候选的第一格会强制从目标稀有度或更高 order 的有效候选中产生。玩家若主动选择较低稀有度，
保底保持激活；选择达到目标的天赋后归零。刷新候选不增加保底进度。如果当前没有符合保底要求的有效候选，
则明确回退到普通抽取，避免生成空会话。

重复策略：

- `exclude`：已拥有天赋不进入候选；
- `refund_token`：允许重复候选，选中后结束会话但不扣抽取次数，也不推进保底；
- `add_counter`：允许重复候选，选中后扣一次抽取次数并增加指定计数器，同时按该天赋稀有度更新保底；
- 若重复奖励计数器已到 1e12 或玩家计数器条目已满，本次不会扣除抽取次数，会话保持可操作。

`excludeOwned` 是旧定义兼容入口：未填写 `duplicatePolicy` 时，true 映射到 `exclude`，false 映射到
`refund_token`；两者同时填写时必须一致。

## 自动计数器

文件：

    data/<namespace>/talent_editor/counters/<id>.json

示例：

    {
      "schema": 2,
      "enabled": true,
      "name": "僵尸击杀数",
      "event": "kill",
      "increment": 1,
      "amountMode": "fixed",
      "amountScale": 1.0,
      "rounding": "round",
      "minimumIncrement": 0,
      "maximum": 10000,
      "cooldownTicks": 0,
      "intervalTicks": 20,
      "resetOnLogin": false,
      "resetOnDeath": true,
      "resetIntervalTicks": 72000,
      "categories": ["example:combat", "example:undead"],
      "visibility": "condition_only",
      "requiredMods": [],
      "requiredTalents": ["example:undead_hunter"],
      "entityTypes": ["minecraft:zombie"],
      "entityTags": [],
      "items": [],
      "itemTags": ["minecraft:swords"],
      "damageTags": []
    }

定义 ID 同时就是写入玩家存档的计数器 ID。当前字段：

| 字段 | 必需 | 说明 |
|---|---|---|
| schema | 是 | 必须为 2 |
| enabled | 否 | 默认 true；禁用后不再自动累计，已有数值保留 |
| name | 是 | 字符串或 LocalizedText 对象 |
| event | 是 | 下表中的事件名称 |
| increment | 否 | 每次匹配增加量，1–1e12，默认 1 |
| amountMode | 否 | `fixed` 或 `event_amount`，默认 `fixed`；后者用于两个伤害事件或 `move_distance` |
| amountScale | 否 | `event_amount` 下的事件数值倍率，0–1e9，默认 1 |
| rounding | 否 | `floor`、`round` 或 `ceil`，默认 `round` |
| minimumIncrement | 否 | `event_amount` 下每次有效伤害的最小增量，0–1e12，默认 0 |
| maximum | 否 | 该规则允许达到的最大值，1–1e12，默认 1e12 |
| cooldownTicks | 否 | 同一玩家、同一规则两次接受事件的最短间隔，0–1,200,000 tick |
| intervalTicks | 否 | `play_time` 的累计周期，1–1,200,000 tick，默认 20 |
| resetOnLogin | 否 | 玩家登录时将此计数器重置为 0，默认 false |
| resetOnDeath | 否 | 玩家死亡并完成能力复制后将此计数器重置为 0，默认 false |
| resetIntervalTicks | 否 | 按玩家累计在线时间周期重置，0 关闭，最大 2,147,483,647 tick |
| categories | 否 | 计数器所属的开放类别 ID 列表；用于类别总值条件，可属于多个类别 |
| visibility | 否 | `full`、`condition_only` 或 `hidden`，默认 `full` |
| requiredMods | 否 | 缺少任一模组时保留定义但停用规则 |
| requiredTalents | 否 | 玩家必须全部拥有的天赋 ID |
| entityTypes/entityTags | 否 | 相关实体类型或实体类型标签 |
| items/itemTags | 否 | 相关物品或物品标签 |
| damageTags | 否 | 伤害类型标签，仅适用于造成/受到伤害事件 |

事件语义：

| event | 玩家 | 相关实体 | 相关物品 |
|---|---|---|---|
| manual | 由动作指定 | 无 | 无 |
| kill | 击杀者 | 被击杀实体 | 击杀者当时的主手物品 |
| damage_dealt | 伤害来源玩家 | 受伤实体 | 攻击者当时的主手物品 |
| damage_taken | 受伤玩家 | 伤害来源实体（环境伤害可为空） | 受伤玩家当时的主手物品 |
| item_use | 完成使用的玩家 | 无 | 完成使用的物品堆 |
| entity_interact | 发起主手交互的玩家 | 交互目标 | 主手物品 |
| move_distance | 在线玩家 | 无 | 无 |
| play_time | 在线玩家 | 无 | 无 |

实体类型和实体标签在同一个选择组内按 OR 匹配；物品 ID 和物品标签也按 OR 匹配。
实体组、物品组、伤害标签组和 `requiredTalents` 之间按 AND 匹配。空组表示不限制。
标签是数据包动态内容，因此编译器只校验标签 ID 语法；实体类型和物品 ID 会对当前服务端注册表校验。
`cooldownTicks` 是运行时防抖，不写入玩家存档；重新登录后重新计时。计数值本身仍持久化。

重置只自动作用于当前启用且 `requiredMods` 可用的规则。周期时钟作为单个玩家生命周期值持久化，
只在玩家在线的服务端 tick 增加，因此离线时间不计入；重新登录、重生或服务器重启后继续累计。
多个周期规则共享该玩家时钟并按各自间隔取模。管理员可使用
`/talent_editor counters reset <targets> <counter>` 手动清零并解除该计数器的运行时冷却。

计数器类别不会另外保存聚合值；运行时快照为启用且前置可用的规则建立类别索引，条件判定时汇总玩家已有数值。
汇总结果安全钳制到 1e12。`visibility` 只控制天赋册展示，不影响自动累计、条件判定、合成消耗或管理员命令：

- `full`：始终进入天赋册计数器页；
- `condition_only`：仅当活动天赋的获取条件、触发条件、合成计数器消耗或活动抽取池的重复计数奖励直接引用该计数器，或条件引用其类别时显示；
- `hidden`：始终不进入天赋册，即使被条件引用；玩家存档中没有当前规则定义的遗留计数器仍显示，以便发现和迁移旧数据。

`fixed` 模式忽略事件携带的数值并使用 `increment`。`event_amount` 模式读取 Forge
`LivingDamageEvent` 的最终伤害量或当 tick 的玩家水平移动距离，计算 `事件数值 × amountScale` 后按 `rounding` 转为 long，再与
`minimumIncrement` 取较大值；非有限值和非正事件值不会增加，结果最终钳制到 `maximum`。

`manual` 没有自动分派器，用于 `queue_draw_constraint.successCounter` 等动作管理的持久数值。
`move_distance` 统计 X/Z 平面距离；跨维度或单 tick 超过 64 格的位置跳变会被当作传送并忽略。

游戏内首页的“自动计数”入口可以新建、打开、验证和保存规则。基本页面编辑事件、数值和前置，
“重置策略”页面编辑登录、死亡和在线周期重置，
“累计方式”页面编辑伤害换算，“事件筛选”页面编辑五类筛选 ID。实体类型和物品可从注册表检索添加；
实体类型标签、物品标签和伤害类型标签可从当前服务端世界已加载的数据包标签目录检索添加。目录未列出的合法标签 ID 仍可手填，以便先配置、后加入数据包。天赋册可查看当前计数进度；
计数变化以 20 tick 批量增量包同步，并周期性补发完整展示状态以重新计算获取/合成条件。

## 属性动作

    {
      "type": "talent_editor:attribute_modifier",
      "attribute": "minecraft:generic.attack_damage",
      "amount": 0.15,
      "operation": "multiply_total",
      "modifierKey": "example.attack_bonus",
      "showInDescription": true
    }

type 可省略 talent_editor 命名空间。当前属性动作只能放在 activeEffects；临时属性动作会在事件运行时阶段单独实现，避免持续/临时生命周期混淆。

`activeEffects` 是动作数组，不是单一属性槽。一个天赋可以同时包含任意多条属性动作，例如：

    "activeEffects": [
      {
        "type": "attribute_modifier",
        "attribute": "minecraft:generic.max_health",
        "amount": 4,
        "operation": "addition"
      },
      {
        "type": "attribute_modifier",
        "attribute": "minecraft:generic.attack_damage",
        "amount": 0.15,
        "operation": "multiply_total"
      }
    ]

游戏内编辑器以天赋为编辑单位，可在天赋内部新增、修改或删除这些属性动作。不同属性动作应使用
不同的 `modifierKey`；省略时编译器会根据天赋 ID 和动作位置生成默认值。

动态属性同样放在 `activeEffects`，但使用受约束的线性公式，不执行任意脚本：

    {
      "type": "dynamic_attribute_modifier",
      "attribute": "minecraft:generic.attack_damage",
      "source": "counter",
      "counter": "example:kills",
      "base": 1,
      "scale": 0.25,
      "minimum": 0,
      "maximum": 20,
      "operation": "addition",
      "modifierKey": "kills_damage",
      "showInDescription": true
    }

实际修饰值为 `clamp(base + sourceValue × scale, minimum, maximum)`。全部数值必须是
`-1e12..1e12` 范围内的有限数字，且 `minimum <= maximum`。`operation` 和 `modifierKey`
与固定属性动作含义相同；固定与动态动作之间也不能共用同一修饰键。`source` 支持：

- `counter`：读取 `counter` 指定的玩家持久计数器原值；计数器必须存在，并进入反向引用保护；
- `health_percent`：读取 `health / maxHealth` 并钳制到 0–1；禁止以此修改
  `minecraft:generic.max_health`，避免公式递归；
- `hunger_percent`：读取当前食物值除以 20，并钳制到 0–1。

服务端每 10 tick 重新计算一次，只在数值/运算变化时替换瞬时属性修饰器。登录、重生、切换维度、
授予/移除天赋和定义热重载也会立即重算。天赋册和抽取候选显示公式而非误导性的固定值，
并同时尊重天赋级及动作级加成显示开关。游戏内路径为
`天赋编辑 → 复杂属性 → +动态属性`；属性与计数器均可从当前服务端目录检索。

创造飞行也是无参数活动动作：

    {"type": "creative_flight"}

一个天赋最多包含一条。玩家持有启用且可用的天赋时获得飞行许可；移除、禁用、失活或登出后，
本模组追踪并授予的生存/冒险飞行许可会撤销，正在飞行则停止。创造与旁观模式不会被强制关闭。
游戏内可在“复杂属性”子页切换；同页还集中提供“+刷新”和“事件效果”入口。

持续状态效果免疫也放在 `activeEffects` 中：

    {
      "type": "effect_immunity",
      "effect": "minecraft:poison"
    }

`effect` 必须是当前服务端已注册的状态效果。只要该天赋启用、前置可用且玩家实际拥有，Forge 的
`MobEffectEvent.Applicable` 判定就会拒绝之后尝试施加的对应效果。获得天赋前已存在的效果不会被自动清除；
如需清除，应另配 `remove_effect` 触发器。游戏内天赋主编辑页的“添加效果免疫”可检索注册表并新增，
列表中选中后可删除。当前天赋册不会为免疫自动生成说明，建议同时填写人工描述。

持续施加给玩家自身的状态效果也可放在 `activeEffects`：

    {
      "type": "add_effect",
      "effect": "minecraft:speed",
      "durationTicks": 60,
      "amplifier": 0,
      "target": "self",
      "ambient": true,
      "showParticles": false,
      "showIcon": true
    }

活动位置只接受 `target:self`。运行时每秒刷新短期租约，不会压低玩家已有的更高等级效果；天赋移除、禁用或失活后不再续租，剩余效果会在最多 10 秒内自然结束。游戏内主编辑页可检索并添加这种“常驻效果”，等级输入 0 表示 I 级。

额外免费刷新次数同样属于 `activeEffects`：

    {
      "type": "draw_refresh_bonus",
      "amount": 2,
      "pools": ["example:main"]
    }

`amount` 范围 1–100；`pools` 可省略或留空表示所有池，填写时每项必须引用存在的池。玩家新建抽取会话时，把所有已拥有且活动天赋的适用加成与池基础 `refreshes` 相加，总数安全钳制到 1000。现有会话不会因中途授予/移除天赋而改写剩余次数。GUI 的“复杂属性 → +刷新”创建作用于全部池的简单项；带 `pools` 的高级项会保留原 JSON。

## 事件触发与状态效果动作

首批运行时触发器直接保存在天赋的 `triggers` 中：

    "triggers": [
      {
        "id": "poison_undead",
        "event": "damage_dealt",
        "priority": 20,
        "chance": 0.35,
        "chanceAttribute": "minecraft:generic.luck",
        "chancePerPoint": 0.01,
        "cooldownTicks": 40,
        "activationLimit": 1,
        "actionFailurePolicy": "stop",
        "condition": {
          "type": "event_entity_tag",
          "tag": "minecraft:undead"
        },
        "actions": [
          {
            "type": "add_effect",
            "effect": "minecraft:poison",
            "durationTicks": 80,
            "amplifier": 1,
            "target": "event_entity",
            "ambient": false,
            "showParticles": true,
            "showIcon": true
          },
          {
            "type": "modify_damage",
            "operation": "multiply",
            "amount": 1.25,
            "minimum": 0,
            "maximum": 100
          }
        ],
        "failureActions": [
          {
            "type": "show_message",
            "mode": "action_bar",
            "text": "主动作未生效"
          }
        ]
      }
    ]

当前由 Forge 服务端分派的事件为：

| event | 事件实体 | 时机 |
|---|---|---|
| damage_dealt_pre | 受到伤害的实体 | `LivingHurtEvent` 末段，拥有天赋的玩家是伤害来源；结果继续进入护甲/药水/附魔等减免 |
| damage_taken_pre | 伤害来源实体（可能为空） | `LivingHurtEvent` 末段，拥有天赋的玩家是受击者；结果继续进入后续减免 |
| damage_dealt | 受到伤害的实体 | `LivingDamageEvent` 中最终伤害大于 0，拥有天赋的玩家是伤害来源 |
| damage_taken | 伤害来源实体（可能为空） | `LivingDamageEvent` 中拥有天赋的玩家受到最终伤害 |
| interact_entity | 玩家主手交互的目标 | `PlayerInteractEvent.EntityInteract` |
| kill | 死亡的实体 | `LivingDeathEvent` 中伤害来源实体为拥有天赋的玩家；事件物品为玩家主手物品 |
| death | 伤害来源实体（可能为空） | 拥有天赋的玩家触发 `LivingDeathEvent` |
| item_use_finish | 无 | 玩家触发 `LivingEntityUseItemEvent.Finish`；事件物品为完成使用的物品 |
| interact_block | 无 | 玩家主手触发 `PlayerInteractEvent.RightClickBlock`；提供主手事件物品和点击位置的事件方块 |
| player_tick | 无 | 服务端玩家 tick 的 END 阶段，每 tick 一次 |
| player_second | 无 | 服务端玩家 tick 的 END 阶段，按玩家实体 tick 每 20 tick 一次 |
| player_move_distance | 无 | 玩家产生有效水平移动时；每个触发器独立累计到 `distanceInterval` |

`add_effect` 可放在触发器中，也可按上一节的约束作为玩家自身常驻效果放进 `activeEffects`。在触发器中，`effect` 必须是服务端当前已注册状态效果；`durationTicks` 范围
1–72000，`amplifier` 范围 0–255（0 表示 I 级）。`target` 为 `self` 或 `event_entity`；事件实体不是
LivingEntity 时不会施加效果。环境效果、粒子和 HUD 图标分别由 `ambient`、`showParticles`、`showIcon` 控制。

触发器也支持移除状态效果：

    {
      "type": "remove_effect",
      "effect": "minecraft:poison",
      "target": "self"
    }

`remove_effect` 的 `effect` 与 `target` 校验规则和 `add_effect` 相同，但不需要持续时间、等级或显示字段。
目标当前确实拥有并成功移除该效果时，动作才算成功；若触发器配置了冷却，空操作不会开始冷却。

复杂事件动作还可直接作用于存活实体：

    {
      "type": "modify_entity_attribute",
      "attribute": "minecraft:generic.attack_damage",
      "operation": "add",
      "amount": 1.5,
      "minimum": 0,
      "maximum": 100,
      "target": "self",
      "radius": 4
    }

    {"type": "damage_entity", "target": "damage_source", "amount": 3, "radius": 4}
    {"type": "heal_entity", "target": "nearby", "amount": 2, "radius": 8}

三类动作的 `target` 可为 `self`、`event_entity`、`damage_source` 或 `nearby`。
`damage_source` 取当前伤害来源的责任实体；无法解析为 LivingEntity 时动作未生效。
`nearby` 以天赋拥有者为中心搜索 `radius`（0.5–64）内存活实体并排除拥有者自身。
`modify_entity_attribute` 修改目标现有 AttributeInstance 的基础值，属性必须已注册且目标必须实际拥有该属性；
`operation` 为 `add` 或 `multiply`，加值范围为 ±1e9、乘数为 0–1000，结果钳制在 -1e9–1e9 内的自定义上下限。
这是基础值修改而非临时修饰器，重复触发会累积并随实体正常存档；需用上下限、概率、冷却或成功次数控制。
`damage_entity` 与 `heal_entity` 的量必须在 (0, 1000000]。对伤害来源使用 `damage_entity` 可构成反伤；
对其他实体造成的伤害以拥有者为荆棘伤害来源，因此其他模组仍可观察该伤害，触发服务的玩家/事件重入保护会阻止同键无限递归。

触发器可为下一次主动刷新排队一次性候选检索约束：

    {
      "type": "queue_draw_constraint",
      "categories": ["example:light"],
      "pools": ["example:main"],
      "guaranteed": 1,
      "queueMode": "replace",
      "successCounter": "example:hope_successes",
      "successIncrement": 1,
      "milestoneEvery": 9,
      "milestone": {
        "categories": ["example:light"],
        "tags": ["example:ur_material"],
        "guaranteed": 1
      }
    }

选择字段为 `talents`、`categories`、`rarities` 和 `tags`，至少一个非空；每个数组内部为 OR，不同非空字段组之间为 AND。`talents` 与 `rarities` 必须引用已存在定义，类别和标签保持开放 ResourceLocation；`pools` 省略或留空表示任意池，填写时必须引用存在的池。`guaranteed` 为 1–5，但最终不会超过池的 `choices`，合格目标不足时会尽量加入全部可用目标并用普通候选补齐。

`queueMode` 可为 `append`（默认，追加）、`dedupe`（完全相同约束已在队列时不再加入）或
`replace`（先移除与新约束池范围重叠的旧项）。`successCounter` 引用已定义计数器，只在约束确实入队时按
`successIncrement` 原子增加；去重拒绝或队列容量不足时不增加。建议将这种计数器的 `event` 设为 `manual`。
`milestone` 与 `milestoneEvery` 成对出现；当本次成功跨过每 N 次边界时，本次入队改用里程碑选择器。里程碑可重写同样的四组筛选、`pools` 和 `guaranteed`；省略后两者时继承主选择器。

队列属于玩家持久化数据，最多 64 条，死亡克隆、重新登录和换维度均保留；不同池的约束不会互相阻塞。初次打开抽取池不消费约束；只在玩家按下刷新、约束至少匹配一个当前合格候选并成功生成新会话候选后，才消费该池最早的一条适用约束和一次刷新次数。无匹配目标时返回现有“无符合条件天赋”结果，队列和刷新次数均保留。

检索约束优先占据保证格位；保底会先尝试在匹配目标中满足，若未满足且仍有普通补位格，则用于第一个普通补位。若保证格位占满整轮且目标不满足保底，保底不会清零或推进，仍在玩家最终选择结果时按原规则结算。GUI 的“检索+”页支持主/里程碑单目标、标签手填、可选池、保证数量、队列策略和成功计数；多值及多组 AND 组合通过高级 JSON 保存。

四类伤害事件支持修改本次事件携带的伤害值：

    {
      "type": "modify_damage",
      "operation": "multiply",
      "amount": 1.5,
      "minimum": 0,
      "maximum": 100
    }

`operation` 为 `add` 或 `multiply`。加值范围为 -1e9–1e9；乘数范围为 0–1000，禁止负乘数。
`minimum` 和 `maximum` 默认分别为 0 与 1e9，且必须满足 `0 <= minimum <= maximum <= 1e9`。
每条动作先对当前值执行运算，再应用自己的上下界；多个动作按触发器优先级和动作数组顺序依次串联。
结果没有实际变化时，该动作不算成功，也不会单独触发冷却。

`damage_dealt_pre` / `damage_taken_pre` 对应 Forge 1.20.1 `LivingHurtEvent`，其结果仍会接受后续护甲、
药水、附魔等减免；`damage_dealt` / `damage_taken` 对应 `LivingDamageEvent`，此时 Forge 已经计算护甲、药水和
吸收修正并消耗相应资源，修改后的数值直接用于生命值扣除，降低最终值不会返还已经消耗的护甲耐久或吸收资源。
两个阶段都在 `EventPriority.LOWEST` 处理；同一次事件固定先应用攻击者 `damage_dealt*`，再应用受击玩家
`damage_taken*`。同优先级的其他模组监听器仍遵循 Forge 事件总线的注册顺序，不承诺跨模组的绝对末位。

四类伤害事件还支持真正取消事件：

    {"type": "cancel_damage"}

`cancel_damage` 没有其他字段。减免前事件取消后，本次伤害不会继续进入护甲、药水、附魔和生命值流程；最终伤害事件取消后不会扣除生命值，但 Forge 在此阶段之前已消耗的护甲耐久、吸收生命等资源不会返还。攻击者侧取消后不会继续执行同一次 Forge 事件的受击者侧天赋。动作本身会被计为成功，并可启动所在触发器的冷却。

触发器专用的事件上下文条件如下，均可继续放入 `all`/`any`/`not`：

| type | 字段 | 匹配内容 |
|---|---|---|
| event_entity_type | entityType | 事件实体的注册类型 |
| event_entity_tag | tag | 事件实体的实体类型标签 |
| damage_type_tag | tag | 事件伤害来源的伤害类型标签 |
| event_item | item | 事件物品的注册 ID 或配置物品逻辑 ID |
| event_item_tag | tag | 事件物品标签 |
| event_block | block | 事件方块的注册 ID |
| event_block_tag | tag | 事件方块标签 |

这些条件放在获取条件等非事件位置会产生编译错误；事件没有对应上下文时匹配失败。实体类型与方块 ID 在编译时检查当前服务端注册表；物品 ID 可来自当前服务端注册表或配置物品定义，标签 ID 严格检查语法并在运行时按当前标签匹配。伤害事件和 `kill` 的事件物品为拥有天赋玩家当时的主手物品；实体/方块交互为主手物品；`item_use_finish` 为完成使用的物品。只有 `interact_block` 提供事件方块。`death`、两个周期事件和 `player_move_distance` 不提供事件物品；这些事件也不提供事件实体或方块。

单个天赋最多 256 个触发器，每个动作列表最多 256 项。只有天赋启用、前置可用且玩家实际拥有它时才执行；
匹配项按 `priority` 从高到低，再按天赋 ID 和触发器 ID 稳定排序。同一玩家的同类事件设有重入保护。

触发器可用 `actionFailurePolicy` 和 `failureActions` 组成通用失败分支。`actionFailurePolicy` 可为
`continue`（默认）或 `stop`：主动作返回“未生效”时，前者继续尝试后续主动作，后者立即停止主链；只要任一已尝试主动作未生效，主链结束后就执行一次 `failureActions`。失败链本身不会递归启动另一条失败链，同一策略也决定失败链遇到未生效动作后是否继续。失败链中成功生效的动作与主动作一样会启动冷却、增加 `activationLimit` 成功次数并计入运行诊断；若两条链均无动作生效，则本次仍为 `no_action_applied`。两条动作列表各自最多 256 项，`failureActions` 可省略或为空。

“未生效”是动作的运行结果，不等同于编译错误，例如向已有相同效果的实体施加未产生变化的效果、移除不存在的效果或去重队列拒绝重复项。已经成功执行的前序动作不会因后续动作失败而回滚；当前失败分支是明确的顺序执行控制，不是原子事务。高级 JSON 可在触发器的任一分支使用 `play_sound` 和 `show_message` 作为反馈动作；游戏内简单动作表单目前仍只直接编辑状态效果、伤害修改/取消和检索队列，未知或更复杂的动作会按 `[JSON]` 原样保留。

`player_tick` 只应配置确实需要逐 tick 精度的规则；常规周期效果应优先使用开销更低、语义更清晰的 `player_second`。运行时在构造背包、类别及标签上下文前先检查该事件是否有绑定、玩家是否拥有至少一个相关天赋。`/talent_editor status` 提供有界内存的累计计数：事件调用、条件匹配、成功动作、冷却跳过、概率未命中和重入跳过；它不记录玩家身份或逐次上下文，因此不是完整 Trace。

`chance` 是整个触发器一次执行的服务端基础随机概率，范围 0–1，省略时为 1。概率来源可在 `chanceAttribute`、`chanceCounter` 或 `chanceCategory` 中三选一，分别读取玩家当前属性、指定计数器值，或指定类别下已拥有且启用/可用的天赋数。属性和计数器必须引用已注册/已定义目标，类别是开放 ResourceLocation。`chancePerPoint` 范围 -1–1、默认 0：

    有效概率 = clamp(chance + 来源数值 × chancePerPoint, 0, 1)

属性没有附加到该玩家时安全回退到基础概率。最终概率为 0 时永不触发，为 1 时必然触发且不会消耗随机数。`player_move_distance` 还必须设置 0.01–1,000,000 格的 `distanceInterval`；每个玩家+天赋+触发器独立持久累计，到达间隔后保留余数并进行一次条件/概率判定。
`cooldownTicks` 是同一玩家、同一天赋、同一触发器的独立冷却，范围 0–1728000，省略时为 0。概率在冷却检查后判定；
一次触发器中的所有动作共用一次概率与一次冷却，至少一个动作实际成功后才开始冷却。冷却使用随玩家存档保存的累计在线 tick，
退出游戏期间不会流逝，死亡复制玩家能力数据时保持不变。冷却是服务端权威内部状态，目前不需要进入客户端网络协议。

`activationLimit` 是玩家持有该天赋期间该触发器允许成功执行的总次数，范围 0–1,000,000，默认 0 表示不限。设置为 1 即为一次性触发器；GUI 的“一次性”快捷开关只是在 0 和 1 之间设置此既有字段，不引入另一份状态。与冷却相同，只要主链或失败链至少一个动作实际成功才增加计数；条件不满足、冷却中、概率未命中或两条链所有动作均为空操作都不消耗次数。计数随玩家存档保存并在死亡复制时保留；主动移除天赋或该天赋被合成消耗时，会同时清除其触发器次数和冷却，因此以后重新获得可重新执行。服务端最多为每个玩家保留 4096 条限次触发器状态，容量不足时会安全拒绝执行新的限次触发器，而不是执行后无法记账。

游戏内入口为“天赋编辑 → 事件效果”。当前表单用“基础/动作/条件/检索+”四页编辑触发器；可直接选择概率属性、计数器或天赋类别，配置移动距离间隔，并编辑检索队列策略、成功计数与里程碑结果。“动作”页可在主动作和失败后动作之间切换，使用同一套增删、排序和校验；“条件”页可设置继续/停止策略和一次性快捷开关。动作严格按各自数组的显示顺序执行，整个触发器只进行一次概率判定并共享一次冷却与限次计数。更复杂的 JSON 条件树、多值检索组合或未知动作仍显示为 `[JSON]` 只读项并原样保留。

管理员可按需开启有界触发追踪：

    /talent_editor trace on
    /talent_editor trace show 20
    /talent_editor trace off
    /talent_editor trace clear

追踪默认关闭；开启后全服只保留最近 256 条、`show` 单次最多显示 40 条且按新到旧排列。每条记录包含玩家、在线 tick、事件、天赋/触发器、有效概率、成功/尝试动作数和 `condition_miss`、`activation_limit`、`cooldown`、`chance_miss`、`no_action_applied` 或 `activated` 结果。单次触发最多保存前 64 个逐动作结果，记录主链/失败链、数组序号、动作类型与 `ok/fail`；完整尝试总数仍单独保留。命令行每条记录显示前 8 个紧凑步骤和省略数量。关闭追踪时不构造这些逐动作列表。环形缓冲区满后覆盖最旧记录，并在 `trace`/`status` 中报告覆盖数量。它不会写入玩家存档，服务器重启后清空。

## 获取条件与复合条件

    {
      "type": "all",
      "children": [
        {"type": "has_talent", "talent": "example:starter"},
        {"type": "counter_at_least", "counter": "example:kills", "value": 10},
        {"type": "category_count_at_least", "category": "example:combat", "count": 2},
        {"type": "counter_category_total_at_least", "category": "example:undead", "value": 100},
        {
          "type": "has_item",
          "item": "minecraft:diamond",
          "count": 3
        }
      ]
    }

将条件对象放在天赋的 `acquireCondition` 字段中。当前可用类型：

| type | 字段 | 语义 |
|---|---|---|
| always | value | 固定 true/false |
| all | children | 所有子条件成立（AND） |
| any | children | 任一子条件成立（OR） |
| not | child | 子条件取反 |
| has_talent | talent | 玩家已拥有指定天赋 ID |
| counter_at_least | counter, value | 指定通用计数器至少为 value（0–1e12） |
| category_count_at_least | category, count | 当前启用且可用的已拥有天赋中，指定类别至少出现 count 次（0–1,000,000） |
| counter_category_total_at_least | category, value | 同类别下所有启用且可用计数器的玩家当前值总和至少为 value（0–1e12） |
| has_item | item, count | 玩家 Inventory 内指定物品的总数至少为 count（1–1,000,000） |

条件树最大深度 16，最多 256 个节点。天赋 ID 按当前服务端定义校验；物品 ID 可引用服务端配置物品定义或当前物品注册表。
`has_item` 当前是“持有检查”，不会消耗物品。配置物品按逻辑 ID 统计玩家 Inventory 中的总数。

天赋类别数量是从当前活动定义和拥有天赋实时派生的只读值，不占用玩家计数器条目；定义缺失、已禁用或因前置模组缺失而不可用的天赋不计入。
计数器类别总值则汇总计数器定义 `categories` 中带有目标类别的活动规则数值。二者使用不同条件类型，避免“拥有几个同类天赋”和“同类事件累计了多少”发生语义混淆。

游戏内“获取条件”页可编辑由五种业务叶子组成的一层 AND/OR 组，并可打开引用选择器：天赋、计数器和物品支持按 ID/显示名检索，天赋类别和计数器类别分别按 ID 及引用次数检索。相同选择器也用于合成消耗、天赋元数据的类别/稀有度、天赋所属抽取池和抽取池高级引用；选择结果会追加到现有输入且自动避免重复，返回时不会丢失未应用的其他字段。选择器标明目录来源和当前状态；编辑页将引用汇总为可用、禁用、不可用、当前未找到和格式错误五类。对开放类别和动态标签而言，“当前未找到”是诊断而非自动删除。嵌套、NOT 及扩展类型可在 JSON 中使用；当前 GUI 会将它们作为只读内容完整保留。

## 定义反向引用

运行时快照会从编译后的完整定义建立反向引用，不要求引用方处于启用状态。已有天赋、稀有度、抽取池、计数器、布局和配置物品的编辑页可以查看引用总数、引用方类型/ID、用途和 JSON 路径。当前索引包括：

- 天赋的 `rarity`、`categories`、`offer.pools`、获取条件、触发条件及合成天赋/计数器消耗；
- 抽取池的 `rarityWeights`、`luckFactors`、`pityRarity` 和 `duplicateCounter`；
- 自动计数器的 `categories` 和 `requiredTalents`；
- 配置物品在 `has_item`/`event_item` 条件、`synthesis.consumeItems` 和自动计数器 `items` 中的逻辑 ID 引用；
- 布局前置页面格位的 `talent`，保留 `$.pages[i].slots[j].talent` 路径；
- 条件树中的直接天赋、直接计数器、天赋类别和计数器类别引用，并保留嵌套 `children`/`child` 路径。

引用索引属于派生元数据，不写入 JSON、不进入定义内容哈希。管理员启动协议发送每个实体定义的准确引用总数，并将全局引用明细限制为 50,000 条；目标明细不完整时界面会提示。引用页是整体定义删除的入口：只有实际来源为 `talent_editor_generated` 且完整入向引用为空的文件型定义才能删除。服务端会重新校验权限、revision、来源和引用，并在移动原文件前编译无目标候选快照。文件移入 `.talent_editor_recovery/deletions`；数据包重载失败时自动尝试恢复。

## 编辑事务与撤销

生成数据包中的新建、覆盖和删除会写入 `.talent_editor_history/<UUID>/transaction.json`，并在存在时保存 `before.json` 和 `after.json`。manifest 记录操作、定义类型/ID、时间、前后 SHA-256 和事务状态；这些编辑元数据不是 Schema 2 定义，不会被资源加载器当成天赋读取。

历史默认最多保留 100 条。对同一类型和 ID，只有最新的未撤销事务可用，且当前生成文件必须与 `afterHash` 一致（删除事务则要求文件仍不存在）。撤销新建相当于受保护删除；撤销覆盖或删除则恢复 `before.json`。所有操作都会再检查管理权限、base revision 和完整候选快照，随后重载数据包；失败时恢复撤销前的文件及运行时。

## 进阶 / 合成

`synthesis` 属于结果天赋本身，因此天赋仍是唯一的编辑主体：

    {
      "schema": 2,
      "text": {"name": "高阶战斗本能"},
      "rarity": "example:rare",
      "acquireCondition": {
        "type": "counter_at_least",
        "counter": "example:kills",
        "value": 20
      },
      "synthesis": {
        "enabled": true,
        "consumeTalents": ["example:combat_instinct"],
        "consumeCounters": {"example:kills": 10},
        "consumeItems": {"minecraft:diamond": 3}
      }
    }

| 字段 | 必需 | 说明 |
|---|---|---|
| enabled | 否 | 是否在合成入口中显示，默认 false |
| consumeTalents | 否 | 成功后移除的已拥有天赋 ID 数组 |
| consumeCounters | 否 | 计数器 ID 到消耗数量的对象，单项 1–1e12 |
| consumeItems | 否 | 注册物品 ID 或配置物品逻辑 ID 到消耗数量的对象，单项 1–1,000,000 |

`acquireCondition` 只做检查，`synthesis` 的三类消耗只在合成成功时扣除。服务端先检查目标、获取条件与所有消耗，再一次提交；条件不足时不会部分扣除。单个合成最多包含 256 个消耗项，且不能消耗结果天赋自身。

游戏内天赋编辑页的“合成设置”可配置全部字段。天赋册可切换到“可合成”模式，查看当前/所需数量并请求服务端执行；也可使用：

    /talent_editor synthesize <结果天赋ID>

## 严格行为

- 未知字段是错误，不会静默忽略拼写错误；
- 任一错误使整批候选定义拒绝激活；
- 上一份有效快照继续运行；
- 缺少 requiredMods 是警告，相关天赋保留但 inactive；
- 缺少未声明前置的属性是错误；
- 对象字段换序不会改变内容哈希；
- 列表顺序仍有语义，会改变哈希。
