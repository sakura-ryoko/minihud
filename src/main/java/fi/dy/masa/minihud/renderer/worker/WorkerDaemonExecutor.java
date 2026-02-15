package fi.dy.masa.minihud.renderer.worker;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IThreadDaemonExecutor;
import fi.dy.masa.minihud.MiniHUD;

public class WorkerDaemonExecutor implements IThreadDaemonExecutor<AbstractWorkerTask<?>>
{
	private final AtomicBoolean running = new AtomicBoolean(true);

	@Override
	public boolean isRunning()
	{
		return this.running.get();
	}

	@Override
	public void start()
	{
		this.running.set(true);
	}

	@Override
	public void stop()
	{
		this.running.set(false);
	}

	@Override
	public void run()
	{
		while (this.isRunning())
		{
			try
			{
				AbstractWorkerTask<?> task = WorkerDaemonHandler.INSTANCE.getNextTask();

				if (task != null)
				{
					this.processTask(task);
				}
			}
			catch (InterruptedException e)
			{
				MiniHUD.LOGGER.debug("Stopping worker thread due to an interrupt");
				return;
			}
			catch (Throwable throwable)
			{
				CrashReport crashreport = CrashReport.forThrowable(throwable, "MiniHUD worker thread");
				Minecraft.getInstance().delayCrashRaw(Minecraft.getInstance().fillReport(crashreport));
				return;
			}
		}
	}

	@Override
	public void processTask(AbstractWorkerTask<?> task) throws InterruptedException
	{
		task.run();
	}
}
