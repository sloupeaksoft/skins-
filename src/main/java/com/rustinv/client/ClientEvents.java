package com.rustinv.client;

import com.rustinv.ModConfig;
import com.rustinv.clothing.ClothingRules;
import com.rustinv.clothing.Coverage;
import com.rustinv.inventory.InventoryRules;
import com.rustinv.network.MsgOpenInventory;
import com.rustinv.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.EnumSet;
import java.util.StringJoiner;

public class ClientEvents {
    public static final KeyBinding KEY_OPEN = new KeyBinding("key.rustinventory.open", Keyboard.KEY_I, "key.categories.rustinventory");
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    private int lastValidSlot = 0;

    private static void openRustInventory() {
        NetworkHandler.CHANNEL.sendToServer(new MsgOpenInventory());
    }

    /** Ванильный инвентарь игрока (или его замена из другого мода)? */
    private static boolean isPlayerInventoryScreen(GuiScreen gui) {
        if (gui instanceof GuiInventory) return true;
        return gui instanceof GuiContainer && ((GuiContainer) gui).inventorySlots instanceof ContainerPlayer;
    }

    /**
     * Запасной вариант: если инвентарь всё-таки открылся (через другой мод, вкладки и т.п.),
     * подменяем его на Rust-инвентарь. LOWEST — чтобы сработать после других модов.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.replaceInventoryScreen || mc.player == null || mc.player.isCreative()) return;
        if (event.getGui() != null && isPlayerInventoryScreen(event.getGui())) {
            event.setCanceled(true);
            openRustInventory();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        if (event.phase == TickEvent.Phase.START) {
            // Перехватываем E раньше ванили, чтобы обычный инвентарь вообще не открывался
            if (ModConfig.replaceInventoryScreen && mc.currentScreen == null && !mc.player.isCreative()
                    && !mc.player.isSpectator()) {
                boolean pressed = false;
                while (mc.gameSettings.keyBindInventory.isPressed()) pressed = true;
                if (pressed) openRustInventory();
            }
            return;
        }
        while (KEY_OPEN.isPressed()) {
            if (mc.currentScreen == null) openRustInventory();
        }
        if (!ModConfig.rustSlotLayout) return;
        // клавиши 7, 8, 9 не работают — в поясе 6 слотов
        if (mc.player.inventory.currentItem >= InventoryRules.BELT) {
            mc.player.inventory.currentItem = lastValidSlot;
        } else {
            lastValidSlot = mc.player.inventory.currentItem;
        }
    }

    /** Прокрутка колесика по кругу только по 6 слотам. */
    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!ModConfig.rustSlotLayout || mc.player == null || mc.currentScreen != null || mc.player.isSpectator()) return;
        int wheel = event.getDwheel();
        if (wheel == 0) return;
        event.setCanceled(true);
        int cur = Math.min(mc.player.inventory.currentItem, InventoryRules.BELT - 1);
        int dir = wheel > 0 ? -1 : 1;
        cur = (cur + dir + InventoryRules.BELT) % InventoryRules.BELT;
        mc.player.inventory.currentItem = cur;
        lastValidSlot = cur;
    }

    /** Хотбар на 6 слотов. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (!ModConfig.rustSlotLayout || event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.isSpectator()) return;
        event.setCanceled(true);

        EntityPlayer player = mc.player;
        ScaledResolution res = event.getResolution();
        int w = res.getScaledWidth(), h = res.getScaledHeight();
        int width = InventoryRules.BELT * 20 + 2;
        int left = w / 2 - width / 2;
        Gui gui = mc.ingameGUI;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        mc.getTextureManager().bindTexture(WIDGETS);
        gui.drawTexturedModalRect(left, h - 22, 0, 0, width - 1, 22);
        gui.drawTexturedModalRect(left + width - 1, h - 22, 181, 0, 1, 22);
        int sel = Math.min(player.inventory.currentItem, InventoryRules.BELT - 1);
        gui.drawTexturedModalRect(left - 1 + sel * 20, h - 23, 0, 22, 24, 22);

        ItemStack off = player.getHeldItemOffhand();
        EnumHandSide offSide = player.getPrimaryHand().opposite();
        if (!off.isEmpty()) {
            if (offSide == EnumHandSide.LEFT) gui.drawTexturedModalRect(left - 29, h - 23, 24, 22, 29, 24);
            else gui.drawTexturedModalRect(left + width, h - 23, 53, 22, 29, 24);
        }

        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < InventoryRules.BELT; i++) {
            renderItem(mc, player, player.inventory.mainInventory.get(i), left + 3 + i * 20, h - 19);
        }
        if (!off.isEmpty()) {
            if (offSide == EnumHandSide.LEFT) renderItem(mc, player, off, left - 26, h - 19);
            else renderItem(mc, player, off, left + width + 10, h - 19);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
    }

    private static void renderItem(Minecraft mc, EntityPlayer player, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        mc.getRenderItem().renderItemAndEffectIntoGUI(player, stack, x, y);
        mc.getRenderItem().renderItemOverlays(mc.fontRenderer, stack, x, y);
    }

    /** В других интерфейсах (сундук, верстак) заблокированные слоты затемнены. */
    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!ModConfig.rustSlotLayout || !(event.getGui() instanceof GuiContainer)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        GuiContainer gc = (GuiContainer) event.getGui();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        for (Slot s : gc.inventorySlots.inventorySlots) {
            if (s.inventory == mc.player.inventory && InventoryRules.isLocked(s.getSlotIndex())) {
                int x = gc.getGuiLeft() + s.xPos, y = gc.getGuiTop() + s.yPos;
                Gui.drawRect(x, y, x + 16, y + 16, 0xD0101010);
            }
        }
        GlStateManager.enableDepth();
    }

    @SubscribeEvent
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!ModConfig.rustSlotLayout || !(event.getGui() instanceof GuiContainer) || !Mouse.getEventButtonState()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;
        Slot s = ((GuiContainer) event.getGui()).getSlotUnderMouse();
        if (s != null && s.inventory == mc.player.inventory && InventoryRules.isLocked(s.getSlotIndex())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        EnumSet<Coverage> cov = ClothingRules.getCoverage(event.getItemStack());
        if (cov.isEmpty()) return;
        StringJoiner j = new StringJoiner(", ");
        for (Coverage c : cov) j.add(I18n.format(c.langKey()));
        event.getToolTip().add("\u00a77" + I18n.format("rustinventory.tooltip.zones", j.toString()));
        if (GuiScreen.isShiftKeyDown()) {
            event.getToolTip().add("\u00a78" + I18n.format("rustinventory.tooltip.hint"));
        }
    }
}
