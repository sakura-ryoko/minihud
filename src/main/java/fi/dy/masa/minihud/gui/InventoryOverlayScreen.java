package fi.dy.masa.minihud.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.CrafterBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.EntitiesDataManager;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.util.RayTraceUtils;

public class InventoryOverlayScreen extends Screen implements Drawable
{
    private InventoryOverlay.Context previewData;
    private int ticks;

    public InventoryOverlayScreen(InventoryOverlay.Context previewData)
    {
        super(Text.literal("Inventory Overlay"));
        this.previewData = previewData;
    }

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
                if (previewData.inv() == null)
                {
                    MiniHUD.logger.error("InventoryOverlayScreen(): Horse inv() = null");
                    return;
                }
                armourItems.add(previewData.entity().getEquippedStack(EquipmentSlot.BODY));
                armourItems.add(previewData.inv().getStack(0));
                startSlot = 1;
                totalSlots = previewData.inv().size() - 1;
            }
            else if (previewData.entity() instanceof WolfEntity)
            {
                armourItems.add(previewData.entity().getEquippedStack(EquipmentSlot.BODY));
                //armourItems.add(ItemStack.EMPTY);
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
            if (previewData.be() instanceof CrafterBlockEntity cbe)
            {
                lockedSlots = BlockUtils.getDisabledSlots(cbe);
            }
            else if (previewData.nbt() != null && previewData.nbt().contains(NbtKeys.DISABLED_SLOTS))
            {
                lockedSlots = BlockUtils.getDisabledSlotsFromNbt(previewData.nbt());
            }

            if (!armourItems.isEmpty())
            {
                Inventory horseInv = new SimpleInventory(armourItems.toArray(new ItemStack[0]));
                InventoryOverlay.renderInventoryBackground(type, xInv, yInv, 1, horseInv.size(), mc);
                InventoryOverlay.renderInventoryBackgroundSlots(type, horseInv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, drawContext);
                InventoryOverlay.renderInventoryStacks(type, horseInv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, horseInv.size(), mc, drawContext, mouseX, mouseY);
                xInv += 32 + 4;
            }

            if (previewData.be() != null && previewData.be().getCachedState().getBlock() instanceof ShulkerBoxBlock sbb)
            {
                RenderUtils.setShulkerboxBackgroundTintColor(sbb, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }

            // Inv Display
            if (totalSlots > 0 && previewData.inv() != null)
            {
                InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, totalSlots, mc);

                if (type == InventoryOverlay.InventoryRenderType.BREWING_STAND)
                {
                    InventoryOverlay.renderBrewerBackgroundSlots(previewData.inv(), xInv, yInv, drawContext);
                }

                //dumpInvStacks(previewData.inv(), world);
                InventoryOverlay.renderInventoryStacks(type, previewData.inv(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, startSlot, totalSlots, lockedSlots, mc, drawContext, mouseX, mouseY);
            }

            // EnderItems Display
            if (previewData.type() == InventoryOverlay.InventoryRenderType.PLAYER &&
                previewData.nbt() != null && previewData.nbt().contains(NbtKeys.ENDER_ITEMS))
            {
                EnderChestInventory enderItems = InventoryUtils.getPlayerEnderItemsFromNbt(previewData.nbt(), world.getRegistryManager());

                if (enderItems == null)
                {
                    enderItems = new EnderChestInventory();
                }

                //dumpInvStacks(enderItems, world);
                yInv = yCenter + 6;
                InventoryOverlay.renderInventoryBackground(InventoryOverlay.InventoryRenderType.GENERIC, xInv, yInv, 9, 27, mc);
                InventoryOverlay.renderInventoryStacks(InventoryOverlay.InventoryRenderType.GENERIC, enderItems, xInv + props.slotOffsetX, yInv + props.slotOffsetY, 9, 0, 27, mc, drawContext, mouseX, mouseY);
            }
            else if (previewData.entity() instanceof PlayerEntity player)
            {
                yInv = yCenter + 6;
                InventoryOverlay.renderInventoryBackground(InventoryOverlay.InventoryRenderType.GENERIC, xInv, yInv, 9, 27, mc);
                InventoryOverlay.renderInventoryStacks(InventoryOverlay.InventoryRenderType.GENERIC, player.getEnderChestInventory(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, 9, 0, 27, mc, drawContext, mouseX, mouseY);
            }

            // Entity Display
            if (previewData.entity() != null)
            {
                InventoryOverlay.renderEquipmentOverlayBackground(x, y, previewData.entity(), drawContext);
                InventoryOverlay.renderEquipmentStacks(previewData.entity(), x, y, mc, drawContext, mouseX, mouseY);
            }

            // Refresh
            if (ticks % 4 == 0)
            {
                // Refresh data
                if (previewData.be() != null)
                {
                    RenderHandler.getInstance().requestBlockEntityAt(world, previewData.be().getPos());
                    previewData = RayTraceUtils.getTargetInventoryFromBlock(previewData.be().getWorld(), previewData.be().getPos(), previewData.be(), previewData.nbt());
                }
                else if (previewData.entity() != null)
                {
                    EntitiesDataManager.getInstance().requestEntity(previewData.entity().getId());
                    previewData = RayTraceUtils.getTargetInventoryFromEntity(previewData.entity(), previewData.nbt());
                }
            }
        }
    }

    @Override
    public boolean shouldPause()
    {
        return false;
    }

    public static void dumpInvStacks(Inventory inv, World world)
    {
        System.out.print("dumpInvStacks() -->\n");

        if (inv == null || inv.size() <= 0 || world == null)
        {
            System.out.print("EMPTY / NULL\n");
            return;
        }

        for (int i = 0; i < inv.size(); i++)
        {
            ItemStack stack = inv.getStack(i);

            if (stack.isEmpty())
            {
                System.out.printf("slot[%d]: [%s]\n", i, "minecraft:air [EMPTY]");
            }
            else
            {
                System.out.printf("slot[%d]: [%s]\n", i, inv.getStack(i).toNbt(world.getRegistryManager()));
            }
        }

        System.out.print("END\n");
    }
}
