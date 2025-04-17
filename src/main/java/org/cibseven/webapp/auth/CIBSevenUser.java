package org.cibseven.webapp.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CIBSevenUser implements User {
  
  private String authToken;
  private String userID;
  private String displayName;
  private boolean isAnonUser;
  
  public CIBSevenUser() {
  }
  
  public CIBSevenUser(String userId) {
    this.userID = userId;
  }
  
  public String getUserID() {
    return userID;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  public boolean isAnonUser() {
    return isAnonUser;
  }

  public void setAnonUser(boolean isAnonUser) {
    this.isAnonUser = isAnonUser;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getId() {
    return userID;
  }

  @Override
  public String toString() {
    return userID; 
  }

  @Override
  public String getDisplayName() {
    if((displayName != null)&&(!displayName.isEmpty())) return displayName;
    else return userID;
  }

  @Override
  public String getAuthToken() {
    return authToken;
  }

  @Override
  public void setAuthToken(String token) {
    this.authToken = token;
  }


}
