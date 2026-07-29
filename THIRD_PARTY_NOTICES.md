# Third-party notices

This inventory covers third-party components resolved by the reproducible build
and tooling committed to the Engine Lite repository. The generated report at
`build/reports/licenses/dependencies.csv` is the version-resolved source of
truth and is checked against `gradle/dependency-licenses.txt`.

## Runtime distribution

The Issue #14 desktop spike distributes the following unmodified runtime JARs
beside Engine Lite's own JARs:

| Components | Purpose | License | Origin |
|---|---|---|---|
| libGDX core, LWJGL3 backend and desktop natives | Cross-platform application, graphics, input, audio and asset APIs | Apache License 2.0 | <https://github.com/libgdx/libgdx> |
| gdx-jnigen-loader | Extraction and loading of libGDX native libraries | Apache License 2.0 | <https://github.com/libgdx/gdx-jnigen> |
| LWJGL core and bindings | JVM bindings and native desktop runtime | BSD 3-Clause | <https://github.com/LWJGL/lwjgl3/tree/3.4.1> |
| GLFW native bundled by LWJGL | Window, context and input integration | Zlib | <https://github.com/glfw/glfw/tree/3.4> |
| jemalloc native bundled by LWJGL | Native memory allocation | BSD 2-Clause | <https://github.com/jemalloc/jemalloc/tree/5.3.0> |
| OpenAL Soft native bundled by LWJGL | Desktop audio implementation | GNU Library General Public License 2.0 or later | <https://github.com/kcat/openal-soft/tree/1.25.1> |
| stb native bundled by LWJGL | Image and font utility bindings | MIT or public domain | <https://github.com/LWJGL/lwjgl3/tree/3.4.1/modules/lwjgl/stb> |
| JLayer for libGDX | MP3 decoder required while the LWJGL3 audio backend initializes | GNU Lesser General Public License 2.1 | <https://github.com/libgdx/jlayer-gdx> |
| JOrbis | Ogg/Vorbis decoder required while the LWJGL3 audio backend initializes | GNU Library General Public License 2.0 or later | <https://www.jcraft.com/jorbis/> |

The ZIP includes the complete license texts and pinned provenance record under
`third_party/`. LWJGL's own `LICENSE.md` is the BSD 3-Clause text for LWJGL; it
is not used as a substitute for the separately licensed native components.

JLayer and JOrbis remain separate, unmodified JARs in the distribution. The
LWJGL3 backend resolves their decoder types while constructing its audio
implementation, even though this spike's executable probe uses PCM WAV.

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

No distributable third-party asset is present. The two textual spike fixtures
are original Engine Lite work under Apache-2.0. Their provenance is recorded in
[`assets/ATTRIBUTION.md`](assets/ATTRIBUTION.md).
