package com.lootbeams;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LootBeams implements ClientModInitializer {

	public static final String MODID = "lootbeams";
	public static final Logger LOGGER = LogManager.getLogger();
	public static List<ItemStack> CRASH_BLACKLIST = new ArrayList<>();

	public static Configuration config;

	public LootBeams() {
		LootBeams.config = Configuration.load();
	}

	@Override
	public void onInitializeClient() {

	}
}
