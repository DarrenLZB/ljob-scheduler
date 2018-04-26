package cn.ljob.schedule.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.RemoteScheduler;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.quartz.AdaptableJobFactory;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.scheduling.quartz.LocalTaskExecutorThreadPool;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.ResourceLoaderClassLoadHelper;
import org.springframework.scheduling.quartz.SchedulerContextAware;
import org.springframework.util.CollectionUtils;

import cn.ljob.Ljob;
import cn.ljob.LjobRunner;
import cn.ljob.annotation.LjobAnnotation;
import cn.ljob.schedule.LjobScheduler;

import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * SpringLjobScheduler(Spring 5.0.+)
 * 
 * @author lin.zb
 */
public class SpringLjobScheduler extends SpringLjobSchedulerAccessor
		implements FactoryBean<Scheduler>, BeanNameAware, ApplicationContextAware, InitializingBean, DisposableBean, SmartLifecycle, LjobScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(SpringLjobScheduler.class);

	private boolean isInit = false;

	private List<Ljob> ljobList = new ArrayList<Ljob>();

	public void setLjobList(List<Ljob> ljobList) {
		this.ljobList = ljobList;
	}

	@Override
	public List<Ljob> getLjobList() {
		if (!isInit) {
			try {
				this.initLjobList();
			}
			catch (Exception e) {
				LOG.error(e.toString(), e);
			}
			isInit = true;
		}

		return ljobList;
	}

	@Override
	public boolean changeCronExpression(Ljob ljob, String cronExpression) {
		CronScheduleBuilder cronScheduleBuilder = null;
		CronTrigger oldCronTrigger = null;
		CronTrigger newCronTrigger = null;
		try {
			cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
			oldCronTrigger = (CronTrigger) ljob.getTargetTrigger();
			newCronTrigger = oldCronTrigger.getTriggerBuilder().withSchedule(cronScheduleBuilder).build();
			ljob.setJobCronExpression(cronExpression);
			ljob.setTargetTrigger(newCronTrigger);
			return true;
		}
		catch (Exception e) {
			LOG.error("change cron expression exception, ljob: " + ljob.toString() + ", cronExpression: " + cronExpression + ", exception: "
					+ e.toString(), e);
			return false;
		}
	}

	@Override
	public boolean addScheduleJob(Ljob ljob) {
		boolean result = false;
		try {
			result = this.addTriggerToScheduler(ljob.getTargetTrigger());
			LOG.info("add schedule job: " + ljob.getJobName() + ", result: " + result);
			return result;
		}
		catch (SchedulerException e) {
			LOG.error("add schedule job failed, ljob: " + ljob.getJobName() + ", exception: " + e.toString(), e);
			return false;
		}
	}

	@Override
	public boolean removeScheduleJob(Ljob ljob) {
		boolean result = false;
		try {
			result = this.removeScheduleJob(ljob.getTargetTrigger());
			LOG.info("remove schedule job: " + ljob.getJobName() + ", result: " + result);
			return result;
		}
		catch (SchedulerException e) {
			LOG.error("remove schedule job failed, ljob: " + ljob.getJobName() + ", exception: " + e.toString(), e);
			return false;
		}
	}

	private void initLjobList() throws Exception {
		Map<String, Object> ljobBeanMap = applicationContext.getBeansWithAnnotation(LjobAnnotation.class);
		if (null != ljobBeanMap) {
			Set<String> keySet = ljobBeanMap.keySet();
			if (null != keySet) {
				Object targetObject = null;
				LjobAnnotation ljobAnnotation = null;
				boolean isDistributed = false;
				boolean isSupportInstantRunReq = false;
				boolean isSupportCustomRunReq = false;
				boolean isSupportCloseJob = false;
				String description = null;
				String targetMethod = "runJob";
				String ljobCron = null;
				MethodInvokingJobDetailFactoryBean methodInvokingJobDetailFactoryBean = null;
				JobDetail jobDetail = null;
				CronTriggerFactoryBean cronTriggerFactoryBean = null;
				Ljob ljob = null;
				String ljobCronKey = null;

				// build ljob
				for (String key : keySet) {
					targetObject = ljobBeanMap.get(key);
					ljobAnnotation = targetObject.getClass().getAnnotation(LjobAnnotation.class);
					isDistributed = ljobAnnotation.distributed();
					isSupportInstantRunReq = ljobAnnotation.supportInstantRunReq();
					isSupportCustomRunReq = ljobAnnotation.supportCustomRunReq();
					isSupportCloseJob = ljobAnnotation.supportCloseJob();
					description = ljobAnnotation.description();
					ljobCron = ljobAnnotation.cron();
					if (null != ljobCron && ljobCron.startsWith("${") && ljobCron.endsWith("}")) {
						ljobCronKey = ljobCron.substring(2, ljobCron.length() - 1);
						if (null != applicationContext.getEnvironment().getProperty(ljobCronKey)) {
							ljobCron = applicationContext.getEnvironment().getProperty(ljobCronKey);
						}
						else {
							LOG.error("could not load [" + targetObject.getClass().getName() + "] cron from properties: " + ljobCronKey);
						}
					}

					if (!(targetObject instanceof LjobRunner)) {
						destroy();
						LOG.error("schedule ljob failed, must extend class[cn.ljob.LjobRunner], jobName: " + targetObject.getClass().getName());
						throw new Exception(
								"schedule ljob failed, must extend class[cn.ljob.LjobRunner], jobName: " + targetObject.getClass().getName());
					}

					// build MethodInvokingJobDetailFactoryBean
					methodInvokingJobDetailFactoryBean = new MethodInvokingJobDetailFactoryBean();
					methodInvokingJobDetailFactoryBean.setTargetObject(targetObject);
					methodInvokingJobDetailFactoryBean.setTargetMethod(targetMethod);
					methodInvokingJobDetailFactoryBean.setName(targetObject.getClass().getName() + "FactoryBean");
					methodInvokingJobDetailFactoryBean.afterPropertiesSet();

					// build CronTriggerFactoryBean
					jobDetail = methodInvokingJobDetailFactoryBean.getObject();
					cronTriggerFactoryBean = new CronTriggerFactoryBean();
					cronTriggerFactoryBean.setJobDetail(jobDetail);
					cronTriggerFactoryBean.setCronExpression(ljobCron);
					cronTriggerFactoryBean.setName(targetObject.getClass().getName() + "TriggerFactoryBean");
					cronTriggerFactoryBean.afterPropertiesSet();

					// build Ljob
					ljob = new Ljob();
					ljob.setTargetTrigger(cronTriggerFactoryBean.getObject());
					ljob.setJobRunner((LjobRunner) targetObject);
					ljob.setGroupName(ljobAnnotation.groupName());
					ljob.setJobName(key);
					ljob.setJobCronExpression(ljobCron);
					ljob.setDescription(description);
					ljob.setDistributed(isDistributed);
					ljob.setSupportInstantRunReq(isSupportInstantRunReq);
					ljob.setSupportCustomRunReq(isSupportCustomRunReq);
					ljob.setSupportCloseJob(isSupportCloseJob);
					ljob.setAutoSchedule(ljobAnnotation.autoSchedule());
					ljobList.add(ljob);
				}
			}
		}
	}

	// *******************************************************************************
	// ** **
	// ** Code from org.springframework.scheduling.quartz.SchedulerFactoryBean *******
	// ** **
	// *******************************************************************************
	public static final String PROP_THREAD_COUNT = "org.quartz.threadPool.threadCount";

	public static final int DEFAULT_THREAD_COUNT = 10;

	private static final ThreadLocal<ResourceLoader> configTimeResourceLoaderHolder = new ThreadLocal<ResourceLoader>();

	private static final ThreadLocal<Executor> configTimeTaskExecutorHolder = new ThreadLocal<Executor>();

	private static final ThreadLocal<DataSource> configTimeDataSourceHolder = new ThreadLocal<DataSource>();

	private static final ThreadLocal<DataSource> configTimeNonTransactionalDataSourceHolder = new ThreadLocal<DataSource>();

	/**
	 * Return the {@link ResourceLoader} for the currently configured Quartz Scheduler, to be used by {@link ResourceLoaderClassLoadHelper}.
	 * <p>
	 * This instance will be set before initialization of the corresponding Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * 
	 * @see #setApplicationContext
	 * @see ResourceLoaderClassLoadHelper
	 */
	public static ResourceLoader getConfigTimeResourceLoader() {
		return configTimeResourceLoaderHolder.get();
	}

	/**
	 * Return the {@link Executor} for the currently configured Quartz Scheduler, to be used by {@link LocalTaskExecutorThreadPool}.
	 * <p>
	 * This instance will be set before initialization of the corresponding Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * 
	 * @since 2.0
	 * @see #setTaskExecutor
	 * @see LocalTaskExecutorThreadPool
	 */
	public static Executor getConfigTimeTaskExecutor() {
		return configTimeTaskExecutorHolder.get();
	}

	/**
	 * Return the {@link DataSource} for the currently configured Quartz Scheduler, to be used by {@link LocalDataSourceJobStore}.
	 * <p>
	 * This instance will be set before initialization of the corresponding Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * 
	 * @since 1.1
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	public static DataSource getConfigTimeDataSource() {
		return configTimeDataSourceHolder.get();
	}

	/**
	 * Return the non-transactional {@link DataSource} for the currently configured Quartz Scheduler, to be used by {@link LocalDataSourceJobStore}.
	 * <p>
	 * This instance will be set before initialization of the corresponding Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * 
	 * @since 1.1
	 * @see #setNonTransactionalDataSource
	 * @see LocalDataSourceJobStore
	 */
	public static DataSource getConfigTimeNonTransactionalDataSource() {
		return configTimeNonTransactionalDataSourceHolder.get();
	}

	private SchedulerFactory schedulerFactory;

	private Class<? extends SchedulerFactory> schedulerFactoryClass = StdSchedulerFactory.class;

	private String schedulerName;

	private Resource configLocation;

	private Properties quartzProperties;

	private Executor taskExecutor;

	private DataSource dataSource;

	private DataSource nonTransactionalDataSource;

	private Map<String, ?> schedulerContextMap;

	private ApplicationContext applicationContext;

	private String applicationContextSchedulerContextKey;

	private JobFactory jobFactory;

	private boolean jobFactorySet = false;

	private boolean autoStartup = true;

	private int startupDelay = 0;

	private int phase = Integer.MAX_VALUE;

	private boolean exposeSchedulerInRepository = false;

	private boolean waitForJobsToCompleteOnShutdown = false;

	private Scheduler scheduler;

	/**
	 * Set an external Quartz {@link SchedulerFactory} instance to use.
	 * <p>
	 * Default is an internal {@link StdSchedulerFactory} instance. If this method is called, it overrides any class specified through
	 * {@link #setSchedulerFactoryClass} as well as any settings specified through {@link #setConfigLocation}, {@link #setQuartzProperties},
	 * {@link #setTaskExecutor} or {@link #setDataSource}.
	 * <p>
	 * <b>NOTE:</b> With an externally provided {@code SchedulerFactory} instance, local settings such as {@link #setConfigLocation} or
	 * {@link #setQuartzProperties} will be ignored here in {@code SchedulerFactoryBean}, expecting the external {@code SchedulerFactory} instance to
	 * get initialized on its own.
	 * 
	 * @since 4.3.15
	 * @see #setSchedulerFactoryClass
	 */
	public void setSchedulerFactory(SchedulerFactory schedulerFactory) {
		this.schedulerFactory = schedulerFactory;
	}

	/**
	 * Set the Quartz {@link SchedulerFactory} implementation to use.
	 * <p>
	 * Default is the {@link StdSchedulerFactory} class, reading in the standard {@code quartz.properties} from {@code quartz.jar}. For applying
	 * custom Quartz properties, specify {@link #setConfigLocation "configLocation"} and/or {@link #setQuartzProperties "quartzProperties"} etc on
	 * this local {@code SchedulerFactoryBean} instance.
	 * 
	 * @see org.quartz.impl.StdSchedulerFactory
	 * @see #setConfigLocation
	 * @see #setQuartzProperties
	 * @see #setTaskExecutor
	 * @see #setDataSource
	 */
	public void setSchedulerFactoryClass(Class<? extends SchedulerFactory> schedulerFactoryClass) {
		this.schedulerFactoryClass = schedulerFactoryClass;
	}

	/**
	 * Set the name of the Scheduler to create via the SchedulerFactory.
	 * <p>
	 * If not specified, the bean name will be used as default scheduler name.
	 * 
	 * @see #setBeanName
	 * @see org.quartz.SchedulerFactory#getScheduler()
	 * @see org.quartz.SchedulerFactory#getScheduler(String)
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * Set the location of the Quartz properties config file, for example as classpath resource "classpath:quartz.properties".
	 * <p>
	 * Note: Can be omitted when all necessary properties are specified locally via this bean, or when relying on Quartz' default configuration.
	 * 
	 * @see #setQuartzProperties
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * Set Quartz properties, like "org.quartz.threadPool.class".
	 * <p>
	 * Can be used to override values in a Quartz properties config file, or to specify all necessary properties locally.
	 * 
	 * @see #setConfigLocation
	 */
	public void setQuartzProperties(Properties quartzProperties) {
		this.quartzProperties = quartzProperties;
	}

	/**
	 * Set a Spring-managed {@link Executor} to use as Quartz backend. Exposed as thread pool through the Quartz SPI.
	 * <p>
	 * Can be used to assign a local JDK ThreadPoolExecutor or a CommonJ WorkManager as Quartz backend, to avoid Quartz's manual thread creation.
	 * <p>
	 * By default, a Quartz SimpleThreadPool will be used, configured through the corresponding Quartz properties.
	 * 
	 * @since 2.0
	 * @see #setQuartzProperties
	 * @see LocalTaskExecutorThreadPool
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
	 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the default {@link DataSource} to be used by the Scheduler. If set, this will override corresponding settings in Quartz properties.
	 * <p>
	 * Note: If this is set, the Quartz settings should not define a job store "dataSource" to avoid meaningless double configuration.
	 * <p>
	 * A Spring-specific subclass of Quartz' JobStoreCMT will be used. It is therefore strongly recommended to perform all operations on the Scheduler
	 * within Spring-managed (or plain JTA) transactions. Else, database locking will not properly work and might even break (e.g. if trying to obtain
	 * a lock on Oracle without a transaction).
	 * <p>
	 * Supports both transactional and non-transactional DataSource access. With a non-XA DataSource and local Spring transactions, a single
	 * DataSource argument is sufficient. In case of an XA DataSource and global JTA transactions, SchedulerFactoryBean's "nonTransactionalDataSource"
	 * property should be set, passing in a non-XA DataSource that will not participate in global transactions.
	 * 
	 * @since 1.1
	 * @see #setNonTransactionalDataSource
	 * @see #setQuartzProperties
	 * @see #setTransactionManager
	 * @see LocalDataSourceJobStore
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Set the {@link DataSource} to be used <i>for non-transactional access</i>.
	 * <p>
	 * This is only necessary if the default DataSource is an XA DataSource that will always participate in transactions: A non-XA version of that
	 * DataSource should be specified as "nonTransactionalDataSource" in such a scenario.
	 * <p>
	 * This is not relevant with a local DataSource instance and Spring transactions. Specifying a single default DataSource as "dataSource" is
	 * sufficient there.
	 * 
	 * @since 1.1
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	public void setNonTransactionalDataSource(DataSource nonTransactionalDataSource) {
		this.nonTransactionalDataSource = nonTransactionalDataSource;
	}

	/**
	 * Register objects in the Scheduler context via a given Map. These objects will be available to any Job that runs in this Scheduler.
	 * <p>
	 * Note: When using persistent Jobs whose JobDetail will be kept in the database, do not put Spring-managed beans or an ApplicationContext
	 * reference into the JobDataMap but rather into the SchedulerContext.
	 * 
	 * @param schedulerContextAsMap Map with String keys and any objects as values (for example Spring-managed beans)
	 * @see JobDetailFactoryBean#setJobDataAsMap
	 */
	public void setSchedulerContextAsMap(Map<String, ?> schedulerContextAsMap) {
		this.schedulerContextMap = schedulerContextAsMap;
	}

	/**
	 * Set the key of an {@link ApplicationContext} reference to expose in the SchedulerContext, for example "applicationContext". Default is none.
	 * Only applicable when running in a Spring ApplicationContext.
	 * <p>
	 * Note: When using persistent Jobs whose JobDetail will be kept in the database, do not put an ApplicationContext reference into the JobDataMap
	 * but rather into the SchedulerContext.
	 * <p>
	 * In case of a QuartzJobBean, the reference will be applied to the Job instance as bean property. An "applicationContext" attribute will
	 * correspond to a "setApplicationContext" method in that scenario.
	 * <p>
	 * Note that BeanFactory callback interfaces like ApplicationContextAware are not automatically applied to Quartz Job instances, because Quartz
	 * itself is responsible for the lifecycle of its Jobs.
	 * 
	 * @see JobDetailFactoryBean#setApplicationContextJobDataKey
	 * @see org.springframework.context.ApplicationContext
	 */
	public void setApplicationContextSchedulerContextKey(String applicationContextSchedulerContextKey) {
		this.applicationContextSchedulerContextKey = applicationContextSchedulerContextKey;
	}

	/**
	 * Set the Quartz {@link JobFactory} to use for this Scheduler.
	 * <p>
	 * Default is Spring's {@link AdaptableJobFactory}, which supports {@link java.lang.Runnable} objects as well as standard Quartz
	 * {@link org.quartz.Job} instances. Note that this default only applies to a <i>local</i> Scheduler, not to a RemoteScheduler (where setting a
	 * custom JobFactory is not supported by Quartz).
	 * <p>
	 * Specify an instance of Spring's {@link SpringBeanJobFactory} here (typically as an inner bean definition) to automatically populate a job's
	 * bean properties from the specified job data map and scheduler context.
	 * 
	 * @since 2.0
	 * @see AdaptableJobFactory
	 * @see SpringBeanJobFactory
	 */
	public void setJobFactory(JobFactory jobFactory) {
		this.jobFactory = jobFactory;
		this.jobFactorySet = true;
	}

	/**
	 * Set whether to automatically start the scheduler after initialization.
	 * <p>
	 * Default is "true"; set this to "false" to allow for manual startup.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Return whether this scheduler is configured for auto-startup. If "true", the scheduler will start after the context is refreshed and after the
	 * start delay, if any.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Specify the phase in which this scheduler should be started and stopped. The startup order proceeds from lowest to highest, and the shutdown
	 * order is the reverse of that. By default this value is {@code Integer.MAX_VALUE} meaning that this scheduler starts as late as possible and
	 * stops as soon as possible.
	 * 
	 * @since 3.0
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Return the phase in which this scheduler will be started and stopped.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Set the number of seconds to wait after initialization before starting the scheduler asynchronously. Default is 0, meaning immediate
	 * synchronous startup on initialization of this bean.
	 * <p>
	 * Setting this to 10 or 20 seconds makes sense if no jobs should be run before the entire application has started up.
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Set whether to expose the Spring-managed {@link Scheduler} instance in the Quartz {@link SchedulerRepository}. Default is "false", since the
	 * Spring-managed Scheduler is usually exclusively intended for access within the Spring context.
	 * <p>
	 * Switch this flag to "true" in order to expose the Scheduler globally. This is not recommended unless you have an existing Spring application
	 * that relies on this behavior. Note that such global exposure was the accidental default in earlier Spring versions; this has been fixed as of
	 * Spring 2.5.6.
	 */
	public void setExposeSchedulerInRepository(boolean exposeSchedulerInRepository) {
		this.exposeSchedulerInRepository = exposeSchedulerInRepository;
	}

	/**
	 * Set whether to wait for running jobs to complete on shutdown.
	 * <p>
	 * Default is "false". Switch this to "true" if you prefer fully completed jobs at the expense of a longer shutdown phase.
	 * 
	 * @see org.quartz.Scheduler#shutdown(boolean)
	 */
	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	@Override
	public void setBeanName(String name) {
		if (this.schedulerName == null) {
			this.schedulerName = name;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	// ---------------------------------------------------------------------
	// Implementation of InitializingBean interface
	// ---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.dataSource == null && this.nonTransactionalDataSource != null) {
			this.dataSource = this.nonTransactionalDataSource;
		}

		if (this.applicationContext != null && this.resourceLoader == null) {
			this.resourceLoader = this.applicationContext;
		}

		// Initialize the SchedulerFactory instance...
		SchedulerFactory schedulerFactory = prepareSchedulerFactory();

		if (this.resourceLoader != null) {
			// Make given ResourceLoader available for SchedulerFactory configuration.
			configTimeResourceLoaderHolder.set(this.resourceLoader);
		}
		if (this.taskExecutor != null) {
			// Make given TaskExecutor available for SchedulerFactory configuration.
			configTimeTaskExecutorHolder.set(this.taskExecutor);
		}
		if (this.dataSource != null) {
			// Make given DataSource available for SchedulerFactory configuration.
			configTimeDataSourceHolder.set(this.dataSource);
		}
		if (this.nonTransactionalDataSource != null) {
			// Make given non-transactional DataSource available for SchedulerFactory configuration.
			configTimeNonTransactionalDataSourceHolder.set(this.nonTransactionalDataSource);
		}

		// Get Scheduler instance from SchedulerFactory.
		try {
			this.scheduler = createScheduler(schedulerFactory, this.schedulerName);
			populateSchedulerContext();

			if (!this.jobFactorySet && !(this.scheduler instanceof RemoteScheduler)) {
				// Use AdaptableJobFactory as default for a local Scheduler, unless when
				// explicitly given a null value through the "jobFactory" bean property.
				this.jobFactory = new AdaptableJobFactory();
			}
			if (this.jobFactory != null) {
				if (this.jobFactory instanceof SchedulerContextAware) {
					((SchedulerContextAware) this.jobFactory).setSchedulerContext(this.scheduler.getContext());
				}
				this.scheduler.setJobFactory(this.jobFactory);
			}
		}

		finally {
			if (this.resourceLoader != null) {
				configTimeResourceLoaderHolder.remove();
			}
			if (this.taskExecutor != null) {
				configTimeTaskExecutorHolder.remove();
			}
			if (this.dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
			if (this.nonTransactionalDataSource != null) {
				configTimeNonTransactionalDataSourceHolder.remove();
			}
		}

		registerListeners();
		registerJobsAndTriggers();
	}

	/**
	 * Create a SchedulerFactory if necessary and apply locally defined Quartz properties to it.
	 * 
	 * @return the initialized SchedulerFactory
	 */
	private SchedulerFactory prepareSchedulerFactory() throws SchedulerException, IOException {
		SchedulerFactory schedulerFactory = this.schedulerFactory;
		if (schedulerFactory == null) {
			// Create local SchedulerFactory instance (typically a StdSchedulerFactory)
			schedulerFactory = BeanUtils.instantiateClass(this.schedulerFactoryClass);
			if (schedulerFactory instanceof StdSchedulerFactory) {
				initSchedulerFactory((StdSchedulerFactory) schedulerFactory);
			}
			else if (this.configLocation != null || this.quartzProperties != null || this.taskExecutor != null || this.dataSource != null) {
				throw new IllegalArgumentException("StdSchedulerFactory required for applying Quartz properties: " + schedulerFactory);
			}
			// Otherwise, no local settings to be applied via StdSchedulerFactory.initialize(Properties)
		}
		// Otherwise, assume that externally provided factory has been initialized with appropriate settings
		return schedulerFactory;
	}

	/**
	 * Initialize the given SchedulerFactory, applying locally defined Quartz properties to it.
	 * 
	 * @param schedulerFactory the SchedulerFactory to initialize
	 */
	private void initSchedulerFactory(StdSchedulerFactory schedulerFactory) throws SchedulerException, IOException {
		Properties mergedProps = new Properties();
		if (this.resourceLoader != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS, ResourceLoaderClassLoadHelper.class.getName());
		}

		if (this.taskExecutor != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, LocalTaskExecutorThreadPool.class.getName());
		}
		else {
			// Set necessary default properties here, as Quartz will not apply
			// its default configuration when explicitly given properties.
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
			mergedProps.setProperty(PROP_THREAD_COUNT, Integer.toString(DEFAULT_THREAD_COUNT));
		}

		if (this.configLocation != null) {
			LOG.info("Loading Quartz config from [" + this.configLocation + "]");
			PropertiesLoaderUtils.fillProperties(mergedProps, this.configLocation);
		}

		CollectionUtils.mergePropertiesIntoMap(this.quartzProperties, mergedProps);
		if (this.dataSource != null) {
			mergedProps.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, LocalDataSourceJobStore.class.getName());
		}
		if (this.schedulerName != null) {
			mergedProps.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, this.schedulerName);
		}

		schedulerFactory.initialize(mergedProps);
	}

	/**
	 * Create the Scheduler instance for the given factory and scheduler name. Called by {@link #afterPropertiesSet}.
	 * <p>
	 * The default implementation invokes SchedulerFactory's {@code getScheduler} method. Can be overridden for custom Scheduler creation.
	 * 
	 * @param schedulerFactory the factory to create the Scheduler with
	 * @param schedulerName the name of the scheduler to create
	 * @return the Scheduler instance
	 * @throws SchedulerException if thrown by Quartz methods
	 * @see #afterPropertiesSet
	 * @see org.quartz.SchedulerFactory#getScheduler
	 */
	protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) throws SchedulerException {

		// Override thread context ClassLoader to work around naive Quartz ClassLoadHelper loading.
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader = (this.resourceLoader != null && !this.resourceLoader.getClassLoader().equals(threadContextClassLoader));
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.resourceLoader.getClassLoader());
		}
		try {
			SchedulerRepository repository = SchedulerRepository.getInstance();
			synchronized (repository) {
				Scheduler existingScheduler = (schedulerName != null ? repository.lookup(schedulerName) : null);
				Scheduler newScheduler = schedulerFactory.getScheduler();
				if (newScheduler == existingScheduler) {
					throw new IllegalStateException("Active Scheduler of name '" + schedulerName + "' already registered "
							+ "in Quartz SchedulerRepository. Cannot create a new Spring-managed Scheduler of the same name!");
				}
				if (!this.exposeSchedulerInRepository) {
					// Need to remove it in this case, since Quartz shares the Scheduler instance by default!
					SchedulerRepository.getInstance().remove(newScheduler.getSchedulerName());
				}
				return newScheduler;
			}
		}
		finally {
			if (overrideClassLoader) {
				// Reset original thread context ClassLoader.
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

	/**
	 * Expose the specified context attributes and/or the current ApplicationContext in the Quartz SchedulerContext.
	 */
	private void populateSchedulerContext() throws SchedulerException {
		// Put specified objects into Scheduler context.
		if (this.schedulerContextMap != null) {
			getScheduler().getContext().putAll(this.schedulerContextMap);
		}

		// Register ApplicationContext in Scheduler context.
		if (this.applicationContextSchedulerContextKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException("SchedulerFactoryBean needs to be set up in an ApplicationContext "
						+ "to be able to handle an 'applicationContextSchedulerContextKey'");
			}
			getScheduler().getContext().put(this.applicationContextSchedulerContextKey, this.applicationContext);
		}
	}

	/**
	 * Start the Quartz Scheduler, respecting the "startupDelay" setting.
	 * 
	 * @param scheduler the Scheduler to start
	 * @param startupDelay the number of seconds to wait before starting the Scheduler asynchronously
	 */
	protected void startScheduler(final Scheduler scheduler, final int startupDelay) throws SchedulerException {
		if (startupDelay <= 0) {
			LOG.info("Starting Quartz Scheduler now");
			scheduler.start();
		}
		else {
			LOG.info("Will start Quartz Scheduler [" + scheduler.getSchedulerName() + "] in " + startupDelay + " seconds");
			// Not using the Quartz startDelayed method since we explicitly want a daemon
			// thread here, not keeping the JVM alive in case of all other threads ending.
			Thread schedulerThread = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(startupDelay * 1000);
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						// simply proceed
					}
					LOG.info("Starting Quartz Scheduler now, after delay of " + startupDelay + " seconds");
					try {
						scheduler.start();
					}
					catch (SchedulerException ex) {
						throw new SchedulingException("Could not start Quartz Scheduler after delay", ex);
					}
				}
			};
			schedulerThread.setName("Quartz Scheduler [" + scheduler.getSchedulerName() + "]");
			schedulerThread.setDaemon(true);
			schedulerThread.start();
		}
	}

	// ---------------------------------------------------------------------
	// Implementation of FactoryBean interface
	// ---------------------------------------------------------------------

	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public Scheduler getObject() {
		return this.scheduler;
	}

	@Override
	public Class<? extends Scheduler> getObjectType() {
		return (this.scheduler != null) ? this.scheduler.getClass() : Scheduler.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	// ---------------------------------------------------------------------
	// Implementation of SmartLifecycle interface
	// ---------------------------------------------------------------------

	@Override
	public void start() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				startScheduler(this.scheduler, this.startupDelay);
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not start Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				this.scheduler.standby();
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not stop Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop(Runnable callback) throws SchedulingException {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				return !this.scheduler.isInStandbyMode();
			}
			catch (SchedulerException ex) {
				return false;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	// ---------------------------------------------------------------------

	/**
	 * Shut down the Quartz scheduler on bean factory shutdown, stopping all scheduled jobs.
	 */
	@Override
	public void destroy() throws SchedulerException {
		if (this.scheduler != null) {
			LOG.info("Shutting down Quartz Scheduler");
			this.scheduler.shutdown(this.waitForJobsToCompleteOnShutdown);
		}
	}
}
