package cn.ljob.server.interceptor;

import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

public class LoginInterceptor implements HandlerInterceptor {

	private static final Logger LOG = LoggerFactory.getLogger(LoginInterceptor.class);

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
		
		LOG.info(currentUrl);

		HttpSession session = request.getSession();
		Long loginTime = (Long) session.getAttribute("loginTime");
		if (null == loginTime) {
			response.sendRedirect("/");
			return false;
		}

		long currentTime = new Date().getTime();
		if (currentTime - loginTime > 900000l) {
			LOG.warn("login session timeout.");
			response.sendRedirect("/");
			return false;
		}

		session.setAttribute("loginTime", currentTime);
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
