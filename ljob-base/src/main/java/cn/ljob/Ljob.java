package cn.ljob;

import org.quartz.Trigger;

/**
 * Ljob
 *
 * @author darren
 * @since 2018年4月25日 上午11:42:37
 */
public class Ljob {

	private String jobName = null;

	private String groupName = null;

	private LjobRunner jobRunner = null;

	private String jobCronExpression = null;

	private boolean isScheduling = false;

	private Trigger targetTrigger = null;

	private boolean isDistributed = false;

	private boolean isSupportInstantRunReq = false;

	private boolean isSupportCloseJob = false;

	private boolean isSupportCustomRunReq = false;

	private String description = null;

	private boolean isAutoSchedule = true;

	private Long startupTime = null;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public LjobRunner getJobRunner() {
		return jobRunner;
	}

	public void setJobRunner(LjobRunner jobRunner) {
		this.jobRunner = jobRunner;
	}

	public String getJobCronExpression() {
		return jobCronExpression;
	}

	public void setJobCronExpression(String jobCronExpression) {
		this.jobCronExpression = jobCronExpression;
	}

	public boolean isScheduling() {
		return isScheduling;
	}

	public void setScheduling(boolean isScheduling) {
		this.isScheduling = isScheduling;
	}

	public Trigger getTargetTrigger() {
		return targetTrigger;
	}

	public void setTargetTrigger(Trigger targetTrigger) {
		this.targetTrigger = targetTrigger;
	}

	public boolean isDistributed() {
		return isDistributed;
	}

	public void setDistributed(boolean isDistributed) {
		this.isDistributed = isDistributed;
	}

	public boolean isSupportInstantRunReq() {
		return isSupportInstantRunReq;
	}

	public void setSupportInstantRunReq(boolean isSupportInstantRunReq) {
		this.isSupportInstantRunReq = isSupportInstantRunReq;
	}

	public boolean isSupportCloseJob() {
		return isSupportCloseJob;
	}

	public void setSupportCloseJob(boolean isSupportCloseJob) {
		this.isSupportCloseJob = isSupportCloseJob;
	}

	public boolean isSupportCustomRunReq() {
		return isSupportCustomRunReq;
	}

	public void setSupportCustomRunReq(boolean isSupportCustomRunReq) {
		this.isSupportCustomRunReq = isSupportCustomRunReq;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public boolean isWorking() {
		if (null != jobRunner) {
			return jobRunner.isWorking();
		}

		return false;
	}

	public boolean isCustomWorking() {
		if (null != jobRunner) {
			return jobRunner.isCustomWorking();
		}

		return false;
	}

	public boolean isAutoSchedule() {
		return isAutoSchedule;
	}

	public void setAutoSchedule(boolean isAutoSchedule) {
		this.isAutoSchedule = isAutoSchedule;
	}

	public Long getStartupTime() {
		return startupTime;
	}

	public void setStartupTime(Long startupTime) {
		this.startupTime = startupTime;
	}

	@Override
	public String toString() {
		return String.format(
				"Ljob [jobName=%s, groupName=%s, jobRunner=%s, jobCronExpression=%s, isScheduling=%s, targetTrigger=%s, isDistributed=%s, isSupportInstantRunReq=%s, isSupportCloseJob=%s, isSupportCustomRunReq=%s, description=%s, isAutoSchedule=%s, startupTime=%s]",
				jobName, groupName, jobRunner, jobCronExpression, isScheduling, targetTrigger, isDistributed, isSupportInstantRunReq,
				isSupportCloseJob, isSupportCustomRunReq, description, isAutoSchedule, startupTime);
	}
}
