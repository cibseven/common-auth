package org.cibseven.webapp.auth;

import jakarta.servlet.http.HttpServletRequest;

public interface UserProvider<T extends Login> {
	
	User login(T params, HttpServletRequest rq) throws AuthenticationException;
	
	User getUserInfo(User user, String userId);
	
	void logout(User user);	
	
	User authenticate(HttpServletRequest rq) throws AuthenticationException;
	
}
