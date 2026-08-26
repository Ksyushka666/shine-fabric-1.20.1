from pathlib import Path
import json,re
root=Path('src/main/resources/assets')
errors=[]; checked=0
for p in root.rglob('*.json'):
    try: d=json.loads(p.read_text(encoding='utf-8-sig'))
    except: continue
    if not isinstance(d,dict) or not ('vertex' in d and 'fragment' in d): continue
    checked+=1
    ns=p.relative_to(root).parts[0]
    base=p.parent
    for kind in ('vertex','fragment'):
        name=d[kind]
        q=base/(name+'.'+('vsh' if kind=='vertex' else 'fsh'))
        if not q.is_file(): errors.append(f'{p}: missing {kind} {q}')
    for u in d.get('uniforms',[]):
        if not isinstance(u,dict) or 'name' not in u: errors.append(f'{p}: malformed uniform')
    f=base/(d['fragment']+'.fsh')
    if f.is_file() and 'bloomColor' in f.read_text(errors='ignore') and 'location = 1' not in f.read_text(errors='ignore'):
        errors.append(f'{p}: bloomColor lacks explicit location 1')
print('shader_program_json=',checked)
print('errors=',len(errors))
for e in errors[:50]: print(e)
raise SystemExit(bool(errors))
