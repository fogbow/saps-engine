package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class GetProcessedTasks implements CatalogRetry<List<SapsImage>> {

	private ImageDataStore imageStore;
	private String region;
	private Date initDate;
	private Date endDate;
	private String inputdownloadingPhaseTag;
	private String preprocessingPhaseTag;
	private String processingPhaseTag;

	public GetProcessedTasks(ImageDataStore imageStore, String region, Date initDate, Date endDate,
			String inputdownloadingPhaseTag, String preprocessingPhaseTag, String processingPhaseTag) {
		this.imageStore = imageStore;
		this.region = region;
		this.initDate = initDate;
		this.endDate = endDate;
		this.inputdownloadingPhaseTag = inputdownloadingPhaseTag;
		this.preprocessingPhaseTag = preprocessingPhaseTag;
		this.processingPhaseTag = processingPhaseTag;
	}

	@Override
	public List<SapsImage> run() throws SQLException {
		return imageStore.getProcessedImages(region, initDate, endDate, inputdownloadingPhaseTag, preprocessingPhaseTag,
				processingPhaseTag);
	}

}