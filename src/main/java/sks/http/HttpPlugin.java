package sks.http;

import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import sks.base.Plugin;
import sks.quartz.handler.Quartz;

public class HttpPlugin implements Plugin{

	@Override
	public void init(Properties p) throws Exception {
		
		Server server = new Server(Integer.parseInt(p.getProperty("port")));
		ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{ "index.html" });
        resource_handler.setResourceBase(p.getProperty("app"));
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resource_handler, new Quartz()});
        server.setHandler(handlers);  
        
		server.start();
	}
}
