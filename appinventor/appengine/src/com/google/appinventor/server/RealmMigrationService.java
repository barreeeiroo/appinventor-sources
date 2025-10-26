// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2025 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

import com.google.appinventor.server.flags.Flag;
import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.server.storage.StoredData.MigrationData;
import com.google.appinventor.shared.rpc.user.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Service for handling realm migration operations in Realm B (destination).
 * This service pulls user data from Realm A (source) and migrates it to Realm B.
 *
 * @author App Inventor Team
 */
public class RealmMigrationService {

  private static final Logger LOG = Logger.getLogger(RealmMigrationService.class.getName());

  private static final Flag<String> MIGRATION_TOKEN = Flag.createFlag("migration.token", "");
  private static final Flag<String> SOURCE_REALM = Flag.createFlag("migration.source.realm", "");

  private static final String MIGRATION_TOKEN_HEADER = "X-Migration-Token";

  // Migration status constants
  private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
  private static final String STATUS_COMPLETED = "COMPLETED";
  private static final String STATUS_FAILED = "FAILED";

  // Thread pool for async migration operations
  private static final ExecutorService migrationExecutor = Executors.newFixedThreadPool(5);

  private final transient StorageIo storageIo = StorageIoInstanceHolder.getInstance();

  private static RealmMigrationService instance;

  /**
   * Get singleton instance of migration service.
   */
  public static synchronized RealmMigrationService getInstance() {
    if (instance == null) {
      instance = new RealmMigrationService();
    }
    return instance;
  }

  private RealmMigrationService() {
    // Private constructor for singleton
  }

  /**
   * Check if a user exists in the source realm and initiate migration if found.
   * This method runs asynchronously and does not block the caller.
   * Uses the user's email for lookup since user IDs differ between realms.
   */
  public void checkAndInitiateMigrationAsync(final String userId) {
    if (SOURCE_REALM.get().isEmpty()) {
      LOG.fine("Not a destination realm - skipping migration check for user: " + userId);
      return;
    }

    if (!storageIo.isMigrationNeeded(userId)) {
      LOG.fine("Migration not needed for user " + userId + " - skipping");
      return;
    }

    migrationExecutor.submit(new Runnable() {
      @Override
      public void run() {
        try {
          checkAndMigrateUser(userId);
        } catch (Exception e) {
          LOG.severe("Error in async migration for user " + userId + ": " + e.getMessage());
        }
      }
    });
  }

