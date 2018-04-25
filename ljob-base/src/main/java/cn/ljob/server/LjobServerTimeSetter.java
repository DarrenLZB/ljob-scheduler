package cn.ljob.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.ljob.LjobMonitor;

/**
 * LjobServerTimeSetter
 *
 * @author darren
 * @since 2018年4月25日 上午11:44:50
 */
public abstract class LjobServerTimeSetter extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobServerTimeSetter.class);

	private LjobMonitor ljobMonitor;

	public void setLjobMonitor(LjobMonitor ljobMonitor) {
		this.ljobMonitor = ljobMonitor;
	}

	public LjobMonitor getLjobMonitor() {
		return ljobMonitor;
	}

	@Override
	public void run() {
		while (!ljobMonitor.isDestroy()) {
			if (ljobMonitor.isMainServer()) {
				this.cacheServerTime(ljobMonitor.getLjobServerTime());
			}

			try {
				Thread.sleep(500);
			}
			catch (Exception e) {
				LOG.error(e.toString(), e);
			}
		}
	}

	public abstract boolean cacheServerTime(String currentServerTime);
}
