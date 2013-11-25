package li.strolch.testbase.runtime;

import java.io.File;
import java.text.MessageFormat;

import li.strolch.runtime.agent.StrolchAgent;
import li.strolch.runtime.component.ComponentContainer;
import li.strolch.runtime.configuration.RuntimeConfiguration;
import li.strolch.runtime.privilege.StrolchPrivilegeHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.eitchnet.utils.helper.FileHelper;

public class RuntimeMock {

	private static final Logger logger = LoggerFactory.getLogger(RuntimeMock.class);
	private static final String TARGET = "target"; //$NON-NLS-1$

	private static ComponentContainer container;
	private static StrolchAgent agent;

	/**
	 * @return the container
	 */
	public static ComponentContainer getContainer() {
		return container;
	}

	public static StrolchPrivilegeHandler getPrivilegeHandler() {
		return container.getComponent(StrolchPrivilegeHandler.class);
	}

	public static void mockRuntime(File rootPathF, File configSrc) {

		if (!rootPathF.getParentFile().getName().equals(TARGET)) {
			String msg = "Mocking path must be in a maven target: {0}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, rootPathF.getAbsolutePath());
			throw new RuntimeException(msg);
		}

		if (!configSrc.isDirectory() || !configSrc.canRead()) {
			String msg = "Could not find config source in: {0}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, configSrc.getAbsolutePath());
			throw new RuntimeException(msg);
		}

		if (rootPathF.exists()) {
			if (!FileHelper.deleteFile(rootPathF, true)) {
				String msg = "Failed to delete {0}"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, rootPathF.getAbsolutePath());
				throw new RuntimeException(msg);
			}
		}

		if (!rootPathF.mkdirs()) {
			String msg = "Failed to create {0}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, rootPathF.getAbsolutePath());
			throw new RuntimeException(msg);
		}

		File configPathF = new File(rootPathF, RuntimeConfiguration.PATH_CONFIG);
		configPathF.mkdir();

		if (!FileHelper.copy(configSrc.listFiles(), configPathF, false)) {
			String msg = "Failed to copy source configs from {0} to {1}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, configSrc.getAbsolutePath(), configPathF.getAbsolutePath());
			throw new RuntimeException(msg);
		}
	}

	public static void startContainer(File rootPathF) {

		try {
			StrolchAgent agent = new StrolchAgent();
			agent.setup(rootPathF);
			agent.initialize();
			agent.start();

			RuntimeMock.agent = agent;
			RuntimeMock.container = agent.getContainer();

		} catch (Exception e) {
			logger.error("Failed to start mocked container due to: " + e.getMessage(), e); //$NON-NLS-1$
			destroyRuntime();
			throw e;
		}
	}

	public static void destroyRuntime() {

		if (agent == null)
			return;

		try {
			agent.stop();
		} catch (Exception e) {
			logger.info("Failed to stop container: " + e.getMessage()); //$NON-NLS-1$
		}

		try {
			agent.destroy();
		} catch (Exception e) {
			logger.info("Failed to destroy container: " + e.getMessage()); //$NON-NLS-1$
		}
	}
}