  /**
   * Check if user exists in source realm and migrate if found.
   */
  private void checkAndMigrateUser(String userId) {
    LOG.info("Checking if user exists in source realm: " + userId);

    try {
      // Get user's email address for cross-realm lookup
      User user = storageIo.getUser(userId);
      if (user == null) {
        LOG.warning("User not found in current realm: " + userId);
        return;
      }

      String userEmail = user.getUserEmail();
      if (userEmail == null || userEmail.isEmpty()) {
        LOG.warning("User has no email address - cannot migrate: " + userId);
        return;
      }

      // Check if user exists in source realm using email
      String sourceUserId = checkUserExistsInSourceRealm(userEmail);
      if (sourceUserId == null) {
        LOG.info("User " + userEmail + " does not exist in source realm - marking as completed to avoid future checks");

        // Create migration record marked as completed to avoid future latency
        MigrationData migration = new MigrationData();
        migration.userId = userId;
        migration.sourceUserId = null; // No source user
        migration.startTime = new Date();
        migration.completionTime = new Date();
        migration.status = STATUS_COMPLETED;
        migration.userDataMigrated = false; // No data to migrate
        migration.userFilesMigrated = false; // No files to migrate
        migration.totalUserFiles = 0;
        migration.migratedUserFiles = 0;
        migration.totalProjects = 0;
        migration.migratedProjects = 0;
        migration.totalFiles = 0;
        migration.migratedFiles = 0;
        migration.retryCount = 0;

        storageIo.storeMigrationData(migration);

        return;
      }

      LOG.info("User " + userEmail + " exists in source realm with ID " + sourceUserId + " - starting migration");

      // Create migration record
      MigrationData migration = new MigrationData();
      migration.userId = userId;
      migration.sourceUserId = sourceUserId; // Store source realm's user ID
      migration.startTime = new Date();
      migration.status = STATUS_IN_PROGRESS;
      migration.userDataMigrated = false;
      migration.userFilesMigrated = false;
      migration.totalUserFiles = 0;
      migration.migratedUserFiles = 0;
      migration.totalProjects = 0;
      migration.migratedProjects = 0;
      migration.totalFiles = 0;
      migration.migratedFiles = 0;
      migration.retryCount = 0;

      storageIo.storeMigrationData(migration);

      // Migrate user data using the source user ID
      String userDataJson = getUserDataFromSourceRealm(sourceUserId);
      if (userDataJson == null) {
        LOG.warning("Failed to get user data from source realm for: " + sourceUserId);
        migration.status = STATUS_FAILED;
        migration.lastError = "Failed to get user data from source realm";
        storageIo.storeMigrationData(migration);
        return;
      }
      
      boolean userMigrationSuccess = parseAndApplyUserData(userId, userDataJson);
      
      // Migrate user files if user data migration was successful
      boolean userFilesMigrationSuccess = false;
      if (userMigrationSuccess) {
        userFilesMigrationSuccess = migrateUserFiles(userId, sourceUserId, userDataJson);
      }

      // Update migration status
      migration = storageIo.loadMigrationData(userId);
      if (migration != null) {
        migration.userDataMigrated = userMigrationSuccess;
        migration.userFilesMigrated = userFilesMigrationSuccess;

        if (userMigrationSuccess && userFilesMigrationSuccess) {
          migration.status = STATUS_COMPLETED;
          migration.completionTime = new Date();
          LOG.info("User migration completed successfully for: " + userId);

          // Notify source realm of completion
          notifySourceRealmOfCompletion(userId);
        } else {
          migration.status = STATUS_FAILED;
          if (!userMigrationSuccess) {
            migration.lastError = "User data migration failed";
          } else {
            migration.lastError = "User files migration failed";
          }
          LOG.warning("User migration failed for: " + userId);
        }

        storageIo.storeMigrationData(migration);
      }

    } catch (Exception e) {
      LOG.severe("Error during migration check/process for user " + userId + ": " + e.getMessage());

      // Update migration status to failed
      try {
        MigrationData migration = storageIo.loadMigrationData(userId);
        if (migration != null) {
          migration.status = STATUS_FAILED;
          migration.lastError = "Migration error: " + e.getMessage();
          storageIo.storeMigrationData(migration);
        }
      } catch (Exception updateError) {
        LOG.severe("Failed to update migration status after error: " + updateError.getMessage());
      }
    }
  }

  /**
   * Check if user exists in source realm via HTTP API call using email.
   * Returns the source realm's user ID if found, null otherwise.
   */
  private String checkUserExistsInSourceRealm(String userEmail) throws IOException {
    String sourceRealm = SOURCE_REALM.get();
    String checkUrl = sourceRealm + "/migrate/check-user/" + URLEncoder.encode(userEmail, "UTF-8");

    HttpURLConnection conn = createAuthenticatedConnection(checkUrl, "GET");

    try {
      int responseCode = conn.getResponseCode();
      if (responseCode == 200) {
        // Parse response to get source user ID
        String response = readResponse(conn);
        if (response.contains("\"exists\": true")) {
          return extractJsonValue(response, "sourceUserId");
        }
      } else {
        LOG.warning("Failed to check user existence. Response code: " + responseCode);
      }
      return null;
    } finally {
      conn.disconnect();
    }
  }

