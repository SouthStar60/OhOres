package com.ohores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.common.ModConfigSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server side configuration of the rare ore extractor.
 *
 * <pre>
 * ores        = ["#c:raw_materials/tin", "#c:raw_materials/platinum=180,8", "#c:dusts/iron=,6"]
 * consumables = ["minecraft:stone", "minecraft:deepslate=2.5"]
 * </pre>
 *
 * ores entries: {@code <id>}, {@code <id>=<seconds>}, {@code <id>=<seconds>,<amount>} or
 * {@code <id>=,<amount>}. Missing numbers fall back to {@code default_seconds} /
 * {@code default_consume}.<br>
 * consumables entries: {@code <id>} or {@code <id>=<poor grade>}; a missing grade falls back to
 * {@code default_poor_grade}. The amount actually eaten is {@code ceil(ore amount * grade)}.
 */
public final class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger("OhOres");

    private static final int FALLBACK_SECONDS = 60;
    private static final int FALLBACK_CONSUME = 4;
    private static final double FALLBACK_GRADE = 1.0D;

    /** vanilla rocks only: no dirt, grass, gravel, sand or other junk in the default set */
    private static final List<String> DEFAULT_CONSUMABLES = List.of(
            "minecraft:stone", "minecraft:cobblestone", "minecraft:granite=1.25", "minecraft:diorite=1.25",
            "minecraft:andesite=1.25", "minecraft:calcite=1.0", "minecraft:basalt=1.25",
            "minecraft:netherrack", "minecraft:tuff=1.5", "minecraft:cobbled_deepslate=2.0",
            "minecraft:deepslate=2.0", "minecraft:blackstone=1.5", "minecraft:end_stone=1.5"
            );

    /** default targets: the natural raw ores and mineral dusts of FTB Materials, reached through c tags */
    private static final List<String> DEFAULT_ORES = List.of(
            "#c:raw_materials/aluminum=45,4", "#c:raw_materials/tin=45,4", "#c:raw_materials/zinc=45,4",
            "#c:raw_materials/lead=50,4", "#c:raw_materials/silicon=60,4", "#c:raw_materials/nickel=90,5",
            "#c:raw_materials/silver=90,5", "#c:raw_materials/titanium=90,6",
            "#c:raw_materials/chromium=120,6", "#c:raw_materials/uranium=120,6",
            "#c:raw_materials/antimony=150,7", "#c:raw_materials/tungsten=180,8",
            "#c:raw_materials/platinum=210,9", "#c:raw_materials/osmium=240,10",
            "#c:raw_materials/iridium=240,10", "#c:raw_materials/plutonium=300,12", "#c:dusts/salt=30,4",
            "#c:dusts/aluminum=45,4", "#c:dusts/bauxite=30,4", "#c:dusts/silicon=60,4",
            "#c:dusts/tin=45,4", "#c:dusts/zinc=45,4", "#c:dusts/lead=50,4", "#c:dusts/niter=45,4",
            "#c:dusts/sulfur=45,4", "#c:dusts/nickel=90,5", "#c:dusts/silver=90,5",
            "#c:dusts/chromium=120,6", "#c:dusts/apatite=60,5", "#c:dusts/cinnabar=60,5",
            "#c:dusts/fluorite=60,5", "#c:dusts/graphite=60,5", "#c:dusts/titanium=90,6",
            "#c:dusts/monazite=90,5", "#c:dusts/uranium=120,6", "#c:dusts/antimony=150,7",
            "#c:dusts/lithium=120,6", "#c:dusts/tungsten=180,8", "#c:dusts/platinum=210,9",
            "#c:dusts/ruby=180,8", "#c:dusts/sapphire=180,8", "#c:dusts/osmium=240,10",
            "#c:dusts/iridium=240,10", "#c:dusts/netherite=300,12", "#c:dusts/plutonium=300,12",
            "#c:dusts/iron=30,4", "#c:dusts/copper=30,4", "#c:dusts/gold=90,5", "#c:dusts/coal=30,4",
            "#c:dusts/charcoal=30,4", "#c:dusts/coal_coke=60,5", "#c:dusts/lapis=90,5",
            "#c:dusts/quartz=45,4", "#c:dusts/diamond=240,10", "#c:dusts/emerald=300,12",
            "#c:dusts/obsidian=60,5", "#c:dusts/refined_glowstone=120,6", "#c:dusts/refined_obsidian=150,7"
            );

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DEFAULT_SECONDS;
    public static final ModConfigSpec.IntValue DEFAULT_CONSUME;
    public static final ModConfigSpec.DoubleValue DEFAULT_POOR_GRADE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ORES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONSUMABLES;

    /** one selectable ore; {@code missing} entries are tags/ids that did not resolve */
    public record OreEntry(Item item, String id, int ticks, int consume, boolean missing) {
        public int seconds() {
            return this.ticks / 20;
        }
    }

    /** one consumable, with its poor grade (consumption multiplier) */
    public record ConsumableEntry(Item item, String id, double grade, boolean missing) {
    }

    private static List<String> rawOres;
    private static List<OreEntry> entries = List.of();
    private static int cachedSeconds = -1;
    private static int cachedConsume = -1;

    private static List<String> rawConsumables;
    private static List<ConsumableEntry> consumables = List.of();
    private static Map<Item, Double> consumableGrades = Map.of();
    private static double cachedGrade = Double.NaN;

    private static List<ConsumableEntry> fallbackEntries;
    private static Map<Item, Double> fallbackGrades;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("========== 稀有矿物提取器 / Rare Ore Extractor ==========",
                "这个文件决定三件事：界面里能选哪些矿物、哪些方块能当消耗品、每样要多久要多少。",
                "改完保存 → 重启游戏或服务器才生效（/reload 不会重读这个文件）。",
                "物品一律写英文 id，例如 minecraft:stone、ftbmaterials:tin_raw_ore；写中文名字没用。",
                "写错的 id 或标签不会崩游戏：界面里会把它显示成「此矿物不存在 / 此消耗品不存在」，",
                "并排在列表最下面，方便你回来改。",
                "");

        builder.push("extractor");

        builder.comment("---- 默认值：下面三项是 ores / consumables 里没写数字时的兜底 ----");
        DEFAULT_SECONDS = builder
                .comment("矿物没写时间时用它，单位秒。写 60 就是 60 秒出 1 个。")
                .defineInRange("default_seconds", 60, 1, 3600);

        DEFAULT_CONSUME = builder
                .comment("矿物没写消耗数量时用它，单位个，范围 1-64。")
                .defineInRange("default_consume", 4, 1, 64);

        DEFAULT_POOR_GRADE = builder
                .comment("消耗品没写贫矿等级时用它，也就是消耗倍数，可以用小数（如 1.5）。",
                        "1.0 = 正常成色，数字越大越费。")
                .defineInRange("default_poor_grade", 1.0D, 0.01D, 64.0D);

        builder.comment("---- 矿物 ores：界面里能选的目标，列表顺序就是界面顺序 ----",
                "写法： id / id=秒 / id=秒,数量 / id=,数量（没写的部分用上面三个默认值）",
                "  \"ftbmaterials:tin_raw_ore=45,4\"  -> 45 秒，消耗 4 个",
                "  \"ftbmaterials:iron_dust=30\"     -> 30 秒，消耗数量用 default_consume",
                "  \"ftbmaterials:iron_dust=,6\"     -> 用默认时间，消耗 6 个",
                "以 # 开头表示标签，例如 \"#c:raw_materials/tin=45,4\"：",
                "标签会展开成标签里的每一种物品，各占一行、各自独立计时；所以装了几个模组都有粗锡时，",
                "写一条标签就能把它们全收进来（这是默认配置大量用 #c: 标签的原因）。",
                "查标签的顺序是先物品标签、再方块标签（方块会自动换成它的物品形态）。",
                "没装提供该标签的模组时标签是空的，界面会显示「此矿物不存在」。",
                "同一种物品重复写会以第一条为准，后面的忽略。");
        ORES = builder
                .defineList("ores", DEFAULT_ORES, o -> o instanceof String s && !s.isBlank());

        builder.comment("---- 消耗品 consumables：能塞进左侧输入格的方块 ----",
                "写法： id / id=贫矿等级",
                "贫矿等级 = 消耗倍数，可用小数；实际消耗 = 该矿物的消耗数量 × 等级，有小数向上取整，",
                "结果夹在 1-64 之间。举例：某矿物要 6 个，青金石等级 2.5 → 消耗 15 个。",
                "  \"minecraft:deepslate=2.0\" -> 同样 6 个矿物，深板岩要 12 个",
                "  \"minecraft:stone\"          -> 等级用 default_poor_grade（默认 1.0）",
                "界面里的 ! 按钮能看到每种方块的贫矿等级，方便玩家挑便宜的用。",
                "也支持标签，例如 \"#c:stones=1.0\"（会展开成标签里的每一种方块，等级统一）。");
        CONSUMABLES = builder
                .defineList("consumables", DEFAULT_CONSUMABLES, o -> o instanceof String s && !s.isBlank());

        builder.pop();
        SPEC = builder.build();
    }

    private Config() {
    }

    /** Drops every parsed cache; call it after tags were (re)bound so tag entries re-resolve. */
    public static void invalidateCaches() {
        rawOres = null;
        entries = List.of();
        rawConsumables = null;
        consumables = List.of();
        consumableGrades = Map.of();
        cachedSeconds = -1;
        cachedConsume = -1;
        cachedGrade = Double.NaN;
    }

    // ------------------------------------------------------------- defaults

    private static int defaultSeconds() {
        try {
            return Math.max(1, DEFAULT_SECONDS.get());
        } catch (Throwable t) {
            return FALLBACK_SECONDS;
        }
    }

    private static int defaultConsume() {
        try {
            return Math.max(1, DEFAULT_CONSUME.get());
        } catch (Throwable t) {
            return FALLBACK_CONSUME;
        }
    }

    private static double defaultGrade() {
        try {
            return Math.max(0.01D, DEFAULT_POOR_GRADE.get());
        } catch (Throwable t) {
            return FALLBACK_GRADE;
        }
    }

    public static int getDefaultSeconds() {
        return defaultSeconds();
    }

    public static int getDefaultConsume() {
        return defaultConsume();
    }

    public static double getDefaultPoorGrade() {
        return defaultGrade();
    }

    // ----------------------------------------------------------------- ores

    /**
     * Resolves one config entry (already stripped of its numbers) to items.
     * A leading '#' means a tag (e.g. {@code #c:gravels}); anything else is an exact item id.
     * Unknown tags resolve to an empty list and are skipped silently.
     */
    private static List<Item> resolveItems(String idOrTag) {
        String key = idOrTag.trim();
        if (key.isEmpty()) {
            return List.of();
        }

        if (key.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(key.substring(1).trim());
            if (id == null) {
                return List.of();
            }

            List<Item> items = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id))
                    .map(set -> set.stream().map(Holder::value).toList())
                    .orElse(List.of());
            if (!items.isEmpty()) {
                return items;
            }

            // not an item tag: accept block tags too and use the block's item form
            List<Item> blocks = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, id))
                    .map(set -> set.stream().map(Holder::value).map(Block::asItem)
                            .filter(item -> item != Items.AIR).toList())
                    .orElse(List.of());
            if (blocks.isEmpty()) {
                LOGGER.debug("[OhOres] Tag '{}' does not exist (or is empty), skipped.", key);
            }
            return blocks;
        }

        ResourceLocation id = ResourceLocation.tryParse(key);
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("[OhOres] Unknown item id '{}' in the config. Entry skipped.", key);
            return List.of();
        }
        return List.of(item);
    }

    private static int separatorIndex(String text) {
        int comma = text.indexOf(',');
        int semicolon = text.indexOf(';');
        if (comma < 0) {
            return semicolon;
        }
        return semicolon < 0 ? comma : Math.min(comma, semicolon);
    }

    /** @return parsed ore list, rebuilt whenever the config value changes. */
    public static List<OreEntry> getEntries() {
        List<? extends String> configured;
        try {
            configured = ORES.get();
        } catch (Throwable t) {
            return entries;
        }

        int secondsDefault = defaultSeconds();
        int consumeDefault = defaultConsume();
        if (rawOres != null && rawOres.equals(configured)
                && cachedSeconds == secondsDefault && cachedConsume == consumeDefault) {
            return entries;
        }

        List<OreEntry> parsed = new ArrayList<>();
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String id;
            String rest;
            int split = raw.indexOf('=');
            if (split > 0) {
                id = raw.substring(0, split).trim();
                rest = raw.substring(split + 1).trim();
            } else {
                id = raw.trim();
                rest = "";
            }

            int seconds = secondsDefault;
            int consume = consumeDefault;
            if (!rest.isEmpty()) {
                int sep = separatorIndex(rest);
                String secondsText = sep >= 0 ? rest.substring(0, sep).trim() : rest.trim();
                if (!secondsText.isEmpty()) {
                    try {
                        seconds = Math.max(1, Integer.parseInt(secondsText));
                    } catch (NumberFormatException ex) {
                        LOGGER.warn("[OhOres] Bad time in ore entry '{}', using {} seconds.", raw, secondsDefault);
                    }
                }
                if (sep >= 0) {
                    String consumeText = rest.substring(sep + 1).trim();
                    if (!consumeText.isEmpty()) {
                        try {
                            consume = Math.min(64, Math.max(1, Integer.parseInt(consumeText)));
                        } catch (NumberFormatException ex) {
                            LOGGER.warn("[OhOres] Bad amount in ore entry '{}', using {}.", raw, consumeDefault);
                        }
                    }
                }
            }

            List<Item> items = resolveItems(id);
            if (items.isEmpty()) {
                // keep it visible in the GUI so the mistake can be spotted in game
                parsed.add(new OreEntry(Items.AIR, id, seconds * 20, consume, true));
                continue;
            }
            for (Item item : items) {
                String realId = BuiltInRegistries.ITEM.getKey(item).toString();
                parsed.add(new OreEntry(item, realId, seconds * 20, consume, false));
            }
        }

        entries = List.copyOf(parsed);
        rawOres = List.copyOf(configured);
        cachedSeconds = secondsDefault;
        cachedConsume = consumeDefault;
        return entries;
    }

    /** @return the entry at {@code index}; wraps around, or {@code null} if the list is empty. */
    public static OreEntry getSelected(int index) {
        List<OreEntry> list = getEntries();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(Math.floorMod(index, list.size()));
    }

    // ---------------------------------------------------------- consumables

    /** @return parsed consumables, rebuilt whenever the config value changes. */
    public static List<ConsumableEntry> getConsumables() {
        List<? extends String> configured;
        try {
            configured = CONSUMABLES.get();
        } catch (Throwable t) {
            return consumables.isEmpty() ? fallbackConsumableEntries() : consumables;
        }

        double gradeDefault = defaultGrade();
        if (rawConsumables != null && rawConsumables.equals(configured)
                && Double.compare(cachedGrade, gradeDefault) == 0) {
            return consumables;
        }

        List<ConsumableEntry> parsed = new ArrayList<>();
        Map<Item, Double> grades = new HashMap<>();
        for (String raw : configured) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String id = raw.trim();
            double grade = gradeDefault;
            int split = raw.indexOf('=');
            if (split > 0) {
                id = raw.substring(0, split).trim();
                String gradeText = raw.substring(split + 1).trim();
                if (!gradeText.isEmpty()) {
                    try {
                        grade = Math.max(0.01D, Math.min(64.0D, Double.parseDouble(gradeText)));
                    } catch (NumberFormatException ex) {
                        LOGGER.warn("[OhOres] Bad grade in consumable '{}', using {}.", raw, gradeDefault);
                    }
                }
            }

            List<Item> items = resolveItems(id);
            if (items.isEmpty()) {
                parsed.add(new ConsumableEntry(Items.AIR, id, grade, true));
                continue;
            }
            for (Item item : items) {
                String realId = BuiltInRegistries.ITEM.getKey(item).toString();
                if (grades.containsKey(item)) {
                    continue;
                }
                parsed.add(new ConsumableEntry(item, realId, grade, false));
                grades.put(item, grade);
            }
        }

        consumables = List.copyOf(parsed);
        consumableGrades = Map.copyOf(grades);
        rawConsumables = List.copyOf(configured);
        cachedGrade = gradeDefault;
        return consumables;
    }

    public static boolean isConsumable(Item item) {
        if (item == Items.AIR) {
            return false;
        }
        for (ConsumableEntry entry : getConsumables()) {
            if (entry.item() == item) {
                return true;
            }
        }
        return false;
    }

    /** @return the poor grade of {@code item}, or 1 when it is not a consumable. */
    public static double gradeOf(Item item) {
        if (item == Items.AIR) {
            return 1.0D;
        }
        try {
            getConsumables();
            Double grade = consumableGrades.get(item);
            if (grade != null) {
                return grade;
            }
        } catch (Throwable ignored) {
            // server config is not loaded here, fall through to the defaults
        }
        return fallbackGrades().getOrDefault(item, 1.0D);
    }

    /** How many items are really eaten: {@code ceil(base * grade)}, clamped to 1..64. */
    public static int consumeFor(Item item, int base) {
        double wanted = Math.max(1, base) * gradeOf(item);
        int rounded = (int) Math.ceil(wanted - 1.0E-9D);
        return Math.min(64, Math.max(1, rounded));
    }

    private static List<ConsumableEntry> fallbackConsumableEntries() {
        if (fallbackEntries == null) {
            List<ConsumableEntry> parsed = new ArrayList<>();
            for (String raw : DEFAULT_CONSUMABLES) {
                String id = raw;
                double grade = FALLBACK_GRADE;
                int split = raw.indexOf('=');
                if (split > 0) {
                    id = raw.substring(0, split).trim();
                    try {
                        grade = Double.parseDouble(raw.substring(split + 1).trim());
                    } catch (NumberFormatException ignored) {
                        grade = FALLBACK_GRADE;
                    }
                }
                ResourceLocation rl = ResourceLocation.tryParse(id);
                Item item = rl == null ? null : BuiltInRegistries.ITEM.get(rl);
                if (item != null && item != Items.AIR) {
                    parsed.add(new ConsumableEntry(item, id, grade, false));
                }
            }
            fallbackEntries = List.copyOf(parsed);
        }
        return fallbackEntries;
    }

    private static Map<Item, Double> fallbackGrades() {
        if (fallbackGrades == null) {
            Map<Item, Double> map = new HashMap<>();
            for (ConsumableEntry entry : fallbackConsumableEntries()) {
                map.put(entry.item(), entry.grade());
            }
            fallbackGrades = Map.copyOf(map);
        }
        return fallbackGrades;
    }
}
