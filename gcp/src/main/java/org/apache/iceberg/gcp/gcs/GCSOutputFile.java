/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg.gcp.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.gcp.GCPProperties;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.io.PositionOutputStream;
import org.apache.iceberg.metrics.MetricsContext;

class GCSOutputFile extends BaseGCSFile implements OutputFile {

  static GCSOutputFile fromLocation(
      String location, PrefixedStorage storage, MetricsContext metrics) {
    return new GCSOutputFile(
        storage.storage(), BlobId.fromGsUtilUri(location), storage.gcpProperties(), metrics);
  }

  GCSOutputFile(
      Storage storage, BlobId blobId, GCPProperties gcpProperties, MetricsContext metrics) {
    super(storage, blobId, gcpProperties, metrics);
  }

  /**
   * Create an output stream for the specified location if the target object does not exist in GCS
   * at the time of invocation.
   *
   * @return output stream
   */
  @Override
  public PositionOutputStream create() {
    if (!exists()) {
      return createOrOverwrite();
    } else {
      throw new AlreadyExistsException("Location already exists: %s", uri());
    }
  }

  @Override
  public PositionOutputStream createOrOverwrite() {
    try {
      return new GCSOutputStream(storage(), blobId(), gcpProperties(), metrics());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to create output stream for location: " + uri(), e);
    }
  }

  @Override
  public InputFile toInputFile() {
    return new GCSInputFile(storage(), blobId(), null, gcpProperties(), metrics());
  }
}
