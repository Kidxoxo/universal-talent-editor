# Schema 2 — 1.3.1 通用困难项接口

1.3.1 继续使用 `"schema": 2`。新字段均为可选项，旧定义无需迁移。

## 1. 持久数值 HUD

`hud_resource` 只能放在 `activeEffects`，将已有持久数值状态显示为 HUD 条：

```json
{
  "type": "hud_resource",
  "state": "example:energy",
  "maximum": "100 + level * 2",
  "color": 5614335,
  "order": 0,
  "showWhenFull": true
}
```

- `state`：命名空间化持久数值 ID。
- `maximum`：受限数值表达式，结果必须大于 0，否则显示端按 1 处理。
- `color`：`0..16777215` 十进制 RGB。
- `order`：`-10000..10000`，较小的值先显示。
- `showWhenFull`：数值已满时是否仍显示。

同一状态同时由多个天赋贡献时，只显示排序最靠前的一条。最多显示 8 条，服务端同步最多 32 条。

## 2. 玩家模型渲染

`player_render` 只能放在 `activeEffects`：

```json
{
  "type": "player_render",
  "alpha": "1 - counter(\"example:transparency\") / 100",
  "scale": "1 + state(\"example:model_growth\") * 0.01",
  "hideNameplate": true
}
```

- `alpha`：受限数值表达式；最终结果限制为 `0.05..1`。仍兼容原有数字写法。
- `scale`：受限数值表达式；最终结果限制为 `0.1..4`。仍兼容原有数字写法。
- `hideNameplate`：隐藏玩家名牌。

多个源的透明度和缩放会相乘，最终值再限制在上述范围。这是客户端渲染控制，不会修改碰撞箱、视角高度或属性；第三方自定义渲染层是否完全遵循 alpha 取决于该模组的渲染实现。

## 3. 物品提交会话

`open_item_submission` 只能用于事件动作：

```json
{
  "type": "open_item_submission",
  "item": "minecraft:emerald",
  "count": "max(1, level)",
  "timeoutTicks": 600,
  "consume": true
}
```

界面只负责展示和确认；背包数量、超时和实际扣除均由服务端再次验证。每个玩家同时只保留一个会话，数量上限为 4096。提交成功后会触发 `item_submitted`，其中 `event_amount` 为实际要求数量，事件物品为被提交的物品。

## 4. 可选 Curios 条件

当且仅当 Curios 已加载时，条件列表才显示“Curios 槽位已装备物品”：

```json
{
  "requiredMods": ["curios"],
  "acquire": {
    "type": "curios_equipped",
    "item": "example:charm"
  }
}
```

运行时通过反射连接 Curios API，因此本模组不强制依赖 Curios。条件检查任意 Curios 槽位内的精确物品 ID，目前不区分槽位类型、NBT 或物品标签。Curios 缺失时，定义必须将 `curios` 放入 `requiredMods`，否则编译器会报错；含有该依赖时则以“不可用”处理。

## 5. 可视化编辑入口

天赋编辑单页的“HUD 与玩家模型渲染”可编辑 `hud_resource` 与 `player_render`；透明度和缩放字段直接接受表达式。`open_item_submission` 在相应动作链中也有独立表单。Curios 条件使用已有的可搜索物品选择器，但只在 Curios 存在时显示。

先前试验性的投射物配置、投射物目录与伤害阶段诊断已在游戏内验证后回退，不属于当前接口。

## 6. 玩家属性作为获取/合成条件

`acquireCondition` 可用 `attribute_compare` 检测玩家当前实际属性值（包含装备和修饰器）。合成也会先检查同一条件，因此可用它设置进阶门槛：

```json
{
  "type": "attribute_compare",
  "attribute": "minecraft:generic.attack_speed",
  "operator": "gte",
  "value": 8
}
```

比较方向为“玩家属性值 `operator` 目标值”。`lt` 小于、`lte` 小于或等于、`eq` 等于、`neq` 不等于、`gte` 大于或等于、`gt` 大于。天赋编辑器的“获取条件”页面可搜索属性 ID、填写小数阈值并切换比较方式。游戏抽取候选和合成判定均检查该条件；没有玩家上下文时不视为满足。

## 7. 检索结果占位

`queue_draw_constraint` 的每条待用检索按队列顺序占据下一次新抽取或刷新中的结果格位，`guaranteed` 指该条检索最多占据的格数。已占满抽取池的 `choices` 格时，后续检索返回失败，不改变已排队的约束或成功计数；特别是 `replace` 也不能覆盖满格队列。若当前没有符合筛选条件的可抽取天赋，则该格会按普通规则补齐。

## 粒子动画动作

`spawn_particles` is available in trigger actions and in `onAcquire`, `onSynthesize`, and `onRemove`.
The visual editor has **Add particles** in each of these action lists. Its picker searches the
client's registered simple particle types by full namespace ID, and Preview emits one local wave.
Saving a definition also validates the server's particle registry. A matching
`assets/<mod>/particles/*.json` file alone does not register a particle type. Missing mod IDs and
unregistered or parameterized particle types are rejected with an explicit validation issue.

```json
{
  "type": "spawn_particles",
  "particle": "minecraft:end_rod",
  "anchor": "body",
  "motion": "inward",
  "count": 12,
  "radius": 1.3,
  "speed": 0.075,
  "durationTicks": 12,
  "intervalTicks": 3,
  "viewDistance": 24,
  "cooldownTicks": 10
}
```

`anchor` is `feet`, `body`, or `head`; `motion` is `burst`, `inward`, or `outward`.
The limits are 1–32 particles per wave, radius 0–4 blocks, speed 0–0.3,
duration 1–60 ticks, wave interval 1–20 ticks, viewing distance 4–64 blocks,
and cooldown 0–1200 ticks. An animation may emit at most 256 particles.
The runtime also limits simultaneous animations to eight per player and 128 per server.
For a high-frequency counter trigger, set a trigger cooldown as well as the action cooldown.
Only viewers within `viewDistance` in the same dimension receive the particle packets.
