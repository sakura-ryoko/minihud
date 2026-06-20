package fi.dy.masa.minihud.renderer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.mixin.entity.IMixinMerchantEntity;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.malilib.util.position.Vec3d;
import fi.dy.masa.malilib.util.text.TextAlignment;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.data.EntityDataManager;
import fi.dy.masa.minihud.mixin.entity.IMixinZombieVillager;
import fi.dy.masa.minihud.util.EntityUtils;

public class OverlayRendererVillagerInfo extends OverlayRendererBase implements IClientTickHandler
{
    public static final OverlayRendererVillagerInfo INSTANCE = new OverlayRendererVillagerInfo();

    // Mini Secondary Cache so villagers' data doesn't ... `Flash`
    private final ConcurrentHashMap<Integer, Pair<Long, Pair<Entity, CompoundData>>> recentEntityData;
    private final ConcurrentHashMap<Entity, List<String>> villagerData;
    private final ConcurrentHashMap<UUID, LastTextPlate> lastPos;
    private long lastTick;
    private final int xViewRange;
    private final int yViewRange;
    private final int zViewRange;

    protected OverlayRendererVillagerInfo()
    {
        this.recentEntityData = new ConcurrentHashMap<>(16, 0.9f, 1);
        this.villagerData = new ConcurrentHashMap<>(16, 0.9f, 1);
        this.lastPos = new ConcurrentHashMap<>(16, 0.9f, 1);
        this.lastTick = System.currentTimeMillis();
        this.xViewRange = 30;
        this.yViewRange = 10;
        this.zViewRange = 30;
        this.renderThrough = false;
    }

    public void reset(boolean isLogout)
    {
        // Dimension change tick
        if (!isLogout)
        {
            MiniHUD.debugLog("OverlayRendererVillagerInfo#reset() - dimension change or log-in");
            long now = System.currentTimeMillis();
            this.lastTick = now - (this.getCacheTimeout() + 5000L);
            this.tickCache(now);
            this.lastTick = now;
        }
        else
        {
            MiniHUD.debugLog("OverlayRendererVillagerInfo#reset() - log-out");
        }

        // Clear
        this.recentEntityData.clear();
    }

