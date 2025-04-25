package org.cibseven.webapp.auth.exception;

public class TokenExpiredException extends AuthenticationException {
	
	public TokenExpiredException(Object...newTokens) {
		super(newTokens);
	}	
	
	@Override
	public boolean isNoAuth() {
		return false;
	}
	
}
