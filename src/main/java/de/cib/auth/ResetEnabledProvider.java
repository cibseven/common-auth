package de.cib.auth;

import java.util.Locale;
import java.util.Map;

public interface ResetEnabledProvider {
	
	/**
	 * This method might immediately send a new password via email (if some security mechanism protects it from abuse) or generate a token and send that.
	 * That link could either point to the frontend with login via URL parameter or to ResetService.resetPassword(String)
	 * @param params will probably contain email address or username and maybe the answer to some security question 
	 */
	void requestPasswordReset(Map<String, String> params, Locale loc);
	
}
