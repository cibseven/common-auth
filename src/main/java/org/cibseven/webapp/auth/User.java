package org.cibseven.webapp.auth;

public interface User {
	
	String getId();
	
	String getDisplayName();
	
	String getAuthToken();
	
	void setAuthToken(String token);
	
	default String getUrlToken() {
		return getAuthToken();
	};

}
