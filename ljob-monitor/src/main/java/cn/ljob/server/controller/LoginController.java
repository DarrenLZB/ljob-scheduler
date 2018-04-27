package cn.ljob.server.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import cn.ljob.util.UUIDGenerator;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Controller
public class LoginController {

	private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

	@Value("${login.user.name:admin}")
	private String userName = null;

	@Value("${login.user.password:admin123}")
	private String userPassword = null;

	@Autowired
	private JedisPool jedisPool = null;

	@RequestMapping(value = "/")
	public ModelAndView toLoginaA() {
		return new ModelAndView("common/login");
	}

	@RequestMapping(value = "/login/to_login")
	public ModelAndView toLoginB() {
		return new ModelAndView("common/login");
	}

	@RequestMapping(value = "/login/do_login")
	public ModelAndView doLogin(String loginName, String password, HttpServletRequest request) {
		LOG.info("Login, loginName: " + loginName + ", password: " + password);
		ModelAndView mav = new ModelAndView("common/login");
		HttpSession session = request.getSession();

		if (!userName.equals(loginName) || !userPassword.equals(password)) {
			mav.addObject("loginName", loginName);
			mav.addObject("msg", "user not found or error password");
			request.getSession(true);
			return mav;
		}

		String loginToken = "token" + UUIDGenerator.generate();
		Jedis jedis = null;
		try {
			jedis = this.jedisPool.getResource();
			jedis.setex(loginToken, 900, loginName);
		}
		catch (Exception e) {
			LOG.error("set login token exception：" + e.toString(), e);
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

		mav = new ModelAndView("common/index");
		mav.addObject("currentUser", loginName);
		session.setAttribute("currentUser", loginName);
		session.setAttribute("loginTime", new Date().getTime());
		session.setAttribute("loginToken", loginToken);
		return mav;
	}

	@RequestMapping(value = "/login/login_out")
	public ModelAndView logout(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String loginToken = (String) session.getAttribute("loginToken");
		if (null != loginToken) {
			Jedis jedis = null;
			try {
				jedis = this.jedisPool.getResource();
				jedis.del(loginToken);
			}
			catch (Exception e) {
				LOG.error("delete login token exception：" + e.toString(), e);
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

		request.getSession(true);
		session = request.getSession();
		return new ModelAndView("common/login");
	}
}
