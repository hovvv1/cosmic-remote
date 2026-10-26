package dev.cosmic.remote;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Cosmic Remote: une télécommande qui invoque des événements cosmiques (trou noir, météorite, soleil, supernova). */
public final class CosmicRemote implements ModInitializer {
    public static final String MOD_ID = "cosmic_remote";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static Item REMOTE;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Config.load();

        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id("cosmic_remote"));
        REMOTE = Registry.register(BuiltInRegistries.ITEM, key,
                new RemoteItem(new Item.Properties().setId(key).stacksTo(1).rarity(Rarity.EPIC)));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(REMOTE));

        ServerTickEvents.END_SERVER_TICK.register(Skills::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Skills.clear());
    }
}
