package org.fogbowcloud.saps.engine.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.blowout.core.model.Command;
import org.fogbowcloud.blowout.core.model.Specification;
import org.fogbowcloud.blowout.core.model.Task;
import org.fogbowcloud.blowout.core.model.TaskImpl;

public class SapsTask {

	public static final String METADATA_TASK_ID = "task_id";
	private static final String METADATA_REPOS_USER = "repository_user";
	private static final String METADATA_NFS_SERVER_IP = "nfs_server_ip";
	private static final String METADATA_NFS_SERVER_PORT = "nfs_server_port";
	private static final String METADATA_EXPORT_PATH = "volume_export_path";
	private static final String METADATA_MOUNT_POINT = "mount_point";
	private static final String METADATA_WORKER_CONTAINER_REPOSITORY = "worker_container_repository";
	private static final String METADATA_WORKER_CONTAINER_TAG = "worker_container_tag";
	private static final String METADATA_MAX_TASK_EXECUTION_TIME = "max_task_execution_time";

	private static final String WORKER_SANDBOX = "worker_sandbox";
	private static final String WORKER_REMOTE_USER = "worker_remote_user";
	private static final String WORKER_EXPORT_PATH = "saps_export_path";
	private static final String WORKER_MOUNT_POINT = "worker_mount_point";
	private static final String WORKER_TASK_TIMEOUT = "worker_task_timeout";
	private static final String SAPS_WORKER_RUN_SCRIPT_PATH = "saps_worker_run_script_path";
	private static final String MAX_RESOURCE_CONN_RETRIES = "max_resource_conn_retries";

	private static final Logger LOGGER = Logger.getLogger(SapsTask.class);

