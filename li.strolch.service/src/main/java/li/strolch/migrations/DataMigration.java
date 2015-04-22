/*
 * Copyright 2015 Robert von Burg <eitch@eitchnet.ch>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.migrations;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.command.XmlImportModelCommand;
import li.strolch.exception.StrolchException;
import li.strolch.model.ModelStatistics;
import li.strolch.persistence.api.StrolchTransaction;
import ch.eitchnet.privilege.model.Certificate;
import ch.eitchnet.utils.Version;

public class DataMigration extends Migration {

	public DataMigration(String realm, Version version, File dataFile) {
		super(realm, version, dataFile);
	}

	@Override
	public void migrate(ComponentContainer container, Certificate certificate) {

		XmlImportModelCommand command;
		try (StrolchTransaction tx = openTx(container, certificate)) {

			command = new XmlImportModelCommand(container, tx);
			command.setModelFile(getDataFile());
			command.setAddOrders(true);
			command.setAddResources(true);
			command.setUpdateOrders(true);
			command.setUpdateResources(true);
			command.setOrderTypes(Collections.emptySet());
			command.setResourceTypes(Collections.emptySet());

			tx.addCommand(command);
			buildMigrationVersionChangeCommand(container, tx);
			tx.commitOnClose();
		} catch (Exception e) {
			String msg = MessageFormat.format("Migration of {0} failed due to {1}", getVersion(), e.getMessage());
			throw new StrolchException(msg, e);
		}

		ModelStatistics statistics = command.getStatistics();
		logger.info(MessageFormat
				.format("[{0}] Data migration for {1} loaded {2} Resources and {3} Orders.", getRealm(), getVersion(), statistics.nrOfResources, statistics.nrOfOrders)); //$NON-NLS-1$
	}
}
