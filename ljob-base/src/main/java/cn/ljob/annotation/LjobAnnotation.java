package cn.ljob.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ljob Annotation
 * 
 * @author lin.zb
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LjobAnnotation {

	/**
	 * group name
	 * 
	 * @return String
	 */
	String groupName() default "default";

	/**
	 * is support distributed, default false
	 * 
	 * @return boolean
	 */
	boolean distributed() default false;

	/**
	 * is support instant run request, default false
	 * 
	 * @return boolean
	 */
	boolean supportInstantRunReq() default false;

	/**
	 * is support close job, default false
	 * if true, can control by ljob-server, include: 
	 * 1. add schedule
	 * 2. remove schedule
	 * 3. change cron-expression
	 * 
	 * @return boolean
	 */
	boolean supportCloseJob() default true;

	/**
	 * job description
	 * 
	 * @return String
	 */
	String description() default "";

	/**
	 * is support custom run request, default false
	 * 
	 * @return boolean
	 */
	boolean supportCustomRunReq() default false;
	
	/**
	 * auto schedult, default true
	 * if ture, job will schedule when the program startup
	 * if false, job will not schedule when the program startup, can control by ljob-server
	 * 
	 * @return boolean
	 */
	boolean autoSchedule() default true;

	/**
	 * job cron-expression (documented on the {@link org.quartz.CronExpression} class)
	 * 
	 * @return String
	 */
	String cron();
}
