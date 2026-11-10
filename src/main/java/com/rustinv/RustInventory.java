package com.rustinv;

import com.rustinv.clothing.ClothingCapability;
import com.rustinv.event.CommonEvents;
import com.rustinv.inventory.GuiHandler;
import com.rustinv.network.NetworkHandler;
import com.rustinv.proxy.CommonProxy;
import com.rustinv.skin.SkinEvents;
import com.rustinv.skin.Skins;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = RustInventory.MODID, name = RustInventory.NAME, version = RustInventory.VERSION,
        acceptedMinecraftVersions = "[1.12.2]", dependencies = "after:minerustaddons")
public class RustInventory {
    public static final String MODID = "rustinventory";
    public static final String NAME = "Rust Inventory";
    public static final String VERSION = "1.0.0";

    public static final int GUI_RUST_INVENTORY = 0;

    @Mod.Instance(MODID)
    public static RustInventory instance;

    @SidedProxy(clientSide = "com.rustinv.proxy.ClientProxy", serverSide = "com.rustinv.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ClothingCapability.register();
        NetworkHandler.init();
        MinecraftForge.EVENT_BUS.register(new CommonEvents());
        Skins.init();
        MinecraftForge.EVENT_BUS.register(new SkinEvents());
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }
}
