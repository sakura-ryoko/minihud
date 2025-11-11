package fi.dy.masa.minihud.info;

import com.mojang.blaze3d.systems.TimerQuery;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.mixin.render.IGlTimer;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;

public class InfoLineProfiler
{
	public static final InfoLineProfiler INSTANCE = new InfoLineProfiler();
	@Nullable
	private TimerQuery.FrameProfile glQuery;
	private long metricsDuration;
	private long lastMetricsTime;
	private double gpuUtilization;
	private boolean measurementEnable = false;

	private InfoLineProfiler() { }

	@ApiStatus.Internal
	private boolean shouldGPUProfilerStop()
	{
		return Minecraft.getInstance().debugEntries.isCurrentlyEnabled(DebugScreenEntries.GPU_UTILIZATION) || !InfoToggle.GPU.getBooleanValue();
	}

	private int getGPUQueryId()
	{
        return ((IGlTimer) TimerQuery.getInstance()).minihud_getQueryId();
	}

	@ApiStatus.Internal
	public void GPUStage1()
	{
		if (this.shouldGPUProfilerStop())
		{
			this.stopGPUProfiler();
			return;
		}

		if ((this.glQuery == null || this.glQuery.isDone()) && this.getGPUQueryId() == 0)
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

		if (this.measurementEnable && this.getGPUQueryId() != 0)
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
