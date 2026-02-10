package fi.dy.masa.minihud.info;

import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.util.Util;

import fi.dy.masa.minihud.mixin.render.IMixinGlTimer;

/**
 * This just causes issues; so it's deprecated.
 */
@Deprecated
public class InfoLineProfiler
{
	public static final InfoLineProfiler INSTANCE = new InfoLineProfiler();
	@Nullable
	private TimerQuery.FrameProfile glQuery;
	private long metricsDuration;
	private long lastMetricsTime;
	private double gpuUtilization;
	private boolean measurementEnable = false;

	private InfoLineProfiler() {}

	@ApiStatus.Internal
	private boolean shouldGPUProfilerStop()
	{
//		return Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.GPU_UTILIZATION) || !InfoToggle.GPU.getBooleanValue();
		return true;
	}

	private boolean isGPUQuerySafe()
	{
		return ((IMixinGlTimer) TimerQuery.getInstance()).minihud_getQuery() != null &&
				((IMixinGlTimer) TimerQuery.getInstance()).minihud_getCommandEncoder() != null;
	}

	@ApiStatus.Internal
	public void GPUStage1()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		if ((this.glQuery == null || this.glQuery.isDone()) && !this.isGPUQuerySafe())
		{
			this.measurementEnable = true;
			TimerQuery.getInstance().beginProfile();
		}
		else
		{
			this.stopGPUProfiler();
		}
	}

	@ApiStatus.Internal
	public void GPUStage2()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		if (this.measurementEnable && this.isGPUQuerySafe())
		{
			TimerQuery.getInstance().endProfile();
		}
		else
		{
			this.stopGPUProfiler();
		}
	}

	@ApiStatus.Internal
	public void GPUStage3()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		final long nanoTime = Util.getNanos();

		if (this.measurementEnable)
		{
			this.metricsDuration = nanoTime - this.lastMetricsTime;
		}

		this.lastMetricsTime = nanoTime;
	}

	@ApiStatus.Internal
	public void GPUStage4()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		if (this.measurementEnable)
		{
			if (this.glQuery != null && this.glQuery.isDone())
			{
				this.gpuUtilization = this.glQuery.get() * 100.0 / this.metricsDuration;
			}
		}
	}

	@ApiStatus.Internal
	private void stopGPUProfiler()
	{
		if (this.glQuery != null)
		{
			this.glQuery.cancel();
			this.glQuery = null;
		}

		this.gpuUtilization = 0.0;
		this.metricsDuration = 0L;
		this.lastMetricsTime = 0L;
		this.measurementEnable = false;
	}

	public double getGpuUtilization()
	{
		return this.gpuUtilization;
	}
}
