from pathlib import Path
import json

root = Path(__file__).parents[1]
core = root / 'src/main/resources/assets/minecraft/shaders/core'
errors = []
entity_fsh = sorted(p for p in core.glob('*entity*.fsh'))
entity_json = sorted(p for p in core.glob('*entity*.json'))
service = {'rendertype_armor_entity_glint', 'rendertype_entity_decal', 'rendertype_entity_glint', 'rendertype_entity_glint_direct', 'rendertype_entity_shadow'}
visual_fsh = [p for p in entity_fsh if p.stem not in service]
visual_json = [p for p in entity_json if p.stem not in service]
if len(visual_fsh) != 11 or len(visual_json) != 11:
    errors.append(f'entity_variant_count visual_fsh={len(visual_fsh)} visual_json={len(visual_json)} expected=11')
for shader in visual_fsh + [core / 'particle.fsh']:
    if not shader.is_file():
        errors.append(f'missing_overlay={shader}')
        continue
    text = shader.read_text(errors='ignore')
    if 'out vec4 bloomColor;' not in text:
        errors.append(f'missing_bloom_output={shader.name}')
    if 'layout(location' in text:
        errors.append(f'glsl150_incompatible_output_layout={shader.name}')
for shader in visual_json + [core / 'particle.json']:
    if not shader.is_file():
        continue
    data = json.loads(shader.read_text(encoding='utf-8-sig'))
    if not any(u.get('name') == ('ShineEntityStrength' if 'entity' in shader.name else 'ShineParticleStrength') for u in data.get('uniforms', [])):
        errors.append(f'missing_source_uniform={shader.name}')
hook = (root / 'src/main/java/com/bloom/mixin/client/ShaderInstanceBloomUniformMixin.java').read_text()
if 'IrisCompat.shouldYieldToShaderPack()' not in hook:
    errors.append('missing_iris_shader_ownership_guard')
for stem in service:
    p = core / (stem + '.fsh')
    if p.is_file() and 'bloomColor' in p.read_text(errors='ignore'):
        errors.append(f'service_variant_has_bloom_output={stem}')
print(f'entity_fragment_overlays={len(visual_fsh)}')
print(f'entity_program_overlays={len(visual_json)}')
print('particle_overlay=1')
print(f'errors={len(errors)}')
for error in errors: print(error)
raise SystemExit(bool(errors))
