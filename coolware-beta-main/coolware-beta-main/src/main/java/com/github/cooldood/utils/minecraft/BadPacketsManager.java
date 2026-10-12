package com.github.cooldood.utils.minecraft;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;

public final class BadPacketsManager {
    private static boolean slot;
    private static boolean attack;
    private static boolean swing;
    private static boolean block;
    private static boolean inventory;

    private BadPacketsManager() {
    }

    public static boolean bad() {
        return bad(true, true, true, true, true);
    }

    public static boolean bad(boolean slot, boolean attack, boolean swing, boolean block, boolean inventory) {
        return (BadPacketsManager.slot && slot)
                || (BadPacketsManager.attack && attack)
                || (BadPacketsManager.swing && swing)
                || (BadPacketsManager.block && block)
                || (BadPacketsManager.inventory && inventory);
    }

    @SubscribeEvent(priority = 9999)
    public static void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof C09PacketHeldItemChange) {
            slot = true;
        } else if (event.packet instanceof C0APacketAnimation) {
            swing = true;
        } else if (event.packet instanceof C02PacketUseEntity) {
            attack = true;
        } else if (event.packet instanceof C08PacketPlayerBlockPlacement
                || event.packet instanceof C07PacketPlayerDigging) {
            block = true;
        } else if (event.packet instanceof C0EPacketClickWindow
                || event.packet instanceof C0DPacketCloseWindow) {
            inventory = true;
        } else if (event.packet instanceof C03PacketPlayer) {
            reset();
        }
    }

    public static void reset() {
        slot = false;
        attack = false;
        swing = false;
        block = false;
        inventory = false;
    }
}