package cn.ljob;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ljob.command.LjobCommandReceiver;
import cn.ljob.command.redis.RedisLjobCommandReceiver;
import cn.ljob.heartbeat.LjobHeartbeatSender;
import cn.ljob.heartbeat.redis.RedisLjobHeartbeatSender;
import cn.ljob.schedule.LjobScheduler;
import cn.ljob.server.LjobServerGetter;
import cn.ljob.server.LjobServerTimeGetter;
import cn.ljob.server.redis.RedisLjobServerGetter;
import cn.ljob.server.redis.RedisLjobServerTimeGetter;
import cn.ljob.support.LjobSupporter;
import cn.ljob.support.redis.RedisLjobSupporter;
import cn.ljob.util.CommonUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * LjobTrigger
 *
 * @author darren
 * @since 2018年4月25日 上午11:42:18
 */
public class LjobTrigger {

	private static final Logger LOG = LoggerFactory.getLogger(LjobTrigger.class);

	private DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	private String ip = null;

	private String protocol = "redis";

	private boolean isDestroy = false;

	private JedisPool jedisPool = null;

	private boolean isJedisEnable = false;

	private LjobScheduler ljobScheduler = null;

	private boolean isLjobServerOnline = false;

	private String ljobServerTime = null;

	private String groupName = null;

	private LjobSupporter ljobSupporter = null;

	public LjobSupporter getLjobSupporter() {
		return ljobSupporter;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getIp() {
		return ip;
	}

	public String getLjobServerTime() {
		return ljobServerTime;
	}

	public void setLjobServerTime(String ljobServerTime) {
		this.ljobServerTime = ljobServerTime;
	}

	public boolean isLjobServerOnline() {
		return isLjobServerOnline;
	}

	public void setLjobServerOnline(boolean isLjobServerOnline) {
		this.isLjobServerOnline = isLjobServerOnline;
	}

	public void setLjobScheduler(LjobScheduler ljobScheduler) {
		this.ljobScheduler = ljobScheduler;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
		this.testJedis();
	}

	private void testJedis() {
		if (null == jedisPool) {
			isJedisEnable = false;
			return;
		}

		Jedis jedis = null;

		try {
			jedis = jedisPool.getResource();
			isJedisEnable = true;
		}
		catch (Exception e) {
			LOG.error("test redis exception：" + e.toString(), e);
			isJedisEnable = false;
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

	public boolean isDestroy() {
		return isDestroy;
	}

	public void destroy() {
		isDestroy = true;
	}

	public void shutdownHook(LjobTrigger ljobTrigger) {
		final LjobTrigger trigger = ljobTrigger;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			public void run() {
				trigger.destroy();

				try {
					LOG.info("start LjobTrigger shutdown...");
					Thread.sleep(10000);
					LOG.info("end LjobTrigger shutdown...");
				}
				catch (Exception e) {
					LOG.error(e.toString(), e);
				}
			}
		}));
	}

