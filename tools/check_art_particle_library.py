"""Check each art routes to the particle library or its shader replacement."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
catalog = (ROOT / 'src/main/java/cn/blockforge/generated/generatedmod/ArtCatalog.java').read_text('utf-8-sig')
vfx = (ROOT / 'src/main/java/cn/blockforge/generated/generatedmod/arts/ArtVfxCatalog.java').read_text('utf-8-sig')
ids = re.findall(r'new Entry\("(art_[^"]+)",', catalog)
recipes = set(re.findall(r'case "(art_[^"]+)" -> new Recipe', vfx))
profiles = set(re.findall(r'case "(art_[^"]+)" -> new AttackProfile', catalog))
shader_source = (ROOT / 'src/main/java/cn/blockforge/generated/generatedmod/arts/FourArtsVisuals.java').read_text('utf-8-sig')
shader_ids = dict(re.findall(r'\b([A-Z_]+)="(art_[^"]+)"', shader_source))
replacement = re.search(r'boolean replaces\(String art\) \{(.*?)\}', shader_source, re.S)
assert replacement, 'missing shader replacement routing'
shader_arts = {shader_ids[name] for name in re.findall(r'([A-Z_]+)\.equals\(art\)', replacement[1])}
assert len(shader_arts) == 5 and shader_arts <= set(ids), f'invalid shader art list: {shader_arts}'
assert not shader_arts & recipes, f'legacy recipes for shader arts: {shader_arts & recipes}'
shader_dir = ROOT / 'src/main/resources/assets/wuxingxiantu/shaders/core'
for suffix in ('json', 'vsh', 'fsh'):
    assert (shader_dir / f'four_arts.{suffix}').is_file(), f'missing shader asset: {suffix}'
missing = sorted(set(ids) - recipes - shader_arts)
assert not missing, f'missing V2 recipe: {missing}'
for art in profiles:
    assert f'case "{art}"' in vfx, f'missing VFX case: {art}'
materials = set()
for row in re.findall(r'new Recipe\("([^\"]+)",\s*"([^\"]+)",\s*"([^\"]+)"', vfx):
    materials.update(row)
for material in materials:
    particle = ROOT / 'src/main/resources/assets/wuxingxiantu/particles' / f'art_{material}.json'
    texture = ROOT / 'src/main/resources/assets/wuxingxiantu/textures/particle/arts' / f'{material}.png'
    assert particle.exists() and texture.exists(), f'missing runtime material: {material}'
print(f'Art particle library: {len(ids)} arts, {len(profiles)} semantic attack profiles, {len(materials)} materials, {len(shader_arts)} shader arts')
