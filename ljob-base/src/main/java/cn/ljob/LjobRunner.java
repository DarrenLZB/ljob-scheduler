package cn.ljob;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import cn.ljob.support.LjobSupporter;

/**
 * Ljob Runner
 *
 * @author darren
 * @since 2018年3月30日 上午11:51:37
 */
public abstract class LjobRunner {
	private static final Logger LOG = LoggerFactory.getLogger(LjobRunner.class);

	private int standAloneLockSeconds = 3600;

	private ThreadPoolExecutor threadPoolExecutor = null;

	private LjobSupporter ljobSupporter = null;

	private boolean isWorking = false;

	private boolean isCustomWorking = false;

	private boolean isDistributed = false;

	private String groupName = null;

	private String jobName = null;

	public void setStandAloneLockSeconds(int standAloneLockSeconds) {
		this.standAloneLockSeconds = standAloneLockSeconds;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setDistributed(boolean isDistributed) {
		this.isDistributed = isDistributed;
	}

	public void setLjobSupporter(LjobSupporter ljobSupporter) {
		this.ljobSupporter = ljobSupporter;
	}

	private synchronized boolean lockCustomJob() {
		if (isCustomWorking) {
			return false;
		}

		isCustomWorking = true;
		return true;
	}

	private synchronized void releaseCustomJob() {
		isCustomWorking = false;
	}

	public boolean isCustomWorking() {
		return isCustomWorking;
	}

	private synchronized boolean lockJob() {
		if (isWorking) {
			return false;
		}

		isWorking = true;
		return true;
	}

	private synchronized void releaseJob() {
		isWorking = false;
	}

	public boolean isWorking() {
		return isWorking;
	}

	public void runJob() {
		if (!lockJob()) {
			LOG.debug("lock job failed, job is running...");
			return;
		}

		StandAloneRuntimeUpdater standAloneRuntimeUpdater = null;
		if (!isDistributed) {
			if (null == threadPoolExecutor) {
				threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
			}

			boolean distributedLockResult = ljobSupporter.getStandAloneJobLock(groupName, jobName, standAloneLockSeconds);
			LOG.debug("get stand-alone job lock, groupName: " + groupName + ", jobName: " + jobName + ", lockSeconds: " + standAloneLockSeconds
					+ ", lockResult: " + distributedLockResult);
			long currentTime = 0;
			Long runtime = null;
			while (!distributedLockResult) {
				try {
					Thread.sleep(3000);
				}
				catch (Exception e) {
					LOG.error(e.toString(), e);
				}

				runtime = ljobSupporter.getStandAloneJobRuntime(groupName, jobName);
				if (null != runtime) {
					currentTime = new Date().getTime();
					if (currentTime - runtime > 10000l) {
						LOG.error("stand-alone job runtime timeout, the lock will be released. runtime: " + runtime + ", groupName: " + groupName
								+ ", jobName: " + jobName);
						ljobSupporter.releaseStandAloneJobLock(groupName, jobName);
					}
				}

				distributedLockResult = ljobSupporter.getStandAloneJobLock(groupName, jobName, standAloneLockSeconds);

				LOG.debug("retry get stand-alone job lock, groupName: " + groupName + ", jobName: " + jobName + ", lockSeconds: "
						+ standAloneLockSeconds + ", lockResult: " + distributedLockResult);
			}

			standAloneRuntimeUpdater = new StandAloneRuntimeUpdater();
			threadPoolExecutor.execute(standAloneRuntimeUpdater);
		}

		try {
			// run job
			this.jobDetail();
		}
		catch (Exception e) {
			LOG.error("run job exception: " + e.toString(), e);
		}

		if (!isDistributed) {
			standAloneRuntimeUpdater.setUpdate(false);
			boolean releaseResult = ljobSupporter.releaseStandAloneJobLock(groupName, jobName);
			LOG.debug("release stand-alone job lock, groupName: " + groupName + ", jobName: " + jobName + ", releaseResult: " + releaseResult);
			int tryCount = 5;
			while (!releaseResult) {
				tryCount = tryCount - 1;
				if (tryCount <= 0) {
					break;
				}

				try {
					Thread.sleep(1000);
				}
				catch (Exception e) {
					LOG.error(e.toString(), e);
				}

				releaseResult = ljobSupporter.releaseStandAloneJobLock(groupName, jobName);
				LOG.debug("retry release stand-alone job lock, groupName: " + groupName + ", jobName: " + jobName + ", releaseResult: "
						+ releaseResult);
			}
		}

		releaseJob();
	}

	public void runJob(JSONObject customParams) {
		if (!lockCustomJob()) {
			LOG.warn("lock custom job failed, custom job is running...");
			return;
		}

		LOG.info("run custom job detail, customParams: " + customParams);
		try {
			// run custom job
			this.jobDetail(customParams);
		}
		catch (Exception e) {
			LOG.error("run custom job exception, customParams: " + customParams + ", exception: " + e.toString(), e);
		}

		releaseCustomJob();
	}

	/**
	 * custom job detail
	 * 
	 * @param customParams JSONObject
	 */
	public abstract void jobDetail(JSONObject customParams);

	/**
	 * job detail
	 */
	public abstract void jobDetail();

	/**
	 * do something after shutdown
	 */
	public void doAfterShutdown() {

	}

	/**
	 * do something before schedule
	 */
	public void doBeforeSchedule() {

	}

	private class StandAloneRuntimeUpdater implements Runnable {

		private boolean isUpdate = true;

		public void setUpdate(boolean isUpdate) {
			this.isUpdate = isUpdate;
		}

		@Override
		public void run() {
			while (isUpdate) {
				ljobSupporter.updateStandAloneJobRuntime(groupName, jobName, new Date().getTime());

				try {
					Thread.sleep(1000);
				}
				catch (Exception e) {
					LOG.error(e.toString(), e);
				}
			}
		}
	}
}
