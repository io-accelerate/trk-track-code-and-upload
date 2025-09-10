package io.accelerate.tracking.app.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.accelerate.tracking.app.util.DiskSpaceUtil;
import io.accelerate.tracking.code.record.SourceCodeRecorder;
import org.slf4j.Logger;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import static org.slf4j.LoggerFactory.getLogger;

@Parameters(commandDescription = "Run internal checks")
public class SelfTestCommand implements HasHelp {
    private static final Logger log = getLogger(SelfTestCommand.class);
    public static final String TEST_PUBLIC_READONLY_BUCKET = "ping.s3.accelerate.io";

    @Parameter(names = {"-h", "--help"}, help = true, description = "Show help for this command")
    private boolean help;
    
    @Parameter(names = "--minimum-required-diskspace-gb", description = "Minimum required diskspace (in GB) on the current volume for the app to run")
    private long minimumRequiredDiskspaceInGB = 1;

    @Override
    public boolean isHelpRequested() {
        return help;
    }
    
    public void run() {
        if (!DiskSpaceUtil.hasEnoughFreeDiskspace(minimumRequiredDiskspaceInGB)) {
            System.exit(-1);
        }
        
        log.info("~~~~~~ Self test starting ~~~~~~");
        log.info("Checking S3 connectivity");
        runS3SanityCheck();
        log.info("Checking source code tracking");
        SourceCodeRecorder.runSanityCheck();
        log.info("~~~~~~ Self test completed successfully ~~~~~~");
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
