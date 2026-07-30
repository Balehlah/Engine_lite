# Third-party runtime materials

This directory records the third-party code shipped by the Issue #14 desktop
spike. It packages upstream license files or the complete canonical license
text identified by the linked source metadata. The root `LICENSE` is also
packaged as `licenses/Apache-2.0.txt`.

| Component | Version or provenance | License | Corresponding source | Packaged text |
|---|---|---|---|---|
| libGDX core and LWJGL3 backend | 1.14.2 | Apache-2.0 | <https://github.com/libgdx/libgdx/tree/1.14.2> | `licenses/Apache-2.0.txt` |
| Curated libGDX Windows/Linux desktop natives | 1.14.2; source SHA-256 `f4847981d27c6524a30f5665036ec8c11f48c8eda7610bb63f742de95ffe1970` | Apache-2.0 | <https://repo1.maven.org/maven2/com/badlogicgames/gdx/gdx-platform/1.14.2/> | `licenses/Apache-2.0.txt`; `gdx-platform-1.14.2-natives-desktop.provenance` |
| gdx-jnigen-loader | 2.5.2 | Apache-2.0 | <https://github.com/libgdx/gdx-jnigen/tree/2.5.2> | `licenses/Apache-2.0.txt` |
| LWJGL bindings and core natives | 3.4.1 | BSD-3-Clause | <https://github.com/LWJGL/lwjgl3/tree/3.4.1> | `licenses/LWJGL-BSD-3-Clause.txt` |
| GLFW native bundled by LWJGL | GLFW 3.4 baseline; LWJGL binary marker `9352d8fe93cd443be18157abe81f16500549aec0` | Zlib | <https://github.com/glfw/glfw/tree/3.4> | `licenses/GLFW-Zlib.txt` |
| jemalloc native bundled by LWJGL | jemalloc 5.3.0 baseline; LWJGL binary marker `1972241cd204c60fb5b66f23c48a117879636161` | BSD-2-Clause | <https://github.com/jemalloc/jemalloc/tree/5.3.0> | `licenses/jemalloc-BSD-2-Clause.txt` |
| OpenAL Soft native bundled by LWJGL | 1.25.1; LWJGL binary marker `c41d64c6a35f6174bf4a27010aeac52a8d3bb2c6` | LGPL-2.0-or-later | <https://github.com/kcat/openal-soft/tree/1.25.1> | `licenses/OpenAL-Soft-LGPL-2.0-or-later.txt` |
| stb native bundled by LWJGL | LWJGL 3.4.1 `lwjgl-stb` artifact | MIT or public domain | <https://github.com/LWJGL/lwjgl3/tree/3.4.1/modules/lwjgl/stb> | `licenses/stb-MIT-or-Public-Domain.txt` |
| JLayer for libGDX | 1.0.1-gdx | LGPL-2.1-only | <https://repo1.maven.org/maven2/com/badlogicgames/jlayer/jlayer/1.0.1-gdx/jlayer-1.0.1-gdx-sources.jar> | `licenses/JLayer-LGPL-2.1.txt` |
| JOrbis | 0.0.17; source headers select GNU Library GPL v2 or later | LGPL-2.0-or-later | <https://repo1.maven.org/maven2/org/jcraft/jorbis/0.0.17/jorbis-0.0.17-sources.jar> | `licenses/JOrbis-LGPL-2.0-or-later.txt` |

The MP3 and Ogg/Vorbis decoders are retained because the upstream LWJGL3 audio
backend resolves their types during construction before the PCM WAV probe is
created. Removing either JAR makes backend initialization fall back to
`MockAudio`, which this spike deliberately rejects.

Except for the explicitly documented libGDX native curation, the libraries
remain separate, unmodified JARs and native binaries under `lib/`. The curated
JAR copies the Windows/Linux payload bytes unchanged. The two `.dylib` entries are excluded
from the distribution. A compatible rebuilt OpenAL Soft native can replace the
corresponding `lwjgl-openal-3.4.1-natives-<platform>.jar`; reverse engineering
for debugging such modifications is permitted. The same separate-library
replacement right applies to JLayer and JOrbis.
