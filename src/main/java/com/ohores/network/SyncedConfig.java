package com.ohores.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client side copy of the server configuration (filled by {@link SyncConfigPayload}).
 * Only plain data: it is safe to load on both sides and is simply empty on a dedicated server.
 */
public final class SyncedConfig {
    /**
     * A selectable ore: the produced item, its config id, how long it takes and how much it eats.
     * {@code missing} entries are config lines that did not resolve (unknown tag / unknown id).
     */
    public record Ore(Item item, String id, int seconds, int consume, String displayName, boolean missing) {
        public ItemStack stack() {
            return this.item == null ? ItemStack.EMPTY : new ItemStack(this.item);
        }
    }

    /** A consumable item and its poor grade (consumption multiplier). */
    public record Consumable(Item item, String id, double grade, String displayName, boolean missing) {
        public ItemStack stack() {
            return this.item == null ? ItemStack.EMPTY : new ItemStack(this.item);
        }
    }

    private static List<Ore> ores = List.of();
    private static List<Consumable> consumables = List.of();
    private static int defaultSeconds = 60;
    private static int defaultConsume = 4;
    private static double defaultGrade = 1.0D;
    private static boolean synced;

    private SyncedConfig() {
    }

    public static void accept(SyncConfigPayload payload) {
        defaultSeconds = Math.max(1, payload.defaultSeconds());
        defaultConsume = Math.max(1, payload.defaultConsume());
        defaultGrade = Math.max(0.01D, payload.defaultGrade());
        ores = parseOres(payload.ores(), defaultSeconds, defaultConsume);
        consumables = parseConsumables(payload.consumables(), defaultGrade);
        synced = true;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static List<Ore> ores() {
        return ores;
    }

    public static List<Consumable> consumables() {
        return consumables;
    }

    public static int defaultConsume() {
        return defaultConsume;
    }

    public static double defaultGrade() {
        return defaultGrade;
    }

    /** Resolves a config string to an item, or {@code null} when the id is unknown. */
    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == Items.AIR ? null : item;
    }

    public static String nameOf(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    /** "2" / "1.5" / "1.25", without trailing zeros. */
    public static String formatGrade(double grade) {
        return java.math.BigDecimal.valueOf(grade).stripTrailingZeros().toPlainString();
    }

    private static List<Ore> parseOres(String blob, int defaultSeconds, int defaultConsume) {
        List<Ore> parsed = new ArrayList<>();
        for (String raw : splitLines(blob)) {
            boolean missing = raw.startsWith("?");
            if (missing) {
                raw = raw.substring(1).trim();
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

            int seconds = defaultSeconds;
            int consume = defaultConsume;
            if (!rest.isEmpty()) {
                int sep = rest.indexOf(',');
                int semicolon = rest.indexOf(';');
                if (sep < 0 || (semicolon >= 0 && semicolon < sep)) {
                    sep = semicolon;
                }
                String secondsText = sep >= 0 ? rest.substring(0, sep).trim() : rest.trim();
                if (!secondsText.isEmpty()) {
                    try {
                        seconds = Math.max(1, Integer.parseInt(secondsText));
                    } catch (NumberFormatException ignored) {
                        seconds = defaultSeconds;
                    }
                }
                if (sep >= 0) {
                    String consumeText = rest.substring(sep + 1).trim();
                    if (!consumeText.isEmpty()) {
                        try {
                            consume = Math.min(64, Math.max(1, Integer.parseInt(consumeText)));
                        } catch (NumberFormatException ignored) {
                            consume = defaultConsume;
                        }
                    }
                }
            }

            Item item = item(id);
            if (item == null) {
                missing = true;
            }
            String name = missing ? id : nameOf(item);
            parsed.add(new Ore(item, id, seconds, consume, name, missing));
        }
        return List.copyOf(parsed);
    }

    private static List<Consumable> parseConsumables(String blob, double defaultGrade) {
        List<Consumable> parsed = new ArrayList<>();
        for (String raw : splitLines(blob)) {
            boolean missing = raw.startsWith("?");
            if (missing) {
                raw = raw.substring(1).trim();
            }
            String id = raw.trim();
            double grade = defaultGrade;
            int split = raw.indexOf('=');
            if (split > 0) {
                id = raw.substring(0, split).trim();
                String gradeText = raw.substring(split + 1).trim();
                if (!gradeText.isEmpty()) {
                    try {
                        grade = Math.max(0.01D, Math.min(64.0D, Double.parseDouble(gradeText)));
                    } catch (NumberFormatException ignored) {
                        grade = defaultGrade;
                    }
                }
            }
            Item item = item(id);
            if (item == null) {
                missing = true;
            }
            String name = missing ? id : nameOf(item);
            parsed.add(new Consumable(item, id, grade, name, missing));
        }
        return List.copyOf(parsed);
    }

    private static List<String> splitLines(String blob) {
        List<String> out = new ArrayList<>();
        for (String line : blob.split("\n")) {
            if (!line.isBlank()) {
                out.add(line.trim());
            }
        }
        return List.copyOf(out);
    }
}
