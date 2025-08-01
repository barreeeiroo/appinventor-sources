package com.google.appinventor.server;

import com.google.appinventor.server.storage.remote.RemoteStorage;
import com.google.appinventor.server.storage.remote.RemoteStorageInstanceHolder;
import com.google.appinventor.shared.rpc.remotestorage.RemoteStorageService;

import java.util.logging.Logger;


public class RemoteStorageServiceImpl extends OdeRemoteServiceServlet implements RemoteStorageService {
  private static final Logger LOG = Logger.getLogger(RemoteStorageServiceImpl.class.getName());

  @Override
  public String getProjectImportUrl(final String fileName) {
    if (!RemoteStorageInstanceHolder.isRemoteConfigured(RemoteStorageInstanceHolder.Usage.IMPORT)) {
      return null;
    }

    final String userId = userInfoProvider.getUserId();

    final RemoteStorage remoteStorage = RemoteStorageInstanceHolder.getInstance(RemoteStorageInstanceHolder.Usage.IMPORT);
    final String objectKey = remoteStorage.getProjectImportObjectKey(userId, fileName);
    final String uploadUrl = remoteStorage.generateUploadUrl(objectKey);
    LOG.info("Upload URL is " + uploadUrl);

    return uploadUrl;
  }
}
