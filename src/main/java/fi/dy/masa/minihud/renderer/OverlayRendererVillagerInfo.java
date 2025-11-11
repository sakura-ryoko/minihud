package fi.dy.masa.minihud.renderer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.mixin.entity.IMixinMerchantEntity;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.nbt.NbtEntityUtils;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.mixin.entity.IMixinZombieVillagerEntity;
import fi.dy.masa.minihud.util.EntityUtils;

public class OverlayRendererVillagerInfo extends OverlayRendererBase implements IClientTickHandler
{
    public static final OverlayRendererVillagerInfo INSTANCE = new OverlayRendererVillagerInfo();

    // Mini Secondary Cache so villagers' data doesn't ... `Flash`
    private final ConcurrentHashMap<Integer, Pair<Long, Pair<Entity, NbtCompound>>> recentEntityData;
    private long lastTick;

    protected OverlayRendererVillagerInfo()
    {
        this.recentEntityData = new ConcurrentHashMap<>();
        this.lastTick = System.currentTimeMillis();
    }

    public void reset(boolean isLogout)
    {
        // Dimension change tick
        if (!isLogout)
        {
            MiniHUD.debugLog("OverlayRendererVillagerInfo#reset() - dimension change or log-in");
            long now = System.currentTimeMillis();
            this.lastTick =  - (this.getCacheTimeout() + 5000L);
            this.tickCache(now);
            this.lastTick = now;
        }
        else
        {
            MiniHUD.debugLog("OverlayRendererVillagerInfo#reset() - log-out");
        }

        // Clear
        synchronized (this.recentEntityData)
        {
            this.recentEntityData.clear();
        }
    }

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        long now = System.currentTimeMillis();

