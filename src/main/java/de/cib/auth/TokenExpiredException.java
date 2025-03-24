package de.cib.auth;

public class TokenExpiredException extends AuthenticationException {
	
	public TokenExpiredException(Object...newTokens) {
		super(newTokens);
	}	
	
	@Override
	public boolean isNoAuth() {
		return false;
	}
	
}
