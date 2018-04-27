package cn.ljob.heartbeat;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LjobHeartbeat
 *
 * @author darren
 * @since 2018年4月25日 上午11:43:38
 */
public class LjobHeartbeat implements Serializable {

	private static final long serialVersionUID = -2388292940273388920L;

	private static final Logger LOG = LoggerFactory.getLogger(LjobHeartbeat.class);

	private DateFormat dateTimeShowFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private String ip = null;

	private String groupName = null;

	private String jobName = null;

	private String jobCronExpression = null;

	private boolean isScheduling = false;

	private boolean isDistributed = false;

	private boolean isSupportInstantRunReq = false;

	private boolean isSupportCloseJob = false;

	private boolean isSupportCustomRunReq = false;

	private String description = null;

	private String sendTime = null;

	private boolean isWorking = false;

	private boolean isCustomWorking = false;

	public boolean isCustomWorking() {
		return isCustomWorking;
	}

	public void setCustomWorking(boolean isCustomWorking) {
		this.isCustomWorking = isCustomWorking;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
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

	public String getSendTime() {
		return sendTime;
	}

	public void setSendTime(String sendTime) {
		this.sendTime = sendTime;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public boolean isWorking() {
		return isWorking;
	}

	public void setWorking(boolean isWorking) {
		this.isWorking = isWorking;
	}

	public String getSendTimeShow() {
		Date sendTime = null;

		if (null == dateTimeShowFormat) {
			dateTimeShowFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}

		try {
			sendTime = new Date(Long.valueOf(this.sendTime));
			return dateTimeShowFormat.format(sendTime);
		}
		catch (Exception e) {
			return null;
		}
	}

	public boolean isValideHeartbeat(String ljobServerTime) {
		if (null == this.sendTime) {
			return false;
		}

		Long sendTimeInteger = null;
		Long currentTimeInteger = null;

		try {
			sendTimeInteger = Long.valueOf(this.sendTime);
			currentTimeInteger = Long.valueOf(ljobServerTime);
		}
		catch (Exception e) {
			LOG.error(e.toString());
			return false;
		}

		if (currentTimeInteger - sendTimeInteger > 120000l) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return String.format(
				"LjobHeartbeat [ip=%s, groupName=%s, jobName=%s, jobCronExpression=%s, isScheduling=%s, isDistributed=%s, isSupportInstantRunReq=%s, isSupportCloseJob=%s, isSupportCustomRunReq=%s, description=%s, sendTime=%s, isWorking=%s, isCustomWorking=%s]",
				ip, groupName, jobName, jobCronExpression, isScheduling, isDistributed, isSupportInstantRunReq, isSupportCloseJob,
				isSupportCustomRunReq, description, sendTime, isWorking, isCustomWorking);
	}
}
