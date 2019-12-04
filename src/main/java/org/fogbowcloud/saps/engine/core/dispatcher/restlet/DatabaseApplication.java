package org.fogbowcloud.saps.engine.core.dispatcher.restlet;

import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.saps.engine.core.dispatcher.SubmissionDispatcher;
import org.fogbowcloud.saps.engine.core.dispatcher.Task;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.ImageResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.MainResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.ProcessedImagesResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.RegionResource;
import org.fogbowcloud.saps.engine.core.dispatcher.restlet.resource.UserResource;
import org.fogbowcloud.saps.engine.core.model.SapsImage;
import org.fogbowcloud.saps.engine.core.model.SapsUser;
import org.fogbowcloud.saps.engine.core.model.enums.ImageTaskState;
import org.fogbowcloud.saps.engine.exceptions.SapsException;
import org.fogbowcloud.saps.engine.utils.SapsPropertiesConstants;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.service.ConnectorService;
import org.restlet.service.CorsService;

public class DatabaseApplication extends Application {
	private static final String DB_WEB_STATIC_ROOT = "./dbWebHtml/static";

	public static final Logger LOGGER = Logger.getLogger(DatabaseApplication.class);

	private Properties properties;
	private SubmissionDispatcher submissionDispatcher;
	private Component restletComponent;

	public DatabaseApplication(Properties properties) throws Exception {
		if (!checkProperties(properties))
			throw new SapsException("Error on validate the file. Missing properties for start Database Application.");

		this.properties = properties;
		this.submissionDispatcher = new SubmissionDispatcher(properties);

		// CORS configuration
		CorsService cors = new CorsService();
		cors.setAllowedOrigins(new HashSet<>(Collections.singletonList("*")));
		cors.setAllowedCredentials(true);
		getServices().add(cors);
	}

	private boolean checkProperties(Properties properties2) {
		if (!properties.containsKey(SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT)) {
			LOGGER.error("Required property " + SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT + " was not set");
			return false;
		}

		LOGGER.debug("All properties are set");
		return true;
	}

	public void start() throws Exception {
		Integer restServerPort = Integer
				.valueOf((String) properties.get(SapsPropertiesConstants.SUBMISSION_REST_SERVER_PORT));

		LOGGER.info("Starting service on port [ " + restServerPort + "]");

		ConnectorService corsService = new ConnectorService();
		this.getServices().add(corsService);

		this.restletComponent = new Component();
		this.restletComponent.getServers().add(Protocol.HTTP, restServerPort);
		this.restletComponent.getClients().add(Protocol.FILE);
		this.restletComponent.getDefaultHost().attach(this);

		this.restletComponent.start();
	}

	public void stopServer() throws Exception {
		this.restletComponent.stop();
	}

	@Override
	public Restlet createInboundRoot() {
		// TODO: change endpoints for new SAPS dashboard
		Router router = new Router(getContext());
		router.attach("/", MainResource.class);
		router.attach("/ui/{requestPath}", MainResource.class);
		router.attach("/static",
				new Directory(getContext(), "file:///" + new File(DB_WEB_STATIC_ROOT).getAbsolutePath()));
		router.attach("/users", UserResource.class);
		router.attach("/processings", ImageResource.class);
		router.attach("/images/{imgName}", ImageResource.class);
		router.attach("/regions/details", RegionResource.class);
		router.attach("/regions/search", RegionResource.class);
		router.attach("/email", ProcessedImagesResource.class);

		return router;
	}

	public List<SapsImage> getTasks() throws SQLException, ParseException {
		return submissionDispatcher.getTaskListInDB();
	}

	public List<SapsImage> getTasksInState(ImageTaskState imageState) throws SQLException {
		return this.submissionDispatcher.getTasksInState(imageState);
	}

	public SapsImage getTask(String taskId) throws SQLException {
		return submissionDispatcher.getTaskInDB(taskId);
	}

	public List<Task> addTasks(String lowerLeftLatitude, String lowerLeftLongitude, String upperRightLatitude,
			String upperRightLongitude, Date initDate, Date endDate, String inputdownloadingPhaseTag,
			String preprocessingPhaseTag, String processingPhaseTag, String priority, String email) {
		return submissionDispatcher.addNewTasks(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude, initDate, endDate, inputdownloadingPhaseTag, preprocessingPhaseTag,
				processingPhaseTag, priority, email);
	}

	public void createUser(String userEmail, String userName, String userPass, boolean userState, boolean userNotify,
			boolean adminRole) throws SQLException {
		submissionDispatcher.addUserInDB(userEmail, userName, userPass, userState, userNotify, adminRole);
	}

	public void addUserNotify(String submissionId, String taskId, String userEmail) throws SQLException {
		submissionDispatcher.addTaskNotificationIntoDB(submissionId, taskId, userEmail);
	}

	public boolean isUserNotifiable(String userEmail) throws SQLException {
		return submissionDispatcher.isUserNotifiable(userEmail);
	}

	public SapsUser getUser(String userEmail) {
		return submissionDispatcher.getUser(userEmail);
	}

	public Properties getProperties() {
		return properties;
	}

	public List<SapsImage> searchProcessedTasks(String lowerLeftLatitude, String lowerLeftLongitude,
			String upperRightLatitude, String upperRightLongitude, Date initDate, Date endDate,
			String inputPreprocessing, String inputGathering, String algorithmExecution) {
		return submissionDispatcher.searchProcessedTasks(lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
				upperRightLongitude, initDate, endDate, inputPreprocessing, inputGathering, algorithmExecution);
	}
}
