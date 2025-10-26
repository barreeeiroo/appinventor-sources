// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2025 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server;

import com.google.appinventor.server.flags.Flag;
import com.google.appinventor.server.storage.StorageIo;
import com.google.appinventor.server.storage.StorageIoInstanceHolder;
import com.google.appinventor.server.storage.StoredData.MigrationTrackingData;
import com.google.appinventor.shared.rpc.user.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for handling realm migration operations.
 * 
 * Supports endpoints for both Realm A (source) and Realm B (destination):
 * - /migrate/check-user/{email} - Check if user exists by email
 * - /migrate/user/{sourceUserId} - Get user data, settings, project IDs, user file names
 * - /migrate/userfile/{sourceUserId}/{fileName} - Get specific user file content
 * - /migrate/projects/{projectId} - Get project metadata (name, type, settings, dates, etc.)
 * - /migrate/projectdata/{projectId} - Get project files as ZIP archive
 * - /migrate/status/{userId} - Get migration status
 * - /migrate/completed - Mark migration as completed (Realm A only)
 *
 */
public class RealmMigrationServlet extends OdeServlet {

  private static final Logger LOG = Logger.getLogger(RealmMigrationServlet.class.getName());
  
  private final transient StorageIo storageIo = StorageIoInstanceHolder.getInstance();
  
  // Configuration flags
  private static final Flag<String> MIGRATION_TOKEN = Flag.createFlag("migration.token", "");
  private static final Flag<String> SOURCE_REALM = Flag.createFlag("migration.source.realm", "");
  
  // Migration token header
  private static final String MIGRATION_TOKEN_HEADER = "X-Migration-Token";

  // Migration status constants
  private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
  private static final String STATUS_COMPLETED = "COMPLETED";
  private static final String STATUS_FAILED = "FAILED";
  
  /**
   * Validate migration token for the requested operation.
   * This servlet only runs on Realm A (source) to serve data to Realm B (destination).
   */
  private boolean validateMigrationRequest(HttpServletRequest req, HttpServletResponse resp, String operation) throws IOException {
    // Ensure this is running on Realm A (source) - should not have a source realm configured
    if (!SOURCE_REALM.get().isEmpty()) {
      LOG.severe("Migration servlet running on Realm B (destination) - this servlet should only run on Realm A (source)");
      sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Migration servlet misconfigured");
      return false;
    }
    
    // Check if migration token is configured
    String migrationToken = MIGRATION_TOKEN.get();
    if (migrationToken.isEmpty()) {
      LOG.warning("Migration endpoint accessed but migration.token not configured");
      sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Migration not configured");
      return false;
    }
    
    // Validate authentication token
    String requestToken = req.getHeader(MIGRATION_TOKEN_HEADER);
    if (requestToken == null || !requestToken.equals(migrationToken)) {
      LOG.warning("Migration request with invalid or missing token from: " + req.getRemoteAddr());
      sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid migration token");
      return false;
    }
    
    // All operations are valid for Realm A (source realm)
    return true;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    
    if (pathInfo == null || pathInfo.length() <= 1) {
      sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid migration endpoint");
      return;
    }

    // Remove leading slash and split path
    String[] pathParts = pathInfo.substring(1).split("/");
    
    if (pathParts.length < 2) {
      sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid migration endpoint");
      return;
    }

    String operation = pathParts[0];
    String identifier = pathParts[1];

    // Validate request authentication and realm role
    if (!validateMigrationRequest(req, resp, operation)) {
      return; // Error response already sent
    }

    try {
      // Decode URL-encoded parameters
      identifier = URLDecoder.decode(identifier, "UTF-8");
      
      switch (operation) {
        case "check-user":
          handleCheckUser(identifier, resp);  // identifier is email address
          break;
        case "user":
          handleGetUser(identifier, resp);
          break;
        case "projects":
          handleGetProjects(identifier, resp);
          break;
        case "projectdata":
          handleGetProjectData(identifier, resp);
          break;
        case "userfile":
          // userfile requires both userId and fileName: /migrate/userfile/{userId}/{fileName}
          if (pathParts.length < 3) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing fileName for userfile operation");
            return;
          }
          String decodedFileName = URLDecoder.decode(pathParts[2], "UTF-8");
          handleGetUserFile(identifier, decodedFileName, resp);  // identifier is already decoded userId
          break;
        case "status":
          handleGetStatus(identifier, resp);
          break;
        default:
          sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown operation: " + operation);
      }
    } catch (Exception e) {
      LOG.severe("Error handling migration request: " + e.getMessage());
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    
    if (pathInfo == null) {
      sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid migration endpoint");
      return;
    }

    // Validate request authentication and realm role
    String operation = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
    if (!validateMigrationRequest(req, resp, operation)) {
      return; // Error response already sent
    }

    try {
      if (pathInfo.equals("/completed")) {
        // This servlet only runs on Realm A (source), so it can accept completion notifications
        handleMigrationCompleted(req, resp);
      } else {
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown POST endpoint");
      }
    } catch (Exception e) {
      LOG.severe("Error handling migration POST request: " + e.getMessage());
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
    }
  }

