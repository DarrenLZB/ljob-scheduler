package cn.ljob.heartbeat.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import cn.ljob.heartbeat.LjobHeartbeat;
import cn.ljob.heartbeat.LjobHeartbeatReceiver;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobHeartbeatReceiver
 *
 * @author darren
 * @since 2018年4月25日 上午11:44:11
 */
public class RedisLjobHeartbeatReceiver extends LjobHeartbeatReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobHeartbeatReceiver.class);

	private JedisPool jedisPool = null;

	public RedisLjobHeartbeatReceiver(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public LjobHeartbeat receiveLjobHeartbeat() {
		Jedis jedis = null;
		LjobHeartbeat ljobHeartbeat = null;
		String temp = null;

		try {
			jedis = this.jedisPool.getResource();
			temp = jedis.rpop(RedisLjobHeartbeatConstant.REDIS_LJOB_HEARTBEAT_QUEUE);
			if (null != temp) {
				ljobHeartbeat = JSONObject.parseObject(temp, LjobHeartbeat.class);
				LOG.debug("receive redis ljob heartbeat: " + ljobHeartbeat.toString());
			}

			return ljobHeartbeat;
		}
		catch (Exception e) {
			LOG.error("receive redis ljob heartbeat exception：" + e.toString(), e);
			return ljobHeartbeat;
		}
		finally {
			if (null != jedis) {
				try {
					jedis.close();
				}
				catch (Exception e) {
					LOG.error(e.toString(), e);
				}
			}
		}
	}

	@Override
	public Map<String, List<LjobHeartbeat>> getCacheOnlineLjobMap() {
		Jedis jedis = null;
		byte[] byteArray = null;
		Map<String, List<LjobHeartbeat>> onlineLjobMapCache = null;

		try {
			jedis = jedisPool.getResource();
			byteArray = jedis.get(RedisLjobHeartbeatConstant.REDIS_LJOB_ONLINE_MAP_CACHE.getBytes());
			if (null != byteArray) {
				onlineLjobMapCache = toOnlineLjobMap(byteArray);
			}

			return onlineLjobMapCache;
		}
		catch (Exception e) {
			LOG.error("get cache online job map exception：" + e.toString(), e);
			return onlineLjobMapCache;
		}
		finally {
			if (null != jedis) {
				try {
					jedis.close();
				}
				catch (Exception ee) {
					LOG.error(ee.toString(), ee);
				}
			}
		}
	}

	@Override
	public boolean cacheOnlineLjobMap(Map<String, List<LjobHeartbeat>> onlineLjobMap) {
		Jedis jedis = null;

		try {
			jedis = jedisPool.getResource();
			jedis.set(RedisLjobHeartbeatConstant.REDIS_LJOB_ONLINE_MAP_CACHE.getBytes(), getOnlineLjobMapByte(onlineLjobMap));
			return true;
		}
		catch (Exception e) {
			LOG.error("cache online job map exception：" + e.toString(), e);
			return false;
		}
		finally {
			if (null != jedis) {
				try {
					jedis.close();
				}
				catch (Exception ee) {
					LOG.error(ee.toString(), ee);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, List<LjobHeartbeat>> toOnlineLjobMap(byte[] bytes) {
		if (null == bytes) {
			return null;
		}

		Map<String, List<LjobHeartbeat>> obj = null;
		ByteArrayInputStream bis = null;
		ObjectInputStream ois = null;

		try {
			bis = new ByteArrayInputStream(bytes);
			ois = new ObjectInputStream(bis);
			obj = (Map<String, List<LjobHeartbeat>>) ois.readObject();
			return obj;
		}
		catch (Exception e) {
			LOG.error(e.toString(), e);
			return null;
		}
		finally {
			if (null != ois) {
				try {
					ois.close();
				}
				catch (IOException e) {
					LOG.error(e.toString(), e);
				}
			}

			if (null != bis) {
				try {
					bis.close();
				}
				catch (IOException e) {
					LOG.error(e.toString(), e);
				}
			}
		}
	}

	private static byte[] getOnlineLjobMapByte(Map<String, List<LjobHeartbeat>> onlineLjobMap) {
		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;

		try {
			bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(onlineLjobMap);
			oos.flush();
			return bos.toByteArray();
		}
		catch (Exception e) {
			LOG.error(e.toString(), e);
			return null;
		}
		finally {
			if (null != oos) {
				try {
					oos.close();
				}
				catch (IOException e) {
					LOG.error(e.toString(), e);
				}
			}

			if (null != bos) {
				try {
					bos.close();
				}
				catch (IOException e) {
					LOG.error(e.toString(), e);
				}
			}
		}
	}
}
