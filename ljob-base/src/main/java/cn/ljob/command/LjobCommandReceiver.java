package cn.ljob.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import cn.ljob.Ljob;
import cn.ljob.LjobTrigger;

/**
 * LjobCommandReceiver
 *
 * @author darren
 * @since 2018年4月25日 上午11:43:06
 */
public abstract class LjobCommandReceiver extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobCommandReceiver.class);

	private LjobTrigger ljobTrigger = null;

	private Ljob ljob = null;

	public void setLjobTrigger(LjobTrigger ljobTrigger) {
		this.ljobTrigger = ljobTrigger;
	}

	public void setLjob(Ljob ljob) {
		this.ljob = ljob;
	}

	public LjobTrigger getLjobTrigger() {
		return ljobTrigger;
	}

	public Ljob getLjob() {
		return ljob;
	}

	@Override
	public void run() {
		LjobCommand ljobCommand = null;
		String command = null;
		while (!ljobTrigger.isDestroy()) {
			ljobCommand = this.getLjobCommand();
			if (null != ljobCommand) {
				command = ljobCommand.getCommand();
				if (null != command && ljobCommand.isValideRequest(ljobTrigger.getLjobServerTime())) {
					LOG.info("receive ljob command: " + ljobCommand.toString());
					if (LjobCommandConstant.ADD_SCHEDULE_COMMAND.equals(command)) {
						if (ljob.isScheduling()) {
							LOG.info("ljob is scheduling, do nothing. ljobCommand: " + ljobCommand.toString());
						}
						else {
							boolean result = ljobTrigger.addScheduleJob(ljob);
							LOG.info("add schedult ljob end, result: " + result + ", ljobCommand: " + ljobCommand.toString());
							if (result) {
								ljob.setScheduling(true);
							}
						}
					}
					else if (LjobCommandConstant.REMOVE_SCHEDULE_COMMAND.equals(command)) {
						if (!ljob.isScheduling()) {
							LOG.info("ljob not in scheduled, do nothing. ljobCommand: " + ljobCommand.toString());
						}
						else {
							boolean result = ljobTrigger.removeScheduleJob(ljob);
							LOG.info("remove schedult ljob end, result: " + result + ", ljobCommand: " + ljobCommand.toString());
							if (result) {
								ljob.setScheduling(false);
							}
						}
					}
					else if (command.startsWith(LjobCommandConstant.CHANGE_SCHEDULE_CRON_COMMAND_PREFIX)) {
						String oldCronExpression = ljob.getJobCronExpression();
						String newCronExpression = command.substring(LjobCommandConstant.CHANGE_SCHEDULE_CRON_COMMAND_PREFIX.length());
						if (null == newCronExpression || "".equals(newCronExpression.trim())) {
							LOG.error("change ljob schedule cron expression failed, new cron expression is null, ljobCommand: "
									+ ljobCommand.toString());
						}
						else if (oldCronExpression.equals(newCronExpression)) {
							LOG.info("ljob schedule cron expression has no change, do nothing. oldCronExpression: " + oldCronExpression
									+ ", ljobCommand: " + ljobCommand.toString());
						}
						else {
							boolean isScheduling = ljob.isScheduling();
							boolean nowScheduling = isScheduling;
							if (isScheduling) {
								boolean result = ljobTrigger.removeScheduleJob(ljob);
								LOG.info("change ljob schedule cron expression, remove schedult ljob end, result: " + result + ", ljobCommand: "
										+ ljobCommand.toString());
								if (result) {
									ljob.setScheduling(false);
									nowScheduling = false;
								}
								else {
									LOG.error("change ljob schedule cron expression failed, remove schedule job failed, ljobCommand: "
											+ ljobCommand.toString());
								}
							}

							if (!nowScheduling) {
								boolean result = ljobTrigger.changeCronExpression(ljob, newCronExpression);
								LOG.info("change ljob schedule cron expression, oldCronExpression: " + oldCronExpression + ", newCronExpression: "
										+ newCronExpression + ", ljobCommand: " + ljobCommand.toString() + ", result: " + result);
								if (!result) {
									result = ljobTrigger.changeCronExpression(ljob, oldCronExpression);
									LOG.info(
											"restore ljob schedule cron expression, oldCronExpression: " + oldCronExpression + ", newCronExpression: "
													+ newCronExpression + ", ljobCommand: " + ljobCommand.toString() + ", result: " + result);
								}

								if (isScheduling) {
									result = ljobTrigger.addScheduleJob(ljob);
									LOG.info("change ljob schedule cron expression, add schedult ljob end, result: " + result + ", ljobCommand: "
											+ ljobCommand.toString());
									if (result) {
										ljob.setScheduling(true);
									}
									else {
										result = ljobTrigger.changeCronExpression(ljob, oldCronExpression);
										LOG.info("restore ljob schedule cron expression, oldCronExpression: " + oldCronExpression
												+ ", newCronExpression: " + newCronExpression + ", ljobCommand: " + ljobCommand.toString()
												+ ", result: " + result);

										if (result) {
											result = ljobTrigger.addScheduleJob(ljob);
											LOG.info("restore ljob schedule cron expression, add schedult ljob end, result: " + result
													+ ", ljobCommand: " + ljobCommand.toString());

											if (result) {
												ljob.setScheduling(true);
											}
										}
									}
								}
							}
						}
					}
					else if (LjobCommandConstant.RUN_JOB_COMMAND.equals(command)) {
						LOG.info("start run lob command, ljobCommand: " + ljobCommand.toString());
						ljob.getJobRunner().runJob();
						LOG.info("end run lob command, ljobCommand: " + ljobCommand.toString());
					}
					else if (command.startsWith(LjobCommandConstant.RUN_CUSTOM_JOB_COMMAND_PREFIX)) {
						JSONObject customParams = new JSONObject();
						String customJobStringParams = command.substring(LjobCommandConstant.RUN_CUSTOM_JOB_COMMAND_PREFIX.length());
						if (null != customJobStringParams && !"".equals(customJobStringParams.trim())) {
							String[] paramArray = customJobStringParams.trim().split(",");
							String[] kv = null;
							String key = null;
							String value = null;
							for (String param : paramArray) {
								if (null != param && !"".equals(param.trim())) {
									kv = param.trim().split(":");
									if (null != kv && kv.length == 2) {
										key = kv[0];
										value = kv[1];
										if (null != key && !"".equals(key.trim()) && null != value && !"".equals(value.trim())) {
											customParams.put(key, value);
										}
									}
								}
							}
						}

						LOG.info("start run custom lob command, ljobCommand: " + ljobCommand.toString() + ", customParams: "
								+ customParams.toJSONString());
						ljob.getJobRunner().runJob(customParams);
						LOG.info("end run custom lob command, ljobCommand: " + ljobCommand.toString());
					}
					else {
						LOG.error("unknown ljob command: " + ljobCommand.toString());
					}
				}

				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					LOG.error(e.toString(), e);
				}
			}
			else {
				try {
					Thread.sleep(3000);
				}
				catch (InterruptedException e) {
					LOG.error(e.toString(), e);
				}
			}
		}
	}

	public abstract LjobCommand getLjobCommand();
}
