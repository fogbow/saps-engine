package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;
import java.util.Date;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class AddNewTask implements CatalogRetry<SapsImage> {

	private ImageDataStore imageStore;
	private String taskId;
	private String dataset;
	private String region;
	private Date date;
	private int priority;
	private String userEmail;
	private String inputdownloadingPhaseTag;
	private String preprocessingPhaseTag;
	private String processingPhaseTag;

	public AddNewTask(ImageDataStore imageStore, String taskId, String dataset, String region, Date date, int priority,
			String userEmail, String inputdownloadingPhaseTag, String preprocessingPhaseTag,
			String processingPhaseTag) {
		this.imageStore = imageStore;
		this.taskId = taskId;
		this.dataset = dataset;
		this.region = region;
		this.date = date;
		this.priority = priority;
		this.userEmail = userEmail;
		this.inputdownloadingPhaseTag = inputdownloadingPhaseTag;
		this.preprocessingPhaseTag = preprocessingPhaseTag;
		this.processingPhaseTag = processingPhaseTag;
	}

	@Override
	public SapsImage run() throws SQLException {
		return imageStore.addImageTask(taskId, dataset, region, date, priority, userEmail, inputdownloadingPhaseTag,
				preprocessingPhaseTag, processingPhaseTag);
	}

}
