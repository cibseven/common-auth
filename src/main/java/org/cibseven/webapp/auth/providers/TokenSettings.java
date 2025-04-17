package org.cibseven.webapp.auth.providers;

import java.time.Duration;

public interface TokenSettings {
	String getSecret();	
	Duration getValid();	
	Duration getProlong();
}