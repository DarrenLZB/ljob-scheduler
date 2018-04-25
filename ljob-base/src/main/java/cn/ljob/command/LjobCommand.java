package cn.ljob.command;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LjobCommand
 *
 * @author darren
 * @since 2018年4月25日 上午11:42:52
 */
public class LjobCommand implements Serializable {

	private static final long serialVersionUID = 2097304461558321910L;

	private static final Logger LOG = LoggerFactory.getLogger(LjobCommand.class);

	private String requestID = null;

	private String requestIP = null;

	private String requestUser = null;

	private String requestTime = null;

	private String groupName = null;

	private String jobName = null;

	private String targetIP = null;

	private String command = null;

	public String getRequestID() {
		return requestID;
	}

	public void setRequestID(String requestID) {
		this.requestID = requestID;
	}

	public String getRequestIP() {
		return requestIP;
	}

	public void setRequestIP(String requestIP) {
		this.requestIP = requestIP;
	}

	public String getRequestUser() {
		return requestUser;
	}

	public void setRequestUser(String requestUser) {
		this.requestUser = requestUser;
	}

	public String getRequestTime() {
		return requestTime;
	}

	public void setRequestTime(String requestTime) {
		this.requestTime = requestTime;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public String getTargetIP() {
		return targetIP;
	}

	public void setTargetIP(String targetIP) {
		this.targetIP = targetIP;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	public String toString() {
		return String.format(
				"LjobCommand [requestID=%s, requestIP=%s, requestUser=%s, requestTime=%s, groupName=%s, jobName=%s, targetIP=%s, command=%s]",
				requestID, requestIP, requestUser, requestTime, groupName, jobName, targetIP, command);
	}

	public boolean isValideRequest(String ljobServerTime) {
		if (null == this.requestTime) {
			return false;
		}

		Long requestTimeInteger = null;
		Long currentTimeInteger = null;

		try {
			requestTimeInteger = Long.valueOf(this.requestTime);
			currentTimeInteger = Long.valueOf(ljobServerTime);
		}
		catch (Exception e) {
			LOG.error(e.toString());
			return false;
		}

		if (currentTimeInteger - requestTimeInteger > 120000l) {
			return false;
		}

		return true;
	}
}
