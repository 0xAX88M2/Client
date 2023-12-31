/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.GameRenderEvent;
import net.ccbluex.liquidbounce.event.ScreenRenderEvent;
import net.ccbluex.liquidbounce.event.WorldRenderEvent;
import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IMixinGameRenderer {

    @Shadow
    @Final
    private Camera camera;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private int ticks;

    @Shadow
    protected abstract void bobView(MatrixStack matrixStack, float f);

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(double d);

    @Shadow
    protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Shadow
    public abstract MinecraftClient getClient();

    /**
     * Hook game render event
     */
    @Inject(method = "render", at = @At("HEAD"))
    public void hookGameRender(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new GameRenderEvent());
    }


    /**
     * Hook world render event
     */
    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(float partialTicks, long finishTimeNano, MatrixStack matrixStack, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new WorldRenderEvent(matrixStack, partialTicks));
    }

    /**
     * Hook screen render event
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    public void hookScreenRender(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        screen.render(context, mouseX, mouseY, delta);
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(screen, context, mouseX, mouseY, delta));
    }

    @Override
    public Matrix4f getCameraMVPMatrix(float tickDelta, boolean bobbing) {
        MatrixStack matrices = new MatrixStack();

        double fov = getFov(camera, tickDelta, true);
        matrices.multiplyPositionMatrix(getBasicProjectionMatrix(fov));

        if (bobbing) {
            tiltViewWhenHurt(matrices, tickDelta);

            if (client.options.getBobView().getValue()) {
                bobView(matrices, tickDelta);
            }

            float f = MathHelper.lerp(tickDelta, client.player.prevNauseaIntensity, client.player.nauseaIntensity) * client.options.getDistortionEffectScale().getValue().floatValue() * client.options.getDistortionEffectScale().getValue().floatValue();
            if (f > 0.0F) {
                int i = client.player.hasStatusEffect(StatusEffects.NAUSEA) ? 7 : 20;
                float g = 5.0F / (f * f + 5.0F) - f * 0.04F;
                g *= g;

                RotationAxis vec3f = RotationAxis.of(new Vector3f(0.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F));
                matrices.multiply(vec3f.rotationDegrees((ticks + tickDelta) * i));
                matrices.scale(1.0F / g, 1.0F, 1.0F);
                float h = -(ticks + tickDelta) * i;
                matrices.multiply(vec3f.rotationDegrees(h));
            }
        }

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        Vec3d cameraPosition = camera.getPos();

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        matrix4f.mul(new Matrix4f().translate((float) -cameraPosition.x, (float) -cameraPosition.y, (float) -cameraPosition.z));
        return matrix4f;
    }
}
