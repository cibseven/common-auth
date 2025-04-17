package org.cibseven.webapp.auth.providers;

import java.time.Duration;

public class JwtTokenSettings implements TokenSettings {

	private String jwtSecret;	
	private long tokenValidMinutes;	
	private long tokenProlongMinutes;
	
	public JwtTokenSettings(String jwtSecret, long tokenValidMinutes, long tokenProlongMinutes) {
	  this.jwtSecret = jwtSecret;
	  this.tokenValidMinutes = tokenValidMinutes;
	  this.tokenProlongMinutes = tokenProlongMinutes;
  }

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public long getTokenValidMinutes() {
    return tokenValidMinutes;
  }

  public void setTokenValidMinutes(long tokenValidMinutes) {
    this.tokenValidMinutes = tokenValidMinutes;
  }

  public long getTokenProlongMinutes() {
    return tokenProlongMinutes;
  }

  public void setTokenProlongMinutes(long tokenProlongMinutes) {
    this.tokenProlongMinutes = tokenProlongMinutes;
  }

  @Override
	public String getSecret() {
		return jwtSecret;
	}
	
	@Override
	public Duration getValid() {
		return Duration.ofMinutes(tokenValidMinutes);
	}
	
	@Override
	public Duration getProlong() {
		return Duration.ofMinutes(tokenProlongMinutes);
	}

}
