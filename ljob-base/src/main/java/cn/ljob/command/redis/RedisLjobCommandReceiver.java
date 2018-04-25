package cn.ljob.command.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import cn.ljob.Ljob;
import cn.ljob.LjobTrigger;
import cn.ljob.command.LjobCommand;
import cn.ljob.command.LjobCommandReceiver;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobCommandReceiver
 *
 * @author darren
 * @since 2018年4月25日 上午11:43:25
 */
public class RedisLjobCommandReceiver extends LjobCommandReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobCommandReceiver.class);

	private JedisPool jedisPool = null;

	private String redisQueueName = null;

	public RedisLjobCommandReceiver(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public LjobCommand getLjobCommand() {
		if (null == redisQueueName) {
			LjobTrigger ljobTrigger = this.getLjobTrigger();
			Ljob ljob = this.getLjob();
			redisQueueName = RedisLjobCommandConstant.REDIS_LJOB_COMMAND_QUEUE_PREFIX + ljobTrigger.getIp() + "_" + ljob.getGroupName() + "_"
					+ ljob.getJobName();
		}

		Jedis jedis = null;
		LjobCommand ljobCommand = null;
		String temp = null;

		try {
			jedis = this.jedisPool.getResource();
			temp = jedis.rpop(redisQueueName);
			if (null != temp) {
				ljobCommand = JSONObject.parseObject(temp, LjobCommand.class);
				LOG.info("receive redis ljob command: " + ljobCommand.toString());
			}

			return ljobCommand;
		}
		catch (Exception e) {
			LOG.error("receive redis ljob command exception：" + e.toString(), e);
			return ljobCommand;
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
