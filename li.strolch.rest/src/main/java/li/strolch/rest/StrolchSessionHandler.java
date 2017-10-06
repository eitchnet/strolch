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
package li.strolch.rest;

import java.util.List;
import java.util.Locale;

import li.strolch.exception.StrolchNotAuthenticatedException;
import li.strolch.privilege.base.PrivilegeException;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.privilege.model.Usage;
import li.strolch.rest.model.UserSession;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public interface StrolchSessionHandler {

	Certificate authenticate(String username, char[] password);

	Certificate validate(String authToken) throws StrolchNotAuthenticatedException;

	PrivilegeContext validate(Certificate certificate) throws StrolchNotAuthenticatedException;

	List<UserSession> getSessions(Certificate certificate);

	UserSession getSession(Certificate certificate, String sessionId);

	void invalidate(Certificate certificate);

	void invalidate(Certificate certificate, String sessionId);

	void setSessionLocale(Certificate certificate, String sessionId, Locale locale);

	/**
	 * Initiate a password reset challenge for the given username
	 *
	 * @param usage
	 * 		the usage for which the challenge is requested
	 * @param username
	 * 		the username of the user to initiate the challenge for
	 */
	void initiateChallengeFor(Usage usage, String username);

	/**
	 * Validate the response of a challenge for the given username
	 *
	 * @param username
	 * 		the username of the user for which the challenge is to be validated
	 * @param challenge
	 * 		the challenge from the user
	 *
	 * @return certificate with which the user can access the system with the {@link Usage} set to the value from the
	 * initiated challenge
	 *
	 * @throws PrivilegeException
	 * 		if anything goes wrong
	 */
	Certificate validateChallenge(String username, String challenge) throws PrivilegeException;

}
