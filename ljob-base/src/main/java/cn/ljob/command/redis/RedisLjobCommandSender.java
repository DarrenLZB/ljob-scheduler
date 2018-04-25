package cn.ljob.command.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import cn.ljob.command.LjobCommand;
import cn.ljob.command.LjobCommandSender;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * RedisLjobCommandSender
 *
 * @author darren
 * @since 2018年4月25日 上午11:43:31
 */
public class RedisLjobCommandSender implements LjobCommandSender {

	private static final Logger LOG = LoggerFactory.getLogger(RedisLjobCommandSender.class);

	private JedisPool jedisPool = null;

	public RedisLjobCommandSender(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public boolean sendLjobCommand(LjobCommand ljobCommand) {
		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			jedis.lpush(RedisLjobCommandConstant.REDIS_LJOB_COMMAND_QUEUE_PREFIX + ljobCommand.getTargetIP() + "_" + ljobCommand.getGroupName() + "_"
					+ ljobCommand.getJobName(), JSON.toJSONString(ljobCommand));
			LOG.info("send redis ljob command: " + JSON.toJSONString(ljobCommand));
			return true;
		}
		catch (Exception e) {
			LOG.error("send redis ljob command exception：" + e.toString(), e);
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
