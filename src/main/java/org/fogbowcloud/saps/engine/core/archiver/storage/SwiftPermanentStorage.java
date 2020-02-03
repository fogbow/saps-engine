package org.fogbowcloud.saps.engine.core.archiver.storage;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.archiver.swift.SwiftAPIClient;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;

public class SwiftPermanentStorage implements PermanentStorage {

	private final SwiftAPIClient swiftAPIClient;
	private final Properties properties;
	private final String swiftExports;
	private final String containerName;
	private final boolean executionMode;

	private static int MAX_ARCHIVE_TRIES = 1;
	private static int MAX_SWIFT_UPLOAD_TRIES = 2;

	public static final Logger LOGGER = Logger.getLogger(SwiftPermanentStorage.class);

	public SwiftPermanentStorage(Properties properties) throws SapsException {
		if (!checkProperties(properties))
			throw new SapsException(
					"Error on validate the file. Missing properties for start Swift Permanent Storage.");

		this.executionMode = properties.containsKey(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE) && properties
				.getProperty(SapsPropertiesConstants.SAPS_EXECUTION_DEBUG_MODE).toLowerCase().equals("true");

		if (this.executionMode && !checkPropertiesDebugMode(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Saps Controller.");

		this.swiftAPIClient = new SwiftAPIClient(properties);
		this.properties = properties;
		this.swiftExports = properties.getProperty(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX);
		this.containerName = properties.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME);

		this.swiftAPIClient.createContainer(properties.getProperty(SapsPropertiesConstants.SWIFT_CONTAINER_NAME));
	}

	/**
	 * This function checks if the essential properties from Swift permanent storage have been set.
	 * 
	 * @param properties Swift permanent storage properties to be check
	 * @return boolean representation, true (case all properties been set) or false
	 *         (otherwise)
	 */
	private boolean checkProperties(Properties properties) {
		if (properties == null) {
			LOGGER.error("Properties arg must not be null.");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_FOLDER_PREFIX + " was not set");
			return false;
		}
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_CONTAINER_NAME)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_CONTAINER_NAME + " was not set");
			return false;
		}
		LOGGER.debug("All properties are set");
		return true;
	}

	/**
	 * This function checks if properties for debug mode have been set.
	 * 
	 * @param properties saps properties to be check
	 * @return boolean representation, true (case all properties been set) or false
	 *         (otherwise)
	 */
	private boolean checkPropertiesDebugMode(Properties properties) {
		if (!properties.containsKey(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS
					+ " was not set (it's necessary when debug mode)");
			return false;
		}

		LOGGER.debug("All properties for debug mode are set");
		return true;
	}

	/**
	 * This function tries to archive a task trying each folder in order
	 * (inputdownloading -> preprocessing -> processing).
	 * 
	 * @param task task to be archived
	 * @return boolean representation, success (true) or failure (false) in to
	 *         archive the three folders.
	 */
	@Override
	public boolean archive(SapsImage task) {
		String taskId = task.getTaskId();

		LOGGER.info("Attempting to archive task [" + taskId + "] with a maximum of " + MAX_ARCHIVE_TRIES
				+ " archiving attempts for each folder (inputdownloading, preprocessing, processing)");

		String sapsExports = task.getState() == ImageTaskState.FAILED
				? properties.getProperty(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX_DEBUG_FAILED_TASKS)
				: properties.getProperty(SapsPropertiesConstants.SWIFT_FOLDER_PREFIX);

		String inputdownloadingLocalDir = sapsExports + File.separator + taskId + File.separator + "inputdownloading";
		String inputdownloadingSwiftDir = swiftExports + File.separator + taskId + File.separator + "inputdownloading";

		String preprocessingLocalDir = sapsExports + File.separator + taskId + File.separator + "preprocessing";
		String preprocessingSwiftDir = swiftExports + File.separator + taskId + File.separator + "preprocessing";

		String processingLocalDir = sapsExports + File.separator + taskId + File.separator + "processing";
		String processingSwiftDir = swiftExports + File.separator + taskId + File.separator + "processing";

		LOGGER.info("Inputdownloading local folder: " + inputdownloadingLocalDir);
		LOGGER.info("Inputdownloading swift folder: " + inputdownloadingSwiftDir);
		boolean inputdownloadingSentSuccess = archive(task, inputdownloadingLocalDir, inputdownloadingSwiftDir);

		LOGGER.info("Preprocessing local folder: " + preprocessingLocalDir);
		LOGGER.info("Preprocessing swift folder: " + preprocessingSwiftDir);
		boolean preprocessingSentSuccess = inputdownloadingSentSuccess
				&& archive(task, preprocessingLocalDir, preprocessingSwiftDir);

		LOGGER.info("Processing local folder: " + processingLocalDir);
		LOGGER.info("Processing swift folder: " + processingSwiftDir);
		boolean processingSentSuccess = preprocessingSentSuccess
				&& archive(task, processingLocalDir, processingSwiftDir);

		LOGGER.info("Archive process result of task [" + task.getTaskId() + ":\nInputdownloading phase: "
				+ (inputdownloadingSentSuccess ? "Sucesso" : "Falha") + "\n" + "Preprocessing phase: "
				+ (preprocessingSentSuccess ? "Sucesso" : "Falha") + "\n" + "Processing phase: "
				+ (processingSentSuccess ? "Sucesso" : "Falha"));

		return inputdownloadingSentSuccess && preprocessingSentSuccess && processingSentSuccess;
	}

	/**
	 * This function tries to archive a task folder in Swift.
	 * 
	 * @param task     task in archive process
	 * @param localDir task folder to be archived
	 * @param swiftDir directory swift to archive new data
	 * @return boolean representation, success (true) or failure (false) to archive
	 */
	private boolean archive(SapsImage task, String localDir, String swiftDir) {
		LOGGER.info("Trying to archive task [" + task.getTaskId() + "] " + localDir + " folder with a maximum of "
				+ MAX_ARCHIVE_TRIES + " archiving attempts");

		File localFileDir = new File(localDir);

		if (!localFileDir.exists() || !localFileDir.isDirectory()) {
			LOGGER.error("Failed to archive task [" + task.getTaskId() + "]. " + localDir
					+ " folder isn't directory or not exists");
			return false;
		}

		for (int itry = 0; itry < MAX_ARCHIVE_TRIES; itry++) {
			if (uploadFiles(task, localFileDir, swiftDir))
				return true;
		}

		return false;
	}

	/**
	 * This function tries upload task folder files to Swift.
	 * 
	 * @param task     task in archive process
	 * @param localDir task folder to be archived
	 * @param swiftDir directory swift to archive new data
	 * @return boolean representation, success (true) or failure (false) to archive
	 */
	private boolean uploadFiles(SapsImage task, File localDir, String swiftDir) {
		LOGGER.info("Trying to archive task [" + task.getTaskId() + "] " + localDir + " folder for swift");
		for (File actualFile : localDir.listFiles()) {
			if (!uploadFile(actualFile, swiftDir)) {
				LOGGER.info("Failure in archiving file [" + actualFile.getAbsolutePath() + "]");
				return false;
			}
		}

		LOGGER.info("Upload to swift succsessfully done");
		return true;
	}

	/**
	 * This function tries upload a task folder file to Swift.
	 * 
	 * @param actualFile file to be uploaded
	 * @param swiftDir   directory swift to archive
	 * @return boolean representation, success (true) or failure (false) to archive
	 */
	private boolean uploadFile(File actualFile, String swiftDir) {
		LOGGER.info("Trying to archive file [" + actualFile.getAbsolutePath() + "] for swift container ["
				+ containerName + "] with a maximum of " + MAX_SWIFT_UPLOAD_TRIES + " uploading attempts");

		for (int itry = 0; itry < MAX_SWIFT_UPLOAD_TRIES; itry++) {
			try {
				swiftAPIClient.uploadFile(containerName, actualFile, swiftDir);
				return true;
			} catch (Exception e) {
				LOGGER.error("Error while uploading file " + actualFile.getAbsolutePath() + " to swift", e);
			}
		}

		return false;
	}

	/**
	 * This function delete all files from task in Permanent Storage.
	 * 
	 * @param task task with files information to be deleted
	 * @return boolean representation, success (true) or failure (false) to delete
	 *         files
	 * @throws Exception
	 */
	@Override
	public boolean delete(SapsImage task) throws Exception {
		String taskId = task.getTaskId();

		LOGGER.debug("Deleting files from task [" + taskId + "] in Swift [" + containerName + "]");

		String prefix = swiftExports + File.separator + taskId;

		List<String> fileNames = swiftAPIClient.listFilesWithPrefix(containerName, prefix);

		LOGGER.info("Files List: " + fileNames);

		for (String file : fileNames) {
			LOGGER.debug("Trying to delete file " + file + " from " + containerName);
			swiftAPIClient.deleteFile(containerName, file);
		}

		return true;
	}

}