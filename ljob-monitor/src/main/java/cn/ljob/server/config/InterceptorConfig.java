package cn.ljob.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import cn.ljob.server.interceptor.LoginInterceptor;
import redis.clients.jedis.JedisPool;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

	@Autowired
	private JedisPool jedisPool = null;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new LoginInterceptor(jedisPool)).addPathPatterns("/**");
	}
}
