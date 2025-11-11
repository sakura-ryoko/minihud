package fi.dy.masa.minihud.mixin.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.renderer.OverlayRendererConduitRange;
import fi.dy.masa.minihud.util.ConduitExtra;

@Mixin(ConduitBlockEntity.class)
public abstract class MixinConduitBlockEntity implements ConduitExtra
{
    @Shadow @Final private List<BlockPos> effectBlocks;
    @Unique private int minihud_activatingBlockCount;

    @Override
    public int minihud$getCurrentActivatingBlockCount()
    {
        return this.effectBlocks.size();
    }

    @Override
    public int minihud$getStoredActivatingBlockCount()
    {
        return this.minihud_activatingBlockCount;
    }

    @Override
    public void minihud$setActivatingBlockCount(int count)
    {
        this.minihud_activatingBlockCount = count;
    }

    @Inject(method = "clientTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;updateHunting(Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;Ljava/util/List;)V"))
    private static void minihud_postActiveBlockScan(Level world, BlockPos pos, BlockState state,
                                                    ConduitBlockEntity blockEntity, CallbackInfo ci)
    {
        if (RendererToggle.OVERLAY_CONDUIT_RANGE.getBooleanValue())
        {
            int count = ((ConduitExtra) blockEntity).minihud$getCurrentActivatingBlockCount();
            int countBefore = ((ConduitExtra) blockEntity).minihud$getStoredActivatingBlockCount();

            if (count != countBefore)
            {
                OverlayRendererConduitRange.INSTANCE.onBlockStatusChange(pos);
                ((ConduitExtra) blockEntity).minihud$setActivatingBlockCount(count);
            }
        }
    }
}
