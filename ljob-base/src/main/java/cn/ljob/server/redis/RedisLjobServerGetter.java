package cn.ljob.server.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.server.LjobServerGetter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobServerGetter
 *
 * @author darren
 * @since 2018年4月25日 上午11:45:02
 */
public class RedisLjobServerGetter extends LjobServerGetter {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobServerGetter.class);

	private JedisPool jedisPool = null;

	public RedisLjobServerGetter(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public String getLjobServer() {
		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			return jedis.get(RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER);
		}
		catch (Exception e) {
			LOG.error("get ljob server exception：" + e.toString(), e);
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
