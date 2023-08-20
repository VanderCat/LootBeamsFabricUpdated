package com.lootbeams;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Configuration {
	public static final Gson GSON;

	static {
		GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.setPrettyPrinting()
				.create();
	}

	public boolean allItems = true;
	public boolean onlyEquipment = false;
	public boolean onlyRare = false;
	public List<String> whitelist = new ArrayList<>();
	public List<String> blacklist = new ArrayList<>();
	public List<String> colorOverrides = new ArrayList<>();

	public boolean renderNameColor = true;
	public boolean renderRarityColor = true;
	public float beamRadius = 1;
	public float beamHeight = 1;
	public float beamYOffset = 0;
	public float beamAlpha = 0.85f;
	public float renderDistance = 24.0f;

	public boolean borders = true;
	public boolean renderNametags = true;
	public boolean renderNametagsOnlook = true;
	public boolean renderStackcount = true;
	public float nametagLookSensitivity = 0.018f;
	public float nametagTextAlpha = 1;
	public float nametagBackgroundAlpha = 0.5f;
	public float nametagScale = 1.0f;
	public float nametagYOffset = 0.75f;
	public boolean dmclootCompatRarity = true;
	public List<String> customRarities = new ArrayList<>();
	public boolean whiteRarities = false;

	public static Configuration load(){
		File configFile = FabricLoader.getInstance().getConfigDir().resolve(LootBeams.MODID + "-client.toml").toFile();
		Configuration inst = new Configuration();
		if (configFile.isFile()) {
			try{
				var main = new Toml().read(configFile).getTable("\"Loot Beams\"");
				inst.renderNameColor = main.getBoolean("render_name_color", true);
				inst.renderRarityColor = main.getBoolean("render_rarity_color", true);
				inst.beamRadius = main.getDouble("beam_radius", 1.0).floatValue();
				inst.beamHeight = main.getDouble("beam_height", 1.0).floatValue();
				inst.beamYOffset = main.getDouble("beam_y_offset", 0.0).floatValue();
				inst.beamAlpha = main.getDouble("beam_alpha", 0.85).floatValue();
				inst.renderDistance = main.getDouble("render_distance", 24.0).floatValue();
				inst.colorOverrides = main.getList("color_overrides", new ArrayList<>());

				var items = main.getTable("Items");
				inst.allItems = items.getBoolean("all_items", true);
				inst.onlyRare = items.getBoolean("only_rare", false);
				inst.onlyEquipment = items.getBoolean("only_equipment", false);
				inst.whitelist = items.getList("whitelist", new ArrayList<>());
				inst.blacklist = items.getList("blacklist", new ArrayList<>());

				var nametags = main.getTable("Nametags");
				inst.borders = nametags.getBoolean("borders", true);
				inst.renderNametags = nametags.getBoolean("render_nametags", true);
				inst.renderNametagsOnlook = nametags.getBoolean("render_nametags_onlook", true);
				inst.renderStackcount = nametags.getBoolean("render_stackcount", true);
				inst.nametagLookSensitivity = nametags.getDouble("nametag_look_sensitivity", 0.018).floatValue();
				inst.nametagTextAlpha = nametags.getDouble("nametag_text_alpha", 1.0).floatValue();
				inst.nametagBackgroundAlpha = nametags.getDouble("nametag_background_alpha", 0.5).floatValue();
				inst.nametagScale = nametags.getDouble("nametag_scale", 1.0).floatValue();
				inst.nametagYOffset = nametags.getDouble("nametag_y_offset", 0.75).floatValue();
				inst.dmclootCompatRarity = nametags.getBoolean("dmcloot_compat_rarity", true);
				inst.customRarities = nametags.getList("custom_rarities", new ArrayList<>());
				inst.whiteRarities = nametags.getBoolean("white_rarities", false);
			}
			catch (Exception e){
				LootBeams.LOGGER.warn("cannot load config file", e);
				return new Configuration();
			}
		}
		else{
			inst.save();
		}
		return inst;
	}

	public void save(){
		File configFile = FabricLoader.getInstance().getConfigDir().resolve(LootBeams.MODID + "-client.toml").toFile();
		HashMap<String, Object> nametags = new HashMap<>();
		nametags.put("borders", this.borders);
		nametags.put("render_nametags", this.renderNametags);
		nametags.put("render_nametags_onlook", this.renderNametagsOnlook);
		nametags.put("render_stackcount", this.renderStackcount);
		nametags.put("nametag_look_sensitivity", this.nametagLookSensitivity);
		nametags.put("nametag_text_alpha", this.nametagTextAlpha);
		nametags.put("nametag_background_alpha", this.nametagBackgroundAlpha);
		nametags.put("nametag_scale", this.nametagScale);
		nametags.put("nametag_y_offset", this.nametagYOffset);
		nametags.put("dmcloot_compat_rarity", this.dmclootCompatRarity);
		nametags.put("custom_rarities", this.customRarities);
		nametags.put("white_rarities", this.whiteRarities);

		HashMap<String, Object> items = new HashMap<>();
		items.put("all_items", this.allItems);
		items.put("only_rare", this.onlyRare);
		items.put("only_equipment", this.onlyEquipment);
		items.put("whitelist", this.whitelist);
		items.put("blacklist", this.blacklist);

		HashMap<String, Object> main = new HashMap<>();
		main.put("render_name_color", this.renderNameColor);
		main.put("render_rarity_color", this.renderRarityColor);
		main.put("beam_radius", this.beamRadius);
		main.put("beam_height", this.beamHeight);
		main.put("beam_y_offset", this.beamYOffset);
		main.put("beam_alpha", this.beamAlpha);
		main.put("render_distance", this.renderDistance);
		main.put("color_overrides", this.colorOverrides);
		main.put("Items", items);
		main.put("Nametags", nametags);

		HashMap<String, Object> root = new HashMap<>();
		root.put("Loot Beams", main);

		var tomlWriter = new TomlWriter.Builder().indentValuesBy(4).indentTablesBy(4).padArrayDelimitersBy(1).build();
		try {
			tomlWriter.write(root, configFile);
		} catch (IOException e) {
			LootBeams.LOGGER.warn("cannot save config file", e);
		}
	}

	public static TextColor getColorFromItemOverrides(Item i) {
		List<String> overrides = LootBeams.config.colorOverrides;
		if (overrides.size() > 0) {
			for (String unparsed : overrides.stream().filter((s) -> (!s.isEmpty())).toList()) {
				String[] configValue = unparsed.split("=");
				if (configValue.length == 2) {
					String nameIn = configValue[0];
					Identifier registry = Identifier.tryParse(nameIn.replace("#", ""));
					TextColor colorIn = null;
					try {
						colorIn = TextColor.parse(configValue[1]);
					} catch (Exception e) {
						LootBeams.LOGGER.error(String.format("Color overrides error! \"%s\" is not a valid hex color for \"%s\"", configValue[1], nameIn));
						return null;
					}

					//Modid
					if (!nameIn.contains(":")) {
						if (Registries.ITEM.getId(i).getNamespace().equals(nameIn)) {
							return colorIn;
						}

					}

					if (registry != null) {
						//Tag
						if (nameIn.startsWith("#")) {
							Optional<RegistryEntryList.Named<Item>> tag = Registries.ITEM.streamTagsAndEntries().filter(pair -> pair.getFirst().id().equals(registry))
									.findFirst().map(Pair::getSecond);
//							Optional<HolderSet.Named<Item>> tag = Registry.ITEM.getTag(TagKey.create(Registry.ITEM_REGISTRY, registry));
							if(tag.isPresent() && tag.get().contains(Registries.ITEM.getEntry(Registries.ITEM.getKey(i).get()).get())){
								return colorIn;
							}
						}

						//Item
						Optional<Item> registryItem = Registries.ITEM.getOrEmpty(registry);
						if (registryItem.isPresent() && registryItem.get().asItem() == i.asItem()) {
							return colorIn;
						}

					}
				}
			}
		}
		return null;
	}
}
