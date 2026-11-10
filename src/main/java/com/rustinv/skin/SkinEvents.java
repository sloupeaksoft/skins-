package com.rustinv.skin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Когда в АК кончаются патроны, MineRust заменяет предмет на "АК без патронов" (и обратно при
 * перезарядке) — новый предмет без NBT, и скин бы терялся. Здесь скин переносится на новый предмет.
 */
public class SkinEvents {
    private static class State {
        final List<String> skins = new ArrayList<>();
        Set<Integer> plainSlots = new HashSet<>();
    }

    private final Map<UUID, State> states = new HashMap<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        InventoryPlayer inv = player.inventory;
        List<String> skins = new ArrayList<>();
        Set<Integer> plain = new HashSet<>();
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!Skins.isSkinnable(s)) continue;
            String skin = Skins.getSkin(s);
            if (skin != null) skins.add(skin);
            else plain.add(i);
        }
        ItemStack cursor = inv.getItemStack();
        if (Skins.isSkinnable(cursor) && Skins.getSkin(cursor) != null) skins.add(Skins.getSkin(cursor));

        State st = states.get(player.getUniqueID());
        if (st != null && skins.size() < st.skins.size()) {
            List<String> lost = new ArrayList<>(st.skins);
            for (String s : skins) lost.remove(s);
            for (int slot : new ArrayList<>(plain)) {
                if (lost.isEmpty()) break;
                if (st.plainSlots.contains(slot)) continue; // этот АК уже был без скина
                String skin = lost.remove(0);
                Skins.apply(inv.getStackInSlot(slot), skin);
                inv.markDirty();
                skins.add(skin);
                plain.remove(slot);
            }
        }
        if (st == null) {
            st = new State();
            states.put(player.getUniqueID(), st);
        }
        st.skins.clear();
        st.skins.addAll(skins);
        st.plainSlots = plain;
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        states.remove(event.player.getUniqueID());
    }
}
