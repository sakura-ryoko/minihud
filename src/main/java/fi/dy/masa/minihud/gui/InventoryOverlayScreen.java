package fi.dy.masa.minihud.gui;

import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.data.EntitiesDataStorage;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.util.RayTraceUtils;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class InventoryOverlayScreen extends Screen
{
    private int ticks;
    public InventoryOverlayScreen(RayTraceUtils.TargetInventory inventory)
    {
        super(Text.literal("Inventory Overlay"));
        this.inventory = inventory;
    }

    RayTraceUtils.TargetInventory inventory;

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta)
    {
        ticks++;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (inventory != null && mc.world != null)
        {
            final int xCenter = GuiUtils.getScaledWindowWidth() / 2;
            final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
            int x = xCenter - 52 / 2;
            int y = yCenter - 92;

            if (inventory.inv() != null && inventory.inv().size() > 0)
            {
                final boolean isHorse = (inventory.entity() instanceof AbstractHorseEntity);
                final int totalSlots = isHorse ? inventory.inv().size() - 2 : inventory.inv().size();
                final int firstSlot = isHorse ? 2 : 0;

                final InventoryOverlay.InventoryRenderType type = (inventory.entity() instanceof VillagerEntity) ? InventoryOverlay.InventoryRenderType.VILLAGER : InventoryOverlay.getInventoryType(inventory.inv());
                final InventoryOverlay.InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(type, totalSlots);
                final int rows = (int) Math.ceil((double) totalSlots / props.slotsPerRow);
                int xInv = xCenter - (props.width / 2);
                int yInv = yCenter - props.height - 6;

                if (rows > 6)
                {
                    yInv -= (rows - 6) * 18;
                    y -= (rows - 6) * 18;
                }

                if (inventory.entity() != null)
                {
                    x = xCenter - 55;
                    xInv = xCenter + 2;
                    yInv = Math.min(yInv, yCenter - 92);
                }

                if (inventory.te() != null && inventory.te().getCachedState().getBlock() instanceof ShulkerBoxBlock sbb)
                {
                    RenderUtils.setShulkerboxBackgroundTintColor(sbb, Configs.Generic.SHULKER_DISPLAY_BACKGROUND_COLOR.getBooleanValue());
                }

                if (isHorse)
                {
                    InventoryOverlay.renderInventoryBackground(type, xInv, yInv, 1, 2, mc);
                    InventoryOverlay.renderInventoryStacks(type, inventory.inv(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, 1, 0, 2, mc, drawContext, mouseX, mouseY);
                    xInv += 32 + 4;
                }

                if (totalSlots > 0)
                {
                    InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, totalSlots, mc);
                    InventoryOverlay.renderInventoryStacks(type, inventory.inv(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, firstSlot, totalSlots, mc, drawContext, mouseX, mouseY);
                }

                if (inventory.entity() instanceof PlayerEntity player)
                {
                    yInv = yCenter + 6;
                    InventoryOverlay.renderInventoryBackground(InventoryOverlay.InventoryRenderType.GENERIC, xInv, yInv, 9, 27, mc);
                    InventoryOverlay.renderInventoryStacks(InventoryOverlay.InventoryRenderType.GENERIC, player.getEnderChestInventory(), xInv + props.slotOffsetX, yInv + props.slotOffsetY, 9, 0, 27, mc, drawContext, mouseX, mouseY);
                }
            }

            if (inventory.entity() != null)
            {
                InventoryOverlay.renderEquipmentOverlayBackground(x, y, inventory.entity(), drawContext);
                InventoryOverlay.renderEquipmentStacks(inventory.entity(), x, y, mc, drawContext, mouseX, mouseY);
            }

            if (ticks % 4 == 0)
            {
                // Refresh data
                if (inventory.te() != null)
                {
                    RenderHandler.getInstance().requestBlockEntityAt(mc.world, inventory.te().getPos());
                    var inv = InventoryUtils.getInventory(mc.world, inventory.te().getPos());
                    inventory = new RayTraceUtils.TargetInventory(inv, mc.world.getBlockEntity(inventory.te().getPos()), null);
                }
                else if (inventory.entity() != null)
                {
                    EntitiesDataStorage.getInstance().requestEntity(inventory.entity().getId());
                    inventory = RayTraceUtils.getTargetInventoryFromEntity(inventory.entity());
                }
            }
        }
    }
}
