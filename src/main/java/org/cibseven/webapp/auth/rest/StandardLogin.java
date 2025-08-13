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
package org.cibseven.webapp.auth.rest;

import org.cibseven.webapp.auth.Login;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The Class StandardLogin.
 */
@Schema(description = "When passing this as body you must include 'type': 'org.cibseven.webapp.auth.rest.StandardLogin'")
public class StandardLogin implements Login {

	/** The username. */
	String username;
	/** The password. */
	String password;

	/**
	 * Instantiates a new standard login.
	 */
	public StandardLogin() {
	}

	/**
	 * Instantiates a new standard login.
	 *
	 * @param username the username
	 * @param password the password
	 */
	public StandardLogin(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Gets the username.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username.
	 *
	 * @param username the new username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Gets the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password.
	 *
	 * @param password the new password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
