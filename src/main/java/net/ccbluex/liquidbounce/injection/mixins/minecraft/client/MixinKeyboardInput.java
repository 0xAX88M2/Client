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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.MovementInputEvent;
import net.ccbluex.liquidbounce.utils.client.TickStateManager;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {

    @Shadow
    @Final
    private GameOptions settings;

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;pressingRight:Z", shift = At.Shift.AFTER, ordinal = 0), allow = 1)
    private void injectMovementInputEvent(boolean slowDown, float f, CallbackInfo ci) {
        var event = new MovementInputEvent(new DirectionalInput(this.pressingForward, this.pressingBack, this.pressingLeft, this.pressingRight), this.jumping);

        EventManager.INSTANCE.callEvent(event);

        var directionalInput = event.getDirectionalInput();

        this.pressingForward = directionalInput.getForwards();
        this.pressingBack = directionalInput.getBackwards();
        this.pressingLeft = directionalInput.getLeft();
        this.pressingRight = directionalInput.getRight();
        this.jumping = event.getJumping();
    }


    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z"))
    private void injectForcedState(KeyboardInput instance, boolean value) {
        Boolean enforceEagle = TickStateManager.INSTANCE.getEnforcedState().getEnforceEagle();
        instance.sneaking = enforceEagle != null ? enforceEagle : value;
    }

}
