/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
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
package li.strolch.model.timedstate;

import li.strolch.model.StrolchValueType;
import li.strolch.model.timevalue.impl.StringSetValue;
import li.strolch.model.visitor.StrolchElementVisitor;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class StringSetTimedState extends AbstractStrolchTimedState<StringSetValue> {

	private static final long serialVersionUID = 1L;

	public StringSetTimedState() {
		super();
	}

	public StringSetTimedState(String id, String name) {
		super(id, name);
	}

	@Override
	public void setStateFromStringAt(Long time, String value) {
		assertNotReadonly();
		getTimeEvolution().setValueAt(time, new StringSetValue(value));
	}

	@Override
	public String getType() {
		return StrolchValueType.STRING_SET.getType();
	}

	@Override
	public <U> U accept(StrolchElementVisitor<U> visitor) {
		return visitor.visitStringState(this);
	}

	@Override
	public StringSetTimedState getClone() {
		StringSetTimedState clone = new StringSetTimedState();
		fillClone(clone);
		return clone;
	}
}
