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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import net.ccbluex.liquidbounce.event.*;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void injectChunkLoadEvent(final ChunkDataS2CPacket packet, final CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkLoadEvent(packet.getX(), packet.getZ()));
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void injectUnloadEvent(final UnloadChunkS2CPacket packet, final CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkUnloadEvent(packet.getX(), packet.getZ()));
    }


    @Inject(method = "onHealthUpdate", at = @At("RETURN"))
    private void injectHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new HealthUpdateEvent(packet.getHealth(), packet.getFood(), packet.getSaturation()));
        if (packet.getHealth() == 0) {
            EventManager.INSTANCE.callEvent(new DeathEvent());
        }
    }
}
