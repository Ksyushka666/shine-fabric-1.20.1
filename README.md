# Shine — Fabric 1.20.1

A Fabric 1.20.1 port of Shine selective bloom with legacy `RenderTarget`/`PostChain` rendering, Vanilla and Sodium 0.5.x compatibility, Iris ownership guards, original visual resources, and dedicated-server-safe common registration.

## Included artifacts

- `shine-fabric-1.20.1.jar` — remapped mod artifact
- `shine-fabric-1.20.1-sources.jar` — remapped sources artifact
- `PORT_STATUS.md` — port status and validation record
- audit scripts and their supporting reports
- `PortPipelineTest.java` and the latest build log

## Runtime requirements

Minecraft 1.20.1, Java 17, Fabric Loader, and Fabric API are required. YACL 3.6.6 is optional for the configuration screen. Sodium 0.5.x and Iris are optional compatibility environments.

## Validation

The latest `gradle clean check build` completed successfully with 28 tasks. The resource-to-hook audit reports 84 particle definitions, one active bloom PostChain resource, two world-render hooks and zero errors. The remaining qualification step is a graphical smoke test in a real Minecraft 1.20.1 client with Vanilla, Sodium 0.5.11 and Iris.

## License

See the license information in `PORT_STATUS.md` and the mod artifact metadata.
