package li.strolch.agent.impl;

import static li.strolch.runtime.StrolchConstants.INTERPRETATION_RESOURCE_REF;

import java.text.MessageFormat;

import li.strolch.agent.api.ResourceMap;
import li.strolch.exception.StrolchException;
import li.strolch.model.Resource;
import li.strolch.model.parameter.Parameter;
import li.strolch.model.parameter.StringParameter;
import li.strolch.persistence.api.ResourceDao;
import li.strolch.persistence.api.StrolchTransaction;

public class CachedResourceMap extends CachedElementMap<Resource> implements ResourceMap {

	@Override
	public Resource getBy(StrolchTransaction tx, StringParameter refP) throws StrolchException {

		if (!refP.getInterpretation().equals(INTERPRETATION_RESOURCE_REF)) {
			String msg = MessageFormat.format("{0} is not an Resource reference as its interpretation is not {1}", //$NON-NLS-1$
					refP.getLocator(), INTERPRETATION_RESOURCE_REF);
			throw new StrolchException(msg);
		}

		if (refP.getUom().equals(Parameter.UOM_NONE)) {
			String msg = MessageFormat.format("{0} is not an Resource reference as its UOM is not set to a type!", //$NON-NLS-1$
					refP.getLocator());
			throw new StrolchException(msg);
		}

		String type = refP.getUom();
		String id = refP.getValue();

		return getBy(tx, type, id);
	}

	@Override
	protected ResourceDao getDao(StrolchTransaction tx) {
		return tx.getPersistenceHandler().getResourceDao(tx);
	}
}
