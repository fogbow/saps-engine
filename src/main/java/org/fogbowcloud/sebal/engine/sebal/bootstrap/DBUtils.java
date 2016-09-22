package org.fogbowcloud.sebal.engine.sebal.bootstrap;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.fogbowcloud.sebal.engine.sebal.model.SebalUser;

public interface DBUtils {

	void setImagesToPurge(String day, boolean forceRemoveNonFetched) throws SQLException, ParseException;

	void listImagesInDB() throws SQLException, ParseException;

	void listCorruptedImages() throws ParseException;

	void getRegionImages(int firstYear, int lastYear, String region) throws SQLException, ParseException;

	List<String> fillDB(int firstYear, int lastYear, List<String> regions, String sebalVersion, String sebalTag) throws IOException;
	
	void addUserInDB(String userEmail, String userName, String userPass, boolean userState,
			boolean userNotify, boolean adminRole) throws SQLException;
	
	void addUserInNotifyDB(String imageName, String userEmail) throws SQLException;

	void updateUserState(String userEmail, boolean userState) throws SQLException;
	
	boolean isUserNotifiable(String userEmail) throws SQLException;
	
	SebalUser getUser(String userEmail);
	
	Map<String, String> getUsersToNotify() throws SQLException;
	
	void removeUserNotify(String imageName, String userEmail) throws SQLException;
	
}
