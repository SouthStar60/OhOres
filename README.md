# OhOres — 稀有矿物提取器

Minecraft **1.21.1** + **NeoForge 21.1.x** 模组。添加一个方块「稀有矿物提取器」：喂它便宜的方块（泥土、圆石……），
等上一段时间，它就会"提炼"出你在界面里选中的那一种矿物。

- modid：`ohores`
- 版本：1.0.0
- 依赖：NeoForge `[21.1, 21.2)`（已在 21.1.209 上编译通过）、Minecraft `[1.21.1, 1.21.2)`

## 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.x（推荐 21.1.209）。
2. 把 `OhOres-1.0.0.jar` 放进 `.minecraft/mods/`。
3. 启动游戏。首次进入世界后，服务端会生成配置文件 `config/ohores-server.toml`（单人游戏在 `.minecraft/config/`，
   服务器在服务端根目录的 `config/`）。

## 玩法

- 获取：工作台合成 —— 8 个铁锭 + 1 个钻石（3×3 外圈铁锭、中间钻石）。
- 界面是原版灰底 + 原版槽位框，但**不是熔炉布局**：三个格子排成同一行，输入 → 目标 → 产物，中间原版箭头显示进度。
  - 第 1 格 = **消耗品**输入（只接受配置里允许的物品）。
  - 第 2 格 = **当前目标**（只读展示，没有槽位框；**点这里**弹出可搜索的矿物列表）。
  - 第 3 格 = **产物输出**（只能取出，不能放入）。
  - 机器区右下角的原版 **`!`** 小按钮（13×13）列出所有可用消耗品，并标出每种的**贫矿等级**。
- **放进就消耗**：输入格数量够（≥ 该矿物数量 × 该物品贫矿等级，向上取整）时，机器立刻把这批扣掉并开始这一轮；
  这一轮已经付过账，中途拿走输入、换目标都不会中断，直到产出 1 个。
- 产物格被别的物品占满时停在 100%，腾出空位后自动收尾；一轮结束后若输入还够，自动开始下一轮。

## 配置 `config/ohores-server.toml`

