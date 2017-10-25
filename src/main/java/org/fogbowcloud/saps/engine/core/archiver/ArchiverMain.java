package org.fogbowcloud.saps.engine.core.archiver;

import java.io.FileInputStream;
import java.util.Properties;

public class ArchiverMain {

	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		String imageStoreIP = args[1];
		String imageStorePort = args[2];
		
		properties.put("datastore_ip", imageStoreIP);
		properties.put("datastore_port", imageStorePort);
		
		Archiver Fetcher = new Archiver(properties);
		Fetcher.exec();
	}
}
