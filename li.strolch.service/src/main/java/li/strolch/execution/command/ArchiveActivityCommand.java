package li.strolch.execution.command;

import static li.strolch.execution.policy.ActivityArchivalPolicy.DEFAULT_ACTIVITY_ARCHIVAL;

import li.strolch.execution.policy.ActivityArchivalPolicy;
import li.strolch.model.Locator;
import li.strolch.model.activity.Activity;
import li.strolch.model.policy.PolicyDef;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.Command;
import li.strolch.utils.dbc.DBC;

public class ArchiveActivityCommand extends Command {

	private Locator activityLoc;

	public ArchiveActivityCommand(StrolchTransaction tx) {
		super(tx);
	}

	public void setActivityLoc(Locator activityLoc) {
		this.activityLoc = activityLoc;
	}

	@Override
	public void validate() {
		DBC.PRE.assertNotNull("activity can not be null!", this.activityLoc);
	}

	@Override
	public void doCommand() {
		Activity activity = tx().getActivityBy(this.activityLoc.get(1), this.activityLoc.get(2));
		if (activity == null) {
			logger.error("Activity " + this.activityLoc + " does not exist anymore, can not archive!");
			return;
		}

		logger.info("Activity " + activity.getLocator() + " is in state " + activity.getState());

		PolicyDef policyDef = activity.getPolicyDef(ActivityArchivalPolicy.class, DEFAULT_ACTIVITY_ARCHIVAL);
		ActivityArchivalPolicy archivalPolicy = tx().getPolicy(policyDef);
		archivalPolicy.archive(activity);
	}
}