```toml
# ========== 稀有矿物提取器 / Rare Ore Extractor ==========
# 这个文件决定三件事：界面里能选哪些矿物、哪些方块能当消耗品、每样要多久要多少。
# 改完保存 → 重启游戏或服务器才生效（/reload 不会重读这个文件）。
# 物品一律写英文 id，例如 minecraft:stone、ftbmaterials:tin_raw_ore；写中文名字没用。
# 写错的 id 或标签不会崩游戏：界面里会把它显示成「此矿物不存在 / 此消耗品不存在」，
# 并排在列表最下面，方便你回来改。

[extractor]

	# ---- 默认值：下面三项是 ores / consumables 里没写数字时的兜底 ----
	# 矿物没写时间时用它，单位秒
	default_seconds = 60
	# 矿物没写消耗数量时用它，单位个（1-64）
	default_consume = 4
	# 消耗品没写贫矿等级时用它，也就是消耗倍数，可为小数；1.0 = 正常成色
	default_poor_grade = 1.0

	# ---- 矿物 ores：界面里能选的目标，列表顺序就是界面顺序 ----
	# 写法： id / id=秒 / id=秒,数量 / id=,数量（没写的部分用上面三个默认值）
	#   "ftbmaterials:tin_raw_ore=45,4"  -> 45 秒，消耗 4 个
	#   "ftbmaterials:iron_dust=30"      -> 30 秒，数量用 default_consume
	#   "ftbmaterials:iron_dust=,6"      -> 默认时间，消耗 6 个
	# 以 # 开头表示标签，例如 "#c:raw_materials/tin=45,4"：标签会展开成标签里的每一种物品，
	# 各占一行、各自独立计时；所以装了多个模组都有粗锡时，一条标签就能全收进来。
	# 查标签顺序：先物品标签，再方块标签（方块自动换成它的物品形态）。
	# 没装提供该标签的模组时标签是空的，界面会显示「此矿物不存在」。
	# 同一种物品重复写以第一条为准。
	ores = [
		# 粗矿类：天然存在的矿物原料，越稀有的越慢越贵
		"#c:raw_materials/aluminum=45,4",
		"#c:raw_materials/tin=45,4",
		"#c:raw_materials/zinc=45,4",
		"#c:raw_materials/lead=50,4",
		"#c:raw_materials/silicon=60,4",
		"#c:raw_materials/nickel=90,5",
		"#c:raw_materials/silver=90,5",
		"#c:raw_materials/titanium=90,6",
		"#c:raw_materials/chromium=120,6",
		"#c:raw_materials/uranium=120,6",
		"#c:raw_materials/antimony=150,7",
		"#c:raw_materials/tungsten=180,8",
		"#c:raw_materials/platinum=210,9",
		"#c:raw_materials/osmium=240,10",
		"#c:raw_materials/iridium=240,10",
		"#c:raw_materials/plutonium=300,12",

		# 粉尘类：模组的天然矿物粉尘，不含合金粉（黄铜/青铜/康铜/因瓦/琥珀金/钢/不锈钢/流明），
		#   合金是熔炉体系的二次加工产物，这里不当作「天然矿源」。
		# 平衡规则：同一种材料如果既有粗矿又有粉末，两者时间和数量保持一致（差值 0），
		#   避免玩家只挑便宜的形态刷；只存在其中一种的材料按自身稀有度取值。
		"#c:dusts/salt=30,4",
		"#c:dusts/aluminum=45,4",
		"#c:dusts/bauxite=30,4",
		"#c:dusts/silicon=60,4",
		"#c:dusts/tin=45,4",
		"#c:dusts/zinc=45,4",
		"#c:dusts/lead=50,4",
		"#c:dusts/niter=45,4",
		"#c:dusts/sulfur=45,4",
		"#c:dusts/nickel=90,5",
		"#c:dusts/silver=90,5",
		"#c:dusts/chromium=120,6",
		"#c:dusts/apatite=60,5",
		"#c:dusts/cinnabar=60,5",
		"#c:dusts/fluorite=60,5",
		"#c:dusts/graphite=60,5",
		"#c:dusts/titanium=90,6",
		"#c:dusts/monazite=90,5",
		"#c:dusts/uranium=120,6",
		"#c:dusts/antimony=150,7",
		"#c:dusts/lithium=120,6",
		"#c:dusts/tungsten=180,8",
		"#c:dusts/platinum=210,9",
		"#c:dusts/ruby=180,8",
		"#c:dusts/sapphire=180,8",
		"#c:dusts/osmium=240,10",
		"#c:dusts/iridium=240,10",
		"#c:dusts/netherite=300,12",
		"#c:dusts/plutonium=300,12",
		"#c:dusts/iron=30,4",
		"#c:dusts/copper=30,4",
		"#c:dusts/gold=90,5",
		"#c:dusts/coal=30,4",
		"#c:dusts/charcoal=30,4",
		"#c:dusts/coal_coke=60,5",
		"#c:dusts/lapis=90,5",
		"#c:dusts/quartz=45,4",
		"#c:dusts/diamond=240,10",
		"#c:dusts/emerald=300,12",
		"#c:dusts/obsidian=60,5",
		"#c:dusts/refined_glowstone=120,6",
		"#c:dusts/refined_obsidian=150,7"
	]

	# ---- 消耗品 consumables：能塞进左侧输入格的方块 ----
	# 写法： id / id=贫矿等级
	# 贫矿等级 = 消耗倍数，可用小数：实际消耗 = 该矿物的消耗数量 × 等级，有小数向上取整，结果夹在 1-64。
	#   例：某矿物要 6 个，深板岩等级 2.0 → 要 12 个；石头等级 1.0 → 要 6 个
	# 界面里的 ! 按钮能看到每种方块的贫矿等级。
	# 也支持标签，例如 "#c:stones=1.0"（展开成标签里每一种方块，等级统一）。
	consumables = [
		"minecraft:stone",
		"minecraft:cobblestone",
		"minecraft:granite=1.25",
		"minecraft:diorite=1.25",
		"minecraft:andesite=1.25",
		"minecraft:calcite=1.0",
		"minecraft:basalt=1.25",
		"minecraft:netherrack",
		"minecraft:tuff=1.5",
		"minecraft:cobbled_deepslate=2.0",
		"minecraft:deepslate=2.0",
		"minecraft:blackstone=1.5",
		"minecraft:end_stone=1.5"
	]
```
规则：

- `ores`：`#c:dusts/iron=30` → 30 秒 + 默认数量；`#c:raw_materials/tin=45,4` → 45 秒 + 4 个；
  `#c:dusts/gold=,6` → 默认时间 + 6 个；只写 id/标签 → 时间和数量都用默认值。
- `consumables`：`=` 后面是**贫矿等级**（消耗倍数，可小数，如 2.5）；不写就用 `default_poor_grade`；1 = 正常成色。
- 实际消耗 = 该矿物数量 × 该物品贫矿等级，**有小数向上取整**（4×1.5=6，3×1.5=5），最终夹在 1~64。
- 列表顺序 = 界面里的显示顺序；物品 id 写错会 warn + 跳过该条；任意物品都能放进列表（不限原版矿物）。
- 支持**标签**：`#` 开头的条目先按物品标签查，查不到再按方块标签查（方块自动转成物品），
  例如 `"#c:gravels=2.5"`、`"#minecraft:coals=30"`、`"#minecraft:base_stone_overworld=2"`；
  解析不到的标签或 id 不会消失：列表里会原样显示那一行，并标注"此矿物不存在 / 此消耗品不存在"
  （这类条目选中也不会产出，并且统一排在列表最下方）；标签在服务端展开，客户端拿到的仍是具体物品 id。
  注意 NeoForge 的标签是 `c:gravels` / `c:stones` 这种命名，`c:gravel_blocks` 之类并不存在。
