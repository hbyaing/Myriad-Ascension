"""Reject custom definitions shadowing GLSL noise built-ins; not a GLSL compiler."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/wuxingxiantu/shaders'
count=0
for path in sorted(root.rglob('*')):
    if path.suffix not in {'.fsh','.vsh','.glsl'}:continue
    count+=1
    source=re.sub(r'/\*.*?\*/|//[^\n]*','',path.read_text(encoding='utf-8-sig'),flags=re.S)
    match=re.search(r'\b(?:float|vec[234]|int|bool|void)\s+(noise[1234])\s*\([^)]*\)\s*\{',source)
    assert match is None, f'{path}: custom {match.group(1)} conflicts with GLSL built-in names'
print(f'Shader built-in-name audit: {count} sources checked (native compilation still required)')
