package org.cibseven.webapp.auth;

import java.util.Locale;

public interface UserManager<W extends Login, R extends Login> extends UserProvider<R> {
	
	User update(User user, W params);
	
	User create(User user, W params, Locale loc);
	
	void delete(User user, String userId);
	
}
