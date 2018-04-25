package cn.ljob;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.command.LjobCommand;
import cn.ljob.command.LjobCommandSender;
import cn.ljob.command.redis.RedisLjobCommandSender;
import cn.ljob.heartbeat.LjobHeartbeatChecker;
import cn.ljob.heartbeat.LjobHeartbeatReceiver;
import cn.ljob.heartbeat.redis.RedisLjobHeartbeatReceiver;
import cn.ljob.server.LjobServerSetter;
import cn.ljob.server.LjobServerTimeGetter;
import cn.ljob.server.LjobServerTimeSetter;
import cn.ljob.server.redis.RedisLjobServerSetter;
import cn.ljob.server.redis.RedisLjobServerTimeGetter;
import cn.ljob.server.redis.RedisLjobServerTimeSetter;
import cn.ljob.util.CommonUtil;
import redis.clients.jedis.JedisPool;

/**
 * LjobMonitor
 *
 * @author darren
 * @since 2018年4月25日 上午11:42:31
 */
public class LjobMonitor {

	private static final Logger LOG = LoggerFactory.getLogger(LjobMonitor.class);

	private DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	private String protocol = "redis";

	private boolean isMainServer = false;

	private boolean isDestroy = false;

	private LjobCommandSender ljobCommandSender = null;

	private LjobServerTimeGetter ljobServerTimeGetter = null;

	private JedisPool jedisPool = null;

	private String ip = null;

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	public boolean isDestroy() {
		return isDestroy;
	}

	public boolean isMainServer() {
		return isMainServer;
	}

	public String getIp() {
		return ip;
	}

	public void setMainServer(boolean isMainServer) {
		this.isMainServer = isMainServer;
	}

	public void init() throws Exception {
		ip = CommonUtil.getLocalIP();
		LOG.info("start LjobMonitor: " + ip);

		if (null == protocol) {
			protocol = LjobProtocol.REDIS.protocol();
			LOG.warn("LjobProtocol is null, default to redis protocol.");
		}

		if (!LjobProtocol.isProtocol(protocol)) {
			protocol = LjobProtocol.REDIS.protocol();
			LOG.warn("LjobProtocol is not valid, default to redis protocol.");
		}
		else {
			LOG.info("use LjobProtocol: " + protocol);
		}

		if (LjobProtocol.REDIS.protocol().equals(protocol)) {
			LOG.info("start RedisLjobServerSetter...");
			LjobServerSetter ljobServerSetter = new RedisLjobServerSetter(jedisPool);
			ljobServerSetter.setLjobMonitor(this);
			ljobServerSetter.setName("LjobServerSetter_" + dateTimeFormat.format(new Date()));
			ljobServerSetter.start();

			LOG.info("start RedisLjobServerTimeSetter...");
			LjobServerTimeSetter ljobServerTimeSetter = new RedisLjobServerTimeSetter(jedisPool);
			ljobServerTimeSetter.setLjobMonitor(this);
			ljobServerTimeSetter.setName("LjobServerTimeSetter_" + dateTimeFormat.format(new Date()));
			ljobServerTimeSetter.start();

			LOG.info("start RedisLjobHeartbeatReceiver...");
			LjobHeartbeatReceiver ljobHeartbeatReceiver = new RedisLjobHeartbeatReceiver(jedisPool);
			ljobHeartbeatReceiver.setLjobMonitor(this);
			ljobHeartbeatReceiver.setName("LjobHeartbeatReceiver_" + dateTimeFormat.format(new Date()));
			ljobHeartbeatReceiver.start();

			LOG.info("start LjobHeartbeatChecker...");
			LjobHeartbeatChecker ljobHeartbeatChecker = new LjobHeartbeatChecker(this, ljobHeartbeatReceiver);
			ljobHeartbeatChecker.setName("LjobHeartbeatChecker_" + dateTimeFormat.format(new Date()));
			ljobHeartbeatChecker.start();

			LOG.info("create RedisLjobServerTimeGetter...");
			ljobServerTimeGetter = new RedisLjobServerTimeGetter(jedisPool);

			LOG.info("create RedisLjobCommandSender...");
			ljobCommandSender = new RedisLjobCommandSender(jedisPool);
		}
	}

	public boolean sendLjobCommand(LjobCommand ljobCommand) {
		if (null == ljobCommand) {
			return false;
		}

		try {
			return ljobCommandSender.sendLjobCommand(ljobCommand);
		}
		catch (Exception e) {
			LOG.error("send ljob command exception, ljobCommand: " + ljobCommand.toString() + ", exception: " + e.toString(), e);
			return false;
		}
	}

	public void destroy() {
		isDestroy = true;
	}

	public String getLjobServerTime() {
		if (isMainServer) {
			return new Date().getTime() + "";
		}

		try {
			return ljobServerTimeGetter.getLjobServerTime();
		}
		catch (Exception e) {
			LOG.error("get ljob server time exception: " + e.toString(), e);
			return new Date().getTime() + "";
		}
	}
}
