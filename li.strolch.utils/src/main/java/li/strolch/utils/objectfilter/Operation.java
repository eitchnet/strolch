/*
 * Copyright 2013 Michael Gatto <michael@gatto.ch>
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
package li.strolch.utils.objectfilter;

/**
 * A discrete set of operations associated to some object / state
 *
 * @author Michael Gatto &lt;michael@gatto.ch&gt;
 */
public enum Operation {

	/**
	 * ADD The operation associated when an object was added/created
	 */
	ADD,

	/**
	 * MODIFY The operation associated when an object was modified
	 */
	MODIFY,

	/**
	 * REMOVE The operation associated when an object was removed
	 */
	REMOVE;
}
