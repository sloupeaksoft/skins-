package com.rustinv.skin;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Скины оружия. Скин хранится в NBT обычного предмета MineRust,
 * поэтому стрельба, патроны и перезарядка работают как у обычного АК.
 */
public final class Skins {
    public static final String TAG = "RustSkin";
    public static final String RATATA = "ratata";
    public static final String[] ALL = {RATATA};

    public static final ResourceLocation AK = new ResourceLocation("minerustaddons", "akrifle");
    public static final ResourceLocation AK_NOAMMO = new ResourceLocation("minerustaddons", "akriflenoammo");

    public static CreativeTabs TAB;

    private Skins() {
    }

    public static void init() {
        TAB = new CreativeTabs("rustskins") {
            @Override
            @SideOnly(Side.CLIENT)
            public ItemStack getTabIconItem() {
                ItemStack s = create(AK, RATATA);
                return s.isEmpty() ? new ItemStack(Items.GUNPOWDER) : s;
            }

            @Override
            @SideOnly(Side.CLIENT)
            public void displayAllRelevantItems(NonNullList<ItemStack> list) {
                super.displayAllRelevantItems(list);
                for (String id : ALL) {
                    ItemStack s = create(AK, id);
                    if (!s.isEmpty()) list.add(s);
                }
            }
        };
    }

    public static boolean isSkinnable(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
        ResourceLocation rl = stack.getItem().getRegistryName();
        return rl.equals(AK) || rl.equals(AK_NOAMMO);
    }

    @Nullable
    public static String getSkin(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;
        if (!stack.getTagCompound().hasKey(TAG, 8)) return null;
        return stack.getTagCompound().getString(TAG);
    }

    public static void apply(ItemStack stack, String skin) {
        stack.setTagInfo(TAG, new NBTTagString(skin));
    }

    public static ItemStack create(ResourceLocation itemId, String skin) {
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(item);
        apply(s, skin);
        return s;
    }
}
