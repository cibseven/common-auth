package org.cibseven.webapp.auth.providers;

import javax.servlet.http.HttpServletRequest;

import org.cibseven.webapp.auth.Login;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.exception.AuthenticationException;

public interface UserProvider<T extends Login> {
	
	User login(T params, HttpServletRequest rq) throws AuthenticationException;
	
	User getUserInfo(User user, String userId);
	
	void logout(User user);	
	
	User authenticate(HttpServletRequest rq) throws AuthenticationException;
	
}
