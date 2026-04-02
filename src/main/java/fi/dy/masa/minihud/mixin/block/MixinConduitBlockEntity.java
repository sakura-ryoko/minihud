package fi.dy.masa.minihud.mixin.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
public abstract class MixinConduitBlockEntity extends BlockEntity implements ConduitExtra
{
    @Shadow @Final private List<BlockPos> effectBlocks;
    @Shadow public abstract boolean isActive();

    @Unique private int minihud_activatingBlockCount;
    @Unique private boolean minihud_WasActive;

    public MixinConduitBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState)
    {
        super(type, worldPosition, blockState);
    }

    @Override
    public boolean minihud$getStoredActiveStatus()
    {
        return this.minihud_WasActive;
    }

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

    @Override
    public void minihud$setWasActive(boolean wasActive)
    {
        this.minihud_WasActive = wasActive;
    }

    @Inject(method = "clientTick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;updateHunting(Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;Ljava/util/List;)V"))
    private static void minihud_postActiveBlockScan(Level world, BlockPos pos, BlockState state,
                                                    ConduitBlockEntity blockEntity, CallbackInfo ci)
    {
        if (RendererToggle.OVERLAY_CONDUIT_RANGE.getBooleanValue())
        {
            final int count = ((ConduitExtra) blockEntity).minihud$getCurrentActivatingBlockCount();
            final int countBefore = ((ConduitExtra) blockEntity).minihud$getStoredActivatingBlockCount();
            final boolean isActive = blockEntity.isActive();
            final boolean wasActive = ((ConduitExtra) blockEntity).minihud$getStoredActiveStatus();

//            if (wasActive != isActive)
//            {
//                System.out.printf("isActive: %s, wasActive: %s\n", isActive, wasActive);
//            }
//            if (countBefore != count)
//            {
//                System.out.printf("count: %d, countBefore: %d\n", count, countBefore);
//            }

            if (isActive != wasActive || count != countBefore)
            {
                OverlayRendererConduitRange.INSTANCE.onBlockStatusChange(pos);
                ((ConduitExtra) blockEntity).minihud$setActivatingBlockCount(count);
                ((ConduitExtra) blockEntity).minihud$setWasActive(isActive);
            }
        }
    }
}
