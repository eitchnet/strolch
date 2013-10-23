package li.strolch.runtime;

import java.text.MessageFormat;
import java.util.Map;

public class StrolchConfiguration {

	private final RuntimeConfiguration runtimeConfiguration;
	private final Map<String, ComponentConfiguration> configurationByComponent;

	public StrolchConfiguration(RuntimeConfiguration runtimeConfiguration,
			Map<String, ComponentConfiguration> configurationByComponent) {
		this.runtimeConfiguration = runtimeConfiguration;
		this.configurationByComponent = configurationByComponent;
	}

	public RuntimeConfiguration getRuntimeConfiguration() {
		return this.runtimeConfiguration;
	}

	public ComponentConfiguration getComponentConfiguration(String componentName) {
		ComponentConfiguration componentConfiguration = this.configurationByComponent.get(componentName);
		if (componentConfiguration == null) {
			String msg = "No configuration exists for the component {0}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, componentName);
			throw new StrolchConfigurationException(msg);
		}
		return componentConfiguration;
	}
}
