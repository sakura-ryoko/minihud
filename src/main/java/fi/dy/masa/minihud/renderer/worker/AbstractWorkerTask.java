package fi.dy.masa.minihud.renderer.worker;

import java.util.concurrent.atomic.AtomicBoolean;

import fi.dy.masa.malilib.interfaces.IThreadTaskBase;

public abstract class AbstractWorkerTask<T> implements IThreadTaskBase, Comparable<T>
{
	private final AtomicBoolean finished = new AtomicBoolean(false);

	public AbstractWorkerTask() {}

	@Override
	public boolean isFinished()
	{
		return this.finished.get();
	}

	@Override
	public void finish()
	{
		this.finished.set(true);
	}
}
