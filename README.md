

Command-line tool that records the screen as Fragmented MP4 and upload the fragments as they are being generated.

## Run

### Download

Download the `track-code-and-upload.jar` from `https://github.com/io-accelerate/trk-code-track-and-upload/releases/latest`

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
java -jar track-code-and-upload/build/libs/code-track-and-upload-*-all.jar --run-self-test
```

### To build artifacts in Github

Commit all changes then:
```bash
export RELEASE_TAG="v$(cat gradle.properties | cut -d= -f2)"
git tag -a "${RELEASE_TAG}" -m "${RELEASE_TAG}"
git push --tags
git push
```