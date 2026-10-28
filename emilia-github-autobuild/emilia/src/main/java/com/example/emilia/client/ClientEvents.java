package com.example.emilia.client;

import com.example.emilia.EmiliaMod;
import com.example.emilia.network.ActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = EmiliaMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (ModKeys.CAST.consumeClick()) {
            PacketDistributor.sendToServer(new ActionPayload(0));
        }
        while (ModKeys.CYCLE.consumeClick()) {
            PacketDistributor.sendToServer(new ActionPayload(Screen.hasShiftDown() ? 2 : 1));
        }
    }
}
