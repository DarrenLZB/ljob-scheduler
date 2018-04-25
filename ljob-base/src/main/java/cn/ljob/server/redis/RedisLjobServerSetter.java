package cn.ljob.server.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.server.LjobServerSetter;
import cn.ljob.util.CommonUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobServerSetter
 *
 * @author darren
 * @since 2018年4月25日 上午11:45:06
 */
public class RedisLjobServerSetter extends LjobServerSetter {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobServerSetter.class);

	private JedisPool jedisPool = null;

	public RedisLjobServerSetter(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public boolean checkAndSetMainServer() {
		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			if (!CommonUtil.lock(jedis, RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER_CHECK_LOCK, 3, 3)) {
				return false;
			}

			String localIP = this.getLjobMonitor().getIp();
			String mainServerIP = jedis.get(RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER);
			if (null == mainServerIP) {
				jedis.setex(RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER, 10, localIP);
			}
			else if (localIP.equals(mainServerIP)) {
				jedis.setex(RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER, 10, localIP);
			}
			else {
				CommonUtil.release(jedis, RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER_CHECK_LOCK);
				return false;
			}

			CommonUtil.release(jedis, RedisLjobServerConstant.REDIS_LJOB_MAIN_SERVER_CHECK_LOCK);
			return true;
		}
		catch (Exception e) {
			LOG.error("check and set ljob main server exception：" + e.toString(), e);
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
