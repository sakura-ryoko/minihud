package fi.dy.masa.minihud.mixin.block;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.minihud.renderer.OverlayRendererBeaconRange;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BeaconBlockEntity.class)
public abstract class MixinBeaconBlockEntity extends BlockEntity
{
    @Shadow int levels;
    @Unique private int levelPre = -1;

    private MixinBeaconBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Inject(method = "setRemoved", at = @At("RETURN"))
    private void minihud_onRemoved(CallbackInfo ci)
    {
        OverlayRendererBeaconRange.INSTANCE.onBlockStatusChange(this.getBlockPos());
    }

    @Inject(method = "tick",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/level/Level;getGameTime()J"))
    private static void minihud_onUpdateSegmentsPre(Level world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity, CallbackInfo ci)
    {
        if (((MixinBeaconBlockEntity) (Object) blockEntity).levelPre != -1)
        {
            if (((MixinBeaconBlockEntity) (Object) blockEntity).levelPre != ((MixinBeaconBlockEntity) (Object) blockEntity).levels)
            {
                ((MixinBeaconBlockEntity) (Object) blockEntity).levelPre = ((MixinBeaconBlockEntity) (Object) blockEntity).levels;
            }
        }
    }

    @Inject(method = "tick",
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
                     target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;levels:I", ordinal = 0))
    private static void minihud_onUpdateSegmentsPost(Level world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity, CallbackInfo ci)
    {
        int newLevel = ((MixinBeaconBlockEntity) (Object) blockEntity).levels;

        if (((MixinBeaconBlockEntity) (Object) blockEntity).levelPre != newLevel)
        {
            OverlayRendererBeaconRange.INSTANCE.onBlockStatusChange(pos);
            ((MixinBeaconBlockEntity) (Object) blockEntity).levelPre = newLevel;
        }
    }

    @Inject(method = "updateBase", at = @At("RETURN"))
    private static void minihud_onUpdateLevel(Level world, int x, int y, int z, CallbackInfoReturnable<Integer> cir)
    {
        BlockPos pos = new BlockPos(x, y, z);
        OverlayRendererBeaconRange.INSTANCE.onBlockStatusChange(pos);
    }

    @Inject(method = "playSound", at = @At("HEAD"))
    private static void minihud_onBeaconPlaySound(Level level, BlockPos blockPos, SoundEvent soundEvent, CallbackInfo ci)
    {
        if (SoundEvents.BEACON_DEACTIVATE.equals(soundEvent) || SoundEvents.BEACON_ACTIVATE.equals(soundEvent))
        {
            OverlayRendererBeaconRange.INSTANCE.onBlockStatusChange(blockPos);
        }
    }
}
