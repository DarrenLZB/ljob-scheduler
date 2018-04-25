package cn.ljob.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.ljob.LjobMonitor;

/**
 * LjobServer Checker
 *
 * @author darren
 * @since 2018年3月27日 下午3:16:10
 */
public abstract class LjobServerSetter extends Thread {

	private static final Logger LOG = LoggerFactory.getLogger(LjobServerSetter.class);

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
			if (this.checkAndSetMainServer()) {
				ljobMonitor.setMainServer(true);
			}
			else {
				ljobMonitor.setMainServer(false);
			}

			try {
				Thread.sleep(5000);
			}
			catch (Exception e) {
				LOG.error(e.toString(), e);
			}
		}
	}

	public abstract boolean checkAndSetMainServer();
}
