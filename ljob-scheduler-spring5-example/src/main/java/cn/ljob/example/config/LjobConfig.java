package cn.ljob.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.ljob.LjobTrigger;
import cn.ljob.schedule.spring.SpringLjobScheduler;
import redis.clients.jedis.JedisPool;

@Configuration
public class LjobConfig {

	@Value("${ljob.group.name:default}")
	private String groupName = null;

	@Autowired
	private JedisPool jedisPool;

	@Autowired
	private SpringLjobScheduler springLjobScheduler;

	@Bean(name = "springLjobScheduler")
	public SpringLjobScheduler springLjobScheduler() {
		return new SpringLjobScheduler();
	}

	@Bean(name = "ljobTrigger", initMethod = "init", destroyMethod = "destroy")
	public LjobTrigger ljobTrigger() {
		LjobTrigger ljobTrigger = new LjobTrigger();
		ljobTrigger.setJedisPool(jedisPool);
		ljobTrigger.setLjobScheduler(springLjobScheduler);
		ljobTrigger.setGroupName(groupName);
		return ljobTrigger;
	}
}
