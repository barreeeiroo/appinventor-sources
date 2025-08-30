// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2024 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads Tomcat-specific properties very early in the application lifecycle
 * to ensure they are available before any static initializers run.
 * 
 * This class MUST be loaded before any other App Inventor classes that
 * depend on system properties in their static blocks.
 */
public class PropertiesLoader {
  
  private static final Logger LOG = Logger.getLogger(PropertiesLoader.class.getName());
  private static final String TOMCAT_PROPERTIES_FILE = "WEB-INF/tomcat.properties";
  private static boolean propertiesLoaded = false;

  // Static block runs immediately when class is first referenced
  static {
    loadPropertiesFromClasspath();
  }

  /**
   * Loads properties from tomcat.properties file in the classpath/WAR
   */
  private static void loadPropertiesFromClasspath() {
    if (propertiesLoaded) {
      return; // Already loaded
    }

    LOG.info("Early loading of Tomcat properties...");
    
    // Try to load from the current thread's context classloader first (tomcat.properties in classpath)
    InputStream propertiesStream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("tomcat.properties");
    
    // If not found, try the system classloader
    if (propertiesStream == null) {
      propertiesStream = PropertiesLoader.class.getClassLoader()
          .getResourceAsStream("tomcat.properties");
    }
    
    // If still not found, try with WEB-INF prefix (in case it's still in WEB-INF)
    if (propertiesStream == null) {
      propertiesStream = PropertiesLoader.class.getClassLoader()
          .getResourceAsStream(TOMCAT_PROPERTIES_FILE);
    }
    
    if (propertiesStream == null) {
      LOG.info("tomcat.properties file not found in classpath, checking system properties");
      // Check if properties are already set via system properties (e.g., -D flags)
      if (System.getProperty("filesystem.provider") != null) {
        LOG.info("Properties appear to be set via system properties");
        propertiesLoaded = true;
        return;
      }
      LOG.warning("No tomcat.properties found and no system properties set");
      propertiesLoaded = true;
      return;
    }

    Properties tomcatProperties = new Properties();
    try {
      tomcatProperties.load(propertiesStream);
      propertiesStream.close();
      
      // Load all properties from the file into system properties
      int loadedCount = 0;
      for (String propertyName : tomcatProperties.stringPropertyNames()) {
        String value = tomcatProperties.getProperty(propertyName);
        
        // Only set if not already set as system property (command line takes precedence)
        if (System.getProperty(propertyName) == null && value != null && !value.trim().isEmpty()) {
          System.setProperty(propertyName, value.trim());
          loadedCount++;
          LOG.fine("Early loaded property: " + propertyName + " = " + value);
        }
      }
      
      LOG.info("Early loading: Successfully loaded " + loadedCount + " properties from tomcat.properties");
      
      // Log the key properties that were loaded for debugging
      logKeyProperties();
      
      propertiesLoaded = true;
      
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to early load tomcat.properties file", e);
      propertiesLoaded = true; // Mark as attempted to avoid retry
    }
  }

  /**
   * Ensures properties are loaded. This method can be called from other
   * classes to ensure properties are available.
   */
  public static void ensurePropertiesLoaded() {
    // The static block will have run by the time this method is called
    if (!propertiesLoaded) {
      loadPropertiesFromClasspath();
    }
  }

  /**
   * Log key properties for debugging purposes
   */
  private static void logKeyProperties() {
    String[] keyProperties = {
      "filesystem.provider",
      "database.provider", 
      "cache.provider",
      "auth.uselocal",
      "auth.usegoogle"
    };
    
    for (String property : keyProperties) {
      String value = System.getProperty(property);
      if (value != null) {
        LOG.info("Early loaded key property - " + property + ": " + value);
      }
    }
  }
}