    @Override
    public void onClientTick(Minecraft mc)
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
        return EntityDataManager.getInstance().getCacheTimeout();
    }

    private void tickCache(long now)
    {
        long timeout = this.getCacheTimeout();

        this.recentEntityData.forEach(((integer, longPair) ->
        {
            if (longPair != null)
            {
                if ((now - longPair.getLeft()) > timeout || longPair.getLeft() > now)
                {
                    MiniHUD.debugLog("villagerOverlayCache: entity Id [{}] has timed out by [{}] ms", integer, timeout);
                    this.recentEntityData.remove(integer);
                }
            }
        }));

        this.lastPos.forEach((uuid, pl) ->
                             {
                                 if ((now - pl.tick()) > timeout || pl.tick() > now)
                                 {
                                     this.lastPos.remove(uuid);
                                 }
                             });
    }

    private boolean isDataValid(CompoundData data)
    {
        if (data.containsLenient(NbtKeys.OFFERS))
        {
            return true;
        }
        else return (data.containsLenient(NbtKeys.ZOMBIE_CONVERSION) &&
                     data.getInt(NbtKeys.ZOMBIE_CONVERSION) > 0) ||
                     data.containsLenient(NbtKeys.CONVERSION_PLAYER);
    }

    private @Nullable Pair<Entity, CompoundData> getVillagerData(Level world, int entityId)
    {
        Pair<Entity, CompoundData> pair = EntityDataManager.getInstance().requestEntity(world, entityId);

        if (pair != null &&
            pair.getRight() != null &&
            !pair.getRight().isEmpty() &&
            this.isDataValid(pair.getRight()))
        {
            long now = System.currentTimeMillis();

            this.recentEntityData.put(entityId, Pair.of(now, pair));

            return pair;
        }
        else if (this.recentEntityData.containsKey(entityId))
        {
            return this.recentEntityData.get(entityId).getRight();
        }

        return null;
    }

    private @Nullable MerchantOffers getTrades(Level world, Villager villager)
    {
        if (world == null || villager == null)
        {
            return null;
        }

        Pair<Entity, CompoundData> pair = this.getVillagerData(world, villager.getId());
        MerchantOffers list = null;

        if (pair != null)
        {
            if (pair.getRight() != null && !pair.getRight().isEmpty())
            {
                list = DataEntityUtils.getTradeOffers(pair.getRight(), world.registryAccess());
            }
            else if (pair.getLeft() != null && pair.getLeft() instanceof Villager entity)
            {
                list = ((IMixinMerchantEntity) entity).malilib_offers();
            }
        }

        return list;
    }

    private int getConversionTime(Level world, ZombieVillager villager)
    {
        if (world == null || villager == null)
        {
            return -1;
        }

        Pair<Entity, CompoundData> pair = this.getVillagerData(world, villager.getId());
        int conversionTime = -1;

        if (pair != null)
        {
            if (pair.getRight() != null && !pair.getRight().isEmpty())
            {
                Pair<Integer, UUID> zombiePair = DataEntityUtils.getZombieConversionTimer(pair.getRight());

                if (zombiePair != null && zombiePair.getLeft() > -1)
                {
                    conversionTime = zombiePair.getLeft();
                }
            }
            else if (pair.getLeft() != null && pair.getLeft() instanceof ZombieVillager zombert)
            {
                conversionTime = ((IMixinZombieVillager) zombert).minihud_conversionTimer();
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
    public boolean shouldRender(Minecraft mc)
    {
        return RendererToggle.OVERLAY_VILLAGER_INFO.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity camera, Minecraft mc)
    {
        return true;
    }

    @Override
    public void update(Vec3d cameraPos, Entity camera, Minecraft mc, ProfilerFiller profiler)
    {
        AABB box = camera.getBoundingBox().inflate(this.xViewRange, this.yViewRange, this.zViewRange);
        Level world = WorldUtils.getBestWorld(mc);

        if (world == null) return;

        if (Configs.Generic.VILLAGER_OFFER_ENCHANTMENT_BOOKS.getBooleanValue())
        {
            List<Villager> librarians = EntityUtils.getEntitiesByClass(mc, Villager.class, box, villager -> villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN));
            Map<Object2IntMap.Entry<Holder<Enchantment>>, Integer> lowestPrices = new HashMap<>();

            // Prepare
            if (Configs.Generic.VILLAGER_OFFER_LOWEST_PRICE_NEARBY.getBooleanValue())
            {
                for (Villager librarian : librarians)
                {
                    MerchantOffers offers = this.getTrades(world, librarian);

                    if (offers == null || offers.isEmpty())
                    {
                        continue;
                    }

                    for (MerchantOffer tradeOffer : offers)
                    {
                        if (tradeOffer.getResult().getItem() == Items.ENCHANTED_BOOK && tradeOffer.getItemCostA().item().value() == Items.EMERALD)
                        {
                            for (Object2IntMap.Entry<Holder<Enchantment>> entry : tradeOffer.getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet())
                            {
                                int emeraldCost = tradeOffer.getItemCostA().count();

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
            for (Villager librarian : librarians)
            {
                MerchantOffers offers = this.getTrades(world, librarian);

                if (offers == null || offers.isEmpty())
                {
                    continue;
                }

                List<String> overlay = new ArrayList<>();

                for (MerchantOffer tradeOffer : offers)
                {
                    if (tradeOffer.getResult().getItem() == Items.ENCHANTED_BOOK)
                    {
                        for (Object2IntMap.Entry<Holder<Enchantment>> entry : tradeOffer.getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet())
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

                            sb.append(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
                            sb.append(GuiBase.TXT_RST);

                            if (tradeOffer.getItemCostA().item().value() == Items.EMERALD)
                            {
                                sb.append(" ");
                                int emeraldCost = tradeOffer.getItemCostA().count();

                                if (Configs.Generic.VILLAGER_OFFER_LOWEST_PRICE_NEARBY.getBooleanValue())
                                {
                                    if (emeraldCost > lowestPrices.getOrDefault(entry, Integer.MAX_VALUE))
                                    {
                                        continue;
                                    }
                                }

                                int lowest = 2 + 3 * entry.getIntValue();
                                int highest = 6 + 13 * entry.getIntValue();

                                if (entry.getKey().is(EnchantmentTags.DOUBLE_TRADE_PRICE))
                                {
                                    lowest *= 2;
                                    highest *= 2;
                                }
                                if (emeraldCost > Mth.lerp(Configs.Generic.VILLAGER_OFFER_PRICE_THRESHOLD.getDoubleValue(), lowest, highest))
                                {
                                    continue;
                                }
                                if (emeraldCost < Mth.lerp(1.0 / 3, lowest, highest))
                                {
                                    sb.append(GuiBase.TXT_GREEN);
                                }
                                if (emeraldCost > Mth.lerp(2.0 / 3, lowest, highest))
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

                this.extractTarget(overlay, librarian);
            }
        }

        if (Configs.Generic.VILLAGER_CONVERSION_TICKS.getBooleanValue())
        {
            List<ZombieVillager> zombieVillagers = EntityUtils.getEntitiesByClass(mc, ZombieVillager.class, box, e -> true);

            for (ZombieVillager villager : zombieVillagers)
            {
                int conversionTimer = this.getConversionTime(world, villager);

                if (conversionTimer > 0)
                {
                    this.extractTarget(List.of(String.format("%ds", Math.round((float) conversionTimer / 20))), villager);
                }
            }
        }
    }

    @Override
    public boolean hasData()
    {
        return !this.villagerData.isEmpty();
    }

    @Override
    public void render(Vec3d cameraPos, Minecraft mc, ProfilerFiller profiler)
    {
        // NO-OP
    }

    @Override
    public void draw(Vec3d cameraPos)
    {
        if (this.hasData())
        {
            Minecraft mc = Minecraft.getInstance();

            this.villagerData.forEach(
                    (target, text) ->
                            this.renderAtEntity(text, target, cameraPos, mc)
            );

            this.villagerData.clear();
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        this.villagerData.clear();
        this.lastPos.clear();
    }

    private void extractTarget(List<String> texts, Entity target)
    {
        this.villagerData.put(target, texts);
    }

    private void renderAtEntity(List<String> texts, Entity targetEntity, Vec3d cameraPos, Minecraft mc)
    {
//        if (cam == null) return;
        float delta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
//        Vec3 cameraPos = cam.getPosition(delta);
        Vec3 targetPos = targetEntity.getPosition(delta);
        double hypot = Mth.length(cameraPos.x - targetPos.x(), cameraPos.z - targetPos.z());
        double distance = 0.8;
        double x = targetPos.x() + (cameraPos.x - targetPos.x()) / hypot * distance;
        double z = targetPos.z() + (cameraPos.z - targetPos.z()) / hypot * distance;
        double y = targetPos.y() + 1.5 + 0.1 * texts.size();
        final float scale = Configs.Generic.VILLAGER_TEXT_SCALE.getFloatValue() * 0.01F;
        boolean atJobSite = false;

        // Render the overlay at its job site, this is useful in trading halls
        if (targetEntity instanceof LivingEntity living)
        {
            Optional<GlobalPos> jobSite = living.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE);

            if (jobSite != null && jobSite.isPresent())
            {
                BlockPos pos = jobSite.get().pos();
                Vec3 center = Vec3.atCenterOf(pos);

                if (targetPos.distanceTo(center) < 1.7)
                {
                    x = pos.getX() + 0.5;
                    z = pos.getZ() + 0.5;
                    atJobSite = true;
                }
            }
        }

        Vec3d attachPos = new Vec3d(x, y, z);
        UUID uuid = targetEntity.getUUID();
        LastTextPlate lastEntry = this.lastPos.remove(uuid);
        double diff;

        if (lastEntry != null && !lastEntry.jobSite())
        {
            diff = lastEntry.pos().getDistanceTo(attachPos);

            if (diff > 0)
            {
                Vec3d lerpPos = MathUtils.lerp(diff, lastEntry.pos(), attachPos);

                RenderUtils.scheduleTextPlate(texts, lerpPos, scale, this.renderThrough, TextAlignment.CENTER);
                this.lastPos.put(uuid, new LastTextPlate(lerpPos, atJobSite, System.currentTimeMillis()));

                return;
            }
        }

        RenderUtils.scheduleTextPlate(texts, attachPos, scale, this.renderThrough, TextAlignment.CENTER);
        this.lastPos.put(uuid, new LastTextPlate(attachPos, atJobSite, System.currentTimeMillis()));

//        for (String line : texts)
//        {
//            // Replace camera entity each call
//            Entity cam = mc.getCameraEntity();
//
//            if (cam != null)
//            {
//                // Get the lerp of Yaw / Pitch
////                RenderUtils.drawTextPlate(List.of(line), x, y, z, 0.02f);
////                RenderUtils.drawTextPlate(List.of(line), x, y, z, cam.getYRot(delta), cam.getXRot(delta), scale, 0xFFFFFFFF, 0x40000000, this.renderThrough);
//                RenderUtils.drawTextPlate(List.of(line), x, y, z, cam.getYRot(), cam.getXRot(), scale, TextAlignment.CENTER, 0xFFFFFFFF, 0x40000000, this.renderThrough);
//                y -= 0.2;
//            }
//        }
    }

    public record LastTextPlate(Vec3d pos, boolean jobSite, long tick) {}
}
