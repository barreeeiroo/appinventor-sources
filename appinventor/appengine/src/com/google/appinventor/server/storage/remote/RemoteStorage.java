// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2025-2025 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.server.storage.remote;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Interface which abstracts the remote storage access patterns.
 * This allows AI2 to store different files, like build outputs, outside
 *   GCP, and provide a presigned URL to download and/or access them.
 */
public abstract class RemoteStorage {
  private static final SecureRandom secureRandom = new SecureRandom();

  // We partition the bucket into different usage types, so specific lifecycle rules could
  //   be applied by prefix, if required.
  private final static String BUILD_OUTPUT_PREFIX = "build";
  private final static String PROJECT_EXPORT_PREFIX = "export";
  private final static String PROJECT_IMPORT_PREFIX = "import";

  /**
   * Generates a, usually, presigned URL to upload the file to the remote
   *   storage server.
   *
   * @param objectKey object key in the remote storage server
   * @return (presigned) upload URL
   */
  public abstract String generateUploadUrl(final String objectKey);

  /**
   * Generates an external URL to access the object. Depending on the
   *   implementation, this may behave as a presigned URL or a "normal"
   *   URL (if using a CDN, for example).
   *
   * @param objectKey object key in the remote storage server
   * @return (presigned) get/retrieval URL
   */
  public abstract String generateRetrieveUrl(final String objectKey);

  /**
   * Generates a constant object key for a given specific project build output.
   *
   * @param target the type of target (Android)
   * @param userId the user ID owning the project
   * @param projectId the given project ID
   * @param projectName the name of the project to store
   * @param extensionName the extension name the build is on
   * @return build/userId/projectId/target/projectName.extensionName
   */
  public final String getBuildOutputObjectKey(final String target, final String userId, final Long projectId,
      final String projectName, final String extensionName) {
    final String fileName = projectName + "." + extensionName;
    return getBuildOutputObjectKey(target, userId, projectId, fileName);
  }

  /**
   * Generates a constant object key for a given specific project build output.
   *
   * @param target the type of target (Android)
   * @param userId the user ID owning the project
   * @param projectId the given project ID
   * @param fileName the file name to download
   * @return build/userId/projectId/target/projectName.extensionName
   */
  public final String getBuildOutputObjectKey(final String target, final String userId, final Long projectId,
      final String fileName) {
    final String filePath = userId + "/" + projectId + "/" + target;
    return BUILD_OUTPUT_PREFIX + "/" + filePath + "/" + fileName;
  }

  /**
   * Generates a constant object key for a given specific project export.
   *
   * @param downloadKind the type of download
   * @param userId the user ID owning the project
   * @param fileName the name of the file to store
   * @return export/userId/downloadKind/timestampHash/file.name
   */
  public final String getProjectExportObjectKey(final String downloadKind, final String userId, final String fileName) {
    final String timestamp = String.valueOf(Instant.now().getEpochSecond());
    final String timestampHash = generateFieldsHash(userId, downloadKind, fileName, timestamp);

    // We introduce a timestamp in between the URL to ensure we don't overwrite objects, and let any lifecycle
    //   rule clean them up.
    final String filePath = userId + "/" + downloadKind + "/" + timestampHash;
    return PROJECT_EXPORT_PREFIX + "/" + filePath + "/" + fileName;
  }

  /**
   * Generates a constant object key for a given specific project import.
   *
   * @param userId the user ID owning the project
   * @param fileName the name of the file to store
   * @return import/timestampRandomHash/file.name
   */
  public final String getProjectImportObjectKey(final String userId, final String fileName) {
    final String randomString = generateRandomString();
    final String timestamp = String.valueOf(Instant.now().getEpochSecond());
    final String timestampHash = generateFieldsHash(fileName, timestamp, randomString);

    final String filePath = userId + "/" + timestampHash;
    return PROJECT_IMPORT_PREFIX + "/" + filePath + "/" + fileName;
  }

  /**
   * Generates a hash based on the input strings, separated by a pipe.
   *
   * @param fields the current timestamp
   * @return a SHA-256 hash as hexadecimal string
   */
  private String generateFieldsHash(final String ...fields) {
    final String input = String.join("|", fields);
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      final StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();

    } catch (NoSuchAlgorithmException e) {
      // Fallback to a simple hash if SHA-256 is not available
      return String.valueOf(Math.abs((input).hashCode()));
    }
  }

  /**
   * Generates some random bytes for increased hashing security, mainly when not
   *   not using userId.
   * @return random bytes
   */
  private String generateRandomString() {
    byte[] randomBytes = new byte[64]; // 512 bits
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

}
