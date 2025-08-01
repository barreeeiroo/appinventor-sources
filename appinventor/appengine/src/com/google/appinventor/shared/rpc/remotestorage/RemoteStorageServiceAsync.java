// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2025-2025 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.shared.rpc.remotestorage;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface RemoteStorageServiceAsync {

  /**
   * @see RemoteStorageService#getProjectImportUrl(String)
   */
   void getProjectImportUrl(final String fileName, final AsyncCallback<String> callback);
}
