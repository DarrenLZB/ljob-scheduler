package cn.ljob.schedule;

import java.util.List;

import cn.ljob.Ljob;

/**
 * LjobScheduler
 *
 * @author darren
 * @since 2018年4月25日 上午11:44:24
 */
public interface LjobScheduler {

	List<Ljob> getLjobList();

	boolean changeCronExpression(Ljob ljob, String cronExpression);

	boolean addScheduleJob(Ljob ljob);

	boolean removeScheduleJob(Ljob ljob);
}
