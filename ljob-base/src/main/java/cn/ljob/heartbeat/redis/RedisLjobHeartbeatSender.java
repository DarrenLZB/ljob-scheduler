package cn.ljob.heartbeat.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import cn.ljob.heartbeat.LjobHeartbeat;
import cn.ljob.heartbeat.LjobHeartbeatSender;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobHeartbeatSender
 *
 * @author darren
 * @since 2018年4月25日 上午11:44:18
 */
public class RedisLjobHeartbeatSender extends LjobHeartbeatSender {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobHeartbeatSender.class);

	private JedisPool jedisPool = null;

	public RedisLjobHeartbeatSender(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public boolean sendLjobHeartbeat(LjobHeartbeat ljobHeartbeat) {
		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			jedis.lpush(RedisLjobHeartbeatConstant.REDIS_LJOB_HEARTBEAT_QUEUE, JSON.toJSONString(ljobHeartbeat));
			LOG.debug("send redis ljob heartbeat: " + JSON.toJSONString(ljobHeartbeat));
			return true;
		}
		catch (Exception e) {
			LOG.error("send redis ljob heartbeat exception：" + e.toString(), e);
			return false;
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
}
