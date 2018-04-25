package cn.ljob.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public final class CommonUtil {

	private static final Logger LOG = LoggerFactory.getLogger(CommonUtil.class);

	public static String getLocalIP() {
		List<String> ipList = getLocalIPList();
		if (null == ipList || ipList.size() == 0) {
			return null;
		}

		if (ipList.size() == 1) {
			return ipList.get(0);
		}

		for (String ip : ipList) {
			if (!"127.0.0.1".equals(ip)) {
				return ip;
			}
		}

		return ipList.get(0);
	}

	public static List<String> getLocalIPList() {
		List<String> ipList = new ArrayList<String>();
		Enumeration<NetworkInterface> networkInterfaces = null;
		NetworkInterface networkInterface = null;
		Enumeration<InetAddress> inetAddresses = null;
		InetAddress inetAddress = null;
		String ip = null;

		try {
			networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				networkInterface = networkInterfaces.nextElement();
				inetAddresses = networkInterface.getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					inetAddress = inetAddresses.nextElement();
					if (inetAddress != null && inetAddress instanceof Inet4Address) {
						// IPV4
						ip = inetAddress.getHostAddress();
						ipList.add(ip);
					}
				}
			}
		}
		catch (Exception e) {
			LOG.error(e.toString(), e);
		}

		return ipList;
	}

	public static boolean lock(Jedis jedis, String key, int lockTime, int timeOut) {
		long waitEndTime = System.currentTimeMillis() + (timeOut * 1000);
		String lockKey = ("JedisLock_".concat(key)).intern();
		long currTime = 0;
		try {
			while (!setLockNx(jedis, lockKey, lockTime)) {
				currTime = System.currentTimeMillis();
				if (waitEndTime < currTime) {
					LOG.debug("{} lock failed, wait timeout!", lockKey);
					return false;
				}
				Thread.sleep(100);
			}
		}
		catch (Exception e) {
			LOG.error("get redis lock exception: " + e.toString(), e);
			return false;
		}

		LOG.debug("key{} lock success, lock seconds: {}s.", key, lockTime);
		return true;
	}

	private static boolean setLockNx(Jedis jedis, String key, int lockSeonds) {
		Long valueLong = null;
		long setnxSuccess = 0;
		String oldValue = null;
		long setnxKill = 0;

		try {
			if (lockSeonds == -1) {
				lockSeonds = 60 * 60;
			}

			valueLong = System.currentTimeMillis() + lockSeonds * 1000L + 1;
			setnxSuccess = jedis.setnx(key, valueLong.toString());
			if (setnxSuccess == 1) {
				long exSuccess = jedis.expire(key, lockSeonds);
				if (exSuccess == 1) {
					return true;
				}
				else {
					LOG.error("set lock timeout failed, key: " + key + ", lockSeonds: " + lockSeonds + ", returnCode: " + exSuccess);
					jedis.del(key);
					return false;
				}
			}
			else {
				oldValue = jedis.get(key);
				if (null != oldValue) {
					long oldLockEndTime = Long.valueOf(oldValue);
					if (System.currentTimeMillis() - 5000 > oldLockEndTime) {
						setnxKill = jedis.setnx("KILL_" + key, System.currentTimeMillis() + "");
						jedis.expire("KILL_" + key, 5);
						if (setnxKill == 1) {
							oldValue = jedis.get(key);
							if (null != oldValue) {
								oldLockEndTime = Long.valueOf(oldValue);
								if (System.currentTimeMillis() - 5000 > oldLockEndTime) {
									LOG.error("lock timeout, will be released, key: " + key + ", value: " + oldValue);
									jedis.del(key);
								}
							}
							jedis.del("KILL_" + key);
						}
					}
				}
			}

			return false;
		}
		catch (Exception e) {
			LOG.error("redis setLockNx exception: " + e.toString(), e);
			return false;
		}
	}

	public static boolean release(Jedis jedis, String key) {
		String lockKey = null;
		boolean ok = false;

		try {
			lockKey = ("JedisLock_".concat(key)).intern();
			ok = jedis.del(lockKey) == 1;
			if (ok) {
				LOG.debug("release lock {} success.", key);
			}
			else {
				LOG.error("release lock {} failed, already released.", key);
			}

			return ok;
		}
		catch (Exception e) {
			LOG.error("release lock exception: " + e.toString() + ", key: " + key, e);
			return false;
		}
	}
}
