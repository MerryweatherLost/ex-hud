package com.github.clevernucleus.exhud.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.clevernucleus.exhud.ExHUD;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
	
	@Final
	@Shadow
	private MinecraftClient client;
	
	@Shadow
	private int scaledWidth;
	
	@Shadow
    private int scaledHeight;
	
	@Shadow
	protected abstract PlayerEntity getCameraPlayer();
	
	@Shadow
	protected abstract LivingEntity getRiddenEntity();
	
	@Shadow
	protected abstract int getHeartCount(LivingEntity livingEntity);

	// Removes vanilla mount health bar and renders our own.
	@Inject(method = "renderMountHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;getHeartCount(Lnet/minecraft/entity/LivingEntity;)I"), cancellable = true)
	private void exhud_renderMountHealth(DrawContext context, CallbackInfo ci) {
		if(!ExHUD.renderCustomHealthbar()) {
			return;
		}
		
		LivingEntity riddenEntity = this.getRiddenEntity();
		int h = (int)Math.min(78.0F / riddenEntity.getMaxHealth() * riddenEntity.getHealth(), 78.0F);
		int x = this.scaledWidth / 2 + 13;
		int y = this.scaledHeight - 37;

		context.drawTexture(ExHUD.GUI_HEALTH_BARS, x, y, 0, ExHUD.healthbarTexture(riddenEntity), 78, 8, 128, 64);
		
		if(MathHelper.ceil(riddenEntity.getAbsorptionAmount()) > 0) {
			context.drawTexture(ExHUD.GUI_HEALTH_BARS, x, y, 0, 40, 78, 8, 128, 64);
		}

		context.drawTexture(ExHUD.GUI_HEALTH_BARS, x, y, 0, 0, 78 - h, 8, 128, 64);
		
		String healthbar = ExHUD.FORMAT.format(riddenEntity.getHealth() + riddenEntity.getAbsorptionAmount()) + "/" + ExHUD.FORMAT.format(riddenEntity.getMaxHealth());
		float healthPos = ((float)this.scaledWidth - (float)this.client.textRenderer.getWidth(healthbar)) * 0.5F;
		float s = 1.0F / 0.7F;

		MatrixStack matrices = context.getMatrices();
		
		matrices.push();
		matrices.scale(0.7F, 0.7F, 0.7F);
		
		ExHUD.drawBorderedText(context, this.client.textRenderer, healthbar, s, healthPos + 56.0F, y + 1.5F, 0xFFFFFF, 0x000000);
		
		matrices.pop();
		ci.cancel();
	}
	
	// Removes vanilla mount jump bar and renders our own.
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderMountJumpBar(Lnet/minecraft/entity/JumpingMount;Lnet/minecraft/client/gui/DrawContext;I)V"))
	private void exhud_renderMountJumpBar(InGameHud instance, JumpingMount mount, DrawContext context, int x) {
		if (this.client.player != null) {
			float f = this.client.player.getMountJumpStrength();
			int j = (int)(f * 183.0F);

			context.drawTexture(ExHUD.GUI_LEVEL_BARS, x, this.scaledHeight - 27, 0, 9, 182, 3, 256, 16);

			if(mount.getJumpCooldown() > 0) {
				context.drawTexture(ExHUD.GUI_LEVEL_BARS, x, this.scaledHeight - 27, 0, 6, 182, 3, 256, 16);
			} else if(j > 0) {
				context.drawTexture(ExHUD.GUI_LEVEL_BARS, x, this.scaledHeight - 27, 0, 12, j, 3, 256, 16);
			}
		}
	}
	
	// Removed vanilla experience bar and level text
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderExperienceBar(Lnet/minecraft/client/gui/DrawContext;I)V"))
	private void exhud_renderExperienceBar(InGameHud instance, DrawContext context, int x) {
		PlayerEntity player = this.getCameraPlayer();
		int l = (int)(183.0F * player.experienceProgress);

		context.drawTexture(ExHUD.GUI_LEVEL_BARS, (this.scaledWidth / 2) - 91, this.scaledHeight - 27, 0, 0, 182, 3, 256, 16);
		context.drawTexture(ExHUD.GUI_LEVEL_BARS, (this.scaledWidth / 2) - 91, this.scaledHeight - 27, 0, 3, l, 3, 256, 16);
		
		if(player.experienceLevel > 0) {
			TextRenderer textRenderer = this.client.textRenderer;

			String level = String.valueOf(player.experienceLevel);
			int levelPos = (this.scaledWidth - textRenderer.getWidth(level)) / 2;

			MatrixStack matrices = context.getMatrices();
			
			matrices.push();
			
			context.drawText(textRenderer, level, (levelPos + 1), (this.scaledHeight - 36), 0x000000, false);
			context.drawText(textRenderer, level, (levelPos - 1), (this.scaledHeight - 36), 0x000000, false);
			context.drawText(textRenderer, level, levelPos, (this.scaledHeight - 35), 0x000000, false);
			context.drawText(textRenderer, level, levelPos, (this.scaledHeight - 37), 0x000000, false);
			context.drawText(textRenderer, level, levelPos, (this.scaledHeight - 36), 8453920, false);
			
			matrices.pop();
		}
	}

	// Removes the vanilla armor bar.
	@ModifyVariable(method = "renderStatusBars", at = @At(value = "STORE", target = "Lnet/minecraft/entity/player/PlayerEntity;getArmor()I"), ordinal = 11)
	private int exhud_getArmor(int u) {
		return ExHUD.renderCustomUtilities() ? 0 : u;
	}
	
	// Removes the vanilla food and air bars and renders our own.
	@Inject(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;getRiddenEntity()Lnet/minecraft/entity/LivingEntity;"), cancellable = true)
	private void exhud_renderStatusBars(DrawContext context, CallbackInfo ci) {
		if(!ExHUD.renderCustomUtilities()) {
			return;
		}
		
		LivingEntity riddenEntity = this.getRiddenEntity();
		
		if(this.getHeartCount(riddenEntity) > 0) {
			ci.cancel();
			return;
		}
		
		PlayerEntity player = this.getCameraPlayer();
		HungerManager hungerManager = player.getHungerManager();
        int foodLevel = hungerManager.getFoodLevel() * 5;
        int airLevel = (int)(100.0F * Math.max((float)player.getAir(), 0.0F) / (float)player.getMaxAir());
        int armor = player.getArmor();
        boolean hunger = player.hasStatusEffect(StatusEffects.HUNGER);

        context.drawTexture(ExHUD.VANILLA_GUI_ICONS_TEXTURE, (this.scaledWidth / 2) + 12, this.scaledHeight - 38, hunger ? 133 : 16, 27, 9, 9, 256, 256);
        context.drawTexture(ExHUD.VANILLA_GUI_ICONS_TEXTURE, (this.scaledWidth / 2) + 12, this.scaledHeight - 38, hunger ? 88 : 52, 27, 9, 9, 256, 256);
        context.drawTexture(ExHUD.VANILLA_GUI_ICONS_TEXTURE, (this.scaledWidth / 2) + (airLevel < 100 ? 44 : 50), this.scaledHeight - 38, 34, 9, 9, 9, 256, 256);
        
        if(airLevel < 100) {
        	context.drawTexture(ExHUD.VANILLA_GUI_ICONS_TEXTURE, (this.scaledWidth / 2) + (armor < 10 ? 66 : (armor < 100 ? 70 : 76)), this.scaledHeight - 38, 16, 18, 9, 9, 256, 256);
        }
        
        var mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
        var offHandStack = player.getStackInHand(Hand.OFF_HAND);
        
        int itemFoodLevel = mainHandStack.isFood() ? mainHandStack.getItem().getFoodComponent().getHunger() : (offHandStack.isFood() ? offHandStack.getItem().getFoodComponent().getHunger() : 0);
        int combinedFoodLevel = Math.min(100, foodLevel + (itemFoodLevel * 5));
        
        float s = 1.0F / 0.7F;

		MatrixStack matrices = context.getMatrices();
		
		matrices.push();
		matrices.scale(0.7F, 0.7F, 0.7F);
		
		ExHUD.drawBorderedText(context, this.client.textRenderer, "x" + armor, s, (this.scaledWidth / 2.0F) + (airLevel < 100 ? 54.0F : 60.0F), this.scaledHeight - 36.0F, 0xFFFFFF, 0x000000);
		
		if(ExHUD.enableFoodStat() && foodLevel < 100 && itemFoodLevel > 0) {
			int tick = (int)((System.currentTimeMillis() / 50L) % 20L);
	        int rate = (int)(((255.0F * Math.sin(Math.toRadians(18 * tick))) + 255.0F) * 0.5F);
	        int white = 0xFFFFFF;
	        int black = 0x000000;
	        
			if(rate > 8) {
				int alpha = rate << 24 & -white;
				
				ExHUD.drawBorderedText(context, this.client.textRenderer, combinedFoodLevel + "%", s, (this.scaledWidth / 2.0F) + 22.0F, this.scaledHeight - 36.0F, white | alpha, black | alpha);
			}
		} else {
			ExHUD.drawBorderedText(context, this.client.textRenderer, foodLevel + "%", s, (this.scaledWidth / 2.0F) + 22.0F, this.scaledHeight - 36.0F, 0xFFFFFF, 0x000000);
		}
		
		if(airLevel < 100) {
			ExHUD.drawBorderedText(context, this.client.textRenderer, airLevel + "%", s, (this.scaledWidth / 2.0F) + (armor < 10 ? 76.0F : (armor < 100 ? 80.0F : 86.0F)), this.scaledHeight - 36.0F, 0xFFFFFF, 0x000000);
		}
		
		matrices.pop();
		ci.cancel();
	}
	
	// Removes the vanilla health bar and renders our own.
	@Inject(method = "renderHealthBar", at = @At("HEAD"), cancellable = true)
	private void exhud_renderHealthBar(DrawContext context, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
		if(!ExHUD.renderCustomHealthbar()) {
			return;
		}
		
		int h = (int)Math.min(78.0F / player.getMaxHealth() * player.getHealth(), 78.0F);

		context.drawTexture(ExHUD.GUI_HEALTH_BARS, x, y + 2, 0, 0, 78, 8, 128, 64);
		context.drawTexture(ExHUD.GUI_HEALTH_BARS, x, y + 2, 0, ExHUD.healthbarTexture(player), h, 8, 128, 64);
		
		if(absorption > 0) {
			context.drawTexture(ExHUD.GUI_HEALTH_BARS, x, y + 2, 0, 40, h, 8, 128, 64);
		}
		
		String healthbar = ExHUD.FORMAT.format(player.getHealth() + player.getAbsorptionAmount()) + "/" + ExHUD.FORMAT.format(player.getMaxHealth());
		float healthPos = ((2.0F * ((float)x + 91.0F)) - (float)this.client.textRenderer.getWidth(healthbar)) * 0.5F;
		float s = 1.0F / 0.7F;

		MatrixStack matrices = context.getMatrices();
		
		matrices.push();
		matrices.scale(0.7F, 0.7F, 0.7F);
		
		ExHUD.drawBorderedText(context, this.client.textRenderer, healthbar, s, healthPos - 48.0F, y + 3.5F, 0xFFFFFF, 0x000000);
		
		matrices.pop();
		ci.cancel();
	}
}
