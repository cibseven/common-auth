package de.cib.auth.rest;

import de.cib.auth.Login;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "When passing this as body you must include 'type': 'de.cib.auth.rest.StandardLogin'")
public class StandardLogin implements Login {
	
	String username;
	String password;
	
	public StandardLogin() {}
	
	public StandardLogin(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
}
