package fi.dy.masa.minihud.renderer;

import java.util.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.*;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.mixin.IMixinMerchantEntity;
import fi.dy.masa.minihud.mixin.IMixinZombieVillagerEntity;
import fi.dy.masa.minihud.util.EntityUtils;

public class OverlayRendererVillagerInfo extends OverlayRendererBase
{
    public static final OverlayRendererVillagerInfo INSTANCE = new OverlayRendererVillagerInfo();
    @Override
    public String getName()
    {
        return "Villager Info Overlay";
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_VILLAGER_INFO.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        return true;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        Box box = entity.getBoundingBox().expand(30, 10, 30);
        World world = WorldUtils.getBestWorld(mc);
        if (world == null) return;

        if (Configs.Generic.VILLAGER_OFFER_ENCHANTMENT_BOOKS.getBooleanValue())
        {
            List<VillagerEntity> librarians = EntityUtils.getEntitiesByClass(mc, VillagerEntity.class, box, villager -> villager.getVillagerData().getProfession() == VillagerProfession.LIBRARIAN);

            Map<Object2IntMap.Entry<RegistryEntry<Enchantment>>, Integer> lowestPrices = new HashMap<>();
            // Prepare
            if (Configs.Generic.VILLAGER_OFFER_LOWEST_PRICE_NEARBY.getBooleanValue())
            {
                for (VillagerEntity librarian : librarians)
                {
                    TradeOfferList offers = ((IMixinMerchantEntity) librarian).minihud_offers();
                    if (offers != null)
                    {
                        for (TradeOffer tradeOffer : offers)
                        {
                            if (tradeOffer.getSellItem().getItem() == Items.ENCHANTED_BOOK && tradeOffer.getFirstBuyItem().item().value() == Items.EMERALD)
                            {
                                for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : tradeOffer.getSellItem().getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, null).getEnchantmentEntries())
                                {
                                    int emeraldCost = tradeOffer.getFirstBuyItem().count();
                                    if (lowestPrices.containsKey(entry))
                                    {
                                        if (emeraldCost < lowestPrices.get(entry))
                                        {
                                            lowestPrices.put(entry, emeraldCost);
                                        }
                                    }
                                    else
                                    {
                                        lowestPrices.put(entry, emeraldCost);
                                    }
                                }
                            }
                        }
                    }

                }
            }

            // Render
            for (VillagerEntity librarian : librarians)
            {
                if (librarian.isClient())
                {
                    EntitiesDataManager.getInstance().requestEntity(librarian.getId());
                }
                List<String> overlay = new ArrayList<>();
                TradeOfferList offers = ((IMixinMerchantEntity) librarian).minihud_offers();
                if (offers == null)
                {
                    continue;
                }

                for (TradeOffer tradeOffer : offers)
                {
                    if (tradeOffer.getSellItem().getItem() == Items.ENCHANTED_BOOK)
                    {
                        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : tradeOffer.getSellItem().getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).getEnchantmentEntries())
                        {
                            StringBuilder sb = new StringBuilder();

                            if (entry.getKey().value().getMaxLevel() == entry.getIntValue())
                            {
                                sb.append(GuiBase.TXT_GOLD);
                            }
                            else if (Configs.Generic.VILLAGER_OFFER_HIGHEST_LEVEL_ONLY.getBooleanValue())
                            {
                                continue;
                            }
                            sb.append(Enchantment.getName(entry.getKey(), entry.getIntValue()).getString());
                            sb.append(GuiBase.TXT_RST);

                            if (tradeOffer.getFirstBuyItem().item().value() == Items.EMERALD)
                            {
                                sb.append(" ");
                                int emeraldCost = tradeOffer.getFirstBuyItem().count();
                                if (Configs.Generic.VILLAGER_OFFER_LOWEST_PRICE_NEARBY.getBooleanValue())
                                {
                                    if (emeraldCost > lowestPrices.getOrDefault(entry, Integer.MAX_VALUE))
                                    {
                                        continue;
                                    }
                                }
                                int lowest = 2 + 3 * entry.getIntValue();
                                int highest = 6 + 13 * entry.getIntValue();
                                if (entry.getKey().isIn(EnchantmentTags.DOUBLE_TRADE_PRICE))
                                {
                                    lowest *= 2;
                                    highest *= 2;
                                }
                                if (emeraldCost > MathHelper.lerp(Configs.Generic.VILLAGER_OFFER_PRICE_THRESHOLD.getDoubleValue(), lowest, highest))
                                {
                                    continue;
                                }
                                if (emeraldCost < MathHelper.lerp(1.0 / 3, lowest, highest))
                                {
                                    sb.append(GuiBase.TXT_GREEN);
                                }
                                if (emeraldCost > MathHelper.lerp(2.0 / 3, lowest, highest))
                                {
                                    sb.append(GuiBase.TXT_RED);
                                }

                                // Can add additional formatting if you like, but this works as is
                                sb.append(emeraldCost);
                                // Add Village Offer Price Range
                                if (Configs.Generic.VILLAGER_OFFER_PRICE_RANGE.getBooleanValue())
                                {
                                    sb.append(' ').append('(').append(lowest).append('-').append(highest).append(')');
                                }
                                sb.append(GuiBase.TXT_RST);
                            }
                            overlay.add(sb.toString());
                        }
                    }
                }

                renderAtEntity(overlay, entity, librarian);
            }
        }

        if (Configs.Generic.VILLAGER_CONVERSION_TICKS.getBooleanValue())
        {
            List<ZombieVillagerEntity> zombieVillagers = EntityUtils.getEntitiesByClass(mc, ZombieVillagerEntity.class, box, e -> true);

            for (ZombieVillagerEntity villager : zombieVillagers)
            {
                if (villager.getWorld().isClient)
                {
                    EntitiesDataManager.getInstance().requestEntity(villager.getId());
                }

                int conversionTimer = ((IMixinZombieVillagerEntity) villager).minihud_conversionTimer();
                if (conversionTimer > 0)
                {
                    renderAtEntity(List.of(String.valueOf(conversionTimer)), entity, villager);
                }
            }
        }
    }

    private void renderAtEntity(List<String> texts, Entity entity, Entity targetEntity)
    {
        float delta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
        var cameraPos = entity.getLerpedPos(delta);
        var targetPos = targetEntity.getLerpedPos(delta);
        double hypot = MathHelper.hypot(cameraPos.getX() - targetPos.getX(), cameraPos.getZ() - targetPos.getZ());
        double distance = 0.8;
        double x = targetPos.getX() + (cameraPos.getX() - targetPos.getX()) / hypot * distance;
        double z = targetPos.getZ() + (cameraPos.getZ() - targetPos.getZ()) / hypot * distance;
        double y = targetPos.getY() + 1.5 + 0.1 * texts.size();

        // Render the overlay at its job site, this is useful in trading halls
        if (targetEntity instanceof LivingEntity living)
        {
            Optional<GlobalPos> jobSite = living.getBrain().getOptionalMemory(MemoryModuleType.JOB_SITE);
            if (jobSite != null && jobSite.isPresent())
            {
                BlockPos pos = jobSite.get().pos();
                if (targetPos.distanceTo(pos.toCenterPos()) < 1.7)
                {
                    x = pos.getX() + 0.5;
                    z = pos.getZ() + 0.5;
                }
            }
        }

        for (String line : texts)
        {
            RenderUtils.drawTextPlate(List.of(line), x, y, z, 0.02f);
            y -= 0.2;
        }
    }
}
