from pathlib import Path
import zipfile

jar = Path('/home/ubuntu/.gradle/caches/modules-2/files-2.1/maven.modrinth/sodium/mc1.20.1-0.5.11/c4293c1483f3b39c1c6c4c69143e8bb2c6e53daf/sodium-mc1.20.1-0.5.11.jar')
with zipfile.ZipFile(jar) as z:
    vsh = z.read('assets/sodium/shaders/blocks/block_layer_opaque.vsh').decode()
    fsh = z.read('assets/sodium/shaders/blocks/block_layer_opaque.fsh').decode()
checks = {
    'vertex_color_anchor': 'out vec4 v_Color;' in vsh,
    'vertex_main_anchor': 'void main() {' in vsh,
    'vertex_light_anchor': 'v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);' in vsh,
    'fragment_output_anchor': 'out vec4 fragColor; // The output fragment for the color framebuffer' in fsh,
    'fragment_final_anchor': 'fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);' in fsh,
}
for key, value in checks.items():
    print(f'{key}={value}')
if not all(checks.values()):
    raise SystemExit(1)
