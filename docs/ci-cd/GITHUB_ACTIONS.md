# GitHub Actions CI/CD Setup

## Overview

This document describes the GitHub Actions workflow configured for the GlobalTranslation Android project. The workflow provides comprehensive continuous integration and delivery automation.

## Workflow File

**Location**: `.github/workflows/android.yml`

## Architecture Context

The GlobalTranslation app uses a multi-module architecture:
- **:core** - Pure Kotlin module with domain models and interfaces
- **:data** - Android Library with Room database and ML Kit providers
- **:app** - Android App with Jetpack Compose UI

The CI/CD pipeline is designed to validate all three modules.

## Workflow Triggers

The workflow runs on:
- **Push** to `main` branch or any `copilot/**` branches
- **Pull Requests** targeting `main` branch

## Jobs

### 1. Build and Unit Tests

**Purpose**: Build all modules and run unit tests

**Steps**:
1. Checkout code with full git history
2. Set up JDK 21 (Temurin distribution)
3. Cache Gradle dependencies for faster builds
4. Make gradlew executable
5. Build all modules (`./gradlew build`)
6. Run unit tests (`./gradlew test`)
7. Upload test results as artifacts
8. Publish test report using test-reporter action

**Outputs**:
- Unit test results (XML format)
- Test reports (HTML format)
- Artifacts retained for 7 days

### 2. Lint and Code Quality

**Purpose**: Run Android lint checks across all modules

**Steps**:
1. Checkout code
2. Set up JDK 21
3. Run lint checks (`./gradlew lint`)
4. Upload lint reports as artifacts
5. Print summary of lint results

**Outputs**:
- Lint reports (HTML and XML formats)
- Artifacts retained for 7 days

**Note**: Lint job continues even if issues are found (non-blocking)

### 3. Assemble APKs

**Purpose**: Build debug and release APKs

**Strategy**: Matrix build for both debug and release variants

**Steps**:
1. Checkout code
2. Set up JDK 21
3. Assemble APK for the variant
4. Upload APK as artifact
5. Display APK information

**Outputs**:
- Debug APK
- Release APK
- Artifacts retained for 14 days

**Dependencies**: Runs after build-and-test and lint jobs complete

### 4. Instrumented Tests (Optional)

**Status**: Currently commented out

The workflow includes a template for running instrumented tests on an Android emulator. This job is disabled by default due to:
- Longer execution time (10-15 minutes)
- Additional resource requirements
- Emulator setup complexity

**To Enable**:
1. Uncomment the `instrumented-tests` job in the workflow
2. Ensure GitHub Actions has sufficient resources
3. Configure test timeout if needed

## Requirements

### Software
- **JDK**: 21 (Temurin LTS distribution)
- **Gradle**: 8.13 (via wrapper)
- **Android Gradle Plugin**: 8.13.0
- **Kotlin**: 2.2.20

### Build Tools
- **Hilt**: 2.57.2 for dependency injection
- **KSP**: 2.2.20-2.0.2 for annotation processing
- **Room**: 2.8.2 for database
- **Compose BOM**: 2025.10.00 for UI

## Caching Strategy

The workflow uses two caching mechanisms:

1. **Gradle Cache** (via setup-java action):
   - Automatically caches Gradle dependencies
   - Key based on JDK version and distribution

2. **Custom Gradle Cache**:
   - Caches `~/.gradle/caches` and `~/.gradle/wrapper`
   - Key includes hashes of build files and version catalog
   - Improves build times by 50-70%

## Artifacts

All workflow runs produce artifacts:

| Artifact | Content | Retention |
|----------|---------|-----------|
| unit-test-results | JUnit XML test results and HTML reports | 7 days |
| lint-reports | Lint results in HTML and XML formats | 7 days |
| app-debug-apk | Debug APK for testing | 14 days |
| app-release-apk | Release APK (unsigned) | 14 days |

## Monitoring and Debugging

### Viewing Test Results

1. Navigate to the Actions tab in GitHub
2. Click on the workflow run
3. View the "Unit Tests" report inline
4. Download "unit-test-results" artifact for detailed reports

### Viewing Lint Results

