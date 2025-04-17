package org.cibseven.webapp.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {

  public static final String PROPERTIES_FILE = "cibseven-plugins.properties";
  
  private static Configuration instance;
  
  private String secret;
  
  public static Configuration getInstance() {
    if (instance == null) {
      instance = new Configuration();
    }
    return instance;
  }
  
  private Configuration() {
    Properties defaultSettings = loadProperties();
    this.secret = getProperty(defaultSettings, "authentication.jwtSecret");
  }
  
  public String getSecret() {
    return secret;
  }
  
  private String getProperty(Properties defaultProperties, String propertyName) {
    return defaultProperties.getProperty(propertyName);
  }
  
  private Properties loadProperties() {
    Properties properties = new Properties();
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
      if (inputStream == null) {
        throw new IllegalStateException("Could not find configuration file " + PROPERTIES_FILE + " in classpath");
      }
      properties.load(inputStream);
      return properties;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to load template configuration from: " + PROPERTIES_FILE, e);
    }
  }

}