  /**
   * Get user data JSON from source realm.
   */
  private String getUserDataFromSourceRealm(String sourceUserId) {
    try {
      String sourceRealm = SOURCE_REALM.get();
      String userDataUrl = sourceRealm + "/migrate/user/" + sourceUserId;

      HttpURLConnection conn = createAuthenticatedConnection(userDataUrl, "GET");

      try {
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
          return readResponse(conn);
        } else {
          LOG.warning("Failed to get user data. Response code: " + responseCode);
          return null;
        }
      } finally {
        conn.disconnect();
      }

    } catch (Exception e) {
      LOG.severe("Error getting user data from source realm for: " + sourceUserId + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * Parse JSON user data and apply it to the destination realm.
   */
  private boolean parseAndApplyUserData(String destinationUserId, String userDataJson) {
    try {
      // Simple JSON parsing (for production, consider using a proper JSON library)
      String settings = extractJsonValue(userDataJson, "settings");

      // Store user settings in destination realm using destination user ID
      if (settings != null && !settings.isEmpty()) {
        storageIo.storeSettings(destinationUserId, settings);
        LOG.info("Migrated user settings for destination user: " + destinationUserId);
      }

      // Ensure user exists in destination realm (should already exist from authentication)
      User user = storageIo.getUser(destinationUserId);
      if (user == null) {
        // This shouldn't happen since migration is triggered after authentication
        LOG.warning("Destination user not found during migration: " + destinationUserId);
        return false;
      }

      LOG.info("Successfully migrated user data for: " + destinationUserId);
      return true;

    } catch (Exception e) {
      LOG.severe("Error parsing and applying user data: " + e.getMessage());
      return false;
    }
  }

  /**
   * Migrate user files from source realm to destination realm.
   */
  private boolean migrateUserFiles(String destinationUserId, String sourceUserId, String userDataJson) {
    try {
      // Extract user file names from the user data JSON
      java.util.List<String> userFileNames = extractUserFileNames(userDataJson);
      
      if (userFileNames == null || userFileNames.isEmpty()) {
        LOG.info("No user files to migrate for user: " + destinationUserId);
        return true; // No files to migrate is success
      }

      LOG.info("Migrating " + userFileNames.size() + " user files for: " + destinationUserId);
      
      // Update migration progress tracking
      MigrationData migration = storageIo.loadMigrationData(destinationUserId);
      if (migration != null) {
        migration.totalUserFiles = userFileNames.size();
        migration.migratedUserFiles = 0;
        storageIo.storeMigrationData(migration);
      }

      int successCount = 0;
      String sourceRealm = SOURCE_REALM.get();

      // Migrate each user file
      for (String fileName : userFileNames) {
        try {
          String userFileUrl = sourceRealm + "/migrate/userfile/" + 
                               URLEncoder.encode(sourceUserId, "UTF-8") + "/" + 
                               URLEncoder.encode(fileName, "UTF-8");
          HttpURLConnection conn = createAuthenticatedConnection(userFileUrl, "GET");

          try {
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
              String userFileJson = readResponse(conn);
              
              // Extract file content and metadata
              String fileContent = extractJsonValue(userFileJson, "content");
              String encoding = extractJsonValue(userFileJson, "encoding");
              
              if (fileContent != null && encoding != null) {
                // Store the user file in the destination realm
                storageIo.uploadUserFile(destinationUserId, fileName, fileContent, encoding);
                successCount++;
                
                // Update progress
                if (migration != null) {
                  migration.migratedUserFiles = successCount;
                  storageIo.storeMigrationData(migration);
                }
                
                LOG.info("Successfully migrated user file: " + fileName + " for user: " + destinationUserId);
              } else {
                LOG.warning("Invalid user file data for: " + fileName);
              }
            } else {
              LOG.warning("Failed to get user file " + fileName + ". Response code: " + responseCode);
            }
          } finally {
            conn.disconnect();
          }
        } catch (Exception e) {
          LOG.warning("Error migrating user file " + fileName + ": " + e.getMessage());
          // Continue with other files
        }
      }

      boolean allSuccess = (successCount == userFileNames.size());
      LOG.info("User files migration completed. " + successCount + "/" + userFileNames.size() + 
               " files migrated successfully for: " + destinationUserId);

      return allSuccess;

    } catch (Exception e) {
      LOG.severe("Error migrating user files for " + destinationUserId + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Extract user file names from user data JSON.
   * Parses the "userFileNames" array from the JSON response.
   */
  private java.util.List<String> extractUserFileNames(String userDataJson) {
    java.util.List<String> fileNames = new java.util.ArrayList<String>();
    
    try {
      // Find the userFileNames array in the JSON
      String pattern = "\"userFileNames\": [";
      int startIndex = userDataJson.indexOf(pattern);
      if (startIndex == -1) {
        return fileNames; // Empty list if no userFileNames found
      }
      
      startIndex += pattern.length();
      int endIndex = userDataJson.indexOf("]", startIndex);
      if (endIndex == -1) {
        return fileNames;
      }
      
      String arrayContent = userDataJson.substring(startIndex, endIndex);
      if (arrayContent.trim().isEmpty()) {
        return fileNames; // Empty array
      }
      
      // Split by comma and extract quoted strings
      String[] parts = arrayContent.split(",");
      for (String part : parts) {
        String trimmed = part.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
          // Remove quotes and unescape
          String fileName = trimmed.substring(1, trimmed.length() - 1)
                                  .replace("\\\"", "\"")
                                  .replace("\\n", "\n")
                                  .replace("\\r", "\r")
                                  .replace("\\t", "\t")
                                  .replace("\\\\", "\\");
          fileNames.add(fileName);
        }
      }
      
    } catch (Exception e) {
      LOG.warning("Error parsing user file names from JSON: " + e.getMessage());
    }
    
    return fileNames;
  }

  /**
   * Extract a value from JSON string (simple implementation).
   */
  private String extractJsonValue(String json, String key) {
    String pattern = "\"" + key + "\": \"";
    int startIndex = json.indexOf(pattern);
    if (startIndex == -1) return null;

    startIndex += pattern.length();
    int endIndex = json.indexOf("\"", startIndex);
    if (endIndex == -1) return null;

    return json.substring(startIndex, endIndex)
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\\", "\\");
  }

  /**
   * Notify source realm that migration is completed.
   */
  private void notifySourceRealmOfCompletion(String userId) {
    try {
      String sourceRealm = SOURCE_REALM.get();
      String completionUrl = sourceRealm + "/migrate/completed";

      HttpURLConnection conn = createAuthenticatedConnection(completionUrl, "POST");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      // Send completion notification
      String payload = String.format(
          "{\"userId\": \"%s\", \"completionTime\": \"%s\"}",
          userId, new Date().toString()
      );

      try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.getBytes("UTF-8"));
      }

      int responseCode = conn.getResponseCode();
      if (responseCode == 200) {
        LOG.info("Successfully notified source realm of migration completion for: " + userId);
      } else {
        LOG.warning("Failed to notify source realm. Response code: " + responseCode);
      }

      conn.disconnect();

    } catch (Exception e) {
      LOG.warning("Error notifying source realm of completion: " + e.getMessage());
      // This is not critical - migration can still be considered successful
    }
  }

  /**
   * Create an authenticated HTTP connection to source realm.
   */
  private HttpURLConnection createAuthenticatedConnection(String urlString, String method) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod(method);
    conn.setRequestProperty(MIGRATION_TOKEN_HEADER, MIGRATION_TOKEN.get());
    conn.setRequestProperty("User-Agent", "App-Inventor-Migration-Service");
    conn.setConnectTimeout(30000); // 30 seconds
    conn.setReadTimeout(60000);    // 60 seconds

    return conn;
  }

  /**
   * Read response from HTTP connection.
   */
  private String readResponse(HttpURLConnection conn) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;

    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();

    return response.toString();
  }

}