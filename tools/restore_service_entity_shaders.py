from pathlib import Path
import zipfile

ROOT = Path(__file__).parents[1]
MC = Path.home() / '.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar'
OUT = ROOT / 'src/main/resources'
service = {
    'rendertype_armor_entity_glint',
    'rendertype_entity_decal',
    'rendertype_entity_glint',
    'rendertype_entity_glint_direct',
    'rendertype_entity_shadow',
}
with zipfile.ZipFile(MC) as archive:
    for name in archive.namelist():
        path = Path(name)
        if path.parent.as_posix() != 'assets/minecraft/shaders/core': continue
        if path.stem not in service or path.suffix not in {'.json', '.vsh', '.fsh'}: continue
        target = OUT / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(archive.read(name))
print(f'restored_service_variants={len(service)}')
