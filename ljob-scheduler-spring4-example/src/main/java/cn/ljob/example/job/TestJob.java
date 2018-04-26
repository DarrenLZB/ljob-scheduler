package cn.ljob.example.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import cn.ljob.LjobRunner;
import cn.ljob.annotation.LjobAnnotation;

@Component
@LjobAnnotation(cron = "${test.job.cron}", distributed = false, supportCloseJob = true, description = "test job")
public class TestJob extends LjobRunner {

	private static final Logger LOG = LoggerFactory.getLogger(TestJob.class);

	@Override
	public void jobDetail(JSONObject customParams) {
	}

	@Override
	public void jobDetail() {
		LOG.info("TestJob start ++++++++++++++++++++");
		try {
			Thread.sleep(2000);
		}
		catch (Exception e) {
			LOG.error(e.toString(), e);
		}
		LOG.info("TestJob end ++++++++++++++++++++");
	}

}
