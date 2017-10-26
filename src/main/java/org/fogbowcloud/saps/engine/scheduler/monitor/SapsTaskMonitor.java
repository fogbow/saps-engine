package org.fogbowcloud.saps.engine.scheduler.monitor;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskProcess;
import org.fogbowcloud.blowout.core.model.TaskState;
import org.fogbowcloud.blowout.core.monitor.TaskMonitor;
import org.fogbowcloud.blowout.infrastructure.model.ResourceState;
import org.fogbowcloud.blowout.pool.BlowoutPool;
import org.fogbowcloud.saps.engine.core.database.ImageDataStore;
import org.fogbowcloud.saps.engine.core.model.ImageTask;
import org.fogbowcloud.saps.engine.core.model.ImageTaskState;
import org.fogbowcloud.saps.engine.core.model.SapsTask;

public class SapsTaskMonitor extends TaskMonitor {

	private ImageDataStore imageStore;
	private static long timeout = 10000;

	private static final Logger LOGGER = Logger.getLogger(SapsTaskMonitor.class);

	public SapsTaskMonitor(BlowoutPool blowoutpool, ImageDataStore imageStore) {
		super(blowoutpool, timeout);
		this.imageStore = imageStore;
	}

	@Override
	public void procMon() {
		for (TaskProcess tp : getRunningProcesses()) {
			if (tp.getStatus().equals(TaskState.RUNNING)) {
				imageTaskToRunning(tp);
			}
			if (tp.getStatus().equals(TaskState.FINNISHED)) {
				imageTaskToFinished(tp);
			}
			if (tp.getStatus().equals(TaskState.TIMEDOUT)) {
				imageTaskToTimedout(tp);
			}
			if (tp.getStatus().equals(TaskState.FAILED)) {
				imageTaskToFailed(tp);
			}
		}
	}

	protected void imageTaskToRunning(TaskProcess tp) {
		try {
			updateImageTaskToRunning(tp);
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void imageTaskToFinished(TaskProcess tp) {
		try {
			updateImageTaskToFinished(tp);
			Task task = getTaskById(tp.getTaskId());
			task.finish();
			getRunningTasks().remove(task);
			if (tp.getResource() != null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void imageTaskToTimedout(TaskProcess tp) {
		try {
			updateImageTaskToQueued(tp);
			getRunningTasks().remove(getTaskById(tp.getTaskId()));
			if (tp.getResource() != null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void imageTaskToFailed(TaskProcess tp) {
		try {
			updateImageTaskToFailed(tp);
			getRunningTasks().remove(getTaskById(tp.getTaskId()));
			if (tp.getResource() != null) {
				getBlowoutPool().updateResource(tp.getResource(), ResourceState.IDLE);
			}
		} catch (SQLException e) {
			LOGGER.error("Error while updating image/task state", e);
		}
	}

	protected void updateImageTaskToRunning(TaskProcess tp) throws SQLException {
		ImageTask imageTask = this.imageStore.getTask(getImageTaskFromTaskProcess(tp));
		if (!imageTask.getState().equals(ImageTaskState.RUNNING)) {
			imageTask.setState(ImageTaskState.RUNNING);
			imageStore.updateImageTask(imageTask);

			// Inserting update time into stateStamps table in DB
			imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
			imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
					imageTask.getUpdateTime());
		}
	}

	protected void updateImageTaskToFinished(TaskProcess tp) throws SQLException {
		ImageTask imageTask = this.imageStore.getTask(getImageTaskFromTaskProcess(tp));
		imageTask.setState(ImageTaskState.FINISHED);
		imageStore.updateImageTask(imageTask);

		// Inserting update time into stateStamps table in DB
		imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
		imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
				imageTask.getUpdateTime());
	}

	protected void updateImageTaskToFailed(TaskProcess tp) throws SQLException {
		ImageTask imageTask = this.imageStore.getTask(getImageTaskFromTaskProcess(tp));
		imageTask.setState(ImageTaskState.FAILED);
		imageTask.setError("ImageTask " + getImageTaskFromTaskProcess(tp) + " process failed");
		imageStore.updateImageTask(imageTask);

		// Inserting update time into stateStamps table in DB
		imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
		imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
				imageTask.getUpdateTime());
	}

	protected void updateImageTaskToQueued(TaskProcess tp) throws SQLException {
		ImageTask imageTask = this.imageStore.getTask(getImageTaskFromTaskProcess(tp));
		imageTask.setState(ImageTaskState.READY);
		imageStore.updateImageTask(imageTask);

		// Inserting update time into stateStamps table in DB
		imageTask.setUpdateTime(imageStore.getTask(imageTask.getTaskId()).getUpdateTime());
		imageStore.addStateStamp(imageTask.getTaskId(), imageTask.getState(),
				imageTask.getUpdateTime());
	}

	public String getImageTaskFromTaskProcess(TaskProcess tp) {
		return getBlowoutPool().getTaskById(tp.getTaskId()).getMetadata(SapsTask.METADATA_TASK_ID);
	}
}