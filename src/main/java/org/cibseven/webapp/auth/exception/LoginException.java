package org.cibseven.webapp.auth.exception;

public class LoginException extends AuthenticationException {
	
	public LoginException() {
		super();
	}	
	
	public LoginException(Object ...data) {
		super(data);
	}	
	
	@Override
	public boolean isNoAuth() {
		return false;
	}
	
}
