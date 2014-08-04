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
package li.strolch.model.query;

import ch.eitchnet.utils.dbc.DBC;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public abstract class StrolchQuery<T extends QueryVisitor> {

	private Navigation navigation;
	private Selection selection;

	public StrolchQuery(Navigation navigation) {
		this.navigation = navigation;
	}

	public boolean hasNavigation() {
		return this.navigation != null;
	}

	public boolean hasSelection() {
		return this.selection != null && this.selection.hasSelection();
	}

	public Selection getSelection() {
		return this.selection;
	}

	public void with(Selection selection) {
		DBC.PRE.assertNull("A selection is already set! Use a boolean operator to perform multiple selections", //$NON-NLS-1$
				this.selection);
		this.selection = selection;
	}

	/**
	 * @deprecated use {@link #with(Selection)} instead
	 */
	@Deprecated
	public void select(Selection selection) {
		with(selection);
	}

	public AndSelection and() {
		DBC.PRE.assertNull("A selection is already set! Create hierarchical boolean selections", this.selection); //$NON-NLS-1$
		AndSelection and = new AndSelection();
		this.selection = and;
		return and;
	}

	public OrSelection or() {
		DBC.PRE.assertNull("A selection is already set! Create hierarchical boolean selections", this.selection); //$NON-NLS-1$
		OrSelection or = new OrSelection();
		this.selection = or;
		return or;
	}

	public void not(Selection selection) {
		DBC.PRE.assertNull("A selection is already set! Create hierarchical boolean selections", this.selection); //$NON-NLS-1$
		this.selection = new NotSelection(selection);
	}

	public void accept(T visitor) {
		DBC.PRE.assertNotNull("No navigation set!", this.navigation); //$NON-NLS-1$
		DBC.PRE.assertNotNull("No selection defined!", this.selection); //$NON-NLS-1$
		this.navigation.accept(visitor);
		this.selection.accept(visitor);
	}
}
