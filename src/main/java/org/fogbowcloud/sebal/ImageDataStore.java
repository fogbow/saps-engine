package org.fogbowcloud.sebal;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public interface ImageDataStore {

	public final static String NONE = "None";
	public final static int UNLIMITED = -1;

	public void addImage(String imageName, String downloadLink, int priority) throws SQLException;
	
	public void addStateStamp(String imageName, ImageState state, Date timestamp) throws SQLException;

	public void updateImage(ImageData imageData) throws SQLException;
	
	public void updateImageState(String imageName, ImageState state) throws SQLException;
	
	public void updateImageMetadata(String imageName, String stationId, String sebalVersion) throws SQLException;
	
	public List<ImageData> getAllImages() throws SQLException;

	public List<ImageData> getIn(ImageState state) throws SQLException;

	public List<ImageData> getIn(ImageState state, int limit) throws SQLException;
	
	public List<ImageData> getPurgedImages() throws SQLException;

	public List<ImageData> getImagesToDownload(String federationMember, int limit) throws SQLException;
	
	public ImageData getImage(String imageName) throws SQLException;
	
	public void dispose();

	public boolean lockImage(String imageName) throws SQLException;

	public boolean unlockImage(String imageName) throws SQLException;
	
	public void removeStateStamp(String imageName, ImageState state, Date timestamp) throws SQLException;

	public List<ImageData> getImagesByFilter(ImageState state, String name, long processDateInit, long processDateEnd)
			throws SQLException;
}
