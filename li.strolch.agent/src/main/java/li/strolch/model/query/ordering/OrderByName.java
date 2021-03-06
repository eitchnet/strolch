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
package li.strolch.model.query.ordering;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class OrderByName extends StrolchQueryOrdering {

	/**
	 * Creates this ordering with ascending order
	 */
	public OrderByName() {
		super(true);
	}

	/**
	 * Creates this ordering the given ascending order
	 * 
	 * @param ascending
	 *            true for ascending, false for descending order
	 */
	public OrderByName(boolean ascending) {
		super(ascending);
	}

	@Override
	public void accept(StrolchQueryOrderingVisitor visitor) {
		visitor.visit(this);
	}
}
