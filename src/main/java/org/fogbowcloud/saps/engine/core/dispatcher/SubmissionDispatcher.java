package org.fogbowcloud.saps.engine.core.dispatcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.database.Catalog;
import org.fogbowcloud.saps.engine.core.database.JDBCImageDataStore;
import org.fogbowcloud.saps.engine.core.dispatcher.notifier.Ward;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.core.util.DatasetUtil;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.ExecutionScriptTag;
import org.fogbowcloud.saps.engine.utils.ExecutionScriptTagUtil;
import org.fogbowcloud.saps.engine.utils.retry.CatalogUtils;

public class SubmissionDispatcher {
	public static final int DEFAULT_PRIORITY = 0;
	public static final String DEFAULT_USER = "admin";

	private Catalog catalog;

	private static final Logger LOGGER = Logger.getLogger(SubmissionDispatcher.class);

	public SubmissionDispatcher(Catalog imageStore) {
		this.catalog = imageStore;
	}

	public SubmissionDispatcher(Properties properties) throws SQLException {
		this.catalog = new JDBCImageDataStore(properties);
	}

	/**
	 * This function is responsible for passing on the information of a new SAPS
	 * user to the communication approach with the Catalog that he will try until he
	 * succeeds. The email (primary key of the SAPS user scheme), name and password
	 * are defined by the user in which the email and password will be used for
	 * authentication on the SAPS platform. There are three other pieces of
	 * information that are:
	 * 
	 * - notify: informs the user about their tasks by email.
	 * 
	 * - state: informs if the user is able to authenticate on the SAPS platform.
	 * 
	 * - administrative role: informs if the user is an administrator of the SAPS
	 * platform.
	 * 
	 * These three pieces of information are booleans and are controlled by an
	 * administrator. By default, state and administrative function are false.
	 * 
	 * 
	 * Note: The message parameter is a string that will be displayed in the SAPS
	 * log before attempting to communicate with the Catalog using the retry
	 * approach.
	 */
	public void addUserInCatalog(String email, String name, String password, boolean state, boolean notify,
			boolean adminRole) {
		CatalogUtils.addNewUser(catalog, email, name, password, state, notify, adminRole,
				"add new user [" + email + "]");
	}

	/**
	 * 
	 * This function calls a function that uses the approach of communicating with
	 * the Catalog trying to even try to retrieve the user's information based on
	 * the email passed by parameter (which is the primary key of the SAPS user
	 * scheme, that is, there are not two users with same email).
	 * 
	 * Note: The message parameter is a string that will be displayed in the SAPS
	 * log before attempting to communicate with the Catalog using the retry
	 * approach.
	 */
	public SapsUser getUserInCatalog(String email) {
		return CatalogUtils.getUser(catalog, email, "get user [" + email + "] information");
	}

	public void removeUserNotification(String submissionId, String taskId, String userEmail) throws SQLException {
		try {
			catalog.removeNotification(submissionId, taskId, userEmail);
		} catch (SQLException e) {
			LOGGER.error(
					"Error while removing task " + taskId + " notification for user " + userEmail + " from Catalogue",
					e);
		}
	}

	/**
	 * This function calling get_wrs (Python script) passing latitude and longitude
	 * as paramater.
	 * 
	 * @param latitude  latitude (point coordinate)
	 * @param longitude longitude (point coordinate)
	 * @return scene path/row from latitude and longitude coordinates
	 * @throws Exception
	 */
	private static String getRegionIds(String latitude, String longitude) throws Exception {

		LOGGER.debug("Calling get_wrs.py and passing (" + latitude + ", " + longitude + ") as parameter");
		Process builder = new ProcessBuilder("python", "./scripts/get_wrs.py", latitude, longitude).start();

		LOGGER.debug("Waiting for the process for execute command [" + builder.toString() + "] ...");
		builder.waitFor();
		LOGGER.debug("Process ended.");

		String result = null;

		if (builder.exitValue() != 0)
			throw new Exception("Process output exit code: " + builder.exitValue());

		BufferedReader reader = new BufferedReader(new InputStreamReader(builder.getInputStream()));
		StringBuilder builderS = new StringBuilder();

		String line;
		while ((line = reader.readLine()) != null) {
			builderS.append(line);
			builderS.append(System.getProperty("line.separator"));
		}

		result = builderS.toString();

		LOGGER.debug("Process output (regions ID's): \n" + result);

		return result;
	}

