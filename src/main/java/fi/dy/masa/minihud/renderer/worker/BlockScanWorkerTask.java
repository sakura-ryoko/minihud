package fi.dy.masa.minihud.renderer.worker;

import org.jspecify.annotations.NonNull;

import net.minecraft.core.Vec3i;

public class BlockScanWorkerTask extends AbstractWorkerTask<BlockScanWorkerTask>
{
	protected final Vec3i referencePosition;
	public final Runnable task;

	public BlockScanWorkerTask(Runnable task, Vec3i referencePosition)
	{
		this.task = task;
		this.referencePosition = referencePosition;
	}

	@Override
	public int compareTo(@NonNull BlockScanWorkerTask o)
	{
		return this.referencePosition.compareTo(o.referencePosition);
	}

	@Override
	public void run()
	{
		this.task.run();
	}
}
