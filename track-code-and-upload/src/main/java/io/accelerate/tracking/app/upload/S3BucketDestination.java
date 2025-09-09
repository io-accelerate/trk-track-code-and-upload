package io.accelerate.tracking.app.upload;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class S3BucketDestination implements RemoteDestination {
    private final S3AsyncClient client;
    private final String s3Bucket;
    private final String s3Prefix;

    public S3BucketDestination(S3AsyncClient client, String s3Bucket, String s3Prefix) {
        this.client = client;
        this.s3Bucket = s3Bucket;
        this.s3Prefix = s3Prefix;
    }

    public S3AsyncClient getClient() {
        return client;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getS3Prefix() {
        return s3Prefix;
    }

    @Override
    public void startS3SyncSession() throws DestinationOperationException {
        putObjectWithTimestamp("last_sync_start.txt");
    }

    @Override
    public void stopS3SyncSession() throws DestinationOperationException {
        putObjectWithTimestamp("last_sync_stop.txt");
    }

    private void putObjectWithTimestamp(String key) throws DestinationOperationException {
        String objectKey = s3Prefix + key;
        String content = "timestamp: " + System.currentTimeMillis();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(objectKey)
                .build();

        CompletableFuture<?> future = client.putObject(
                request,
                AsyncRequestBody.fromString(content, StandardCharsets.UTF_8)
        );

        try {
            // Block until complete so we know permissions are valid
            future.join();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof S3Exception) {
                throw new DestinationOperationException(cause.getMessage(), cause);
            }
            throw new DestinationOperationException("Failed to upload object to S3", cause);
        }
    }
}
