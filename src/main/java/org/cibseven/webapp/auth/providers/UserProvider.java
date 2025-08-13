/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.auth.providers;

import org.cibseven.webapp.auth.Login;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.exception.AuthenticationException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Interface UserProvider.
 *
 * @param <T> the generic type
 */
public interface UserProvider<T extends Login> {

	/**
	 * Login.
	 *
	 * @param params the params
	 * @param rq     the rq
	 * @return the user
	 * @throws AuthenticationException the authentication exception
	 */
	User login(T params, HttpServletRequest rq) throws AuthenticationException;

	/**
	 * Gets the user info.
	 *
	 * @param user   the user
	 * @param userId the user id
	 * @return the user info
	 */
	User getUserInfo(User user, String userId);

	/**
	 * Logout.
	 *
	 * @param user the user
	 */
	void logout(User user);

	/**
	 * Authenticate.
	 *
	 * @param rq the rq
	 * @return the user
	 * @throws AuthenticationException the authentication exception
	 */
	User authenticate(HttpServletRequest rq) throws AuthenticationException;

}
