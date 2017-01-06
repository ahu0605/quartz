package sks.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sks.http.HttpPlugin;
import sks.quartz.QuartzPlugin;

public class Starter {
	private static Logger log = LoggerFactory.getLogger(Starter.class); 
	private static Properties config ;
	public static final String PATH=System.getProperty("res");
	public static void main(String[] args) throws Exception {

		log.info("-----init-----");
		config = init();
		log.info("-----starter-----");
		start();
		log.info("-----start finished-----");
	}
	
	public static void start() throws Exception{
		
		Plugin plugin = new HttpPlugin();
		plugin.init(config);
		
		plugin = QuartzPlugin.getInstance();
		plugin.init(config);
		
	}
	
	public static Properties init() throws IOException{
		//log.info("path="+ Thread.currentThread().setContextClassLoader());
		//InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
		InputStream is = null;
		Properties property = new Properties();
		try{
			is = new FileInputStream(new File(PATH+"/config.properties"));
			if( is == null){
				log.error("-----quartz.properties failed-----");
				throw new RuntimeException();
			}
			property.load(is);
		}finally{
			is.close();
		}
		return property;	
	}
	
	public static Properties getConfig(){
		return config;
	}
}
