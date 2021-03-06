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
package li.strolch.privilege.base;

/**
 * Main {@link RuntimeException} thrown if something goes wrong in Privilege
 *
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class PrivilegeException extends RuntimeException {

	/**
	 * Default constructor
	 *
	 * @param string
	 * 		message to go with the exception
	 */
	public PrivilegeException(String string) {
		super(string);
	}

	/**
	 * Constructor with underlying exception
	 *
	 * @param string
	 * 		message to go with the exception
	 * @param t
	 * 		throwable to wrap with this exception which is the underlying exception of this exception
	 */
	public PrivilegeException(String string, Throwable t) {
		super(string, t);
	}
}
