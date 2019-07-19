package snu.kdd.substring_syn.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;

public class Log {
	
	public static final Logger log;
	public static final String logpath;
	
	static {
		log = LogManager.getFormatterLogger();
		Appender appender = ((org.apache.logging.log4j.core.Logger)log).getAppenders().get("File");
		logpath = ((FileAppender)appender).getFileName();
	}
}
