package sks.quartz.task;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.jobs.ee.mail.SendMailJob;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import sks.base.Starter;
import sks.base.util.HttpUtils;
import sks.quartz.QuartzPlugin;
import sks.quartz.handler.Quartz;
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class CommonJob implements Job {
	private static Logger log = LoggerFactory.getLogger(CommonJob.class);  
	private static final String SQL = "SELECT INTERFACE,PARAMETERS "
									 +"FROM quartz_post_url "
									 +"WHERE SCHED_NAME='%s' AND TRIGGER_NAME='%s' AND TRIGGER_GROUP='%s' ";
    public CommonJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException{
    	    	TriggerKey key = context.getTrigger().getKey();    
    	    	log.info("excute : "+key.getName());
    	    	JsonObject map = null;
    			JsonObject parameters = null;
	    		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    			String lastTime = format.format(context.getTrigger().getPreviousFireTime()!=null?
    												context.getTrigger().getPreviousFireTime():System.currentTimeMillis()
    					);
    			
    	    	String result ;
    	    	try {
    	    
    				map = getAll(key.getName(),key.getGroup());
    				
    				if(map.get("parameters")!=null && !map.get("parameters").isJsonNull()){
    					parameters = map.get("parameters").getAsJsonObject();   	    		
    				}else{
    					parameters = new JsonObject();
    	    		}
    				
    				parameters.addProperty("lastTime", lastTime);
    				result = HttpUtils.requestPost(map.get("interface").getAsString(),parameters.toString());
    				log.info("trigger name : "+key.getName()+" interface : "+map.get("interface") + " result : " + result);
    			} catch (Exception  e) {
    				
    				JobExecutionException je = new JobExecutionException(e);
    										je.setUnscheduleFiringTrigger(true);
    				try {
						sendMail(context.getScheduler(),lastTime);
					} catch (UnknownHostException | SchedulerException e1) {
						log.error("sendmail is error : ",e);
					}
    				log.error("interface is error : ",e);
    				throw je;
    			} 
    	    	
   }
    
    private JsonObject getAll(String name,String group) throws SQLException{
    		
    		DBConnectionManager manager = DBConnectionManager.getInstance();
			Connection connect=null;
			Statement stat = null;
			ResultSet rs = null;

			try{
				
				connect = manager.getConnection(Quartz.DS_NANE);
				stat = connect.createStatement();
				rs = stat.executeQuery(String.format(SQL, QuartzPlugin.SCHED_NAME,name,group));
				JsonObject obj = null;
				
				if(rs.next()){
					
					obj = new JsonObject();
					obj.addProperty("interface",rs.getString("INTERFACE"));	
					obj.addProperty("parameters", rs.getString("PARAMETERS"));
							
				}
				
				return obj;
			
			}finally{
				if(rs != null){
					rs.close();
				}
				if(stat != null){
					stat.close();
				}
				if(connect != null){
					connect.close();
				}
			}
	
    }
    
    private void sendMail(Scheduler sched ,String lastTime) throws SchedulerException, UnknownHostException{
    	
    	JobKey jobKey = new JobKey("error","quartz ");
    	
    	JobDetail job = null;
		if( !sched.checkExists(jobKey)){
			job = JobBuilder.newJob(SendMailJob.class).storeDurably(true)
								.withIdentity("error","quartz ") // name "myJob", group "group1"
									.storeDurably(true)
	      	      						.build();
			
			
			sched.addJob(job, false);
		}else{
		
			job = sched.getJobDetail(jobKey);
			
		}
    	
		JobDataMap  data = initJobData(Starter.getConfig(),lastTime);
		
		sched.triggerJob(jobKey, data);
    }
    
    private JobDataMap initJobData(Properties p,String lastTime) throws UnknownHostException{

		JobDataMap data = new JobDataMap();
		data.put("mail.smtp.port",  p.getProperty("mail.port"));
		data.put("mail.smtp.auth", "true");
		data.put(SendMailJob.PROP_SMTP_HOST, p.getProperty("mail.smtp"));
		data.put(SendMailJob.PROP_RECIPIENT,  p.getProperty("mail.to"));   
		data.put(SendMailJob.PROP_CC_RECIPIENT, p.getProperty("mail.cc"));   
		data.put(SendMailJob.PROP_USERNAME,  p.getProperty("mail.from"));
	    data.put(SendMailJob.PROP_PASSWORD, p.getProperty("mail.password"));
	    data.put(SendMailJob.PROP_SENDER, p.getProperty("mail.from"));
	    data.put(SendMailJob.PROP_SUBJECT, "timer job is error");		
	    data.put(SendMailJob.PROP_MESSAGE, InetAddress.getLocalHost().getHostAddress()+" timer job is error lastTime is "+lastTime);	
	    
	    return data;
    }

}