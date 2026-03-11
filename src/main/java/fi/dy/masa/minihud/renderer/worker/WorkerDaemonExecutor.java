package fi.dy.masa.minihud.renderer.worker;

import java.util.concurrent.atomic.AtomicBoolean;

import fi.dy.masa.malilib.interfaces.IThreadDaemonExecutor;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.minihud.MiniHUD;

public class WorkerDaemonExecutor implements IThreadDaemonExecutor<AbstractWorkerTask<?>>
{
	private final AtomicBoolean running = new AtomicBoolean(true);
	private final AtomicBoolean paused = new AtomicBoolean(false);
	private final long sleepTime;
	private final float sleepDelay;
	private long lastTaskTime;

	public WorkerDaemonExecutor()
	{
		this(600000L);  // 10 min
	}

	public WorkerDaemonExecutor(long sleepTime)
	{
		this.sleepTime = MathUtils.clamp(sleepTime, 60000L, Long.MAX_VALUE); // 1 min
		this.sleepDelay = 0.75F;     // <1-second sleep delay (Must be 1/2 tick rate)
	}

	@Override
	public boolean isRunning()
	{
		return this.running.get();
	}

	@Override
	public boolean isPaused()
	{
		return this.paused.get();
	}

	@Override
	public void start()
	{
		if (!this.isRunning())
		{
			MiniHUD.debugLog("Executor: Starting");
			if (this.isPaused())
			{
				this.paused.set(false);
			}

			this.running.set(true);
		}

		this.run();
	}

	@Override
	public void interrupt(InterruptedException interrupt)
	{
		MiniHUD.debugLog("Executor: Interrupt Signal: {}", interrupt.getLocalizedMessage() != null
		                                                   ? interrupt.getLocalizedMessage()  // This is null sometimes?
		                                                   : "received interrupt signal");
		if (this.isPaused() || !this.isRunning())
		{
			this.resume();
		}
	}

	@Override
	public void pause()
	{
		MiniHUD.debugLog("Executor: Pausing");
		this.paused.set(true);
	}

	@Override
	public void resume()
	{
		if (this.isPaused())
		{
			MiniHUD.debugLog("Executor: Resuming");
			this.paused.set(false);
		}

		this.start();
	}

	@Override
	public void stop()
	{
		MiniHUD.debugLog("Executor: Stopping");
		if (!this.isPaused())
		{
			this.paused.set(true);
		}
		if (this.isRunning())
		{
			this.running.set(false);
		}
	}

	@Override
	public long sleepTime()
	{
		return this.sleepTime;
	}

	@Override
	public String getName()
	{
		return WorkerDaemonHandler.INSTANCE.getName();
	}

	@Override
	public boolean hasTasks()
	{
		return WorkerDaemonHandler.INSTANCE.hasTasks();
	}

	@Override
	public void run()
	{
		if (!this.isCorrectThread()) { return; }
		this.lastTaskTime = System.currentTimeMillis();
		MiniHUD.debugLog("Executor: Running: [{}/{}]", this.isRunning(), this.isPaused());

		while (this.isRunning())
		{
			if (this.isPaused() && this.hasTasks())
			{
				this.resume();
			}
			else if (!this.isPaused() && this.loopSafe())
			{
				this.paused.set(true);
				this.sleep();
				return;
			}
		}
	}

	@Override
	public boolean loopSafe()
	{
		try
		{
			AbstractWorkerTask<?> task = this.takeNextTask();

			if (task != null)
			{
				this.processTask(task);
				this.lastTaskTime = System.currentTimeMillis();
				return false;
			}
		}
		catch (InterruptedException e)
		{
			this.interrupt(e);
		}
		catch (Exception err)
		{
			MiniHUD.LOGGER.error("loopSafe: Exception: {}", err.getLocalizedMessage());
		}

		return this.shouldPause();
	}

	@Override
	public boolean shouldPause()
	{
		if (this.hasTasks()) { return false; }
		return (System.currentTimeMillis() - this.lastTaskTime) > (this.sleepDelay * 1000L);
	}

	private AbstractWorkerTask<?> takeNextTask() throws InterruptedException
	{
		return WorkerDaemonHandler.INSTANCE.getNextTask();
	}

	@Override
	public void processTask(AbstractWorkerTask<?> task) throws InterruptedException
	{
		task.run();
	}
}
