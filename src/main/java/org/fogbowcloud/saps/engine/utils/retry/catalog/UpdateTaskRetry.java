package org.fogbowcloud.saps.engine.utils.retry.catalog;

import java.sql.SQLException;

import org.fogbowcloud.saps.engine.core.database.Catalog;
import org.fogbowcloud.saps.engine.core.model.SapsImage;

public class UpdateTaskRetry implements CatalogRetry<Boolean>{

	private Catalog imageStore;
	private SapsImage task;

	public UpdateTaskRetry(Catalog imageStore, SapsImage task) {
		this.imageStore = imageStore;
		this.task = task;
	}
	
	@Override
	public Boolean run() throws SQLException {
		task.setUpdateTime(imageStore.getTask(task.getTaskId()).getUpdateTime());
		imageStore.updateImageTask(task);
		return true;
	}

}
