# Third-party notices

This inventory covers third-party components resolved by the reproducible build
and tooling committed to the Engine Lite repository. The generated report at
`build/reports/licenses/dependencies.csv` is the version-resolved source of
truth and is checked against `gradle/dependency-licenses.txt`.

## Runtime distribution

Engine Lite currently has no third-party production runtime dependency.

## Test dependencies

| Components | Purpose | License | Origin |
|---|---|---|---|
| JUnit Jupiter, JUnit Platform and JUnit BOM | Automated tests only | Eclipse Public License 2.0 | <https://github.com/junit-team/junit5> |
| OpenTest4J | Test assertion/exception interoperability | Eclipse Public License 2.0 | <https://github.com/ota4j-team/opentest4j> |

These components are not bundled into Engine Lite production JARs.

## Build tooling

| Component | Purpose | License | Origin |
|---|---|---|---|
| Gradle Wrapper | Downloads and starts the pinned Gradle distribution | Apache License 2.0 | <https://github.com/gradle/gradle> |
| Eclipse Temurin JDK 21 and 25 | Build and compatibility toolchains | GPL-2.0-only with Classpath Exception 2.0 | <https://adoptium.net/temurin/> |

The downloaded Gradle distribution is build tooling, not part of Engine Lite
artifacts, and carries its own license and notice files. The JDKs compile and
run the build; they are not bundled into Engine Lite JARs.

## Continuous-integration actions

| Component | License | Origin |
|---|---|---|
| `actions/checkout@v6` | MIT | <https://github.com/actions/checkout> |
| `actions/setup-java@v5` | MIT | <https://github.com/actions/setup-java> |
| `actions/upload-artifact@v7` | MIT | <https://github.com/actions/upload-artifact> |
| `gradle/actions/setup-gradle@v6` | Primarily MIT; includes a separately licensed vendor caching component | <https://github.com/gradle/actions> |

These actions run only in GitHub-hosted CI and are not bundled in Engine Lite
artifacts. The Gradle action's repository `LICENSE`, `DISTRIBUTION.md` and
`NOTICE` govern its vendor-included caching component.

## Assets

No distributable third-party asset is currently present. Asset provenance is
governed by [`assets/ATTRIBUTION.md`](assets/ATTRIBUTION.md).
