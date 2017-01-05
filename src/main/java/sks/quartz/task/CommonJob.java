package sks.quartz.task;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.http.client.ClientProtocolException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.TriggerKey;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

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

    public void execute(JobExecutionContext context)
    	      throws JobExecutionException
    	    {
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
    			} catch (SQLException e) {
    				log.error("interface is error : ",e);
    				throw new JobExecutionException("interface is error");
    			} catch (ClientProtocolException e) {
    				log.error("interface is error : ",e);
    				throw new JobExecutionException("interface is error");
    			} catch (ParseException e) {
    				log.error("interface is error : ",e);
    				throw new JobExecutionException("interface is error");
    			} catch (IOException e) {
    				log.error("interface is error : ",e);
    				throw new JobExecutionException("interface is error");
    			} catch (Exception e) {
    				log.error("interface is error : ",e);
    				throw new JobExecutionException("interface is error");
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

}