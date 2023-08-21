package com.lootbeams;

import com.google.common.collect.Lists;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public abstract class LootBeamRenderer extends RenderLayer {

	/**
	 * ISSUES:
	 * Beam renders behind things like chests/clouds/water/beds/entities.
	 */

	private static final Identifier LOOT_BEAM_TEXTURE = new Identifier(LootBeams.MODID, "textures/entity/loot_beam.png");
	private static final RenderLayer LOOT_BEAM_RENDERTYPE = createRenderType();

	public LootBeamRenderer(String string, VertexFormat vertexFormat, VertexFormat.DrawMode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2) {
		super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
	}

	public static void renderLootBeam(MatrixStack stack, VertexConsumerProvider buffer, float pticks, long worldtime, ItemEntity item) {
		float beamAlpha = LootBeams.config.beamAlpha;
		float fadeDistance = LootBeams.config.fadeDistance;
		//Fade out when close
		var player = MinecraftClient.getInstance().player;
		var distance = player.squaredDistanceTo(item);
		if (distance < fadeDistance) {
			beamAlpha *= Math.max(0, distance-fadeDistance+1);
		}
		//Dont render beam if its too transparent
		if (beamAlpha <= 0.1f) {
			return;
		}

		float glowAlpha = beamAlpha * 0.4f;

		float beamRadius = 0.05f * LootBeams.config.beamRadius;
		float glowRadius = beamRadius + (beamRadius * 0.2f);
		float beamHeight = LootBeams.config.beamHeight;
		float yOffset = LootBeams.config.beamYOffset;

		TextColor color = getItemColor(item);
		float R = ((color.getRgb() >> 16) & 0xff) / 255f;
		float G = ((color.getRgb() >> 8) & 0xff) / 255f;
		float B = (color.getRgb() & 0xff) / 255f;

		//I will rewrite the beam rendering code soon! I promise!

		stack.push();

		//Render main beam
		stack.push();
		float rotation = (float) Math.floorMod(worldtime, 40L) + pticks;
		stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation * 2.25F - 45.0F));
		stack.translate(0, yOffset, 0);
		stack.translate(0, 1, 0);
		stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
		renderPart(stack, buffer.getBuffer(LOOT_BEAM_RENDERTYPE), R, G, B, beamAlpha, beamHeight, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius);
		stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180));
		renderPart(stack, buffer.getBuffer(LOOT_BEAM_RENDERTYPE), R, G, B, beamAlpha, beamHeight, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius);
		stack.pop();

		//Render glow around main beam
		stack.translate(0, yOffset, 0);
		stack.translate(0, 1, 0);
		stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
		renderPart(stack, buffer.getBuffer(LOOT_BEAM_RENDERTYPE), R, G, B, glowAlpha, beamHeight, -glowRadius, -glowRadius, glowRadius, -glowRadius, -beamRadius, glowRadius, glowRadius, glowRadius);
		stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180));
		renderPart(stack, buffer.getBuffer(LOOT_BEAM_RENDERTYPE), R, G, B, glowAlpha, beamHeight, -glowRadius, -glowRadius, glowRadius, -glowRadius, -beamRadius, glowRadius, glowRadius, glowRadius);

		stack.pop();

		if (LootBeams.config.renderNametags) {
			renderNameTag(stack, buffer, item, color);
		}
	}

	private static void renderNameTag(MatrixStack stack, VertexConsumerProvider buffer, ItemEntity item, TextColor color) {
		//If player is crouching or looking at the item
		if (MinecraftClient.getInstance().player.isInSneakingPose() || (LootBeams.config.renderNametagsOnlook && isLookingAt(MinecraftClient.getInstance().player, item, LootBeams.config.nametagLookSensitivity))) {

			float foregroundAlpha = LootBeams.config.nametagTextAlpha;
			float backgroundAlpha = LootBeams.config.nametagBackgroundAlpha;
			double yOffset = LootBeams.config.nametagYOffset;
			int foregroundColor = (color.getRgb() & 0xffffff) | ((int) (255 * foregroundAlpha) << 24);
			int backgroundColor = (color.getRgb() & 0xffffff) | ((int) (255 * backgroundAlpha) << 24);

			stack.push();

			//Render nametags at heights based on player distance
			stack.translate(0.0D, Math.min(1D, MinecraftClient.getInstance().player.squaredDistanceTo(item) * 0.025D) + yOffset, 0.0D);
			stack.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());

			float nametagScale = LootBeams.config.nametagScale;
			stack.scale(-0.02F * nametagScale, -0.02F * nametagScale, 0.02F * nametagScale);

			//Render stack counts on nametag
			TextRenderer fontrenderer = MinecraftClient.getInstance().textRenderer;
			String itemName = StringHelper.stripTextFormat(item.getStack().getName().getString());
			if (LootBeams.config.renderStackcount) {
				int count = item.getStack().getCount();
				if (count > 1) {
					itemName = itemName + " x" + count;
				}
			}

			//Move closer to the player so we dont render in beam, and render the tag
			stack.translate(0, 0, -10);
			RenderText(fontrenderer, stack, buffer, itemName, foregroundColor, backgroundColor, backgroundAlpha);

			//Render small tags
			stack.translate(0.0D, 10, 0.0D);
			stack.scale(0.75f, 0.75f, 0.75f);
			boolean textDrawn = false;
			List<Text> tooltip = item.getStack().getTooltip(null, TooltipContext.Default.BASIC);
			if (tooltip.size() >= 2) {
				Text tooltipRarity = tooltip.get(1);

				//Render dmcloot rarity small tags
				//NOFIX: as dmcloot has no support with Fabric and Minecraft 1.18.x, such support is commented out
//				if (LootBeams.config.DMCLOOT_COMPAT_RARITY.get() && FabricLoader.getInstance().isModLoaded("dmcloot")) {
//					if (item.getItem().hasTag() && item.getItem().getTag().contains("dmcloot.rarity")) {
//						Color rarityColor = LootBeams.config.WHITE_RARITIES.get() ? Color.WHITE : getRawColor(tooltipRarity);
//						TranslatableComponent translatedRarity = new TranslatableComponent("rarity.dmcloot." + item.getItem().getTag().getString("dmcloot.rarity"));
//						RenderText(fontrenderer, stack, buffer, translatedRarity.getString(), rarityColor.getRGB(), backgroundColor, backgroundAlpha);
//						textDrawn = true;
//					}
//				}

				//Render custom rarities
				if (!textDrawn && LootBeams.config.customRarities.contains(tooltipRarity.getString())) {
					TextColor rarityColor = LootBeams.config.whiteRarities ? TextColor.fromFormatting(Formatting.WHITE) : getRawColor(tooltipRarity);
					foregroundColor = (rarityColor.getRgb() & 0xffffff) | ((int) (255 * foregroundAlpha) << 24);
					backgroundColor = (rarityColor.getRgb() & 0xffffff) | ((int) (255 * backgroundAlpha) << 24);
					RenderText(fontrenderer, stack, buffer, tooltipRarity.getString(), foregroundColor, backgroundColor, backgroundAlpha);
				}
			}

			stack.pop();
		}
	}

	private static void RenderText(
			TextRenderer fontRenderer,
			MatrixStack stack,
			VertexConsumerProvider buffer,
			String text,
			int foregroundColor,
			int backgroundColor,
			float backgroundAlpha
	) {
		if (LootBeams.config.borders) {
			float w = -fontRenderer.getWidth(text) / 2f;
			int bg = new Color(0, 0, 0, (int) (255 * backgroundAlpha)).getRGB();

			var matrix = stack.peek().getPositionMatrix();
			//Draws background (border) text
			fontRenderer.draw(text, w + 1f, 0, bg, false, matrix, buffer, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0);
			fontRenderer.draw(text, w - 1f, 0, bg, false, matrix, buffer, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0);
			fontRenderer.draw(text, w, 1f, bg, false, matrix, buffer, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0);
			fontRenderer.draw(text, w ,-1f, bg, false, matrix, buffer, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0);

			//Draws foreground text in front of border
			stack.translate(0.0D, 0.0D, -0.01D);
			matrix = stack.peek().getPositionMatrix();
			fontRenderer.draw(text, w, 0, foregroundColor, false, matrix, buffer, TextRenderer.TextLayerType.NORMAL, 0, 15728864);
			stack.translate(0.0D, 0.0D, 0.01D);
		} else {
			fontRenderer.draw(text, (float) (-fontRenderer.getWidth(text) / 2), 0f, foregroundColor, false, stack.peek().getPositionMatrix(), buffer, TextRenderer.TextLayerType.NORMAL, backgroundColor, 15728864);
		}
	}

	/**
	 * Returns the color from the item's name, rarity, tag, or override.
	 */
	private static TextColor getItemColor(ItemEntity item) {
		if(LootBeams.CRASH_BLACKLIST.contains(item.getStack())) {
			return TextColor.fromFormatting(Formatting.WHITE);
		}

		try {

			//From Config Overrides
			TextColor override = Configuration.getColorFromItemOverrides(item.getStack().getItem());
			if (override != null) {
				return override;
			}

			//From NBT
			if (item.getStack().hasNbt() && item.getStack().getNbt().contains("lootbeams.color")) {
				return TextColor.parse(item.getStack().getNbt().getString("lootbeams.color"));
			}

			//From Name
			if (LootBeams.config.renderNameColor) {
				TextColor nameColor = getRawColor(item.getStack().getName());
				if (!nameColor.equals(TextColor.fromFormatting(Formatting.WHITE))) {
					return nameColor;
				}
			}

			//From Rarity
			if (LootBeams.config.renderRarityColor && item.getStack().getRarity().formatting != null) {
				return TextColor.fromFormatting(item.getStack().getRarity().formatting);
			} else {
				return TextColor.fromFormatting(Formatting.WHITE);
			}
		} catch (Exception e) {
			LootBeams.LOGGER.error("Failed to get color for ("+ item.getStack().toHoverableText() + "), added to temporary blacklist");
			LootBeams.CRASH_BLACKLIST.add(item.getStack());
			LootBeams.LOGGER.info("Temporary blacklist is now : " );
			for(ItemStack s : LootBeams.CRASH_BLACKLIST){
				LootBeams.LOGGER.info(s.toHoverableText());
			}
			return TextColor.fromFormatting(Formatting.WHITE);
		}
	}

	/**
	 * Gets color from the first letter in the text component.
	 */
	private static TextColor getRawColor(Text text) {
		List<Style> list = Lists.newArrayList();
		text.visit((acceptor, styleIn) -> {
			TextVisitFactory.visitFormatted(styleIn, acceptor, (string, style, consumer) -> {
				list.add(style);
				return true;
			});
			return Optional.empty();
		}, Style.EMPTY);
		if (list.get(0).getColor() != null) {
			return list.get(0).getColor();
		}
		return TextColor.fromFormatting(Formatting.WHITE);
	}

	private static void renderPart(MatrixStack stack, VertexConsumer builder, float red, float green, float blue, float alpha, float height, float radius_1, float radius_2, float radius_3, float radius_4, float radius_5, float radius_6, float radius_7, float radius_8) {
		MatrixStack.Entry matrixentry = stack.peek();
		Matrix4f matrixpose = matrixentry.getPositionMatrix();
		Matrix3f matrixnormal = matrixentry.getNormalMatrix();
		renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_1, radius_2, radius_3, radius_4);
		renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_7, radius_8, radius_5, radius_6);
		renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_3, radius_4, radius_7, radius_8);
		renderQuad(matrixpose, matrixnormal, builder, red, green, blue, alpha, height, radius_5, radius_6, radius_1, radius_2);
	}

	private static void renderQuad(Matrix4f pose, Matrix3f normal, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float z1, float texu1, float z, float texu) {
		addVertex(pose, normal, builder, red, green, blue, alpha, y, z1, texu1, 1f, 0f);
		addVertex(pose, normal, builder, red, green, blue, alpha, 0f, z1, texu1, 1f, 1f);
		addVertex(pose, normal, builder, red, green, blue, alpha, 0f, z, texu, 0f, 1f);
		addVertex(pose, normal, builder, red, green, blue, alpha, y, z, texu, 0f, 0f);
	}

	private static void addVertex(Matrix4f pose, Matrix3f normal, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x, float z, float texu, float texv) {
		builder.vertex(pose, x, y, z).color(red, green, blue, alpha).texture(texu, texv).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(normal, 0.0F, 1.0F, 0.0F).next();
	}

	private static String toBinaryName(String mapName){
		return "L" + mapName.replace('.', '/') + ";";
	}

	private static RenderLayer createRenderType() {
		RenderLayer.MultiPhaseParameters state = RenderLayer.MultiPhaseParameters.builder()
				.texture(new RenderPhase.Texture(LOOT_BEAM_TEXTURE, false, false))
				.lightmap(ENABLE_LIGHTMAP)
				.transparency(TRANSLUCENT_TRANSPARENCY)
				.program(RenderLayer.TRANSLUCENT_PROGRAM)
				.overlay(RenderPhase.DISABLE_OVERLAY_COLOR)
				.depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
				.writeMaskState(RenderLayer.COLOR_MASK).build(false);
		try {
			Method method = RenderLayer.class.getDeclaredMethod(
					FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_1921", "method_24049",
							"(Ljava/lang/String;" + toBinaryName("net.minecraft.class_293")
									+ toBinaryName("net.minecraft.class_293$class_5596")
									+ "IZZ"
									+ toBinaryName("net.minecraft.class_1921$class_4688") + ")"
									+ toBinaryName("net.minecraft.class_1921$class_4687")),
					String.class, VertexFormat.class, VertexFormat.DrawMode.class, int.class, boolean.class, boolean.class, MultiPhaseParameters.class);
			method.setAccessible(true);
			return (RenderLayer) method.invoke(null, "loot_beam", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS, 256, false, true, state);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return RenderLayer.getEntityTranslucent(LOOT_BEAM_TEXTURE, false);
	}

	/**
	 * Checks if the player is looking at the given entity, accuracy determines how close the player has to look.
	 */
	private static boolean isLookingAt(ClientPlayerEntity player, Entity target, double accuracy) {
		Vec3d difference = new Vec3d(target.getX() - player.getX(), target.getEyeY() - player.getEyeY(), target.getZ() - player.getZ());
		double length = difference.length();
		double dot = player.getRotationVec(1.0F).normalize().dotProduct(difference.normalize());
		return dot > 1.0D - accuracy / length && player.canSee(target);
	}

}
