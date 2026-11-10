package com.rustinv.proxy;

import com.rustinv.RustInventory;
import com.rustinv.client.ClientEvents;
import com.rustinv.client.GuiRustInventory;
import com.rustinv.client.LayerRustClothing;
import com.rustinv.client.SkinModels;
import com.rustinv.clothing.ClothingCapability;
import com.rustinv.clothing.ClothingHandler;
import com.rustinv.inventory.ContainerRust;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        MinecraftForge.EVENT_BUS.register(new SkinModels());
    }

    @Override
    public void init() {
        ClientRegistry.registerKeyBinding(ClientEvents.KEY_OPEN);
    }

    @Override
    public void postInit() {
        for (RenderPlayer render : Minecraft.getMinecraft().getRenderManager().getSkinMap().values()) {
            render.addLayer(new LayerRustClothing(render));
        }
    }

    @Override
    public Object getClientGui(int id, EntityPlayer player) {
        if (id == RustInventory.GUI_RUST_INVENTORY) return new GuiRustInventory(new ContainerRust(player));
        return null;
    }

    @Override
    public void handleClothingSync(final int entityId, final NBTTagCompound data) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            Entity e = mc.world.getEntityByID(entityId);
            ClothingHandler h = ClothingCapability.get(e);
            if (h != null) h.deserializeNBT(data);
        });
    }
}
