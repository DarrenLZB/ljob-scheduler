package cn.ljob.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.Ljob;
import cn.ljob.LjobTrigger;

/**
 * LjobHeartbeatSender
 *
 * @author darren
 * @since 2018年4月25日 上午11:43:54
 */
public abstract class LjobHeartbeatSender extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobHeartbeatSender.class);

	private LjobTrigger ljobTrigger = null;

	private Ljob ljob = null;

	public void setLjobTrigger(LjobTrigger ljobTrigger) {
		this.ljobTrigger = ljobTrigger;
	}

	public void setLjob(Ljob ljob) {
		this.ljob = ljob;
	}

	@Override
	public void run() {
		LjobHeartbeat ljobHeartbeat = new LjobHeartbeat();
		ljobHeartbeat.setIp(ljobTrigger.getIp());
		ljobHeartbeat.setGroupName(ljob.getGroupName());
		ljobHeartbeat.setJobName(ljob.getJobName());
		ljobHeartbeat.setDistributed(ljob.isDistributed());
		ljobHeartbeat.setSupportCloseJob(ljob.isSupportCloseJob());
		ljobHeartbeat.setSupportCustomRunReq(ljob.isSupportCustomRunReq());
		ljobHeartbeat.setSupportInstantRunReq(ljob.isSupportInstantRunReq());
		ljobHeartbeat.setDescription(ljob.getDescription());
		while (!ljobTrigger.isDestroy()) {
			if (ljobTrigger.isLjobServerOnline() && null != ljobTrigger.getLjobServerTime()) {
				ljobHeartbeat.setJobCronExpression(ljob.getJobCronExpression());
				ljobHeartbeat.setScheduling(ljob.isScheduling());
				ljobHeartbeat.setWorking(ljob.isWorking());
				ljobHeartbeat.setCustomWorking(ljob.isCustomWorking());
				ljobHeartbeat.setSendTime(ljobTrigger.getLjobServerTime());
				if (this.sendLjobHeartbeat(ljobHeartbeat)) {
					try {
						Thread.sleep(2000);
					}
					catch (InterruptedException e) {
						LOG.error(e.toString(), e);
					}
				}
				else {
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						LOG.error(e.toString(), e);
					}
				}
			}
			else {
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					LOG.error(e.toString(), e);
				}
			}
		}
	}

	public abstract boolean sendLjobHeartbeat(LjobHeartbeat ljobHeartbeat);
}
