package cn.ljob;

/**
 * LjobProtocol
 *
 * @author darren
 * @since 2018年4月25日 上午11:42:43
 */
public enum LjobProtocol {

	REDIS("redis");

	private String protocol;

	LjobProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String protocol() {
		return this.protocol;
	}

	public static boolean isProtocol(String protocol) {
		LjobProtocol[] ljobProtocols = LjobProtocol.values();
		for (LjobProtocol ljobProtocol : ljobProtocols) {
			if (protocol.equals(ljobProtocol.protocol)) {
				return true;
			}
		}

		return false;
	}
}