package cn.ljob.heartbeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.LjobMonitor;
import cn.ljob.util.UUIDGenerator;

/**
 * LjobHeartbeatChecker
 *
 * @author darren
 * @since 2018年4月25日 上午11:43:43
 */
public class LjobHeartbeatChecker extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobHeartbeatChecker.class);

	private LjobMonitor ljobMonitor = null;

	private LjobHeartbeatReceiver ljobHeartbeatReceiver = null;

	public LjobHeartbeatChecker(LjobMonitor ljobMonitor, LjobHeartbeatReceiver ljobHeartbeatReceiver) {
		this.ljobMonitor = ljobMonitor;
		this.ljobHeartbeatReceiver = ljobHeartbeatReceiver;
	}

	@Override
	public void run() {
		Map<String, List<LjobHeartbeat>> targetLjobMap = null;
		Map<String, List<LjobHeartbeat>> tmpLjobMap = null;
		Map<String, List<LjobHeartbeat>> onlineLjobMap = null;
		Set<String> keySet = null;
		List<LjobHeartbeat> tmpLjobList = null;
		List<LjobHeartbeat> ljobList = null;
		String lockUUID = null;

		while (!ljobMonitor.isDestroy()) {
			if (ljobMonitor.isMainServer()) {
				onlineLjobMap = new HashMap<String, List<LjobHeartbeat>>();
				tmpLjobMap = new HashMap<String, List<LjobHeartbeat>>();
				lockUUID = UUIDGenerator.generate();
				targetLjobMap = LjobHeartbeatReceiver.lockLjobMap(lockUUID);
				while (null == targetLjobMap) {
					try {
						Thread.sleep(1000);
					}
					catch (Exception e) {
						LOG.error(e.toString(), e);
					}

					targetLjobMap = LjobHeartbeatReceiver.lockLjobMap(lockUUID);
				}

				tmpLjobMap.putAll(targetLjobMap);
				keySet = tmpLjobMap.keySet();
				if (null != keySet) {
					for (String key : keySet) {
						tmpLjobList = tmpLjobMap.get(key);
						if (null != tmpLjobList && tmpLjobList.size() > 0) {
							ljobList = new ArrayList<LjobHeartbeat>();
							for (LjobHeartbeat ljob : tmpLjobList) {
								if (ljob.isValideHeartbeat(ljobMonitor.getLjobServerTime())) {
									ljobList.add(ljob);
								}
							}

							onlineLjobMap.put(key, ljobList);
						}
					}
				}

				ljobHeartbeatReceiver.cacheOnlineLjobMap(onlineLjobMap);

				try {
					LjobHeartbeatReceiver.releaseLjobMap(onlineLjobMap, lockUUID);
				}
				catch (Exception e) {
					LOG.error("release ljob map exception: " + e.toString(), e);
				}
			}

			try {
				Thread.sleep(10000);
			}
			catch (Exception e) {
				LOG.error(e.toString(), e);
			}
		}
	}
}
