package cn.ljob.support.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.support.LjobSupporter;
import cn.ljob.util.CommonUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobSupporter
 *
 * @author darren
 * @since 2018年4月25日 上午11:45:28
 */
public class RedisLjobSupporter implements LjobSupporter {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobSupporter.class);

	private JedisPool jedisPool = null;

	public RedisLjobSupporter(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public boolean getStandAloneJobLock(String groupName, String jobName, int lockSeconds) {
		String lockKey = RedisLjobSupporterConstant.REDIS_STAND_ALONE_JOB_LOCK_PREFIX + groupName + "_" + jobName;

		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			return CommonUtil.lock(jedis, lockKey, lockSeconds, 10);
		}
		catch (Exception e) {
			LOG.error("get stand-alone job lock exception, lockKey: " + lockKey + ", exception: " + e.toString(), e);
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

	@Override
	public boolean releaseStandAloneJobLock(String groupName, String jobName) {
		String lockKey = RedisLjobSupporterConstant.REDIS_STAND_ALONE_JOB_LOCK_PREFIX + groupName + "_" + jobName;

		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			return CommonUtil.release(jedis, lockKey);
		}
		catch (Exception e) {
			LOG.error("release stand-alone job lock exception, lockKey: " + lockKey + ", exception: " + e.toString(), e);
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

	@Override
	public boolean updateStandAloneJobRuntime(String groupName, String jobName, long currentTime) {
		String key = RedisLjobSupporterConstant.REDIS_STAND_ALONE_JOB_RUNTIME_PREFIX + groupName + "_" + jobName;

		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			jedis.setex(key, 60, currentTime + "");
			return true;
		}
		catch (Exception e) {
			LOG.error("update stand-alone job runtime exception, key: " + key + ", currentTime: " + currentTime + ", exception: " + e.toString(), e);
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

	@Override
	public Long getStandAloneJobRuntime(String groupName, String jobName) {
		String key = RedisLjobSupporterConstant.REDIS_STAND_ALONE_JOB_RUNTIME_PREFIX + groupName + "_" + jobName;

		Jedis jedis = null;
		String value = null;
		Long time = 0L;
		try {
			jedis = this.jedisPool.getResource();
			value = jedis.get(key);
			if (null != value) {
				time = Long.valueOf(value);
			}
			return time;
		}
		catch (Exception e) {
			LOG.error("get stand-alone job runtime exception, key: " + key + ", exception: " + e.toString(), e);
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