	/**
	 * This function returns regions set from area.
	 * 
	 * @param lowerLeftLatitude   lower left latitude (coordinate)
	 * @param lowerLeftLongitude  lower left longitude (coordinate)
	 * @param upperRightLatitude  upper right latitude (coordinate)
	 * @param upperRightLongitude upper right longitude (coordinate)
	 * @return string set (regions set)
	 */
	private Set<String> regionsFromArea(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude) {

		String regionLowerLeft, regionUpperRight;
		Set<String> regionsFound = new HashSet<String>();

		try {
			regionLowerLeft = getRegionIds(lowerLeftLatitude, lowerLeftLongitude).trim();
			regionUpperRight = getRegionIds(upperRightLatitude, upperRightLongitude).trim();

			int pathRegionLL = Integer.parseInt(regionLowerLeft.substring(0, 3));
			int rowRegionLL = Integer.parseInt(regionLowerLeft.substring(4, 6));

			int pathRegionUR = Integer.parseInt(regionUpperRight.substring(0, 3));
			int rowRegionUR = Integer.parseInt(regionUpperRight.substring(4, 6));

			LOGGER.info("pathRegionLL: " + pathRegionLL + "\n" + "rowRegionLL: " + rowRegionLL + "\n" + "pathRegionUR: "
					+ pathRegionUR + "\n" + "rowRegionUR: " + rowRegionUR + "\n");

			for (int i = pathRegionLL; i >= pathRegionUR; i--) {
				for (int j = rowRegionLL; j >= rowRegionUR; j--)
					regionsFound.add(formatPathRow(i, j));
			}

			LOGGER.info("Regions found: " + regionsFound.toString());

		} catch (Exception e) {
			LOGGER.error("Error while calling the ConvertToWRS script", e);
		}

		return regionsFound;
	}

	/**
	 * Get path and row and create format PPPRRR, where PPP is path of scene RRR is
	 * row of scene
	 */
	private String formatPathRow(int path, int row) {
		String pathAux = Integer.toString(path);
		String rowAux = Integer.toString(row);
		if (rowAux.length() == 1)
			rowAux = "00" + rowAux;
		else if (rowAux.length() == 2)
			rowAux = "0" + rowAux;

		return pathAux + rowAux;
	}

	/**
	 * This function adds new task in Catalog.
	 * 
	 * @param taskId                   task id
	 * @param dataset                  task dataset
	 * @param region                   task region
	 * @param date                     task region
	 * @param priority                 task priority
	 * @param userEmail                user email that is creating task
	 * @param inputdownloadingPhaseTag inputdownloading phase tag
	 * @param preprocessingPhaseTag    preprocessing phase tag
	 * @param processingPhaseTag       processing phase tag
	 * @param digestProcessing
	 * @param processingPhaseTag2
	 * @param digestPreprocessing
	 * @return new saps image
	 */
	private SapsImage addNewTaskInCatalog(String taskId, String dataset, String region, Date date, int priority,
			String userEmail, String inputdownloadingPhaseTag, String digestInputdownloading,
			String preprocessingPhaseTag, String digestPreprocessing, String processingPhaseTag,
			String digestProcessing) {
		return CatalogUtils.addNewTask(catalog, taskId, dataset, region, date, priority, userEmail,
				inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag, digestPreprocessing,
				processingPhaseTag, digestProcessing, "add new task [" + taskId + "]");
	}

	/**
	 * This function add new tuple in time stamp table and updates task time stamp.
	 * 
	 * @param task    task to be update
	 * @param message information message
	 */
	private void addTimestampTaskInCatalog(SapsImage task, String message) {
		CatalogUtils.addTimestampTask(catalog, task, message);
	}