	public void init() throws Exception {
		// generate ip with random no
		Random random = new Random();
		ip = CommonUtil.getLocalIP() + "_" + random.nextInt(10) + random.nextInt(10) + random.nextInt(10) + random.nextInt(10);
		LOG.info("start LjobTrigger, ip: " + ip + ", groupName: " + groupName);

		if (null == ljobScheduler) {
			LOG.error("start LjobTrigger failed, LjobScheduler is null.");
			throw new Exception("start LjobTrigger failed, LjobScheduler is null.");
		}

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

		String errMsg = validateTrigger();
		if (null != errMsg) {
			LOG.error(errMsg);
			throw new Exception(errMsg);
		}

		// add shutdownHook
		this.shutdownHook(this);

		if (LjobProtocol.REDIS.protocol().equals(protocol)) {
			LOG.info("start RedisLjobServerGetter...");
			LjobServerGetter ljobServerGetter = new RedisLjobServerGetter(jedisPool);
			ljobServerGetter.setLjobTrigger(this);
			ljobServerGetter.setName("LjobServerGetter_" + dateTimeFormat.format(new Date()));
			ljobServerGetter.start();

			LOG.info("start RedisLjobServerTimeGetter...");
			LjobServerTimeGetter ljobServerTimeGetter = new RedisLjobServerTimeGetter(jedisPool);
			ljobServerTimeGetter.setLjobTrigger(this);
			ljobServerTimeGetter.setName("LjobServerTimeGetter_" + dateTimeFormat.format(new Date()));
			ljobServerTimeGetter.start();

			LOG.info("create RedisLjobSupporter...");
			ljobSupporter = new RedisLjobSupporter(jedisPool);
		}

		// get Ljob list
		List<Ljob> ljobList = ljobScheduler.getLjobList();
		if (null != ljobList && ljobList.size() > 0) {
			String ljobGroupName = null;
			LjobRunner ljobRunner = null;
			for (Ljob ljob : ljobList) {
				ljobGroupName = ljob.getGroupName();
				if (null != groupName && (null == ljobGroupName || "default".equals(ljobGroupName))) {
					ljob.setGroupName(groupName);
				}

				ljobRunner = ljob.getJobRunner();
				ljobRunner.setGroupName(ljob.getGroupName());
				ljobRunner.setJobName(ljob.getJobName());
				ljobRunner.setDistributed(ljob.isDistributed());
				ljobRunner.setLjobSupporter(ljobSupporter);

				if (LjobProtocol.REDIS.protocol().equals(protocol)) {
					LOG.info("start job[" + ip + " ** " + ljob.getGroupName() + " ** " + ljob.getJobName() + "] redis heartbeat sender...");
					LjobHeartbeatSender ljobHeartbeatSender = new RedisLjobHeartbeatSender(jedisPool);
					ljobHeartbeatSender.setLjobTrigger(this);
					ljobHeartbeatSender.setLjob(ljob);
					ljobHeartbeatSender.setName(ljob.getJobName() + "_RedisHeartbeatSender_" + dateTimeFormat.format(new Date()));
					ljobHeartbeatSender.start();

					if (ljob.isSupportCloseJob()) {
						LOG.info("start job[" + ip + " ** " + ljob.getGroupName() + " ** " + ljob.getJobName() + "] redis command receiver...");
						LjobCommandReceiver ljobCommandReceiver = new RedisLjobCommandReceiver(jedisPool);
						ljobCommandReceiver.setLjobTrigger(this);
						ljobCommandReceiver.setLjob(ljob);
						ljobCommandReceiver.setName(ljob.getJobName() + "_RedisLjobCommandReceiver_" + dateTimeFormat.format(new Date()));
						ljobCommandReceiver.start();
					}
				}

				LOG.info("start job[" + ip + " ** " + ljob.getGroupName() + " ** " + ljob.getJobName() + "] with cron expression["
						+ ljob.getJobCronExpression() + "]...");
				if (ljob.isAutoSchedule()) {
					if (this.addScheduleJob(ljob)) {
						ljob.setScheduling(true);
					}
					else {
						LOG.error("add schedule job failed, ljob: " + ljob.toString());
					}
				}
			}
		}
	}

	public boolean addScheduleJob(Ljob ljob) {
		try {
			return ljobScheduler.addScheduleJob(ljob);
		}
		catch (Exception e) {
			LOG.error("add schedule job exception, ljob: " + ljob.toString() + ", exception: " + e.toString(), e);
			return false;
		}
	}

	public boolean removeScheduleJob(Ljob ljob) {
		try {
			return ljobScheduler.removeScheduleJob(ljob);
		}
		catch (Exception e) {
			LOG.error("remove schedule job exception, ljob: " + ljob.toString() + ", exception: " + e.toString(), e);
			return false;
		}
	}

	public boolean changeCronExpression(Ljob ljob, String cronExpression) {
		try {
			return ljobScheduler.changeCronExpression(ljob, cronExpression);
		}
		catch (Exception e) {
			LOG.error("change cron expression exception, ljob: " + ljob.toString() + ", cronExpression: " + cronExpression + ", exception: "
					+ e.toString(), e);
			return false;
		}
	}

	private String validateTrigger() {
		if (protocol.equals(LjobProtocol.REDIS.protocol())) {
			if (!isJedisEnable) {
				return "start LjobTrigger failed, JedisPool is null.";
			}
		}

		return null;
	}
}
