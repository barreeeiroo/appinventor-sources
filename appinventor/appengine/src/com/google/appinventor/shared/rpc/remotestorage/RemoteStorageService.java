// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2025-2025 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.shared.rpc.remotestorage;

import com.google.appinventor.shared.rpc.ServerLayout;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;


@RemoteServiceRelativePath(ServerLayout.REMOTE_STORAGE_SERVICE)
public interface RemoteStorageService extends RemoteService {

  /**
   * Returns a presigned URL to upload a new project from a compressed
   *   file.
   *
   * @param fileName file name to upload
   * @return import URL if remote storage is configured
   */
  String getProjectImportUrl(final String fileName);
}
