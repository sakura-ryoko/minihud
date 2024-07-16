package fi.dy.masa.minihud.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.ShulkerBoxBlock;
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
import net.minecraft.text.Text;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.EntitiesDataStorage;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.util.RayTraceUtils;

public class InventoryOverlayScreen extends Screen
{
    private int ticks;

    public InventoryOverlayScreen(RayTraceUtils.InventoryPreviewData previewData)
    {
        super(Text.literal("Inventory Overlay"));
        this.previewData = previewData;
    }

    RayTraceUtils.InventoryPreviewData previewData;

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta)
    {
        ticks++;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (previewData != null && mc.world != null)
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

            final InventoryOverlay.InventoryRenderType type = (previewData.entity() instanceof VillagerEntity) ? InventoryOverlay.InventoryRenderType.VILLAGER : InventoryOverlay.getInventoryType(previewData.inv());
            final InventoryOverlay.InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(type, totalSlots);
            final int rows = (int) Math.ceil((double) totalSlots / props.slotsPerRow);
            int xInv = xCenter - (props.width / 2);
            int yInv = yCenter - props.height - 6;

            if (rows > 6)
            {
                yInv -= (rows - 6) * 18;
                y -= (rows - 6) * 18;
            }

            if (previewData.entity() != null)
            {
                x = xCenter - 55;
                xInv = xCenter + 2;
                yInv = Math.min(yInv, yCenter - 92);
            }

            if (!armourItems.isEmpty())
            {
                InventoryOverlay.renderInventoryBackground(type, xInv, yInv, 1, armourItems.size(), mc);
                InventoryOverlay.renderInventoryStacks(type, new SimpleInventory(armourItems.toArray(new ItemStack[0])), xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, armourItems.size(), mc, drawContext, mouseX, mouseY);
                xInv += 32 + 4;
            }

            if (previewData.te() != null && previewData.te().getCachedState().getBlock() instanceof ShulkerBoxBlock sbb)
            {
                RenderUtils.setShulkerboxBackgroundTintColor(sbb, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
            }

            if (totalSlots > 0 && previewData.inv() != null)
            {
                InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, totalSlots, mc);
                InventoryOverlay.renderInventoryStacks(type, previewData.inv(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, startSlot, totalSlots, mc, drawContext, mouseX, mouseY);
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
                if (previewData.te() != null)
                {
                    RenderHandler.getInstance().requestBlockEntityAt(mc.world, previewData.te().getPos());
                    var inv = InventoryUtils.getInventory(mc.world, previewData.te().getPos());
                    previewData = new RayTraceUtils.InventoryPreviewData(inv, mc.world.getBlockEntity(previewData.te().getPos()), null);
                }
                else if (previewData.entity() != null)
                {
                    EntitiesDataStorage.getInstance().requestEntity(previewData.entity().getId());
                    previewData = RayTraceUtils.getTargetInventoryFromEntity(previewData.entity());
                }
            }
        }
    }

    @Override
    public boolean shouldPause()
    {
        return false;
    }
}
