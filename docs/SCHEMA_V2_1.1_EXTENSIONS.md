# Schema 2 — 1.1.0 运行时扩展

1.1.0 保持 `"schema": 2`，旧版定义无需迁移。本页只列新增字段；基础字段仍见 `SCHEMA_V2.md`。

## 实施顺序与覆盖范围

本版本按以下依赖顺序实现：

1. 安全数值表达式与持久状态；
2. 天赋获取历史、在线时间/抽取次数生命周期和 `onRemove`；
3. 授予/移除天赋、计数器和物品等转化动作；
4. 升级、抽取、刷新、计数变化、致命伤害与附近死亡事件；
5. 公式伤害/治疗、永久成长、临时属性和通用目标选择；
6. 游戏内生命周期、表达式、动作链与目标选择可视化表单，并保留原始 JSON 兜底入口。

按需求，本版本不修改幸运值与 SSR 抽取率的关系，也不加入模型透明、真实投射物、Curios 槽位联动等困难项。

## 安全表达式

表达式最长 512 字符、最多 256 个 AST 节点。它不是脚本：不能访问 Java、命令、文件、网络或反射。

运算符：`+ - * / % ^`、一元 `+ -`、括号。除数为零时结果为 0；非有限结果归零。

白名单函数：

- `min(a,b)`、`max(a,b)`、`pow(a,b)`、`clamp(value,min,max)`
- `sqrt(x)`、`abs(x)`、`floor(x)`、`ceil(x)`、`round(x)`
- `random()`：使用玩家服务端随机源，范围 `[0,1)`
- `counter("namespace:id")`
- `state("namespace:id")`、`target_state("namespace:id")`
- `attribute("namespace:id")`、`target_attribute("namespace:id")`
- `attribute_base("namespace:id")`：读取自身属性基础值
- `attribute_ratio_bonus("namespace:id")`：读取自身属性相对基础值的增幅比例
- `category_count("namespace:id")`、`rarity_count("namespace:id")`
- `has_talent("namespace:id")`、`has_ever_talent("namespace:id")`、`has_item("namespace:id")`；返回 0 或 1

内置变量：

`level`、`experience_progress`、`health`、`max_health`、`health_percent`、`hunger`、`hunger_percent`、`saturation`、`current_damage`、`event_amount`、`distance_to_target`、`target_health`、`target_max_health`、`target_health_percent`、`online_players`、`owned_count`、`world_time`、`talent_age_ticks`、`draws_since_acquired`。

示例：

```text
max_health * 0.15 + counter("example:growth") * 0.5
clamp(1 - state("example:shield") * 0.02, 0.1, 1)
random()
```

## 生命周期

天赋根对象可加入：

```json
"lifecycle": {
  "durationTicks": 1200,
  "removeAfterDraws": 2,
  "retainEffects": true,
  "retainCategoryContribution": true
}
```

- `durationTicks`：获得后的在线 tick；0 禁用。
- `removeAfterDraws`：获得后再完成多少次选择才消失；0 禁用。新获得天赋不会被同一次抽取立即移除。
- `retainEffects`：消失后继续应用 `activeEffects`，但不再运行 triggers。
- `retainCategoryContribution`：消失后继续计入类别/稀有度表达式统计。
- 到期会执行 `onRemove`，刷新属性并同步客户端。

## 新事件

| event | `event_amount` | 说明 |
|---|---:|---|
| `level_up` | 本次增加等级 | 只在正向升级时触发 |
| `draw_selected` | 0 | 一次抽取选择成功结算后 |
| `draw_refreshed` | 0 | 抽取候选刷新成功后 |
| `counter_changed` | 变化后的计数 | 自动计数、重置及外部动作变化 |
| `fatal_damage` | 当前最终伤害 | 玩家将受到不低于当前生命的伤害时；可改伤或取消 |
| `nearby_entity_death` | 0 | 同维度 64 格内实体死亡；死亡实体是 event entity |

旧事件保持不变。

## 表达式条件

```json
{
  "type": "expression_compare",
  "left": "counter(\"example:kills\")",
  "operator": "gte",
  "right": "10"
}
```

`operator`：`lt`、`lte`、`eq`、`neq`、`gte`、`gt`。概率公式可写成 `left: "random()"`、`operator: "lt"`、`right: "目标概率公式"`。

## 通用状态与资源动作

### `modify_state`

```json
{"type":"modify_state","state":"example:growth","operation":"add",
 "expression":"1","minimum":0,"maximum":100,"target":"self"}
```

`operation` 为 `set/add/multiply`。目标可为 `self/event_entity/damage_source/nearby`；非玩家实体状态存入实体持久 NBT。

### `modify_counter`

```json
{"type":"modify_counter","counter":"example:kills","operation":"add",
 "expression":"2","minimum":0,"maximum":1000000}
```

变化会进入原有计数器同步并触发 `counter_changed`。

### `give_item` / `take_item`

```json
{"type":"give_item","item":"minecraft:emerald","count":"2+level/10","dropOverflow":true}
{"type":"take_item","item":"minecraft:emerald","count":"4"}
```

