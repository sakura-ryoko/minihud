package fi.dy.masa.minihud.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.EntitiesDataStorage;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.util.RayTraceUtils;

public class InventoryOverlayScreen extends Screen
{
    private int ticks;

    //public InventoryOverlayScreen(RayTraceUtils.InventoryPreviewData previewData)
    public InventoryOverlayScreen(InventoryOverlay.Context previewData)
    {
        super(Text.literal("Inventory Overlay"));
        this.previewData = previewData;
    }

    //RayTraceUtils.InventoryPreviewData previewData;
    InventoryOverlay.Context previewData;

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta)
    {
        ticks++;
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = WorldUtils.getBestWorld(mc);

        if (previewData != null && world != null)
        {
            final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
            final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
            int x = xCenter - 52 / 2;
            int y = yCenter - 92;

            int startSlot = 0;
            int totalSlots = previewData.inv() == null ? 0 : previewData.inv().size();
            List<ItemStack> armourItems = new ArrayList<>();
            if (previewData.entity() instanceof AbstractHorseEntity)
            {
                armourItems.add(previewData.inv().getStack(0));
                armourItems.add(previewData.entity().getEquippedStack(EquipmentSlot.BODY));
                startSlot = 1;
                totalSlots = previewData.inv().size() - 1;
            }
            else if (previewData.entity() instanceof WolfEntity)
            {
                armourItems.add(previewData.entity().getEquippedStack(EquipmentSlot.BODY));
            }

            final InventoryOverlay.InventoryRenderType type = (previewData.entity() instanceof VillagerEntity) ? InventoryOverlay.InventoryRenderType.VILLAGER : InventoryOverlay.getBestInventoryType(previewData.inv(), previewData.nbt() != null ? previewData.nbt() : new NbtCompound(), previewData);
            final InventoryOverlay.InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(type, totalSlots);
            final int rows = (int) Math.ceil((double) totalSlots / props.slotsPerRow);
            Set<Integer> lockedSlots = new HashSet<>();
            int xInv = xCenter - (props.width / 2);
            int yInv = yCenter - props.height - 6;

            if (rows > 6)
            {
                yInv -= (rows - 6) * 18;
                y -= (rows - 6) * 18;
            }

            /*
            MiniHUD.logger.warn("render():0: type [{}], previewData.type [{}], previewData.inv [{}], previewData.be [{}], previewData.ent [{}], previewData.nbt [{}]", type.toString(), previewData.type().toString(),
                                 previewData.inv() != null, previewData.be() != null, previewData.entity() != null, previewData.nbt() != null ? previewData.nbt().getString("id") : null);
            MiniHUD.logger.error("0: -> inv.type [{}] // nbt.type [{}]", previewData.inv() != null ? InventoryOverlay.getInventoryType(previewData.inv()) : null, previewData.nbt() != null ? InventoryOverlay.getInventoryType(previewData.nbt()) : null);
             */

            if (previewData.entity() != null)
            {
                x = xCenter - 55;
                xInv = xCenter + 2;
                yInv = Math.min(yInv, yCenter - 92);
            }
            //if (previewData.te() instanceof CrafterBlockEntity cbe)
            if (previewData.be() instanceof CrafterBlockEntity cbe)
            {
                lockedSlots = BlockUtils.getDisabledSlots(cbe);
            }
            else if (previewData.nbt() != null && previewData.nbt().contains("disabled_slots"))
            {
                lockedSlots = BlockUtils.getDisabledSlotsFromNbt(previewData.nbt());
            }

            if (!armourItems.isEmpty())
            {
                InventoryOverlay.renderInventoryBackground(type, xInv, yInv, 1, armourItems.size(), mc);
                InventoryOverlay.renderInventoryStacks(type, new SimpleInventory(armourItems.toArray(new ItemStack[0])), xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, armourItems.size(), mc, drawContext, mouseX, mouseY);
                xInv += 32 + 4;
            }

            //if (previewData.te() != null && previewData.te().getCachedState().getBlock() instanceof ShulkerBoxBlock sbb)
            if (previewData.be() != null && previewData.be().getCachedState().getBlock() instanceof ShulkerBoxBlock sbb)
            {
                RenderUtils.setShulkerboxBackgroundTintColor(sbb, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }

            if (totalSlots > 0 && previewData.inv() != null)
            {
                InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, totalSlots, mc);
                InventoryOverlay.renderInventoryStacks(type, previewData.inv(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, startSlot, totalSlots, lockedSlots, mc, drawContext, mouseX, mouseY);
            }

            if (previewData.entity() instanceof PlayerEntity player)
            {
                yInv = yCenter + 6;
                InventoryOverlay.renderInventoryBackground(InventoryOverlay.InventoryRenderType.GENERIC, xInv, yInv, 9, 27, mc);
                InventoryOverlay.renderInventoryStacks(InventoryOverlay.InventoryRenderType.GENERIC, player.getEnderChestInventory(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, 9, 0, 27, mc, drawContext, mouseX, mouseY);
            }

            if (previewData.entity() != null)
            {
                InventoryOverlay.renderEquipmentOverlayBackground(x, y, previewData.entity(), drawContext);
                InventoryOverlay.renderEquipmentStacks(previewData.entity(), x, y, mc, drawContext, mouseX, mouseY);
            }

            if (ticks % 4 == 0)
            {
                // Refresh data
                /*
                MiniHUD.logger.warn("render():1: type [{}], previewData.type [{}], previewData.inv [{}], previewData.be [{}], previewData.ent [{}], previewData.nbt [{}]",
                                    type.toString(), previewData.type().toString(),
                                    previewData.inv() != null ? previewData.inv().size() : "",
                                    previewData.be() != null, previewData.entity() != null,
                                    previewData.nbt() != null ? previewData.nbt().getString("id") : null);
                MiniHUD.logger.error("1: -> inv.type [{}] // nbt.type [{}]", previewData.inv() != null ? InventoryOverlay.getInventoryType(previewData.inv()) : null, previewData.nbt() != null ? InventoryOverlay.getInventoryType(previewData.nbt()) : null);

                if (previewData.nbt() != null && previewData.nbt().contains("disabled_slots"))
                {
                    MiniHUD.logger.warn("1: -> disabled_slots [{}]", previewData.nbt().get("disabled_slots"));
                }
                 */

                if (previewData.be() != null)
                {
                    RenderHandler.getInstance().requestBlockEntityAt(world, previewData.be().getPos());
                    previewData = RayTraceUtils.getTargetInventoryFromBlock(previewData.be().getWorld(), previewData.be().getPos(), previewData.be(), previewData.nbt());
                }
                else if (previewData.entity() != null)
                {
                    EntitiesDataStorage.getInstance().requestEntity(previewData.entity().getId());
                    previewData = RayTraceUtils.getTargetInventoryFromEntity(previewData.entity(), previewData.nbt());
                }

                /*
                if (previewData.te() != null)
                {
                    RenderHandler.getInstance().requestBlockEntityAt(world, previewData.te().getPos());
                    var inv = InventoryUtils.getInventory(world, previewData.te().getPos());
                    previewData = new RayTraceUtils.InventoryPreviewData(inv, world.getBlockEntity(previewData.te().getPos()), null);
                 */
                /*
                if (previewData.be() != null)
                {
                    Inventory lastInv = previewData.inv();
                    BlockEntity lastBe = previewData.be();
                    NbtCompound lastNbt = previewData.nbt();
                    Inventory inv;
                    BlockEntity be = world.getBlockEntity(previewData.be().getPos());
                    NbtCompound nbt = new NbtCompound();

                    if (be != null)
                    {

                    }

                    MiniHUD.logger.warn("render():1: type [{}], previewData.type [{}], previewData.inv [{}], previewData.be [{}], previewData.ent [{}], previewData.nbt [{}]", type.toString(), previewData.type().toString(),
                                        previewData.inv() != null, previewData.be() != null, previewData.entity() != null, previewData.nbt() != null ? previewData.nbt().getString("id") : null);
                    MiniHUD.logger.error("1: -> inv.type [{}] // nbt.type [{}]", previewData.inv() != null ? InventoryOverlay.getInventoryType(previewData.inv()) : null, previewData.nbt() != null ? InventoryOverlay.getInventoryType(previewData.nbt()) : null);

                    if (previewData.nbt() != null && previewData.nbt().contains("disabled_slots"))
                    {
                        MiniHUD.logger.warn("1: -> disabled_slots [{}]", previewData.nbt().get("disabled_slots"));
                    }

                    nbt = RenderHandler.getInstance().requestBlockEntityAt(world, previewData.be().getPos());
                    inv = InventoryUtils.getInventory(world, previewData.be().getPos());

                    if (inv == null)
                    {
                        inv = InventoryUtils.getNbtInventory(previewData.nbt(), previewData.inv() != null ? previewData.inv().size() : -1, world.getRegistryManager());
                    }
                    //world.getBlockEntity(previewData.be().getPos())
                    previewData = new InventoryOverlay.Context(InventoryOverlay.getBestInventoryType(inv, previewData.nbt(), previewData), inv, world.getBlockEntity(previewData.be().getPos()), null, previewData.nbt());
                }
                else if (previewData.entity() != null)
                {
                    MiniHUD.logger.warn("render():2: type [{}], previewData.type [{}], previewData.inv [{}], previewData.be [{}], previewData.ent [{}], previewData.nbt [{}]", type.toString(), previewData.type().toString(),
                                        previewData.inv() != null, previewData.be() != null, previewData.entity() != null, previewData.nbt() != null ? previewData.nbt().getString("id") : null);
                    MiniHUD.logger.error("2: -> inv.type [{}] // nbt.type [{}]", previewData.inv() != null ? InventoryOverlay.getInventoryType(previewData.inv()) : null, previewData.nbt() != null ? InventoryOverlay.getInventoryType(previewData.nbt()) : null);

                    if (previewData.nbt() != null && previewData.nbt().contains("disabled_slots"))
                    {
                        MiniHUD.logger.warn("2: -> disabled_slots [{}]", previewData.nbt().get("disabled_slots"));
                    }
                    EntitiesDataStorage.getInstance().requestEntity(previewData.entity().getId());
                    previewData = RayTraceUtils.getTargetInventoryFromEntity(previewData.entity(), previewData.nbt());
                }
                else if (previewData.nbt() != null)
                {
                    MiniHUD.logger.warn("render():3: type [{}], previewData.type [{}], previewData.inv [{}], previewData.be [{}], previewData.ent [{}], previewData.nbt [{}]", type.toString(), previewData.type().toString(),
                                        previewData.inv() != null, previewData.be() != null, previewData.entity() != null, previewData.nbt() != null ? previewData.nbt().getString("id") : null);
                    MiniHUD.logger.error("3: -> inv.type [{}] // nbt.type [{}]", previewData.inv() != null ? InventoryOverlay.getInventoryType(previewData.inv()) : null, previewData.nbt() != null ? InventoryOverlay.getInventoryType(previewData.nbt()) : null);

                    NbtCompound nbt = previewData.nbt().copy();

                    if (nbt.contains("disabled_slots"))
                    {
                        MiniHUD.logger.warn("3: -> disabled_slots [{}]", nbt.get("disabled_slots"));
                    }
                    var inv = InventoryUtils.getNbtInventory(nbt, previewData.inv() != null ? previewData.inv().size() : -1, world.getRegistryManager());
                    previewData = new InventoryOverlay.Context(InventoryOverlay.getBestInventoryType(inv != null ? inv : previewData.inv(), nbt, previewData),
                                                               inv != null ? inv : previewData.inv(), previewData.be() != null ? previewData.be() : null,
                                                               previewData.entity() != null ? previewData.entity() : null, nbt);
                }
                 */
            }
        }
    }

    @Override
    public boolean shouldPause()
    {
        return false;
    }
}
