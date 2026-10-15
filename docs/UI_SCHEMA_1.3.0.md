# Schema 2：1.3.0 UI 扩展

布局文件仍在 `data/<namespace>/talent_editor/layouts/<id>.json`，新增可选 `bookUi`、`drawUi` 对象。省略某个对象表示保留对应旧界面。

```json
{
  "schema": 2,
  "priority": 10,
  "bookUi": {
    "width": 480,
    "height": 270,
    "backdrop": -1609550816,
    "elements": [
      {"id":"title","type":"text","x":12,"y":8,"width":400,"height":20,"binding":"title","color":0},
      {"id":"first","type":"slot","index":0,"x":12,"y":44,"width":180,"height":54,"textX":32},
      {"id":"details","type":"details","x":206,"y":44,"width":260,"height":186,"textY":30,"rarityY":44,"bodyY":60},
      {"id":"previous","type":"button","action":"previous","x":12,"y":238,"width":50,"height":22},
      {"id":"next","type":"button","action":"next","x":66,"y":238,"width":50,"height":22},
      {"id":"draw","type":"button","action":"draw","x":300,"y":238,"width":100,"height":22},
      {"id":"close","type":"button","action":"close","anchor":"bottom_right","x":-12,"y":-10,"width":60,"height":22}
    ]
  }
}
```

画布：`width` 240–4096，默认 480；`height` 135–4096，默认 270；`backdrop` 有符号 32 位 ARGB，默认 `0xA0101420`；`elements` 最多 320 项，数组顺序就是从底到顶的图层顺序。

正式页面与全屏预览使用 `backdrop` 填满整个屏幕，包括等比缩放后画布外的区域。图片保持原画布比例；需要完全遮盖游戏画面时将底色 Alpha 设为 `FF`。编辑画布时底色仅绘制在画布内，不覆盖编辑工具。

| 字段 | 默认值 / 范围 |
|---|---|
| id | 稳定且唯一的 `[a-z0-9_.-]{1,64}` |
| type | panel / image / text / button；bookUi 另有 slot / details，drawUi 另有 offer |
| anchor | top_left；另有 top_center / top_right / middle_left / middle_center / middle_right / bottom_left / bottom_center / bottom_right |
| x / y | 0；-4096–4096；相对锚点的偏移 |
| width / height | 100 / 24；4–4096 |
| opacity | 1；有限数字 0–1 |
| color / hoverColor / textColor | `0xB0223044` / `0xD04A7098` / `0xFFFFFFFF` 的有符号 32 位十进制值 |
| texture / hoverTexture | 空；显式命名空间的 `textures/*.png` 资源 ID，最多 512 字符 |
| text | 空；最多 2048 字符；按钮留空使用动态功能标签 |
| binding | 空，或允许的动态内容绑定 |
| action | close；必须属于该页面的功能白名单 |
| target | 空，或完整资源 ID；进入抽取时作为池 ID |
| index | 0；slot 为 0–255，offer 为 0–4 |
| textScale | 1；0.5–3 |
| align | left；left / center / right；文字水平对齐 |
| buttonStyle | custom；custom / vanilla；vanilla 使用原版按钮底图 |
| iconSize | 16；0–128，0 隐藏图标 |
| iconX / iconY | 4 / 4；-4096–4096 |
| textX / textY | 4 / 4；-4096–4096；模块内的文字起点 |
| rarityY | 18；-4096–4096；模块内的稀有度文字纵坐标 |
| bodyY | 34；0–4096；正文起点 |

旧的 1.3.0 UI 对象省略 `align` / `buttonStyle` 时仍使用左对齐和自定义底图；新建模板显式设置居中标题与原版按钮，不会隐式改变已保存的自定义界面。

JSON 颜色是 Java 有符号 int 的十进制；编辑表单使用更直观的 `#AARRGGBB`。例如白色为 `-1`，完全透明为 `0`。纹理不读取 URL 或任意本地文件。

bookUi 的绑定：`title`、`summary`、`page`、`page_background`（用于背景图片）；drawUi 的绑定：`title`、`summary`、`mechanics`、`status`。`page_background` 优先选择当前前置页的背景覆盖，留空或后续页面使用该模块的 `texture`。

`summary` 在天赋册仅显示已拥有天赋数，在抽取页显示剩余抽取次数和刷新次数；`mechanics` 显示幸运值与非零稀有度概率，稀有度仅显示 ID 的路径部分，不带命名空间。计数器卡片只显示当前计数。默认抽取模板不放置实际加成开关，作者仍可自行添加 `effects` 动作按钮。

bookUi 的动作：`close`、`draw`、`mode`、`owned`、`synthesis`、`counters`、`synthesize`、`previous`、`next`。drawUi 的动作：`close`、`confirm`、`refresh`、`effects`。

slot 索引必须从 0 连续排列且不得重复，至少一项；offer 必须完整提供 0–4 五项。天赋册前置页面格位数不得超过自定义 slot 数量。超出屏幕的几何允许用于自由设计，编辑器的图层列表仍能选中它们；应通过预览确保所需入口可见。Esc 始终由 Screen 保留。

严格编译器拒绝未知字段、非法动作、无效资源 ID、非有限数字、重复索引和超限配置。网络用同一解码器解析两个 UI 对象，协议 45；旧协议客户端不能连接。单个 UI JSON 最多 262144 字符，补齐默认字段后的网络 JSON 另限 262144 个 UTF-8 字节（中文等多字节内容按字节计数）；整个布局通过编辑器保存时仍受原有 131072 字符上限约束。

## 编辑器元数据（协议 45）

元素可选字段 `label`（字符串，最多 128 字符，默认空）是图层别名；`locked`（布尔，默认 false）阻止编辑器移动、删除等排版操作。这两个字段不改变玩家界面的外观或按钮功能。旧布局缺省时保持可编辑，Schema 仍为 2。