- 改完配置：单人退出重进世界，服务器重启（`/reload` 不会重读 NeoForge 服务端配置）。
- 旧键 `consume_amount` / `poor_blocks` / `poor_multiplier` 已废弃；想要干净默认值就删掉旧 toml 重启重新生成。
- 这是 **SERVER** 配置，客户端界面内容由服务端下发快照，玩家不需要改客户端。

默认配置对准 **FTB Materials**：`#c:raw_materials/*` 与 `#c:dusts/*` 会展开成
`ftbmaterials:tin_raw_ore`、`ftbmaterials:iron_dust` 这类物品；没装这个模组时这些标签是空的，
列表里会显示"此矿物不存在"（不报错），删掉对应行换成自己的 id 即可。
原版岩石以外的方块（泥土、砂砾、沙子等）默认不再是消耗品。

## 从源码构建

```bash
# 需要 JDK 21
./gradlew build          # 或已安装 Gradle 8.12+ 时: gradle build
```

产物在 `build/libs/OhOres-1.0.0.jar`。

本仓库用的是 **ModDevGradle 2.0.148**，构建时会自动下载 NeoForge 的 Maven 仓库
（`maven.neoforged.net`）与 Minecraft 资源。如果这个域名在你的网络下无法直连，可以走镜像：

```bash
gradle --init-script mirror-init.gradle build
```

（`mirror-init.gradle` 会把 `maven.neoforged.net` 重写到 `https://mirrors.cernet.edu.cn/bmclapi`，
插件仓库也已在 `settings.gradle` 里配了同一个镜像。）

## 源码结构

```
src/main/java/com/ohores/
├── OhOres.java                       模组主类，注册方块/物品/方块实体/菜单/配置
├── Config.java                       SERVER 配置：consumables / ores / consume_amount
├── ModBlocks.java                    方块、BlockItem、创造模式标签页
├── CommonEvents.java                 把方块塞进原版「功能性方块」标签页
├── block/
│   ├── RareOreExtractorBlock.java    有朝向的 BaseEntityBlock，右键开界面，破坏时掉落内容物
│   ├── RareOreExtractorBlockEntity.java  计时、消耗、产出；同时是 Container / WorldlyContainer / ContainerData
│   └── ModBlockEntities.java
├── menu/
│   ├── RareOreExtractorMenu.java     3 个槽位（输入 / 目标预览 / 输出）+ 4 个同步数据
│   └── ModMenus.java
├── network/
│   ├── SetTargetPayload.java         C→S：选中的矿物下标，服务端用菜单反查方块并校验
│   ├── SyncConfigPayload.java        S→C：打开界面时下发的配置快照（原始字符串）
│   ├── SyncedConfig.java             客户端解析后的可选矿物 / 消耗品缓存
│   └── ModNetworking.java
└── client/
    ├── RareOreExtractorScreen.java   原版熔炉界面的容器界面 + 选矿/消耗品叠加层
    ├── GuiPanel.java                 原版灰底面板的画法
    └── ClientSetup.java              注册 Screen

src/main/resources/
├── META-INF/neoforge.mods.toml
├── pack.mcmeta                       pack_format 48
├── assets/ohores/{blockstates,models,lang,textures/block}
└── data/{ohores/recipe,ohores/loot_table,minecraft/tags/block/mineable}
```

方块贴图由 `tools/generate_textures.py`（Pillow）生成，改配色后重跑即可；
容器界面直接用原版 `minecraft:textures/gui/container/furnace.png`，不需要自绘底图。

## 实现说明（几个容易踩的点）

- 进度数据走 `ContainerData`（4 个 int：进度百分比、总秒数、目标序号、目标总数），由 `AbstractContainerMenu`
  每 tick 自动同步，客户端不需要额外的包。
- 选矿与消耗品列表都在**同一个容器界面里做叠加层**（而不是 `setScreen` 开子界面）：`AbstractContainerScreen`
  被替换时会调用 `removed()` 把容器关掉，开子界面会导致机器界面直接关闭。
- `SetTargetPayload` 只带一个下标：服务端收到后用 `player.containerMenu` + `stillValid` 反查方块实体，
  再用自己的配置列表校验下标，客户端伪造不了别的方块。
- `SyncConfigPayload` 在玩家打开界面时下发一份 `consumables / ores / consume_amount` 的原始配置字符串，
  客户端自己解析出图标和名称，所以列表内容永远和服务端配置一致。
- 方块实体实现了 `WorldlyContainer`，漏斗可以从上方/侧面输入消耗品、从任意面抽取产物。
- 破坏方块时 `onRemove` 会把输入槽和输出槽的东西掉出来，不会吞物品。
