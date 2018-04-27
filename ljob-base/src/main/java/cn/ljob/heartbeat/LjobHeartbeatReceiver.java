package cn.ljob.heartbeat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.ljob.LjobMonitor;

/**
 * LjobHeartbeatReceiver
 *
 * @author darren
 * @since 2018年4月25日 上午11:43:48
 */
public abstract class LjobHeartbeatReceiver extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobHeartbeatReceiver.class);

	private static final int ADD_LJOB_SUCCESS = 0;

	private static final int ADD_LJOB_FAILED_WITH_LOCK = 1;

	private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

	private static boolean isMainServer = false;

	private static Map<String, List<LjobHeartbeat>> onlineLjobMap = new HashMap<String, List<LjobHeartbeat>>();

	private static boolean isLockingLjobMap = false;

	private static String lockLjobMapUUID = null;

	private static String lockTime = null;

	private LjobMonitor ljobMonitor;

	public void setLjobMonitor(LjobMonitor ljobMonitor) {
		this.ljobMonitor = ljobMonitor;
	}

	public static boolean isMainServer() {
		return isMainServer;
	}

	public static Map<String, List<LjobHeartbeat>> getOnlineLjobMap() {
		return onlineLjobMap;
	}

	public static LjobHeartbeat getOnlineLjob(String groupName, String jobName, String ip) {
		return getOnlineLjob(groupName + "_" + jobName, ip);
	}

	public static LjobHeartbeat getOnlineLjob(String ljobKey, String ip) {
		List<LjobHeartbeat> ljobHeartbeatList = onlineLjobMap.get(ljobKey);
		if (null != ljobHeartbeatList && ljobHeartbeatList.size() > 0) {
			for (LjobHeartbeat ljobHeartbeat : ljobHeartbeatList) {
				if (ljobHeartbeat.getIp().equals(ip)) {
					return ljobHeartbeat;
				}
			}
		}

		return null;
	}

	public synchronized static Map<String, List<LjobHeartbeat>> lockLjobMap(String lockUUID) {
		if (null == lockUUID) {
			LOG.error("lock online job map failed, uuid is null...");
			return null;
		}

		if (isLockingLjobMap) {
			if (isLockTimeout()) {
				releaseLjobMap();
			}
			else {
				return null;
			}
		}

		lockTime = DATE_TIME_FORMAT.format(new Date());
		lockLjobMapUUID = lockUUID;
		isLockingLjobMap = true;
		return onlineLjobMap;
	}

	public synchronized static void releaseLjobMap(Map<String, List<LjobHeartbeat>> ljobMap, String lockUUID) throws Exception {
		if (lockUUID == null || !lockUUID.equals(lockLjobMapUUID)) {
			LOG.error("release online job map lock failed, uuid not correct, lockUUID: " + lockUUID + ", lockLjobMapUUID: " + lockLjobMapUUID);
			throw new Exception("release online job map lock failed, uuid not correct");
		}

		lockTime = null;
		lockLjobMapUUID = null;
		isLockingLjobMap = false;
		onlineLjobMap = ljobMap;
	}

	private int addLjob(LjobHeartbeat ljobHeartbeat) throws Exception {
		if (isLjobMapLocked()) {
			return ADD_LJOB_FAILED_WITH_LOCK;
		}

		String ip = ljobHeartbeat.getIp();
		String groupName = ljobHeartbeat.getGroupName();
		String jobName = ljobHeartbeat.getJobName();
		String ljobKey = groupName + "_" + jobName;

		List<LjobHeartbeat> ljobHeartbeatList = onlineLjobMap.get(ljobKey);
		if (null == ljobHeartbeatList) {
			ljobHeartbeatList = new ArrayList<LjobHeartbeat>();
			ljobHeartbeatList.add(ljobHeartbeat);
		}

		boolean isNewLjob = true;
		for (LjobHeartbeat oldljobHeartbeat : ljobHeartbeatList) {
			if (oldljobHeartbeat.getIp().equals(ip)) {
				oldljobHeartbeat.setSendTime(ljobHeartbeat.getSendTime());
				oldljobHeartbeat.setScheduling(ljobHeartbeat.isScheduling());
				oldljobHeartbeat.setWorking(ljobHeartbeat.isWorking());
				oldljobHeartbeat.setCustomWorking(ljobHeartbeat.isCustomWorking());
				oldljobHeartbeat.setJobCronExpression(ljobHeartbeat.getJobCronExpression());
				oldljobHeartbeat.setDistributed(ljobHeartbeat.isDistributed());
				oldljobHeartbeat.setSupportInstantRunReq(ljobHeartbeat.isSupportInstantRunReq());
				oldljobHeartbeat.setSupportCustomRunReq(ljobHeartbeat.isSupportCustomRunReq());
				oldljobHeartbeat.setDescription(ljobHeartbeat.getDescription());
				oldljobHeartbeat.setSupportCloseJob(ljobHeartbeat.isSupportCloseJob());
				isNewLjob = false;
			}
		}

		if (isNewLjob) {
			ljobHeartbeatList.add(ljobHeartbeat);
		}

		onlineLjobMap.put(ljobKey, ljobHeartbeatList);
		return ADD_LJOB_SUCCESS;
	}

	private synchronized boolean isLjobMapLocked() {
		if (isLockingLjobMap) {
			if (isLockTimeout()) {
				releaseLjobMap();
			}
			else {
				return true;
			}
		}

		return false;
	}

	private static void releaseLjobMap() {
		lockTime = null;
		lockLjobMapUUID = null;
		isLockingLjobMap = false;
	}

	private static boolean isLockTimeout() {
		if (null == lockTime) {
			return true;
		}

		Long lockTimeInteger = null;
		Long currentTimeInteger = null;

		try {
			DATE_TIME_FORMAT.parse(lockTime);
			lockTimeInteger = Long.valueOf(lockTime);
			currentTimeInteger = Long.valueOf(DATE_TIME_FORMAT.format(new Date()));
		}
		catch (Exception e) {
			LOG.error(e.toString(), e);
			lockTime = null;
			return true;
		}

		if (currentTimeInteger - lockTimeInteger > 120000) {
			lockTime = null;
			return true;
		}

		return false;
	}

	@Override
	public void run() {
		LjobHeartbeat ljobHeartbeat = null;
		int addLjobResult = ADD_LJOB_FAILED_WITH_LOCK;
		while (!ljobMonitor.isDestroy()) {
			if (ljobMonitor.isMainServer()) {
				isMainServer = true;
				ljobHeartbeat = this.receiveLjobHeartbeat();
				if (null == ljobHeartbeat) {
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						LOG.error(e.toString(), e);
					}
				}
				else if (ljobHeartbeat.isValideHeartbeat(ljobMonitor.getLjobServerTime())) {
					try {
						addLjobResult = ADD_LJOB_FAILED_WITH_LOCK;
						while (addLjobResult == ADD_LJOB_FAILED_WITH_LOCK) {
							addLjobResult = this.addLjob(ljobHeartbeat);
							if (addLjobResult == ADD_LJOB_FAILED_WITH_LOCK) {
								try {
									Thread.sleep(100);
								}
								catch (Exception e) {
									LOG.error(e.toString(), e);
								}
							}
						}
					}
					catch (Exception e) {
						LOG.error(e.toString(), e);
					}
				}
			}
			else {
				isMainServer = false;
				Map<String, List<LjobHeartbeat>> tempMap = this.getCacheOnlineLjobMap();
				if (null == tempMap) {
					tempMap = new HashMap<String, List<LjobHeartbeat>>();
				}

				try {
					onlineLjobMap = tempMap;
					LOG.debug("get cache online job map: " + tempMap.toString());
				}
				catch (Exception e) {
					LOG.error(e.toString(), e);
				}

				try {
					Thread.sleep(5000);
				}
				catch (InterruptedException e) {
					LOG.error(e.toString(), e);
				}
			}
		}
	}

	public abstract LjobHeartbeat receiveLjobHeartbeat();

	public abstract Map<String, List<LjobHeartbeat>> getCacheOnlineLjobMap();

	public abstract boolean cacheOnlineLjobMap(Map<String, List<LjobHeartbeat>> onlineLjobMap);
}
