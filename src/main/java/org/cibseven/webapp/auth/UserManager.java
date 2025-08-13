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
package org.cibseven.webapp.auth;

import java.util.Locale;

import org.cibseven.webapp.auth.providers.UserProvider;

/**
 * The Interface UserManager.
 *
 * @param <W> the generic type
 * @param <R> the generic type
 */
public interface UserManager<W extends Login, R extends Login> extends UserProvider<R> {

	/**
	 * Update.
	 *
	 * @param user   the user
	 * @param params the params
	 * @return the user
	 */
	User update(User user, W params);

	/**
	 * Creates the.
	 *
	 * @param user   the user
	 * @param params the params
	 * @param loc    the loc
	 * @return the user
	 */
	User create(User user, W params, Locale loc);

	/**
	 * Delete.
	 *
	 * @param user   the user
	 * @param userId the user id
	 */
	void delete(User user, String userId);

}
