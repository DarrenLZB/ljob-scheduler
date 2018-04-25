package cn.ljob.server.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

	private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

	@Value("${login.user.name:admin}")
	private String userName = null;

	@Value("${login.user.password:admin123}")
	private String userPassword = null;

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

		mav = new ModelAndView("common/index");
		mav.addObject("currentUser", loginName);
		session.setAttribute("currentUser", loginName);
		session.setAttribute("loginTime", new Date().getTime());
		return mav;
	}

	@RequestMapping(value = "/login/login_out")
	public ModelAndView logout(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String currentUser = (String) session.getAttribute("currentUser");

		request.getSession(true);
		session = request.getSession();
		session.setAttribute("logoutUser", currentUser);
		return new ModelAndView("common/login");
	}
}
