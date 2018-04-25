package cn.ljob.schedule.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.xml.XMLSchedulingDataProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.quartz.ResourceLoaderClassLoadHelper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * SpringLjobSchedulerAccessor
 * 
 * @author lin.zb
 */
public abstract class SpringLjobSchedulerAccessor implements ResourceLoaderAware {
	private static final Logger LOG = LoggerFactory.getLogger(SpringLjobSchedulerAccessor.class);

	// *******************************************************************************
	// ** **
	// ** Code from org.springframework.scheduling.quartz.SchedulerAccessor **********
	// ** **
	// *******************************************************************************
	private boolean overwriteExistingJobs = false;

	@Nullable
	private String[] jobSchedulingDataLocations;

	@Nullable
	private List<JobDetail> jobDetails;

	@Nullable
	private Map<String, Calendar> calendars;

	@Nullable
	private List<Trigger> triggers;

	@Nullable
	private SchedulerListener[] schedulerListeners;

	@Nullable
	private JobListener[] globalJobListeners;

	@Nullable
	private TriggerListener[] globalTriggerListeners;

	@Nullable
	private PlatformTransactionManager transactionManager;

	@Nullable
	protected ResourceLoader resourceLoader;

	/**
	 * Set whether any jobs defined on this SchedulerFactoryBean should overwrite existing job definitions. Default is "false", to not overwrite
	 * already registered jobs that have been read in from a persistent job store.
	 */
	public void setOverwriteExistingJobs(boolean overwriteExistingJobs) {
		this.overwriteExistingJobs = overwriteExistingJobs;
	}

	/**
	 * Set the location of a Quartz job definition XML file that follows the "job_scheduling_data_1_5" XSD or better. Can be specified to
	 * automatically register jobs that are defined in such a file, possibly in addition to jobs defined directly on this SchedulerFactoryBean.
	 * 
	 * @see org.quartz.xml.XMLSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocation(String jobSchedulingDataLocation) {
		this.jobSchedulingDataLocations = new String[] { jobSchedulingDataLocation };
	}

	/**
	 * Set the locations of Quartz job definition XML files that follow the "job_scheduling_data_1_5" XSD or better. Can be specified to automatically
	 * register jobs that are defined in such files, possibly in addition to jobs defined directly on this SchedulerFactoryBean.
	 * 
	 * @see org.quartz.xml.XMLSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocations(String... jobSchedulingDataLocations) {
		this.jobSchedulingDataLocations = jobSchedulingDataLocations;
	}

	/**
	 * Register a list of JobDetail objects with the Scheduler that this FactoryBean creates, to be referenced by Triggers.
	 * <p>
	 * This is not necessary when a Trigger determines the JobDetail itself: In this case, the JobDetail will be implicitly registered in combination
	 * with the Trigger.
	 * 
	 * @see #setTriggers
	 * @see org.quartz.JobDetail
	 */
	public void setJobDetails(JobDetail... jobDetails) {
		// Use modifiable ArrayList here, to allow for further adding of
		// JobDetail objects during autodetection of JobDetail-aware Triggers.
		this.jobDetails = new ArrayList<>(Arrays.asList(jobDetails));
	}

	/**
	 * Register a list of Quartz Calendar objects with the Scheduler that this FactoryBean creates, to be referenced by Triggers.
	 * 
	 * @param calendars Map with calendar names as keys as Calendar objects as values
	 * @see org.quartz.Calendar
	 */
	public void setCalendars(Map<String, Calendar> calendars) {
		this.calendars = calendars;
	}

	/**
	 * Register a list of Trigger objects with the Scheduler that this FactoryBean creates.
	 * <p>
	 * If the Trigger determines the corresponding JobDetail itself, the job will be automatically registered with the Scheduler. Else, the respective
	 * JobDetail needs to be registered via the "jobDetails" property of this FactoryBean.
	 * 
	 * @see #setJobDetails
	 * @see org.quartz.JobDetail
	 */
	public void setTriggers(Trigger... triggers) {
		this.triggers = Arrays.asList(triggers);
	}

	/**
	 * Specify Quartz SchedulerListeners to be registered with the Scheduler.
	 */
	public void setSchedulerListeners(SchedulerListener... schedulerListeners) {
		this.schedulerListeners = schedulerListeners;
	}

	/**
	 * Specify global Quartz JobListeners to be registered with the Scheduler. Such JobListeners will apply to all Jobs in the Scheduler.
	 */
	public void setGlobalJobListeners(JobListener... globalJobListeners) {
		this.globalJobListeners = globalJobListeners;
	}

	/**
	 * Specify global Quartz TriggerListeners to be registered with the Scheduler. Such TriggerListeners will apply to all Triggers in the Scheduler.
	 */
	public void setGlobalTriggerListeners(TriggerListener... globalTriggerListeners) {
		this.globalTriggerListeners = globalTriggerListeners;
	}

