package org.cibseven.webapp.auth.exception;

public class AuthenticationException extends RuntimeException {
	
	protected Object[] data;
	
	public Object[] getData() {
		return data;
	}
	
	public AuthenticationException(Object ...data) {
		this.data = data;
	}	
	
	public boolean isNoAuth() {
		return data.length == 0;
	}
	
}