`take_item` 先检查完整数量，数量不足时不扣任何物品并让动作返回失败，可配合 `actionFailurePolicy:"stop"`。

### `give_draw_tokens`

```json
{"type":"give_draw_tokens","amount":"1"}
```

### `grant_talent` / `revoke_talent`

```json
{"type":"grant_talent","talent":"example:awakened","onlyIfNeverAcquired":true}
{"type":"revoke_talent","talent":"example:seed","retainEffects":true,
 "retainCategoryContribution":true}
```

授予动作会检查目标天赋可用性及 `acquireCondition`，执行获得动作、刷新属性并同步。移除动作会执行目标天赋的 `onRemove`。

## 公式战斗与属性动作

### `modify_damage_expression`

仅可用于伤害事件和 `fatal_damage`：

```json
{"type":"modify_damage_expression","operation":"multiply",
 "expression":"clamp(1-state(\"example:guard\")*0.01,0.1,1)",
 "minimum":0,"maximum":1000000}
```

### `affect_health_expression`

```json
{"type":"affect_health_expression","operation":"heal",
 "expression":"max_health*0.2","maximum":1000,"target":"self"}
```

`operation` 为 `damage` 或 `heal`。

### `modify_attribute_base_expression`

永久修改所选实体的属性基础值，适合“每次触发永久成长”：

```json
{"type":"modify_attribute_base_expression","attribute":"minecraft:generic.max_health",
 "operation":"add","expression":"1","minimum":1,"maximum":2048,"target":"self"}
```

### `timed_attribute_modifier`

```json
{"type":"timed_attribute_modifier","attribute":"minecraft:generic.attack_damage",
 "expression":"2+level*0.1","operation":"addition","modifierKey":"rage",
 "durationTicks":100,"target":"nearby","radius":8,"relation":"ally",
 "order":"nearest","limit":4}
```

再次应用相同玩家、天赋和 `modifierKey` 会刷新数值及到期时间。`operation` 为 `addition/multiply_base/multiply_total`。

## 目标选择器

支持选择器的新动作接受：

- `target`: `self/event_entity/damage_source/nearby`
- `radius`: 0.5–64，仅 nearby
- `relation`: `any/ally/enemy`
- `order`: `nearest/farthest/random`
- `limit`: 1–256

## 游戏内编辑

`F8 → 天赋 → 编辑天赋` 的单页分组列表提供：

- “生命周期/消失”：结构化编辑 lifecycle；
- “触发器条件与动作”：选择任一触发器后，以表单编辑 `expression_compare`、正常动作链和可选的失败补救动作链；补救链只在正常动作返回未生效时执行，并非重复触发事件；
- “获得时/合成时/移除时动作”：按顺序添加、编辑、删除及移动相应阶段动作；
- 高级动作表单：覆盖本页列出的 11 种新增动作，并按动作类型显示资源 ID、表达式、运算、上下界、持续时间和布尔选项；`modify_state` 的状态 ID 可从当前定义、未保存草稿及玩家存档中搜索选择；
- “实体目标选择器”：单独编辑目标来源、半径、敌我关系、顺序和目标上限；
- “原始完整 JSON”：仅用于未来字段或尚未识别的第三方扩展。

主页按显示、抽取与成长、持有效果、事件与动作、高级分组；生命周期仅出现一次。任一直接子页面保存或返回后会回到该单页列表，并保留滚动位置。新增的六种事件也已加入普通触发器事件选择器。表单完成后仍需“仅验证/保存并启用”，所有字段由服务端编译器校验。混合在 `onAcquire` / `onSynthesize` 中的表现动作与高级动作会互相保留。

## 天赋树预览与关系编辑

`F8 → 天赋树预览` 将每个已有天赋显示为带图标的节点，并把两类天赋间关系绘制为有方向的连线：

- 青色实线：`acquireCondition` 中正向的 `has_talent` 前置关系；
- 金色虚线：`synthesis.consumeTalents` 中的进阶/合成消耗关系；禁用的合成配置以灰线显示。

图使用稳定的从左到右分层布局，支持按 ID/名称定位、滚轮缩放、空白区域拖动平移，以及只显示获取或进阶/合成关系。单击节点选择，双击节点打开完整天赋；单击连线选择，再次单击或右键打开对应获取条件或合成条件表单。选中节点后可从天赋目录新增前置/消耗关系，选中连线后可删除。新增和删除仅先修改客户端草稿并打开原表单复核，最终仍由“保存并启用”的 revision 与服务端编译校验提交。

一层 `all` / `any` 和基础叶子条件可从关系图安全增删。嵌套组、`not` 或扩展条件仍会提取其中正向天赋关系用于展示，但相关获取连线标记为只读，图界面不会把高级条件扁平化；其他计数器、类别和物品条件/消耗会在选中节点的摘要中显示数量，并可进入原表单继续编辑。

完整示例见 `examples/advanced_runtime_pack`。