  /**
   * Check if a user exists in this realm by email address.
   * Returns the source realm's userId if found.
   */
  private void handleCheckUser(String email, HttpServletResponse resp) throws IOException {
    LOG.info("Checking if user exists by email: " + email);
    
    try {
      // Look up user by email (Realm A will have different userId than Realm B)
      User user = storageIo.getUserFromEmail(email);
      
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      
      if (user != null) {
        writer.write(String.format(
          "{\"exists\": true, \"sourceUserId\": \"%s\", \"email\": \"%s\"}", 
          escapeJsonString(user.getUserId()),
          escapeJsonString(email)
        ));
      } else {
        writer.write(String.format(
          "{\"exists\": false, \"email\": \"%s\"}", 
          escapeJsonString(email)
        ));
      }
      
      writer.close();
    } catch (Exception e) {
      LOG.warning("Error checking user existence by email: " + e.getMessage());
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error checking user");
    }
  }

  /**
   * Get user-specific data for migration (Realm A only - serves data).
   * This returns user settings and other user-specific data that needs to be migrated.
   * The identifier can be either email or sourceUserId depending on the migration flow.
   */
  private void handleGetUser(String identifier, HttpServletResponse resp) throws IOException {
    LOG.info("Getting user data for migration: " + identifier);
    
    try {
      // The identifier could be userId (sourceUserId from check-user response)
      User user = storageIo.getUser(identifier);
      if (user == null) {
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
        return;
      }
      
      String userId = user.getUserId(); // Get the actual userId for this realm
      
      // Get user settings (this is the main data we need to migrate)
      String userSettings = storageIo.loadSettings(userId);
      if (userSettings == null) {
        userSettings = "";
      }
      
      // Get user's project list (we'll need this for project migration later)
      java.util.List<Long> projectIds = storageIo.getProjects(userId);
      
      // Get user's file list (we'll need this for user file migration)
      java.util.List<String> userFileNames = storageIo.getUserFiles(userId);
      
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      
      // Return user-specific data in JSON format
      writer.write(String.format(
        "{"
        + "\"sourceUserId\": \"%s\", "
        + "\"settings\": \"%s\", "
        + "\"projectIds\": [%s], "
        + "\"userFileNames\": [%s]"
        + "}",
        escapeJsonString(userId),
        escapeJsonString(userSettings),
        formatProjectIds(projectIds),
        formatUserFileNames(userFileNames)
      ));
      writer.close();
      
    } catch (Exception e) {
      LOG.severe("Error getting user data for migration: " + e.getMessage());
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving user data");
    }
  }
  
  /**
   * Format project IDs as comma-separated JSON array elements.
   */
  private String formatProjectIds(java.util.List<Long> projectIds) {
    if (projectIds == null || projectIds.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < projectIds.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append(projectIds.get(i));
    }
    return sb.toString();
  }
  
  /**
   * Format user file names as comma-separated JSON array elements.
   */
  private String formatUserFileNames(java.util.List<String> userFileNames) {
    if (userFileNames == null || userFileNames.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < userFileNames.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append("\"").append(escapeJsonString(userFileNames.get(i))).append("\"");
    }
    return sb.toString();
  }
  
  /**
   * Escape special characters in JSON strings.
   */
  private String escapeJsonString(String str) {
    if (str == null) return "";
    return str.replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
              .replace("\t", "\\t");
  }

  /**
   * Get project metadata for migration by project ID.
   * Returns project name, type, settings, dates, etc. (but not the files).
   */
  private void handleGetProjects(String projectId, HttpServletResponse resp) throws IOException {
    LOG.info("Getting project metadata for migration: " + projectId);
    
    // TODO: Implement project metadata serialization
    // This will need to get project metadata (name, type, settings, dates, etc.) and serialize to JSON
    // The project files will be handled by the separate /migrate/projectdata/{projectId} endpoint
    sendError(resp, HttpServletResponse.SC_NOT_IMPLEMENTED, "Project metadata migration not yet implemented");
  }

