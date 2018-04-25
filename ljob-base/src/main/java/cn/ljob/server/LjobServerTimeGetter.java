package cn.ljob.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.ljob.LjobTrigger;

/**
 * LjobServerTimeGetter
 *
 * @author darren
 * @since 2018年4月25日 上午11:44:38
 */
public abstract class LjobServerTimeGetter extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobServerTimeGetter.class);

	private LjobTrigger ljobTrigger;

	public LjobTrigger getLjobTrigger() {
		return ljobTrigger;
	}

	public void setLjobTrigger(LjobTrigger ljobTrigger) {
		this.ljobTrigger = ljobTrigger;
	}

	@Override
	public void run() {
		while (!ljobTrigger.isDestroy()) {
			if (ljobTrigger.isLjobServerOnline()) {
				ljobTrigger.setLjobServerTime(this.getLjobServerTime());
			}

			try {
				Thread.sleep(500);
			}
			catch (Exception e) {
				LOG.error(e.toString(), e);
			}
		}
	}

	public abstract String getLjobServerTime();
}