1. Navigate to the Actions tab
2. Click on the workflow run
3. Download "lint-reports" artifact
4. Open the HTML files in a browser

### Common Build Failures

#### AGP Plugin Not Found
**Error**: `Plugin [id: 'com.android.application', version: '8.13.0'] was not found`

**Solution**: 
- AGP 8.13.0 is now available in Google Maven (as of October 2025)
- Ensure the runner has access to Google Maven repository
- Check that the version in `gradle/libs.versions.toml` matches

#### JVM Target Mismatch
**Error**: `Inconsistent JVM-target compatibility`

**Solution**:
- All modules use JVM target 21
- Verify `compileOptions` and `kotlin.compilerOptions` in build files
- Check that CI uses JDK 21

#### Out of Memory
**Error**: `OutOfMemoryError` during build

**Solution**:
- Workflow uses default GitHub Actions runner (7GB RAM)
- Add `org.gradle.jvmargs=-Xmx4g` to `gradle.properties` if needed
- Consider using `--max-workers=2` flag

## Performance

### Typical Execution Times

| Job | Duration | Notes |
|-----|----------|-------|
| build-and-test | 5-8 minutes | Includes all modules |
| lint | 3-5 minutes | Parallel execution |
| assemble (debug) | 2-3 minutes | After build cache |
| assemble (release) | 2-3 minutes | After build cache |
| **Total** | **12-19 minutes** | All jobs complete |

### Optimization Tips

1. **Enable Gradle Build Cache**: Already configured
2. **Use Configuration Cache**: Experimental, not yet enabled
3. **Parallel Execution**: Enabled by default
4. **Incremental Builds**: Works with caching

## Future Enhancements

### Planned Improvements

1. **Code Coverage Reports**
   - Add JaCoCo plugin
   - Upload coverage to Codecov or Coveralls
   - Set minimum coverage thresholds

2. **Dependency Checks**
   - Add dependency vulnerability scanning
   - Integrate with GitHub Dependabot
   - Check for outdated dependencies

3. **Release Automation**
   - Create GitHub releases automatically
   - Sign release APKs with secrets
   - Generate changelog from commits

4. **Performance Testing**
   - Add baseline profile generation
   - Measure app startup time
   - Track APK size changes

5. **Security Scanning**
   - Integrate static security analysis
   - Scan for hardcoded secrets
   - Check for vulnerable dependencies

### Optional Additions

- **Matrix Testing**: Test on multiple API levels (29, 33, 34)
- **Slack Notifications**: Alert on build failures
- **Deploy to Play Store**: Automated beta releases
- **Screenshot Testing**: Visual regression testing

## Troubleshooting

### Workflow Not Triggering

1. Check branch patterns in `on.push.branches`
2. Verify PR targets `main` branch
3. Check GitHub Actions is enabled in repository settings

### Tests Failing in CI but Passing Locally

1. Check JDK version matches (21)
2. Verify timezone settings (CI uses UTC)
3. Check for file system case sensitivity issues
4. Ensure no environment-specific dependencies

### Slow Build Times

1. Check cache hit rate in workflow logs
2. Verify `gradle-wrapper.properties` hasn't changed
3. Consider reducing parallel workers if out of memory
4. Check network speed for dependency downloads

## Best Practices

### Workflow Maintenance

- **Version Pins**: All actions use specific versions (e.g., `@v4`)
- **Documentation**: Keep this file updated with workflow changes
- **Testing**: Test workflow changes in branches before merging
- **Monitoring**: Review workflow runs weekly for issues

### Security

- **Secrets Management**: Use GitHub Secrets for sensitive data
- **Permissions**: Workflow uses minimal required permissions
- **Dependencies**: Regular updates for security patches
- **Branch Protection**: Require CI success before merge

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android Gradle Plugin Release Notes](https://developer.android.com/studio/releases/gradle-plugin)
- [Gradle Caching Guide](https://docs.gradle.org/current/userguide/build_cache.html)
- [JUnit Test Reporter](https://github.com/dorny/test-reporter)

---

**Last Updated**: October 2025  
**Maintained By**: Project Contributors  
**Related**: See [`docs/README.md`](../README.md) for complete documentation
