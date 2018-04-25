package cn.ljob.support;

/**
 * LjobSupporter
 *
 * @author darren
 * @since 2018年4月25日 上午11:45:22
 */
public interface LjobSupporter {

	boolean getStandAloneJobLock(String groupName, String jobName, int lockSeconds);

	boolean releaseStandAloneJobLock(String groupName, String jobName);

	boolean updateStandAloneJobRuntime(String groupName, String jobName, long currentTime);

	Long getStandAloneJobRuntime(String groupName, String jobName);
}
