package sks.quartz.task;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class HelloJob implements Job {
	private static Logger log = LoggerFactory.getLogger(HelloJob.class);  
    public HelloJob() {
    }

    public void execute(JobExecutionContext context)
      throws JobExecutionException
    {
    	log.info("Hello!  HelloJob is executing.");
    	System.err.println("Hello!  HelloJob is executing.");
    }

}