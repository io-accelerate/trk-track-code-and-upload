package io.accelerate.tracking.app.upload;

import software.amazon.awssdk.services.s3.S3AsyncClient;

public interface RemoteDestination {

    S3AsyncClient getClient();
    
    String getS3Bucket();

    String getS3Prefix();
    
    void startS3SyncSession() throws DestinationOperationException;

    void stopS3SyncSession() throws DestinationOperationException;
}