	/**
	 * This function add new tasks in Catalog.
	 * 
	 * @param lowerLeftLatitude        lower left latitude (coordinate)
	 * @param lowerLeftLongitude       lower left longitude (coordinate)
	 * @param upperRightLatitude       upper right latitude (coordinate)
	 * @param upperRightLongitude      upper right longitude (coordinate)
	 * @param initDate                 initial date
	 * @param endDate                  end date
	 * @param inputdownloadingPhaseTag inputdownloading phase tag
	 * @param preprocessingPhaseTag    preprocessing phase tag
	 * @param processingPhaseTag       processing phase tag
	 * @param priority                 priority of new tasks
	 * @param email                    user email
	 */
	public void addNewTasks(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude, Date initDate, Date endDate, String inputdownloadingPhaseTag,
			String preprocessingPhaseTag, String processingPhaseTag, int priority, String email) {

		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(initDate);
		GregorianCalendar endCal = new GregorianCalendar();
		endCal.setTime(endDate);
		endCal.add(Calendar.DAY_OF_YEAR, 1);

		ExecutionScriptTag imageDockerInputdownloading = getExecutionScriptTag(ExecutionScriptTagUtil.INPUT_DOWNLOADER,
				inputdownloadingPhaseTag);
		ExecutionScriptTag imageDockerPreprocessing = getExecutionScriptTag(ExecutionScriptTagUtil.PRE_PROCESSING,
				preprocessingPhaseTag);
		ExecutionScriptTag imageDockerProcessing = getExecutionScriptTag(ExecutionScriptTagUtil.PROCESSING,
				processingPhaseTag);

		String digestInputdownloading = getDigest(imageDockerInputdownloading);
		String digestPreprocessing = getDigest(imageDockerPreprocessing);
		String digestProcessing = getDigest(imageDockerProcessing);

		Set<String> regions = regionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude);

		while (cal.before(endCal)) {
			int startingYear = cal.get(Calendar.YEAR);
			List<String> datasets = DatasetUtil.getSatsInOperationByYear(startingYear);

			for (String dataset : datasets) {
				LOGGER.debug("Adding new tasks with dataset " + dataset);

				for (String region : regions) {
					String taskId = UUID.randomUUID().toString();

					SapsImage task = addNewTaskInCatalog(taskId, dataset, region, cal.getTime(), priority, email,
							inputdownloadingPhaseTag, digestInputdownloading, preprocessingPhaseTag,
							digestPreprocessing, processingPhaseTag, digestProcessing);
					addTimestampTaskInCatalog(task, "updates task [" + taskId + "] timestamp");
				}
			}
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
	}

	/**
	 * This function gets script tag based in repository.
	 *
	 * @param repository task repository
	 * @param tag        task tag
	 * @return task information (tag, repository, type and name)
	 */
	private ExecutionScriptTag getExecutionScriptTag(String repository, String tag) {
		try {
			return ExecutionScriptTagUtil.getExecutionScriptTag(tag, repository);
		} catch (SapsException e) {
			LOGGER.error("Error while trying get tag and repository Docker.", e);
			return null;
		}
	}

	/**
	 * This function gets immutable identifier based in repository and tag
	 * 
	 * @param imageDockerInfo image docker information
	 * @return immutable identifier that match with repository and tag passed
	 */
	private String getDigest(ExecutionScriptTag imageDockerInfo) {

		String dockerRepository = imageDockerInfo.getDockerRepository();
		String dockerTag = imageDockerInfo.getDockerTag();

		String result = null;

		try {
			Process builder = new ProcessBuilder("bash", "./scripts/get_digest.sh", dockerRepository, dockerTag)
					.start();

			LOGGER.debug("Waiting for the process for execute command: " + builder.toString());
			builder.waitFor();

			if (builder.exitValue() != 0)
				throw new Exception("Process output exit code: " + builder.exitValue());

			BufferedReader reader = new BufferedReader(new InputStreamReader(builder.getInputStream()));

			result = reader.readLine();
		} catch (Exception e) {
			LOGGER.error("Error while trying get digest from Docker image [" + dockerRepository + "] with tag ["
					+ dockerTag + "].", e);
		}

		return result;
	}

	/**
	 * This function get all tasks in Catalog.
	 * 
	 * @param catalog catalog component
	 * @return SAPS image list
	 */
	private List<SapsImage> getAllTasksInCatalog() {
		return CatalogUtils.getAllTasks(catalog, "get all tasks");
	}

