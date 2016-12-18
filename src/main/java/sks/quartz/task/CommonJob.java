package sks.quartz.task;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Trigger;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sks.quartz.QuartzPlugin;
import sks.quartz.handler.Quartz;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    	Trigger t = context.getTrigger();
    	log.info("---excute---");
    	try {
			JsonObject json = getAll(t.getKey().getName(),t.getKey().getGroup());
			log.info("---excute interface---"+json.toString());
    	} catch (SQLException e) {
			log.error("execute error : ", e);
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