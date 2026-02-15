package fi.dy.masa.minihud.renderer.worker;

import org.jspecify.annotations.NonNull;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

public class ChunkWorkerTask extends AbstractWorkerTask<ChunkWorkerTask>
{
	protected final ChunkPos pos;
	protected final Vec3i referencePosition;
	public final Runnable task;

	public ChunkWorkerTask(Runnable task, ChunkPos pos, Vec3i referencePosition)
	{
		this.task = task;
		this.pos = pos;
		this.referencePosition = referencePosition;
	}

	private double distanceSq(ChunkPos pos)
	{
		double dx = (double) (pos.x << 4) - this.referencePosition.getX();
		double dz = (double) (pos.z << 4) - this.referencePosition.getZ();

		return dx * dx + dz * dz;
	}

	@Override
	public int compareTo(@NonNull ChunkWorkerTask other)
	{
		double dist1 = this.distanceSq(this.pos);
		double dist2 = this.distanceSq(other.pos);

		if (dist1 == dist2)
		{
			return 0;
		}

		return dist1 < dist2 ? -1 : 1;
	}

	@Override
	public void run()
	{
		this.task.run();
	}
}
