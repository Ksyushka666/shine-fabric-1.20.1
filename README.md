<img width="2035" height="574" alt="LOGO_4" src="https://github.com/user-attachments/assets/b9c0afcf-8c61-497f-a1a4-33ad22396f32" />

___
[![YouTube](https://img.shields.io/badge/28-FF0000?style=for-the-badge&logo=youtube&logoColor=white&label=YouTube)](https://www.youtube.com/@tapeqz) [![Discord](https://img.shields.io/badge/JOIN-5865F2?style=for-the-badge&logo=discord&logoColor=white&label=Discord)](https://discord.gg/wX836fj6pz) [![Modrinth](https://img.shields.io/badge/PROFILE-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white&label=Modrinth)](https://modrinth.com/user/tapeQ) [![CurseForge](https://img.shields.io/badge/PROJECTS-F16436?style=for-the-badge&logo=curseforge&logoColor=white&label=CurseForge)](https://www.curseforge.com/members/tapeq/projects) [![GitHub](https://img.shields.io/badge/Shine-181717?style=for-the-badge&logo=github&logoColor=white&label=GitHub)](https://github.com/tapeQz/Shine) 
___
![DESCRIPTION HEADER](https://cdn.modrinth.com/data/cached_images/12610f312c2104ff2e38408d6b52f9824260434d.png)



# What does it do?
Shine adds these features, **without shaders**:
- Selective Bloom
- Colored lighting
- Rim lighting (outline around blocks)
- Optional disabled shading (cartoonish look)

Little to **no performance loss** when enabled, with support for [Sodium](https://modrinth.com/mod/sodium).

**Highly customizable** and **easy to use** config screen. You can change almost everything to your preference.

## Downloads

The latest verified build is **Shine Fabric 1.20.1 1.0.3**. Download the files directly from [GitHub Releases](https://github.com/Ksyushka666/shine-fabric-1.20.1/releases/tag/v1.0.3-build-10):

| File | Download |
|---|---|
| Mod JAR | [shine-fabric-1.20.1-1.0.3.jar](https://github.com/Ksyushka666/shine-fabric-1.20.1/releases/download/v1.0.3-build-10/shine-fabric-1.20.1-1.0.3.jar) |
| Sources JAR | [shine-fabric-1.20.1-1.0.3-sources.jar](https://github.com/Ksyushka666/shine-fabric-1.20.1/releases/download/v1.0.3-build-10/shine-fabric-1.20.1-1.0.3-sources.jar) |
| SHA-256 checksums | [SHA256SUMS.txt](https://github.com/Ksyushka666/shine-fabric-1.20.1/releases/download/v1.0.3-build-10/SHA256SUMS.txt) |

For future builds, the [Releases page](https://github.com/Ksyushka666/shine-fabric-1.20.1/releases) always contains the newest automatically verified download links.

## Requirements and installation

For **Minecraft 1.20.1**, the minimum installation requires **Fabric Loader**, **Fabric API for 1.20.1**, Java 17, and the Shine JAR from the [latest release](https://github.com/Ksyushka666/shine-fabric-1.20.1/releases). Put the following files in `.minecraft/mods/`:

| Mod or component | Required | Purpose |
|---|---:|---|
| Minecraft Java Edition 1.20.1 | Yes | Target game version |
| Fabric Loader | Yes | Mod loader |
| Fabric API for 1.20.1 | Yes | Fabric events, registries, and runtime hooks |
| Shine `shine-fabric-1.20.1-<version>.jar` | Yes | Selective bloom and visual effects |
| YACL 3.6.6 for 1.20.1 | No | Extended configuration screen |
| Mod Menu | No | Opens Shine settings from the mods list |
| Sodium 0.5.x for 1.20.1 | No | Performance optimization and optional compatibility path |
| Iris for 1.20.1 | No | Shader-pack support and framebuffer ownership guard |

A minimal client setup is:

```text
.minecraft/mods/
├── fabric-api-<version>-1.20.1.jar
└── shine-fabric-1.20.1-<version>.jar
```

For the full configuration UI, add YACL and optionally Mod Menu. Sodium and Iris are optional and must be the releases built for **Minecraft 1.20.1**; do not use their 1.21.x builds. Without YACL, Shine can still run, but its extended configuration screen is unavailable. Without Sodium or Iris, Shine uses the Vanilla rendering path.

For a dedicated server, install Fabric Loader, Fabric API, and Shine only if the server is intended to load the common registry content. Do not install Sodium, Iris, YACL, or Mod Menu on the server. Bloom rendering is client-side, and the common entrypoint is kept free of client-only class dependencies.
___
![DEMONSTRATION HEADER](https://cdn.modrinth.com/data/cached_images/ffdae0a44fc0bbc0157a3652aca2549b1046befa.png)

<img width="3840" height="2160" alt="huge_2026-05-18_15 39 55" src="https://github.com/user-attachments/assets/7cbc1608-9877-476c-9ce9-f71d80acfd3e" />
<img width="3840" height="2160" alt="2_1" src="https://github.com/user-attachments/assets/bdcf6166-6b54-4beb-8650-17ebd35c96ee" />
<img width="3840" height="2160" alt="colored light demo" src="https://github.com/user-attachments/assets/e57f2649-a86a-4c07-b233-ea5b756ad4bd" />





___
# **Incompatible mods**

<details>
<summary>Spoiler</summary>

- nvidium
- immersive portals
- vulkanmod
- sodium core shader support

</details>

___
Copyright © 2026 tapeQ

- **You can include this mod in a modpack**, as long as it provides credit and links to the Modrinth or CurseForge page.

## Continuous integration

Every push, pull request, or manual workflow dispatch runs `./gradlew clean check build` on Java 17. The workflow uploads the verified mod and sources JARs only when the build succeeds. Optional YACL, Mod Menu, and Sodium integrations resolve from Maven coordinates, so the build does not depend on a local developer cache.

## Versioning

The mod version is controlled by `mod_version` in `gradle.properties`. Release artifacts use the format `shine-fabric-1.20.1-<mod_version>.jar`, with the sources artifact ending in `-sources.jar`. The release workflow reads this value after a successful build and creates a unique release tag for the build.
