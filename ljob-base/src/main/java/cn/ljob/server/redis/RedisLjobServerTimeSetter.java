package cn.ljob.server.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.server.LjobServerTimeSetter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobServerTimeSetter
 *
 * @author darren
 * @since 2018年4月25日 上午11:45:16
 */
public class RedisLjobServerTimeSetter extends LjobServerTimeSetter {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobServerTimeSetter.class);

	private JedisPool jedisPool = null;

	public RedisLjobServerTimeSetter(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public boolean cacheServerTime(String currentServerTime) {
		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			jedis.setex(RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER_TIME, 5, currentServerTime);
			return true;
		}
		catch (Exception e) {
			LOG.error("cache ljob server time exception：" + e.toString(), e);
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