  /**
   * Get project files as ZIP archive for migration by project ID.
   * Uses the existing exportProjectSourceZip functionality.
   */
  private void handleGetProjectData(String projectId, HttpServletResponse resp) throws IOException {
    LOG.info("Getting project data ZIP for migration: " + projectId);
    
    // TODO: Implement project ZIP export for migration
    // This will use the existing exportProjectSourceZip method in ObjectifyStorageIo
    // Need to determine project owner and call:
    // storageIo.exportProjectSourceZip(userId, projectId, includeHistory, includeScreenshots, includeKeystore, zipName)
    sendError(resp, HttpServletResponse.SC_NOT_IMPLEMENTED, "Project data ZIP migration not yet implemented");
  }

  /**
   * Get user file content for migration.
   * Returns the content and metadata for a specific user file.
   */
  private void handleGetUserFile(String userId, String fileName, HttpServletResponse resp) throws IOException {
    LOG.info("Getting user file for migration: " + userId + "/" + fileName);
    
    try {
      // Download the user file content using UTF-8 encoding
      String fileContent = storageIo.downloadUserFile(userId, fileName, "UTF-8");
      if (fileContent == null) {
        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User file not found");
        return;
      }
      
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      
      // Return user file data in JSON format
      // Note: For binary files, we may need to use base64 encoding in the future
      writer.write(String.format(
        "{"
        + "\"userId\": \"%s\", "
        + "\"fileName\": \"%s\", "
        + "\"content\": \"%s\", "
        + "\"encoding\": \"UTF-8\""
        + "}",
        escapeJsonString(userId),
        escapeJsonString(fileName),
        escapeJsonString(fileContent)
      ));
      writer.close();
      
    } catch (Exception e) {
      LOG.severe("Error getting user file for migration: " + e.getMessage());
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving user file");
    }
  }

  /**
   * Get migration status for a user in Realm A (source).
   * Returns whether the user has been migrated to Realm B.
   */
  private void handleGetStatus(String userId, HttpServletResponse resp) throws IOException {
    LOG.info("Getting migration status for user: " + userId);
    
    resp.setContentType("application/json");
    PrintWriter writer = resp.getWriter();
    
    try {
      // Check for MigrationTrackingData (Realm A tracks completed migrations)
      MigrationTrackingData trackingData = storageIo.loadMigrationTrackingData(userId);
      if (trackingData != null) {
        writer.write(String.format(
          "{\"userId\": \"%s\", \"status\": \"MIGRATED\", \"migrationDate\": \"%s\"}",
          userId, trackingData.migrationDate
        ));
      } else {
        writer.write(String.format(
          "{\"userId\": \"%s\", \"status\": \"NOT_MIGRATED\"}",
          userId
        ));
      }
    } catch (Exception e) {
      LOG.warning("Error getting migration status: " + e.getMessage());
      writer.write(String.format(
        "{\"userId\": \"%s\", \"status\": \"ERROR\", \"error\": \"%s\"}",
        userId, e.getMessage()
      ));
    }
    
    writer.close();
  }

  /**
   * Handle migration completion notification from Realm B to Realm A.
   */
  private void handleMigrationCompleted(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    LOG.info("Received migration completion notification");
    
    try {
      // Read JSON request body
      StringBuilder jsonBuffer = new StringBuilder();
      BufferedReader reader = req.getReader();
      String line;
      while ((line = reader.readLine()) != null) {
        jsonBuffer.append(line);
      }
      String jsonPayload = jsonBuffer.toString();
      
      // Parse userId from JSON (simple parsing)
      String userId = extractJsonValue(jsonPayload, "userId");
      if (userId == null || userId.isEmpty()) {
        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing userId in completion notification");
        return;
      }
      
      // Create migration tracking record
      MigrationTrackingData tracking = new MigrationTrackingData();
      tracking.userId = userId;
      tracking.migrationDate = new Date();
      tracking.dataCleaned = false; // Will be cleaned up later based on policy
      
      storageIo.storeMigrationTrackingData(tracking);
      
      LOG.info("Migration tracking record created for user: " + userId);
      
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      writer.write("{\"status\": \"acknowledged\", \"userId\": \"" + escapeJsonString(userId) + "\"}");
      writer.close();
      
    } catch (Exception e) {
      LOG.severe("Error handling migration completion notification: " + e.getMessage());
      sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing completion notification");
    }
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
   * Send error response with JSON format.
   */
  private void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
    resp.setStatus(statusCode);
    resp.setContentType("application/json");
    PrintWriter writer = resp.getWriter();
    writer.write("{\"error\": \"" + message + "\"}");
    writer.close();
  }
}