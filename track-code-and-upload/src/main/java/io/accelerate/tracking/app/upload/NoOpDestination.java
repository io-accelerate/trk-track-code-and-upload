package io.accelerate.tracking.app.upload;

import software.amazon.awssdk.services.s3.S3AsyncClient;

public class NoOpDestination implements RemoteDestination {
    @Override
    public S3AsyncClient getClient() {
        return null;
    }

    @Override
    public String getS3Bucket() {
        return "";
    }

    @Override
    public String getS3Prefix() {
        return "";
    }

    @Override
    public void startS3SyncSession() {
        
    }

    @Override
    public void stopS3SyncSession() {

    }
}
