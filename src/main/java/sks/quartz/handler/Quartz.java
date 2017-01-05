package sks.quartz.handler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sks.base.Starter;
import sks.quartz.QuartzPlugin;

public class Quartz extends AbstractHandler{
	private final static Logger log = LoggerFactory.getLogger(QuartzPlugin.class); 
	
	private static final Map<String,Method> route ;
	
	public static final Properties properties = new Properties();
	
	public static String DS_NANE ;
	
	private static final String SQL = "SELECT url.SCHED_NAME AS schedName,url.TRIGGER_GROUP AS triggerGroup,url.TRIGGER_NAME AS triggerName,c.CRON_EXPRESSION AS express,url.INTERFACE AS interface,url.PARAMETERS AS parameters"
	+ " FROM quartz_post_url url INNER JOIN qrtz_cron_triggers c "
	+"ON url.SCHED_NAME=c.SCHED_NAME AND url.TRIGGER_GROUP=c.TRIGGER_GROUP AND url.TRIGGER_NAME=c.TRIGGER_NAME";
	
	
	static{
		//is = Thread.currentThread().getContextClassLoader().getResourceAsStream("quartz.properties");
		route = new HashMap<>();
		route.put("get", new Get());
		route.put("put", new Put());
		route.put("delete", new Del());
		route.put("post", new Update());

		try {
			InputStream is = new FileInputStream(new File(Starter.PATH+"/quartz.properties"));
			properties.load(is);
			DS_NANE = properties.getProperty("org.quartz.jobStore.dataSource");
		} catch (IOException e) {
			log.error(" Quartz init error :",e);
		}
	}
	
	interface Method{
		public void invoke( HttpServletRequest arg2,HttpServletResponse arg3)throws Exception ;
	}
	
	static class Get implements Method{

		@Override
		public void invoke(HttpServletRequest req, HttpServletResponse res) throws SQLException {
			
			DBConnectionManager manager = DBConnectionManager.getInstance();
			Connection connect=null;
			Statement stat = null;
			ResultSet rs = null;
			JsonArray arr = new JsonArray();
			try{
				
				connect = manager.getConnection(DS_NANE);
				stat = connect.createStatement();
				rs = stat.executeQuery(SQL);
				JsonObject obj ;
				
				while(rs.next()){
					
					obj = new JsonObject();
					
					obj.addProperty("schedName",rs.getString("schedName"));
					obj.addProperty("interface",rs.getString("interface"));
					obj.addProperty("parameters", rs.getString("parameters"));
					obj.addProperty("triggerGroup", rs.getString("triggerGroup"));
					obj.addProperty("triggerName", rs.getString("triggerName"));
					obj.addProperty("express", rs.getString("express"));
					
					arr.add(obj);
					
				}
			
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
			
			reactJson( arr ,res);

		}
		
	}
	
	static class Update implements Method{

		@Override
		public void invoke(HttpServletRequest arg2, HttpServletResponse arg3) {
			String json = getJson(arg2);
			JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
			TriggerKey triggerKey = new TriggerKey(obj.get("triggerName").getAsString(),obj.get("triggerGroup").getAsString());
			
			try {
				int ret = QuartzPlugin.getInstance().update(triggerKey, 
					   	obj.get("express").getAsString(), 
					   		obj.get("interface").getAsString(),  
									obj.get("parameters").isJsonNull()?null:obj.get("parameters").getAsString());
				reactJson(arg3 ,ret);
				
			} catch (SchedulerException  e) {
				
				log.error(" Update error :",e);
			}

		}
		
	}
	
	static class Del implements Method{

		@Override
		public void invoke(HttpServletRequest arg2, HttpServletResponse arg3) {
			
			String json = getJson(arg2);
			JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
			TriggerKey triggerKey = new TriggerKey(obj.get("triggerName").getAsString(),obj.get("triggerGroup").getAsString());
			
			try {
				int ret = QuartzPlugin.getInstance().del(triggerKey);
				reactJson(arg3 ,ret);
			} catch (SchedulerException e) {
				
				log.error(" Del error :",e);
			}
		
		}
		
	}
	
	static class Put implements Method{

		@Override
		public void invoke(HttpServletRequest arg2, HttpServletResponse arg3) {
			String json = getJson(arg2);
			
			JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
			TriggerKey triggerKey = new TriggerKey(obj.get("triggerName").getAsString(),obj.get("triggerGroup").getAsString());
			
			try {
				int ret = QuartzPlugin.getInstance().add(triggerKey, 
											   	obj.get("express").getAsString(), 
											   		obj.get("interface").getAsString(),  
															obj.get("parameters").getAsString());
				
				reactJson(arg3,ret);
			} catch (SchedulerException e) {
				
				log.error(" Put error :",e);
			}
			
		}
		
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		
		String method = request.getMethod().toLowerCase();
		Method m = route.get(method);
		if(m != null)
			try {
				m.invoke(request, response);
			} catch (Exception e) {
				log.error(" handle error :",e);
			}
		else{
			response.sendError(500);
		}
		
	}
	

	public static void reactJson(JsonArray arr ,HttpServletResponse res){
		//JsonObject obj = new JsonObject();
		//obj.addProperty("total", 0);
		if(!arr.isJsonNull()){
			//obj.addProperty("total", arr.size());
			//obj.add("rows", arr);
			OutputStream ops = null;
			byte[] b = arr.toString().getBytes();
			try{
				res.setContentType("application/json");
				res.setStatus(200);
				ops = res.getOutputStream();
				ops.write(b);
				res.setContentLength(b.length);
				ops.flush();
			}catch(IOException ioe){
				log.error("out error : ", ioe);
			}finally{
				if(ops != null)
					try {
						ops.close();
					} catch (IOException e) {
						
					}
			}
		}
		
	}
	

	public static void reactJson(HttpServletResponse res,int ret){
	
			OutputStream ops = null;
			byte[] b = null;
			if(ret !=1)
				b = "{\"ret\":false}".getBytes();
			else
				b = "{\"ret\":true}".getBytes();
			
			try{
				res.setContentType("application/json");
				res.setStatus(200);
				ops = res.getOutputStream();
				ops.write(b);
				res.setContentLength(b.length);
				ops.flush();
			}catch(IOException ioe){
				log.error("out error : ", ioe);
			}finally{
				if(ops != null)
					try {
						ops.close();
					} catch (IOException e) {
						
					}
			}
		
	}
	
	public static String getJson(HttpServletRequest request){
		BufferedInputStream bis = null;
		try{
			int len = request.getContentLength();
			bis = new BufferedInputStream(request.getInputStream());
			byte[] bytes = new byte[len];
			bis.read(bytes);
			String str = new String(bytes);
			log.info(str);
			return str;
		}catch(IOException e){
			log.error(" getJson error :",e);
			throw new RuntimeException();
		}finally{
			if(bis != null){
				try {
					bis.close();
				} catch (IOException e) {}
			}
		}
	}
	
}
