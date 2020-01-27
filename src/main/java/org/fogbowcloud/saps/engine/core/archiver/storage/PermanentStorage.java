package org.fogbowcloud.saps.engine.core.archiver.storage;

import org.fogbowcloud.saps.engine.core.model.SapsImage;

public interface PermanentStorage {

	/**
	 * This function tries to archive a task trying each folder in order
	 * (inputdownloading -> preprocessing -> processing).
	 * 
	 * @param task task to be archived
	 * @return boolean representation, success (true) or failure (false) in to
	 *         archive the three folders.
	 */
	public boolean archive(SapsImage task);

	/**
	 * This function delete all files from task in Permanent Storage.
	 * 
	 * @param task task with files information to be deleted
	 * @return boolean representation, success (true) or failure (false) to delete
	 *         files
	 * @throws Exception
	 */
	public boolean delete(SapsImage task) throws Exception;

}
