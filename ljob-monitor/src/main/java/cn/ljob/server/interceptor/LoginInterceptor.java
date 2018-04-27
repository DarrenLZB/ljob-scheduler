package cn.ljob.server.interceptor;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class LoginInterceptor implements HandlerInterceptor {

	private static final Logger LOG = LoggerFactory.getLogger(LoginInterceptor.class);

	private JedisPool jedisPool = null;

	public LoginInterceptor(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (handler != null) {
			if (handler instanceof ResourceHttpRequestHandler) {
				return true;
			}
		}

		String currentUrl = request.getServletPath().toLowerCase(Locale.getDefault());
		if (currentUrl.equals("/") || currentUrl.startsWith("/login") || currentUrl.startsWith("/error")) {
			return true;
		}

		LOG.debug(currentUrl);

		HttpSession session = request.getSession();
		String loginToken = (String) session.getAttribute("loginToken");
		String currentUser = (String) session.getAttribute("currentUser");
		Long loginTime = (Long) session.getAttribute("loginTime");
		if (null == loginToken || null == currentUser || null == loginTime) {
			response.sendRedirect("/");
			return false;
		}

		Jedis jedis = null;
		String cacheUser = null;
		try {
			jedis = this.jedisPool.getResource();
			cacheUser = jedis.get(loginToken);
			if (!currentUser.equals(cacheUser)) {
				response.sendRedirect("/");
				return false;
			}

			jedis.setex(loginToken, 900, currentUser);
		}
		catch (Exception e) {
			LOG.error("validate login token exception：" + e.toString(), e);
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

		return true;
	}

	@Override
	public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView)
			throws Exception {

	}

	@Override
	public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e)
			throws Exception {

	}
}
