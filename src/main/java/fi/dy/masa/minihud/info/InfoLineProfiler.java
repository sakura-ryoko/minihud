package fi.dy.masa.minihud.info;

// fixme -- doesn't seem to work right
@Deprecated
public class InfoLineProfiler
{
//	public static final InfoLineProfiler INSTANCE = new InfoLineProfiler();
//	@Nullable
//	private GlTimer.Query glQuery;
//	private long metricsDuration;
//	private long lastMetricsTime;
//	private double gpuUtilization;
//	private boolean measurementEnable = false;
//
//	private InfoLineProfiler() { }
//
//	@ApiStatus.Internal
//	private boolean shouldGPUProfilerStop()
//	{
////		return MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud() || !InfoToggle.GPU.getBooleanValue();
//		return true;
//	}
//
//	private int getGPUQueryId()
//	{
//		AtomicInteger result = new AtomicInteger(-1);
//
//        GlTimer.getInstance().ifPresent(inst -> result.set(((IGlTimer) inst).minihud_getQueryId()));
//
//		return result.get();
//	}
//
//	@ApiStatus.Internal
//	public void GPUStage1()
//	{
//		if (this.shouldGPUProfilerStop())
//		{
//			this.stopGPUProfiler();
//			return;
//		}
//
//		if ((this.glQuery == null || this.glQuery.isResultAvailable()) && this.getGPUQueryId() == 0)
//		{
//			this.measurementEnable = true;
//			GlTimer.getInstance().ifPresent(GlTimer::beginProfile);
//		}
//		else
//		{
//			this.stopGPUProfiler();
//		}
//	}
//
//	@ApiStatus.Internal
//	public void GPUStage2()
//	{
//		if (this.shouldGPUProfilerStop())
//		{
//			this.stopGPUProfiler();
//			return;
//		}
//
//		if (this.measurementEnable && this.getGPUQueryId() != 0)
//		{
//			GlTimer.getInstance().ifPresent(inst -> this.glQuery = inst.endProfile());
//		}
//		else
//		{
//			this.stopGPUProfiler();
//		}
//	}
//
//	@ApiStatus.Internal
//	public void GPUStage3()
//	{
//		if (this.shouldGPUProfilerStop())
//		{
//			this.stopGPUProfiler();
//			return;
//		}
//
//		final long nanoTime = Util.getMeasuringTimeNano();
//
//		if (this.measurementEnable)
//		{
//			this.metricsDuration = nanoTime - this.lastMetricsTime;
//		}
//
//		this.lastMetricsTime = nanoTime;
//	}
//
//	@ApiStatus.Internal
//	public void GPUStage4()
//	{
//		if (this.shouldGPUProfilerStop())
//		{
//			this.stopGPUProfiler();
//			return;
//		}
//
//		if (this.measurementEnable)
//		{
//			if (this.glQuery != null && this.glQuery.isResultAvailable())
//			{
//				this.gpuUtilization = this.glQuery.queryResult() * 100.0 / this.metricsDuration;
//			}
//
//			this.stopGPUProfiler();
//		}
//	}
//
//	@ApiStatus.Internal
//	public void stopGPUProfiler()
//	{
//		if (this.glQuery != null)
//		{
//			this.glQuery.close();
//			this.glQuery = null;
//		}
//
////		this.gpuUtilization = 0.0;
//		this.metricsDuration = 0L;
//		this.lastMetricsTime = 0L;
//		this.measurementEnable = false;
//	}
//
//	public double getGpuUtilization()
//	{
//		return this.gpuUtilization;
//	}
}
