package com.rustinv.client;

import com.google.common.collect.ImmutableMap;
import com.rustinv.RustInventory;
import com.rustinv.skin.Skins;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Клиент: текстуры и модели скинов. Модель та же, что у АК из MineRust, меняется только текстура. */
public class SkinModels {

    private static ResourceLocation texture(String skin) {
        return new ResourceLocation(RustInventory.MODID, "items/ak_" + skin);
    }

    @SubscribeEvent
    public void onStitch(TextureStitchEvent.Pre event) {
        for (String skin : Skins.ALL) event.getMap().registerSprite(texture(skin));
    }

    @SubscribeEvent
    public void onBake(ModelBakeEvent event) {
        List<ModelResourceLocation> targets = new ArrayList<>();
        for (ModelResourceLocation mrl : event.getModelRegistry().getKeys()) {
            if (!"minerustaddons".equals(mrl.getResourceDomain()) || !"inventory".equals(mrl.getVariant())) continue;
            String path = mrl.getResourcePath();
            if (path.equals(Skins.AK.getResourcePath()) || path.equals(Skins.AK_NOAMMO.getResourcePath())) targets.add(mrl);
        }
        for (ModelResourceLocation mrl : targets) {
            IBakedModel base = event.getModelRegistry().getObject(mrl);
            if (base == null) continue;
            Map<String, IBakedModel> skinned = new HashMap<>();
            for (String skin : Skins.ALL) {
                try {
                    IModel model = ModelLoaderRegistry.getModel(new ResourceLocation("minerustaddons", "item/" + mrl.getResourcePath()));
                    String tex = texture(skin).toString();
                    IModel re = model.retexture(ImmutableMap.of("0", tex, "missing", tex, "particle", tex));
                    skinned.put(skin, re.bake(re.getDefaultState(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter()));
                } catch (Exception e) {
                    System.err.println("[RustInventory] Failed to bake skin " + skin + " for " + mrl + ": " + e);
                }
            }
            if (!skinned.isEmpty()) event.getModelRegistry().putObject(mrl, new SkinnedModel(base, skinned));
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        String skin = Skins.getSkin(event.getItemStack());
        if (skin == null || !Skins.isSkinnable(event.getItemStack())) return;
        event.getToolTip().add(1, "\u00a7b" + I18n.format("rustinventory.skin.label", I18n.format("rustinventory.skin." + skin)));
    }

    private static class SkinnedModel extends BakedModelWrapper<IBakedModel> {
        private final ItemOverrideList overrides;

        SkinnedModel(IBakedModel base, final Map<String, IBakedModel> skinned) {
            super(base);
            final ItemOverrideList baseOverrides = base.getOverrides();
            this.overrides = new ItemOverrideList(Collections.emptyList()) {
                @Override
                public IBakedModel handleItemState(IBakedModel original, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
                    String skin = Skins.getSkin(stack);
                    if (skin != null && skinned.containsKey(skin)) return skinned.get(skin);
                    return baseOverrides.handleItemState(original, stack, world, entity);
                }
            };
        }

        @Override
        public ItemOverrideList getOverrides() {
            return overrides;
        }
    }
}