	/**
	 * Set the transaction manager to be used for registering jobs and triggers that are defined by this SchedulerFactoryBean. Default is none;
	 * setting this only makes sense when specifying a DataSource for the Scheduler.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Register jobs and triggers (within a transaction, if possible).
	 */
	protected void registerJobsAndTriggers() throws SchedulerException {
		TransactionStatus transactionStatus = null;
		if (this.transactionManager != null) {
			transactionStatus = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
		}

		try {
			if (this.jobSchedulingDataLocations != null) {
				ClassLoadHelper clh = new ResourceLoaderClassLoadHelper(this.resourceLoader);
				clh.initialize();
				XMLSchedulingDataProcessor dataProcessor = new XMLSchedulingDataProcessor(clh);
				for (String location : this.jobSchedulingDataLocations) {
					dataProcessor.processFileAndScheduleJobs(location, getScheduler());
				}
			}

			// Register JobDetails.
			if (this.jobDetails != null) {
				for (JobDetail jobDetail : this.jobDetails) {
					addJobToScheduler(jobDetail);
				}
			}
			else {
				// Create empty list for easier checks when registering triggers.
				this.jobDetails = new LinkedList<>();
			}

			// Register Calendars.
			if (this.calendars != null) {
				for (String calendarName : this.calendars.keySet()) {
					Calendar calendar = this.calendars.get(calendarName);
					getScheduler().addCalendar(calendarName, calendar, true, true);
				}
			}

			// Register Triggers.
			if (this.triggers != null) {
				for (Trigger trigger : this.triggers) {
					addTriggerToScheduler(trigger);
				}
			}
		}

		catch (Throwable ex) {
			if (transactionStatus != null) {
				try {
					this.transactionManager.rollback(transactionStatus);
				}
				catch (TransactionException tex) {
					LOG.error("Job registration exception overridden by rollback exception", ex);
					throw tex;
				}
			}
			if (ex instanceof SchedulerException) {
				throw (SchedulerException) ex;
			}
			if (ex instanceof Exception) {
				throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage(), ex);
			}
			throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage());
		}

		if (transactionStatus != null) {
			this.transactionManager.commit(transactionStatus);
		}
	}

	/**
	 * Add the given job to the Scheduler, if it doesn't already exist. Overwrites the job in any case if "overwriteExistingJobs" is set.
	 * 
	 * @param jobDetail the job to add
	 * @return {@code true} if the job was actually added, {@code false} if it already existed before
	 * @see #setOverwriteExistingJobs
	 */
	private boolean addJobToScheduler(JobDetail jobDetail) throws SchedulerException {
		if (this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null) {
			getScheduler().addJob(jobDetail, true);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Add the given trigger to the Scheduler, if it doesn't already exist. Overwrites the trigger in any case if "overwriteExistingJobs" is set.
	 * 
	 * @param trigger the trigger to add
	 * @return {@code true} if the trigger was actually added, {@code false} if it already existed before
	 * @see #setOverwriteExistingJobs
	 */
	public boolean addTriggerToScheduler(Trigger trigger) throws SchedulerException {
		boolean triggerExists = (getScheduler().getTrigger(trigger.getKey()) != null);
		if (triggerExists && !this.overwriteExistingJobs) {
			return false;
		}

		// Check if the Trigger is aware of an associated JobDetail.
		JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().remove("jobDetail");
		if (triggerExists) {
			if (jobDetail != null && this.jobDetails != null && !this.jobDetails.contains(jobDetail) && addJobToScheduler(jobDetail)) {
				this.jobDetails.add(jobDetail);
			}
			getScheduler().rescheduleJob(trigger.getKey(), trigger);
		}
		else {
			try {
				if (jobDetail != null && this.jobDetails != null && !this.jobDetails.contains(jobDetail)
						&& (this.overwriteExistingJobs || getScheduler().getJobDetail(jobDetail.getKey()) == null)) {
					getScheduler().scheduleJob(jobDetail, trigger);
					this.jobDetails.add(jobDetail);
				}
				else {
					getScheduler().scheduleJob(trigger);
				}
			}
			catch (ObjectAlreadyExistsException ex) {
				LOG.debug("Unexpectedly found existing trigger, assumably due to cluster race condition: " + ex.getMessage()
						+ " - can safely be ignored");

				if (this.overwriteExistingJobs) {
					getScheduler().rescheduleJob(trigger.getKey(), trigger);
				}
			}
		}
		return true;
	}

	/**
	 * Register all specified listeners with the Scheduler.
	 */
	protected void registerListeners() throws SchedulerException {
		ListenerManager listenerManager = getScheduler().getListenerManager();
		if (this.schedulerListeners != null) {
			for (SchedulerListener listener : this.schedulerListeners) {
				listenerManager.addSchedulerListener(listener);
			}
		}
		if (this.globalJobListeners != null) {
			for (JobListener listener : this.globalJobListeners) {
				listenerManager.addJobListener(listener);
			}
		}
		if (this.globalTriggerListeners != null) {
			for (TriggerListener listener : this.globalTriggerListeners) {
				listenerManager.addTriggerListener(listener);
			}
		}
	}

	/**
	 * Template method that determines the Scheduler to operate on. To be implemented by subclasses.
	 */
	protected abstract Scheduler getScheduler();

	/**
	 * remove schedule job
	 * 
	 * @param trigger Trigger
	 * @return boolean
	 */
	public boolean removeScheduleJob(Trigger trigger) throws SchedulerException {
		boolean triggerExists = (getScheduler().getTrigger(trigger.getKey()) != null);
		if (!triggerExists) {
			LOG.warn("remove schedule job faile, Job not exists.");
			return true;
		}

		boolean result = false;

		try {
			result = getScheduler().unscheduleJob(trigger.getKey());
			if (result) {
				LOG.info("remove schedule job success, jobTriggerKey: " + trigger.getKey());
			}
			else {
				LOG.error("remove schedule job failed, jobTriggerKey: " + trigger.getKey());
			}

			return result;
		}
		catch (Exception e) {
			LOG.error("remove schedule job exception, jobTriggerKey: " + trigger.getKey() + ", exception: " + e.toString(), e);
			return false;
		}
	}
}
