package de.cib.auth;

public interface User {
	
	String getId();
	
	String getDisplayName();
	
	String getAuthToken();
	
	default String getUrlToken() {
		return getAuthToken();
	};

}