	public static TaskImpl createSapsTask(TaskImpl taskImpl, Properties properties,
			Specification spec, String federationMember, String nfsServerIP, String nfsServerPort,
			String workerContainerRepository, String workerContainerTag) {
		LOGGER.debug("Creating Saps task " + taskImpl.getId() + " for Blowout");

		settingCommonTaskMetadata(properties, taskImpl);

		// setting image R execution properties
		taskImpl.putMetadata(METADATA_TASK_ID, taskImpl.getId());
		taskImpl.putMetadata(METADATA_WORKER_CONTAINER_REPOSITORY, workerContainerRepository);
		taskImpl.putMetadata(METADATA_WORKER_CONTAINER_TAG, workerContainerTag);
		taskImpl.putMetadata(METADATA_EXPORT_PATH, properties.getProperty(WORKER_EXPORT_PATH));
		taskImpl.putMetadata(METADATA_MAX_TASK_EXECUTION_TIME,
				properties.getProperty(METADATA_MAX_TASK_EXECUTION_TIME));

		taskImpl.putMetadata(METADATA_MOUNT_POINT, properties.getProperty(WORKER_MOUNT_POINT));

		taskImpl.putMetadata(METADATA_NFS_SERVER_IP, nfsServerIP);
		taskImpl.putMetadata(METADATA_NFS_SERVER_PORT, nfsServerPort);
		taskImpl.putMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH,
				taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX) + "/exit_" + taskImpl.getId());

		// cleaning environment
		String cleanEnvironment = "sudo rm -rf " + properties.getProperty(WORKER_SANDBOX);
		taskImpl.addCommand(new Command(cleanEnvironment, Command.Type.REMOTE));

		// creating sandbox
		String mkdirCommand = "mkdir -p " + taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX);
		taskImpl.addCommand(new Command(mkdirCommand, Command.Type.REMOTE));

		// creating run worker script for this task
		File localRunScriptFile = createScriptFile(properties, taskImpl);
		String remoteRunScriptPath = taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX)
				+ File.separator + localRunScriptFile.getName();

		// adding commands
		String scpUploadCommand = createSCPUploadCommand(localRunScriptFile.getAbsolutePath(),
				remoteRunScriptPath);
		LOGGER.debug("ScpUploadCommand=" + scpUploadCommand);
		taskImpl.addCommand(new Command(scpUploadCommand, Command.Type.LOCAL));

		// adding remote commands
		String remoteChmodRunScriptCommand = createChmodScriptCommand(remoteRunScriptPath);
		taskImpl.addCommand(new Command(remoteChmodRunScriptCommand, Command.Type.REMOTE));

		String remoteExecScriptCommand = createRemoteScriptExecCommand(remoteRunScriptPath,
				taskImpl);
		LOGGER.debug("remoteExecCommand=" + remoteExecScriptCommand);
		taskImpl.addCommand(new Command(remoteExecScriptCommand, Command.Type.REMOTE));

		return taskImpl;
	}

	private static String createSCPUploadCommand(String localFilePath, String remoteFilePath) {
		return "scp -i $PRIVATE_KEY_FILE -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -P $SSH_PORT "
				+ localFilePath + " $SSH_USER@$HOST:" + remoteFilePath;
	}

	private static void settingCommonTaskMetadata(Properties properties, Task task) {
		// task property
		task.putMetadata(TaskImpl.METADATA_MAX_RESOURCE_CONN_RETRIES,
				properties.getProperty(MAX_RESOURCE_CONN_RETRIES));

		// sdexs properties
		task.putMetadata(TaskImpl.METADATA_SANDBOX,
				properties.getProperty(WORKER_SANDBOX) + "/" + task.getId());
		task.putMetadata(TaskImpl.METADATA_REMOTE_OUTPUT_FOLDER,
				properties.getProperty(WORKER_SANDBOX) + "/output");
		task.putMetadata(TaskImpl.METADATA_TASK_TIMEOUT,
				properties.getProperty(WORKER_TASK_TIMEOUT));

		// repository properties
		task.putMetadata(METADATA_REPOS_USER, properties.getProperty(WORKER_REMOTE_USER));
		task.putMetadata(METADATA_MOUNT_POINT, properties.getProperty(WORKER_MOUNT_POINT));
	}

	private static String createChmodScriptCommand(String remoteScript) {
		return "\"chmod +x " + remoteScript + "\"";
	}

	private static String createRemoteScriptExecCommand(String remoteScript, TaskImpl taskImpl) {

		Path pathToRemoteScript = Paths.get(remoteScript);
		String execScriptCommand = null;
		String runOutName = pathToRemoteScript.getFileName().toString() + "." + "out";
		String runErrName = pathToRemoteScript.getFileName().toString() + "." + "err";

		execScriptCommand = "\"nohup " + remoteScript + " >> "
				+ taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX) + File.separator + runOutName
				+ " 2>> " + taskImpl.getMetadata(TaskImpl.METADATA_SANDBOX) + File.separator
				+ runErrName + "\"";

		return execScriptCommand;
	}

	private static File createScriptFile(Properties props, TaskImpl task) {
		File tempFile = null;
		FileOutputStream fos = null;
		FileInputStream fis = null;
		try {
			tempFile = File.createTempFile("temp-worker-run-", ".sh");
			fis = new FileInputStream(props.getProperty(SAPS_WORKER_RUN_SCRIPT_PATH));

			String origExec = IOUtils.toString(fis);
			fos = new FileOutputStream(tempFile);
			IOUtils.write(replaceVariables(props, task, origExec), fos);
		} catch (IOException e) {
			LOGGER.error("Error while creating script " + tempFile.getName() + " file", e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (Throwable t) {
				LOGGER.error(t);
				// Do nothing, best effort
			}
		}
		return tempFile;
	}

	public static String replaceVariables(Properties props, TaskImpl task, String command) {
		command = command.replaceAll(Pattern.quote("${TASK_ID}"),
				task.getMetadata(METADATA_TASK_ID));
		command = command.replaceAll(Pattern.quote("${SANDBOX}"),
				task.getMetadata(TaskImpl.METADATA_SANDBOX));
		command = command.replaceAll(Pattern.quote("${EXPORT_PATH}"),
				task.getMetadata(METADATA_EXPORT_PATH));
		command = command.replaceAll(Pattern.quote("${SAPS_MOUNT_POINT}"),
				task.getMetadata(METADATA_MOUNT_POINT));
		command = command.replaceAll(Pattern.quote("${NFS_SERVER_IP}"),
				task.getMetadata(METADATA_NFS_SERVER_IP));
		command = command.replaceAll(Pattern.quote("${NFS_SERVER_PORT}"),
				task.getMetadata(METADATA_NFS_SERVER_PORT));
		command = command.replaceAll(Pattern.quote("${WORKER_CONTAINER_REPOSITORY}"),
				task.getMetadata(METADATA_WORKER_CONTAINER_REPOSITORY));
		command = command.replaceAll(Pattern.quote("${WORKER_CONTAINER_TAG}"),
				task.getMetadata(METADATA_WORKER_CONTAINER_TAG));
		command = command.replaceAll(Pattern.quote("${REMOTE_COMMAND_EXIT_PATH}"),
				task.getMetadata(TaskImpl.METADATA_REMOTE_COMMAND_EXIT_PATH));

		LOGGER.debug("Command that will be executed: " + command);
		return command;
	}
}
