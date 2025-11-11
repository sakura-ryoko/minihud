package fi.dy.masa.minihud.mixin.debug;

import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Path.class)
public class MixinPath
{
//    @Shadow @Final private List<PathNode> nodes;
//    @Shadow @Nullable private Path.DebugNodeInfo debugNodeInfos;
//    @Shadow @Final private BlockPos target;
//
//    @Inject(method = "toBuf", at = @At("HEAD"))
//    private void minihud_PathfindingFix(PacketByteBuf buf, CallbackInfo ci)
//    {
//        this.debugNodeInfos = new Path.DebugNodeInfo(this.nodes.stream().filter((pathNode) ->
//                              !pathNode.visited).toArray(PathNode[]::new), this.nodes.stream().filter((pathNode) ->
//                               pathNode.visited).toArray(PathNode[]::new), Set.of(new TargetPathNode(this.target.getX(), this.target.getY(), this.target.getZ())));
//    }
}
