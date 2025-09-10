package io.accelerate.tracking.app;

import io.accelerate.tracking.code.record.SourceCodeRecorder;
import org.slf4j.Logger;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import static org.slf4j.LoggerFactory.getLogger;

public class SelfTestAction {
    private static final Logger log = getLogger(SelfTestAction.class);
    public static final String TEST_PUBLIC_READONLY_BUCKET = "ping.s3.accelerate.io";

    public static void run() {
        runS3SanityCheck();
        SourceCodeRecorder.runSanityCheck();
    }
    
    public static void runS3SanityCheck() {
        // Touch S3 to fail fast if the service is unreachable in this environment.
        // Using anonymous creds mirrors the v1 “null creds” idea without needing real IAM.
        try (S3Client s3 = S3Client.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .build()) {

            // Cheapest probe
            s3.headBucket(HeadBucketRequest.builder()
                    .bucket(TEST_PUBLIC_READONLY_BUCKET)
                    .build());
        }
    }
}
