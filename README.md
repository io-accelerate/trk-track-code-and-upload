

Command-line tool that records the screen as Fragmented MP4 and upload the fragments as they are being generated.

## Run

### Download

Download the `code-track-and-upload.jar` from `https://github.com/io-accelerate/trk-code-track-and-upload/releases/latest`

### Configure

Configuration for running this service should be placed in file `.private/aws-test-secrets` in Java Properties file format. For examples.

```properties
aws_access_key_id=ABCDEFGHIJKLM
aws_secret_access_key=ABCDEFGHIJKLM
s3_region=ap-southeast-1
s3_bucket=bucketname
s3_prefix=prefix/
```

### Run

## Development - building

### Build as a OS specific fat Jar

This will crate a maven based Jar that will download the required dependencies before running the app:

```bash
./gradlew shadowJar -i
```

## Development - Testing

### Unit tests

Run the test suite with Gradle:
```bash
./gradlew clean test --info --console=plain
```

### Packaging tests

Run the self-test on the generated jar file:
```bash
java -jar build/libs/track-code-and-upload-*-SNAPSHOT.jar --run-self-test
```

### Minimum disk requirement tests

Run these below tests to verify the app is checking for minimum disk space requirements:

#### Run the app in a docker container with just 4mb of memory

```bash
$ cd scripts/tests

$ ./runDockerContainerWith4MBRAM.sh
```

Note: The docker container script maps the current project directory into the container so the script works seamlessly.

#### Run the app in a low-disk space environment (i.e. VM with <1GB free disk space) 

```bash
$ cd scripts/tests

$ ./runAppNotMeetingRequirement.sh
```

Note: In the above case, the current project directory will need to be mapped as a volume so the respective scripts runs seamlessly.  

#### Run the app simulating a low-disk space environment (using the --minimum-required-diskspace-gb CLI param) 

```bash
$ cd scripts/tests

$ ./runAppNotMeetingRequirementViaCLIParam.sh
```

Note: In the above case, the test can be run in any environment, expect ensure free diskspace is lower than 100GB, necessary test condition.  

#### Run the app under happy path setup

```bash
$ cd scripts/tests

$ ./runAppMeetingRequirement.sh
```

Note: In the above case, the test can be run in an environment with diskspace greater than 1GB, in order for the test to work.  

**Note:** `common-functions.sh` has be used in all the above scripts, contains common aspects of the test scripts.

### To build artifacts in Github

Commit all changes then:
```bash
export RELEASE_TAG="v$(cat gradle.properties | cut -d= -f2)"
git tag -a "${RELEASE_TAG}" -m "${RELEASE_TAG}"
git push --tags
git push
```