        if ((now - this.lastTick) > 50)
        {
            this.lastTick = now;

            if (RendererToggle.OVERLAY_VILLAGER_INFO.getBooleanValue())
            {
                this.tickCache(now);
            }
            else
            {
                if (!this.recentEntityData.isEmpty())
                {
                    this.recentEntityData.clear();
                }
            }
        }
    }

    private long getCacheTimeout()
    {
        return EntitiesDataManager.getInstance().getCacheTimeout();
    }

    private void tickCache(long now)
    {
        long timeout = this.getCacheTimeout();

        synchronized (this.recentEntityData)
        {
            this.recentEntityData.forEach(((integer, longPair) ->
            {
                if ((now - longPair.getLeft()) > timeout || longPair.getLeft() > now)
                {
//                    MiniHUD.debugLog("villagerOverlayCache: entity Id [{}] has timed out by [{}] ms", integer, timeout);
                    this.recentEntityData.remove(integer);
                }
            }));
        }
    }

    private boolean isNbtValid(NbtCompound nbt)
    {
        if (nbt.contains(NbtKeys.OFFERS))
        {
            return true;
        }
        else return (nbt.contains(NbtKeys.ZOMBIE_CONVERSION) &&
                     nbt.getInt(NbtKeys.ZOMBIE_CONVERSION, -1) > 0) ||
                     nbt.contains(NbtKeys.CONVERSION_PLAYER);
    }

    private @Nullable Pair<Entity, NbtCompound> getVillagerData(World world, int entityId)
    {
        Pair<Entity, NbtCompound> pair = EntitiesDataManager.getInstance().requestEntity(world, entityId);

        if (pair != null &&
            pair.getRight() != null &&
            !pair.getRight().isEmpty() &&
            this.isNbtValid(pair.getRight()))
        {
            long now = System.currentTimeMillis();

            synchronized (this.recentEntityData)
            {
                this.recentEntityData.put(entityId, Pair.of(now, pair));
            }

            return pair;
        }
        else if (this.recentEntityData.containsKey(entityId))
        {
            return this.recentEntityData.get(entityId).getRight();
        }

        return null;
    }

    private @Nullable TradeOfferList getTrades(World world, VillagerEntity villager)
    {
        if (world == null || villager == null)
        {
            return null;
        }

        Pair<Entity, NbtCompound> pair = this.getVillagerData(world, villager.getId());
        TradeOfferList list = null;

        if (pair != null)
        {
            if (pair.getRight() != null && !pair.getRight().isEmpty())
            {
                list = NbtEntityUtils.getTradeOffersFromNbt(pair.getRight(), world.getRegistryManager());
            }
            else if (pair.getLeft() != null && pair.getLeft() instanceof VillagerEntity entity)
            {
                list = ((IMixinMerchantEntity) entity).malilib_offers();
            }
        }

        return list;
    }

    private int getConversionTime(World world, ZombieVillagerEntity villager)
    {
        if (world == null || villager == null)
        {
            return -1;
        }

        Pair<Entity, NbtCompound> pair = this.getVillagerData(world, villager.getId());
        int conversionTime = -1;

        if (pair != null)
        {
            if (pair.getRight() != null && !pair.getRight().isEmpty())
            {
                Pair<Integer, UUID> zombiePair = NbtEntityUtils.getZombieConversionTimerFromNbt(pair.getRight());

                if (zombiePair != null && zombiePair.getLeft() > -1)
                {
                    conversionTime = zombiePair.getLeft();
                }
            }
            else if (pair.getLeft() != null && pair.getLeft() instanceof ZombieVillagerEntity zombert)
            {
                conversionTime = ((IMixinZombieVillagerEntity) zombert).minihud_conversionTimer();
            }
        }

        return conversionTime;
    }

    @Override
    public String getName()
    {
        return "VillagerInfo";
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
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc, Profiler profiler)
    {
        Box box = entity.getBoundingBox().expand(30, 10, 30);
        World world = WorldUtils.getBestWorld(mc);

        if (world == null) return;

        if (Configs.Generic.VILLAGER_OFFER_ENCHANTMENT_BOOKS.getBooleanValue())
        {
            List<VillagerEntity> librarians = EntityUtils.getEntitiesByClass(mc, VillagerEntity.class, box, villager -> villager.getVillagerData().profession().matchesKey(VillagerProfession.LIBRARIAN));
            Map<Object2IntMap.Entry<RegistryEntry<Enchantment>>, Integer> lowestPrices = new HashMap<>();

            // Prepare
            if (Configs.Generic.VILLAGER_OFFER_LOWEST_PRICE_NEARBY.getBooleanValue())
            {
                for (VillagerEntity librarian : librarians)
                {
                    TradeOfferList offers = this.getTrades(world, librarian);

                    if (offers == null || offers.isEmpty())
                    {
                        continue;
                    }

                    for (TradeOffer tradeOffer : offers)
                    {
                        if (tradeOffer.getSellItem().getItem() == Items.ENCHANTED_BOOK && tradeOffer.getFirstBuyItem().item().value() == Items.EMERALD)
                        {
                            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : tradeOffer.getSellItem().getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).getEnchantmentEntries())
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

            // Render
            for (VillagerEntity librarian : librarians)
            {
                TradeOfferList offers = this.getTrades(world, librarian);

                if (offers == null || offers.isEmpty())
                {
                    continue;
                }

                List<String> overlay = new ArrayList<>();

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

                this.renderAtEntity(overlay, entity, librarian);
            }
        }

        if (Configs.Generic.VILLAGER_CONVERSION_TICKS.getBooleanValue())
        {
            List<ZombieVillagerEntity> zombieVillagers = EntityUtils.getEntitiesByClass(mc, ZombieVillagerEntity.class, box, e -> true);

            for (ZombieVillagerEntity villager : zombieVillagers)
            {
                int conversionTimer = this.getConversionTime(world, villager);

                if (conversionTimer > 0)
                {
                    this.renderAtEntity(List.of(String.format("%ds", Math.round((float) conversionTimer / 20))), entity, villager);
                }
            }
        }
    }

    @Override
    public boolean hasData()
    {
        return false;
    }

    @Override
    public void render(Vec3d cameraPos, MinecraftClient mc, Profiler profiler)
    {
        // NO-OP
    }

    @Override
    public void reset()
    {
        super.reset();
    }

    private void renderAtEntity(List<String> texts, Entity entity, Entity targetEntity)
    {
        float delta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
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
//            RenderUtils.drawTextPlate(List.of(line), x, y, z, entity.getYaw(), entity.getPitch(), 0.02f, 0xFFFFFFFF, 0x40000000, false);
            y -= 0.2;
        }
    }
}
