package cn.ljob.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

@Configuration
public class RedisConfig {

	@Value("${redis.ip}")
	private String redisIP = null;

	@Value("${redis.port}")
	private Integer redisPort = null;

	@Value("${redis.password}")
	private String redisPassword = null;

	@Bean(name = "jedisPool")
	public JedisPool jedisPool() {
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(50);
		jedisPoolConfig.setMaxIdle(5);
		jedisPoolConfig.setTestOnBorrow(true);
		jedisPoolConfig.setTestWhileIdle(true);

		JedisPool jedisPool = null;
		if (null == redisPassword || "".equals(redisPassword.trim())) {
			jedisPool = new JedisPool(jedisPoolConfig, redisIP, redisPort, Protocol.DEFAULT_TIMEOUT);
		}
		else {
			jedisPool = new JedisPool(jedisPoolConfig, redisIP, redisPort, Protocol.DEFAULT_TIMEOUT, redisPassword);
		}

		return jedisPool;
	}
}