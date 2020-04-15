package snu.kdd.substring_syn.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;

public class Log {
	
	public static final Logger log;
	public static final String logpath;
	
	static {
		log = LogManager.getFormatterLogger("logger");
		Appender appender = ((org.apache.logging.log4j.core.Logger)log).getAppenders().get("File");
		logpath = ((FileAppender)appender).getFileName();
	}
	
	public static void disable() {
		Configurator.setLevel("logger", Level.OFF);
	}
}
