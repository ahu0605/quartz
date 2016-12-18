package sks.quartz;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sks.base.Plugin;
import sks.base.Starter;
import sks.quartz.handler.Quartz;
import sks.quartz.task.CommonJob;

public class QuartzPlugin implements Plugin{

	private static Logger log = LoggerFactory.getLogger(QuartzPlugin.class); 
	private static final String INSERT_TASK = "INSERT quartz_post_url(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,INTERFACE,PARAMETERS) VALUE ('%s','%s','%s','%s','%s')";
	private static final String UPDATE_TASK = "UPDATE quartz_post_url SET INTERFACE='%s',PARAMETERS='%s' WHERE SCHED_NAME='%s' AND TRIGGER_NAME='%s' AND TRIGGER_GROUP='%s'";
	private static final String DEL_TASK = "DELETE FROM quartz_post_url WHERE SCHED_NAME='%s' AND TRIGGER_NAME='%s' AND TRIGGER_GROUP='%s'";
	public static final String JOB_NAME = "job" ;
	public static final String JOB_GROUP = "group" ;
	public static final String SCHED_NAME = "skTimer1";
	
	private final SchedulerFactory sf ;

	private final Scheduler sched ;
	
	private static QuartzPlugin instance;
	
	static{
		
		try {
			instance = new QuartzPlugin();
		} catch (SchedulerException e) {
			instance = null;
		}
		
	}
	
	public static QuartzPlugin getInstance() {
		return instance;
	}
	
	private QuartzPlugin() throws SchedulerException{
		
		sf = new StdSchedulerFactory();
		
		((StdSchedulerFactory) sf).initialize(Starter.PATH+"/quartz.properties");
		
		sched = sf.getScheduler();
		
	}
	@Override
	public void init(Properties p) throws Exception {
		JobKey jobKey = new JobKey(JOB_NAME,JOB_GROUP);
		//JobDetail job = sched.getJobDetail(jobKey);
		if( !sched.checkExists(jobKey)){
			JobDetail job = JobBuilder.newJob(CommonJob.class)
								.withIdentity(JOB_NAME,JOB_GROUP) // name "myJob", group "group1"
									.storeDurably(true)
	      	      						.build();
			sched.addJob(job, false);
		}

		
		if(!sched.isStarted()){
			log.info("-----sched init-----");
			sched.start();
		}
	
	}
	
	public void stop() throws SchedulerException{
		if(sched.isStarted())
			sched.shutdown(true);
		
	}
	
	public int add(TriggerKey triggerKey ,String cron,String url,String parameters) throws SchedulerException{
		JobKey key = new JobKey(JOB_NAME,JOB_GROUP);
		JobDetail jobDetail = sched.getJobDetail(key);  
		
		sched.scheduleJob( TriggerBuilder
										.newTrigger()
											.withSchedule(CronScheduleBuilder.cronSchedule(cron))
												.withIdentity(triggerKey).forJob(jobDetail)
													.build());
		
		return updateTask(String.format(INSERT_TASK,SCHED_NAME,triggerKey.getName(),triggerKey.getGroup(),url,parameters));
	}
	
	public int del(TriggerKey triggerKey) throws SchedulerException{
		int ret=0;
		if(sched.checkExists(triggerKey)){
			sched.pauseTrigger(triggerKey);// 停止触发器  
			sched.unscheduleJob(triggerKey);// 移除触发器  
			ret = updateTask(String.format(DEL_TASK,SCHED_NAME,triggerKey.getName(),triggerKey.getGroup()));
		}
        return ret;
	}
	
	public int update(TriggerKey triggerKey,String cron,String url ,String parameters) throws SchedulerException{
		int ret=0;
		if(sched.checkExists(triggerKey)){
			sched.pauseTrigger(triggerKey);// 停止触发器  
			sched.unscheduleJob(triggerKey);// 移除触发器  
			JobKey jobKey = new JobKey(JOB_NAME,JOB_GROUP);
			sched.unscheduleJob(triggerKey);// 移除触发器
			sched.scheduleJob(TriggerBuilder
									.newTrigger()
										.withSchedule(CronScheduleBuilder.cronSchedule(cron))
											.withIdentity(triggerKey).forJob(jobKey)
															.build());
			ret = updateTask(String.format(UPDATE_TASK,url,parameters,SCHED_NAME,triggerKey.getName(),triggerKey.getGroup()));
		
		}
		return ret;
	}
	
	private int updateTask(String sql){
		  
		  DBConnectionManager manager = DBConnectionManager.getInstance();
		  Connection connect=null; 
		  Statement stat = null;
		  int ret = 0;
		  try {
			  
				connect = manager.getConnection(Quartz.DS_NANE);
				stat = connect.createStatement();
				ret = stat.executeUpdate(sql);
			
		  } catch (SQLException e) {
			    log.error("update error :",e);
		  }finally{
			  	if(stat != null){
					try {
						stat.close();
					} catch (SQLException e) {}
				}
				if(connect != null){
					try {
						connect.close();
					} catch (SQLException e) {}
				}
		  }
		
		  
		  return ret;
	}
	
}
