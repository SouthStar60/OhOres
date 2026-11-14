package com.ohores.block;

import com.ohores.OhOres;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OhOres.MODID);

    public static final Supplier<BlockEntityType<RareOreExtractorBlockEntity>> RARE_ORE_EXTRACTOR =
            BLOCK_ENTITIES.register("rare_ore_extractor",
                    () -> BlockEntityType.Builder.of(RareOreExtractorBlockEntity::new,
                            com.ohores.ModBlocks.RARE_ORE_EXTRACTOR.get()).build(null));

    private ModBlockEntities() {
    }
}