	/**
	 * This function get all tasks.
	 * 
	 * @return SAPS image list
	 */
	public List<SapsImage> getAllTasks() {
		return getAllTasksInCatalog();
	}

	// TODO
	public List<Ward> getUsersToNotify() throws SQLException {
		List<Ward> wards = catalog.getUsersToNotify();
		return wards;
	}

	/**
	 * This function gets task by id.
	 * 
	 * @param taskId task id to be searched
	 * @return SAPS image with id
	 */
	private SapsImage getTaskByIdInCatalog(String taskId) {
		return CatalogUtils.getTaskById(catalog, taskId, "gets task with id [" + taskId + "]");
	}

	/**
	 * This function gets task by id.
	 * 
	 * @param taskId task id to be searched
	 * @return SAPS image with id
	 * @throws SQLException
	 */
	public SapsImage getTaskById(String taskId) {
		return getTaskByIdInCatalog(taskId);
	}

	/**
	 * This function gets tasks in specific state in Catalog.
	 * 
	 * @param state   specific state for get tasks
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	private List<SapsImage> getTasksInCatalog(ImageTaskState state, int limit, String message) {
		return CatalogUtils.getTasks(catalog, state, limit, message);
	}

	/**
	 * This function gets tasks in specific state.
	 * 
	 * @param state   specific state for get tasks
	 * @param limit   limit value of tasks to take
	 * @param message information message
	 * @return tasks in specific state
	 */
	public List<SapsImage> getTasksInState(ImageTaskState state) throws SQLException {
		return getTasksInCatalog(state, Catalog.UNLIMITED, "gets tasks with " + state.getValue() + " state");
	}

	/**
	 * This function gets all processed tasks in Catalog.
	 * 
	 * @param region                   task region
	 * @param initDate                 initial date
	 * @param endDate                  end date
	 * @param inputdownloadingPhaseTag inputdownloading phase tag
	 * @param preprocessingPhaseTag    preprocessing phase tag
	 * @param processingPhaseTag       processing phase tag
	 * @return processed tasks list
	 */
	private List<SapsImage> getProcessedTasksInCatalog(String region, Date initDate, Date endDate,
			String inputdownloadingPhaseTag, String preprocessingPhaseTag, String processingPhaseTag) {
		return CatalogUtils.getProcessedTasks(catalog, region, initDate, endDate, inputdownloadingPhaseTag,
				preprocessingPhaseTag, processingPhaseTag,
				"gets all processed tasks with region [" + region + "], inputdownloading tag ["
						+ inputdownloadingPhaseTag + "], preprocessing tag [" + preprocessingPhaseTag
						+ "], processing tag [" + processingPhaseTag + "] beetwen " + initDate + " and " + endDate);
	}

	/**
	 * This function search all processed tasks from area (between latitude and
	 * longitude coordinates) between initial date and end date with
	 * inputdownloading, preprocessing and processing tags.
	 * 
	 * @param lowerLeftLatitude        lower left latitude (coordinate)
	 * @param lowerLeftLongitude       lower left longitude (coordinate)
	 * @param upperRightLatitude       upper right latitude (coordinate)
	 * @param upperRightLongitude      upper right longitude (coordinate)
	 * @param initDate                 initial date
	 * @param endDate                  end date
	 * @param inputdownloadingPhaseTag inputdownloading phase tag
	 * @param preprocessingPhaseTag    preprocessing phase tag
	 * @param processingPhaseTag       processing phase tag
	 * @return processed tasks list following description
	 */
	public List<SapsImage> searchProcessedTasks(String lowerLeftLatitude, String lowerLeftLongitude,
			String upperRightLatitude, String upperRightLongitude, Date initDate, Date endDate,
			String inputdownloadingPhaseTag, String preprocessingPhaseTag, String processingPhaseTag) {

		List<SapsImage> filteredTasks = new ArrayList<>();
		Set<String> regions = regionsFromArea(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude);

		for (String region : regions) {
			List<SapsImage> tasksInCurrentRegion = getProcessedTasksInCatalog(region, initDate, endDate,
					inputdownloadingPhaseTag, preprocessingPhaseTag, processingPhaseTag);
			filteredTasks.addAll(tasksInCurrentRegion);
		}
		return filteredTasks;
	}
}