package cn.ljob.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.ljob.LjobTrigger;

/**
 * LjobServerGetter
 *
 * @author darren
 * @since 2018年4月25日 上午11:44:30
 */
public abstract class LjobServerGetter extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobServerGetter.class);

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
			if (null != this.getLjobServer()) {
				ljobTrigger.setLjobServerOnline(true);
			}
			else {
				ljobTrigger.setLjobServerOnline(false);
			}

			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				LOG.error(e.toString(), e);
			}
		}
	}

	public abstract String getLjobServer();
}
