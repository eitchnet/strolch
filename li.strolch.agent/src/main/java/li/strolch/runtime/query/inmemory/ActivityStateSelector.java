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
package li.strolch.runtime.query.inmemory;

import li.strolch.model.State;
import li.strolch.model.activity.Activity;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class ActivityStateSelector implements Selector<Activity> {

	private State state;

	/**
	 * @param state
	 */
	public ActivityStateSelector(State state) {
		this.state = state;
	}

	@Override
	public boolean select(Activity element) {
		return this.state.equals(element.getState());
	}
}
