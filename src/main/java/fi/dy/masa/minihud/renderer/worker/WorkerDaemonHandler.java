package fi.dy.masa.minihud.renderer.worker;

import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import com.google.common.collect.Queues;

import net.minecraft.client.Minecraft;

import fi.dy.masa.malilib.interfaces.IThreadDaemonHandler;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.thread.ThreadExecutorPair;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;

// New Thread Worker system utilizing the MaLiLib Interface.
public class WorkerDaemonHandler implements IThreadDaemonHandler<AbstractWorkerTask<?>>
{
	public static final WorkerDaemonHandler INSTANCE = new WorkerDaemonHandler();
	private static final int MAX_PLATFORM_THREADS = 1;          // The hard limit of usable Threads
	private static final float TASK_INTERVAL = 1.50F;           // The amount of time in between task check updates
	private boolean useVirtual = false;
	private final String namePrefix = Reference.MOD_NAME+" Worker Thread";
	private final int threadCount = this.calculateMaxThreads();
	private final ConcurrentHashMap<String, ThreadExecutorPair<AbstractWorkerTask<?>>> threadMap = this.builder();
	private final PriorityBlockingQueue<AbstractWorkerTask<?>> queue = Queues.newPriorityBlockingQueue();
	private long lastTick;
	private boolean forceStop = false;

	private int calculateMaxThreads()
	{
		final int result = this.getThreadCountSafe();
		if (result < 1) { this.useVirtual = true; }

		return MathUtils.clamp(result, 1, MAX_PLATFORM_THREADS);
	}

	private ConcurrentHashMap<String, ThreadExecutorPair<AbstractWorkerTask<?>>> builder()
	{
		ConcurrentHashMap<String, ThreadExecutorPair<AbstractWorkerTask<?>>> threads = new ConcurrentHashMap<>(this.threadCount, 0.9f, 1);

		for (int i = 0; i < this.threadCount; i++)
		{
			final String name = this.threadCount > 1 ? this.namePrefix+" "+ (i+1) : this.namePrefix;
			threads.put(name, this.threadFactory(name, this.useVirtual, new WorkerDaemonExecutor()));
		}

		return threads;
	}

	private WorkerDaemonHandler()
	{
		this.lastTick = System.currentTimeMillis();
	}

	@Override
	public String getName()
	{
		return this.namePrefix;
	}

	@Override
	public void start()
	{
		if (this.forceStop) { return; }
		// , this.getProfile().getDisplayName()
		MiniHUD.LOGGER.info("Starting [{}] Worker Daemon threads", this.threadMap.size());
		Set<String> keys = this.threadMap.keySet();

		for (String key : keys)
		{
			ThreadExecutorPair<AbstractWorkerTask<?>> pair = this.threadMap.get(key);

			try
			{
				this.safeStart(pair);
			}
			catch (ConcurrentModificationException cme)
			{
				// Busy
			}
			catch (IllegalStateException is)
			{
				// Terminated
				pair = this.threadFactory(key, this.useVirtual, new WorkerDaemonExecutor());
				pair.thread().start();

				synchronized (this.threadMap)
				{
					this.threadMap.replace(key, pair);
				}
			}
			catch (RuntimeException re)
			{
				// Already Running
			}
			catch (Exception ignored) {}
		}
	}

	@Override
	public void stop()
	{
		MiniHUD.LOGGER.info("Stopping [{}] Worker Daemon threads", this.threadMap.size());
		Set<String> keys = this.threadMap.keySet();

		for (String key : keys)
		{
			ThreadExecutorPair<AbstractWorkerTask<?>> pair = this.threadMap.get(key);

			try
			{
				this.safeStop(pair);
			}
			catch (ConcurrentModificationException cme)
			{
				// Busy
				MiniHUD.LOGGER.warn("Thread [{}] is currently busy, and shouldn't be stopped", key);
			}
			catch (IllegalStateException is)
			{
				// Terminated already
			}
			catch (IllegalThreadStateException is)
			{
				// Never started
			}
			catch (Exception ignored) {}
		}
	}

	@Override
	public synchronized void reset()
	{
		this.queue.clear();
	}

	@Override
	public synchronized void addTask(AbstractWorkerTask task)
	{
		if (this.queue.size() < 64000)
		{
			final boolean wasEmpty = this.queue.isEmpty();
			this.queue.offer(task);

			if (wasEmpty)
			{
				this.ensureThreadsAreAlive();
			}
		}
	}

	@Override
	public synchronized AbstractWorkerTask<?> getNextTask() throws InterruptedException
	{
		return this.queue.poll();
	}

	protected int getTaskCount()
	{
		return this.queue.size();
	}

	@Override
	public boolean hasTasks()
	{
		return !this.queue.isEmpty();
	}

	@Override
	public long getTaskInterval()
	{
		return MathUtils.floor(TASK_INTERVAL * 1000L);
	}

	@Override
	public void onClientTick(Minecraft mc)
	{
		if (this.forceStop) { return; }
		final long now = System.currentTimeMillis();

		if ((now - this.lastTick) > this.getTaskInterval())
		{
			if (mc.level != null)
			{
				this.ensureThreadsAreAlive();
			}

			this.lastTick = now;
		}
	}

	private void ensureThreadsAreAlive()
	{
		final int count = this.getTaskCount();

		if (count > 0)
		{
			MiniHUD.debugLog("WorkerDaemonHandler: {} tasks detected --> checking Thread states", count);
			Set<String> keySet = this.threadMap.keySet();

			for (String key : keySet)
			{
				ThreadExecutorPair<AbstractWorkerTask<?>> pair = this.threadMap.get(key);

				try
				{
					this.safeStart(pair);
				}
				catch (IllegalStateException is)
				{
					// Terminated (Replace)
					pair = this.threadFactory(key, this.useVirtual, new WorkerDaemonExecutor());
					pair.thread().start();

					synchronized (this.threadMap)
					{
						this.threadMap.replace(key, pair);
					}
				}
				catch (RuntimeException ignored) {}
			}
		}
	}

	public void resetForceStop()
	{
		this.forceStop = false;
	}

	public boolean isForceStop()
	{
		return this.forceStop;
	}

	@Override
	public void endAll()
	{
		this.forceStop = true;
		this.reset();
		this.stop();
	}

	@Override
	public void close() throws Exception
	{
		this.endAll();
	}
}
