# Schema 2 — 1.2.0 通用接口扩展

1.2.0 继续使用 `"schema": 2`。所有字段均为可选项；旧配置不需要迁移，未启用新字段时行为与 1.1.0 相同。

## 1. 消失后保留事件效果

生命周期新增 `retainTriggers`：

```json
"lifecycle": {
  "durationTicks": 600,
  "retainEffects": true,
  "retainCategoryContribution": false,
  "retainTriggers": true
}
```

到期后天赋不再属于“持有”，但其 `triggers` 仍会参与事件匹配；触发器冷却、触发次数和移动距离进度会继续保留。重新获得天赋时自动合并回正常持有来源。

`revoke_talent` 动作同样支持：

```json
{
  "type": "revoke_talent",
  "talent": "example:temporary",
  "retainEffects": false,
  "retainCategoryContribution": false,
  "retainTriggers": true
}
```

生命周期页面和高级动作页面均提供独立开关。

## 2. 抽取所选天赋上下文

`draw_selected` 现在携带本次选中的天赋 ID、类别和稀有度，可使用以下事件条件：

```json
{"type":"event_talent","talent":"example:lucky_star"}
{"type":"event_talent_category","category":"example:luck"}
{"type":"event_talent_rarity","rarity":"example:ur"}
```

三种条件已加入普通事件效果页面的条件选择器和对应搜索列表。其他事件没有所选天赋上下文，这些条件会返回 `false`。

表达式另增加：

- `ever_category_count("namespace:id")`：历史上曾获得过的该类别天赋数量。
- `ever_rarity_count("namespace:id")`：历史上曾获得过的该稀有度天赋数量。

抽到一个此前从未获得的 UR 后，事件执行阶段的 UR 历史数量为 1，因此可与 `event_talent_rarity` 组合判断“第一次获得该稀有度”。

## 3. 事件实体状态效果

新增事件条件：

```json
{"type":"event_entity_effect","effect":"minecraft:slowness"}
```

它检查当前事件实体是否拥有指定状态效果，可用于眩晕、冻结或其他模组注册的效果。编辑器使用可搜索的状态效果列表。

## 4. 静止与逐 tick 移动表达式

新增变量：

- `stationary_ticks`：玩家连续静止的服务端 tick 数。
- `stationary_seconds`：`stationary_ticks / 20`。
- `movement_distance_tick`：玩家上一 tick 的三维移动距离。

移动超过 0.001 格会重置静止计时；维度变化和超过 64 格的位置跳跃也会重置，避免传送被误判为普通移动。该状态在退出服务器时清除。

示例：

```json
{
  "type": "expression_compare",
  "left": "stationary_seconds",
  "operator": "gte",
  "right": "5"
}
```

通常将此条件用于 `player_second`，并通过冷却或成功次数限制控制执行频率。

## 5. 附近实体计数

新增表达式查询：

```text
nearby_count("radius=100;kind=player;relation=any")
```

参数以分号分隔：

| 参数 | 值 | 说明 |
|---|---|---|
| `radius` | `0.5..256` | 必填，查询半径 |
| `kind` | `living` / `player` | 默认 `living` |
| `relation` | `any` / `ally` / `enemy` | 默认 `any` |
| `type` | 命名空间实体 ID | 可选 |
| `tag` | 命名空间实体类型标签 | 可选 |
| `include_self` | `true` / `false` | 默认 `false` |

示例：

```text
nearby_count("radius=32;kind=living;relation=enemy;tag=minecraft:undead")
nearby_count("radius=100;kind=player;relation=any;include_self=true")
```

表达式编译时会检查格式、命名空间和半径上限。一次查询最多计入 4096 个实体。高频触发器应尽量使用较小半径，或放在 `player_second` 事件中。

## 6. 负值执行相反生命操作

`affect_health_expression` 新增 `invertNegative`，默认 `false`：

```json
{
  "type": "affect_health_expression",
  "operation": "damage",
  "expression": "random()*20-10",
  "invertNegative": true,
  "maximum": 20,
  "target": "event_entity"
}
```

- 表达式为正：执行 `operation`。
- 表达式为负且 `invertNegative=true`：取绝对值并执行相反操作；伤害变治疗，治疗变伤害。
- 表达式为负且未启用：仍按旧行为钳制为 0。
- 最终绝对值仍受 `maximum` 限制。

高级动作参数页面提供“负值执行相反生命操作”开关。

## 7. 仍保留的边界

本阶段没有加入客户端模型透明、自定义 HUD/资源条、专属投射物或 Curios 槽位 API。这些能力涉及客户端渲染、跨模组可选依赖或新的交互协议，应作为后续独立模块实现，避免把某张天赋表的专用逻辑硬编码进通用运行时。
