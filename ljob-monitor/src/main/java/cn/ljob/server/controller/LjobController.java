package cn.ljob.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import cn.ljob.LjobMonitor;
import cn.ljob.command.LjobCommand;
import cn.ljob.command.LjobCommandConstant;
import cn.ljob.heartbeat.LjobHeartbeat;
import cn.ljob.heartbeat.LjobHeartbeatReceiver;
import cn.ljob.util.UUIDGenerator;

@Controller
@RequestMapping("/ljob")
public class LjobController {

	private static final Logger LOG = LoggerFactory.getLogger(LjobController.class);

	@Autowired
	private LjobMonitor ljobMonitor;

	@RequestMapping(value = "/to_ljob_index")
	public ModelAndView toLjobIndex() {
		return new ModelAndView("ljob/ljob_index");
	}

	@RequestMapping(value = "/get_online_ljobs")
	public ModelAndView getOnlineLjobs(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView("ljob/ljob_data");
		List<LjobHeartbeat> ljobList = new ArrayList<LjobHeartbeat>();
		Map<String, Map<String, List<LjobHeartbeat>>> ljobMap = new HashMap<String, Map<String, List<LjobHeartbeat>>>();
		Map<String, List<LjobHeartbeat>> groupLjobMap = null;
		List<LjobHeartbeat> groupLjobList = null;

		Map<String, List<LjobHeartbeat>> onlineLjobMap = LjobHeartbeatReceiver.getOnlineLjobMap();
		if (null != onlineLjobMap) {
			Set<String> keySet = onlineLjobMap.keySet();
			if (null != keySet) {
				List<LjobHeartbeat> tmpLjobList = null;
				String groupName = null;
				String ljobName = null;
				for (String key : keySet) {
					tmpLjobList = onlineLjobMap.get(key);
					for (LjobHeartbeat ljobHeartbeat : tmpLjobList) {
						if (null != ljobHeartbeat && ljobHeartbeat.isValideHeartbeat(ljobMonitor.getLjobServerTime())) {
							groupName = ljobHeartbeat.getGroupName();
							ljobName = ljobHeartbeat.getJobName();

							groupLjobMap = ljobMap.get(groupName);
							if (null == groupLjobMap) {
								groupLjobMap = new HashMap<String, List<LjobHeartbeat>>();
							}

							groupLjobList = groupLjobMap.get(ljobName);
							if (null == groupLjobList) {
								groupLjobList = new ArrayList<LjobHeartbeat>();
							}

							groupLjobList.add(ljobHeartbeat);
							groupLjobMap.put(ljobName, groupLjobList);
							ljobMap.put(groupName, groupLjobMap);
						}
					}
				}
			}
		}

		Set<String> keySet = ljobMap.keySet();
		Set<String> ljobNameSet = null;
		if (null != keySet && keySet.size() > 0) {
			for (String groupName : keySet) {
				groupLjobMap = ljobMap.get(groupName);
				if (null != groupLjobMap) {
					ljobNameSet = groupLjobMap.keySet();
					if (null != ljobNameSet && ljobNameSet.size() > 0) {
						for (String ljobName : ljobNameSet) {
							groupLjobList = groupLjobMap.get(ljobName);
							if (null != groupLjobList) {
								for (LjobHeartbeat ljobHeartbeat : groupLjobList) {
									ljobList.add(ljobHeartbeat);
								}
							}
						}
					}
				}
			}
		}

		mav.addObject("jobList", ljobList);
		mav.addObject("serverStatus", LjobHeartbeatReceiver.isMainServer() ? "Main Server" : "Slave Server");
		return mav;
	}

	@RequestMapping(value = "/to_change_cron")
	public ModelAndView toChangeCron(HttpServletRequest request, String groupName, String jobName, String ip) {
		ModelAndView mav = new ModelAndView("ljob/ljob_change_cron");
		LjobHeartbeat ljobHeartbeat = LjobHeartbeatReceiver.getOnlineLjob(groupName, jobName, ip);
		mav.addObject("job", ljobHeartbeat);
		return mav;
	}
	
	@RequestMapping(value = "/to_custom_run")
	public ModelAndView toCustomJob(HttpServletRequest request, String groupName, String jobName, String ip) {
		ModelAndView mav = new ModelAndView("ljob/ljob_custom_run");
		LjobHeartbeat ljobHeartbeat = LjobHeartbeatReceiver.getOnlineLjob(groupName, jobName, ip);
		mav.addObject("job", ljobHeartbeat);
		return mav;
	}

	@RequestMapping(value = "/send_ljob_command")
	@ResponseBody
	public String sentLjobCommand(HttpServletRequest request, String ip, String groupName, String jobName, String command, String cron,
			String customParams) {
		LOG.info(
				"send ljob command, ip: " + ip + ", groupName: " + groupName + ", jobName: " + jobName + ", command: " + command + ", cron: " + cron);
		LjobHeartbeat ljobHeartbeat = LjobHeartbeatReceiver.getOnlineLjob(groupName, jobName, ip);
		if (null != ljobHeartbeat && ljobHeartbeat.isValideHeartbeat(ljobMonitor.getLjobServerTime())) {
			LjobCommand ljobCommand = new LjobCommand();
			ljobCommand.setRequestID(UUIDGenerator.generate());
			ljobCommand.setRequestIP(getIpAddr(request));
			ljobCommand.setRequestUser("ljob-server");
			ljobCommand.setRequestTime(ljobMonitor.getLjobServerTime());
			ljobCommand.setGroupName(groupName);
			ljobCommand.setJobName(jobName);
			ljobCommand.setTargetIP(ip);

			if ("0".equals(command)) {
				ljobCommand.setCommand(LjobCommandConstant.REMOVE_SCHEDULE_COMMAND);
			}
			else if ("1".equals(command)) {
				ljobCommand.setCommand(LjobCommandConstant.ADD_SCHEDULE_COMMAND);
			}
			else if ("2".equals(command)) {
				if (null == cron || "".equals(cron.trim())) {
					LOG.error("change ljob cron expression failed, cron is null.");
					return "err=cron not found";
				}

				ljobCommand.setCommand(LjobCommandConstant.CHANGE_SCHEDULE_CRON_COMMAND_PREFIX + cron.trim());
			}
			else if ("3".equals(command)) {
				ljobCommand.setCommand(LjobCommandConstant.RUN_JOB_COMMAND);
			}
			else if ("4".equals(command)) {
				if (null != customParams) {
					customParams = customParams.trim();
				}
				else {
					customParams = "";
				}

				ljobCommand.setCommand(LjobCommandConstant.RUN_CUSTOM_JOB_COMMAND_PREFIX + customParams);
			}
			else {
				LOG.error("send ljob command failed, command not found.");
				return "err=command not found";
			}

			if (ljobMonitor.sendLjobCommand(ljobCommand)) {
				LOG.info("send ljob command success. ljobCommand: " + ljobCommand.toString());
				return "success";
			}
			else {
				LOG.error("send ljob command failed. ljobCommand: " + ljobCommand.toString());
				return "err=send command failed";
			}
		}
		else {
			LOG.error("send ljob command failed, job not found.");
			return "err=job not found";
		}
	}

	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		return ip;
	}
}