package cn.ljob.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.ljob.LjobMonitor;
import redis.clients.jedis.JedisPool;

@Configuration
public class LjobConfig {

	@Autowired
	private JedisPool jedisPool;

	@Bean(name = "ljobMonitor", initMethod = "init", destroyMethod = "destroy")
	public LjobMonitor ljobMonitor() {
		LjobMonitor ljobMonitor = new LjobMonitor();
		ljobMonitor.setJedisPool(jedisPool);
		return ljobMonitor;
	}
}
