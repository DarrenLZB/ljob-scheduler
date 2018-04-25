package cn.ljob.server.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.server.LjobServerTimeGetter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobServerTimeGetter
 *
 * @author darren
 * @since 2018年4月25日 上午11:45:11
 */
public class RedisLjobServerTimeGetter extends LjobServerTimeGetter {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobServerTimeGetter.class);

	private JedisPool jedisPool = null;

	public RedisLjobServerTimeGetter(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public String getLjobServerTime() {
		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			return jedis.get(RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER_TIME);
		}
		catch (Exception e) {
			LOG.error("get ljob server time exception：" + e.toString(), e);
			return null;
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
