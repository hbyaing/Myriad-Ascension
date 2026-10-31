"""Build the huahong screenshot review from successful, isolated Forge captures.

Open this review through the project's unified workbench launcher. Requires Pillow.
The page displays recorded game frames; it does not edit the live game's settings.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


def check_capture(source, allow_photo_gaps=False):
    report = json.loads((source / 'verification.json').read_text(encoding='utf-8'))
    if not report['pointer']['passed']:
        raise ValueError(f'{source}: pointer protection failed')
    if report['failures']:
        summary = json.loads((source / 'dynamics-summary.json').read_text(encoding='utf-8'))
        gaps = summary['failures']
        expected = 'java.lang.AssertionError: Dynamics acceptance: [' + ', '.join(gaps) + ']'
        if not (allow_photo_gaps and gaps and all(x.startswith('Missing real moving normal-exposure photograph: ') for x in gaps)
                and report['failures'] == [expected]):
            raise ValueError(f'{source}: failed capture: {report["failures"]}')
    return report


def dynamic_rows(sources, skins):
    batches = []
    for index, source in enumerate(sources):
        check_capture(source, allow_photo_gaps=index == 0 and len(sources) > 1)
        batches.append((source, json.loads((source / 'dynamics-frames.json').read_text(encoding='utf-8'))))
    rows = []
    for index, skin in enumerate(skins):
        for view, segment in [('side', 'straight'), ('turn', 'uturn-180')]:
            candidates = []
            for source, frames in reversed(batches):
                for frame in frames:
                    if (frame['skin'].lower() != skin['id'] or frame['segment'] != segment
                            or frame['stage'] != 2 or frame['strength'] < .995 or frame['speed'] < 8
                            or frame['view'] != ('three-quarter-side' if view == 'side' else 'near-chase')
                            or frame['exposureAlphaMultiplier'] != 1 or not frame['cameraSettled']):
                        continue
                    if skin['id'] == 'dense':
                        volume = frame.get('volume', {})
                        if (volume.get('submitted', 0) < 1 or volume.get('frame') != frame['frame']
                                or not any(p.get('submitted') and p.get('frame') == frame['frame']
                                           for p in volume.get('currentPackets', []))):
                            continue
                    matches = list((source / 'motion').glob(f"*-e1.0000-{frame['frame']:07d}.png"))
                    if matches:
                        candidates.append((source, frame, matches[0]))
                if candidates:
                    break
            if not candidates:
                raise ValueError(f"Missing normal-exposure {skin['id']} / {segment}")
            source, frame, raw = candidates[len(candidates) // 2]
            rows.append({'image': f"skin-{index}-{skin['id']}-{view}.png", 'rawImage': raw.relative_to(source).as_posix(),
                         'sourceReport': source.resolve().relative_to(ROOT).as_posix(),
                         'stage': frame['stage'], 'strength': frame['strength'], 'scenarioPhase': frame['phase'],
                         'segment': frame['segment'],
                         'sourceFrame': frame['frame'], 'speed': frame['speed'], 'view': frame['view'],
                         'meshFile': frame.get('headMeshFile', frame.get('meshFile')),
                         'fullMeshFile': frame.get('fullMeshFile'), 'exposure': 1})
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--native', type=Path, required=True)
    parser.add_argument('--solas', type=Path, required=True)
    parser.add_argument('--native-ui', type=Path, help='Optional later native UI-only capture')
    parser.add_argument('--solas-ui', type=Path, help='Optional later Solas UI-only capture')
    parser.add_argument('--native-supplement', type=Path, action='append', default=[], help='Explicit later per-component evidence; priority over earlier photos')
    parser.add_argument('--solas-supplement', type=Path, action='append', default=[])
    parser.add_argument('--output', type=Path, default=ROOT / 'deliverables/ui-workbench-20261017/huahong-skins')
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    enum = (ROOT / 'src/main/java/cn/blockforge/generated/generatedmod/yujian/HuahongSkin.java').read_text(encoding='utf-8')
    skins = [{'id': key.lower(), 'label': label, 'description': desc} for key, label, desc in re.findall(r'\b([A-Z_]+)\("([^"]+)", "([^"]+)"\)', enum)]
    manifest = {'date': '2026-10-31', 'route': '背包 → 设置 → 御剑飞行 → 化虹皮肤',
                'config': 'FlyingSwordConfig.HUAHONG_SKIN', 'skins': skins, 'captures': {}}
    for backend, source in [('native', args.native), ('solas', args.solas)]:
        supplements = args.native_supplement if backend == 'native' else args.solas_supplement
        report = check_capture(source, allow_photo_gaps=bool(supplements))
        ui_source = (args.native_ui if backend == 'native' else args.solas_ui) or source
        ui_report = json.loads((ui_source / 'verification.json').read_text(encoding='utf-8'))
        if ui_report['failures'] or not ui_report['pointer']['passed']:
            raise ValueError(f'{backend}: failed UI capture')
        rows = (dynamic_rows([source] + supplements, skins) if report.get('dynamics') else
                [dict(r) for r in report['rows'] if r.get('image', '').startswith('skin-')])
        rows += [dict(r) for r in ui_report['rows'] if r.get('image', '').startswith(('settings-', 'sense-shortcuts-'))]
        if sum(r['image'].startswith('skin-') for r in rows) != 16:
            raise ValueError(f'{backend}: expected 16 skin frames')
        for size in ('1920x1080', '2560x1440'):
            for gui in (3, 4):
                prefix = f'settings-{size}-gui{gui}-'
                for state in ('normal', 'hover', 'focus', 'disabled', 'expanded-focus-disabled'):
                    if not any(r['image'] == prefix + state + '.png' for r in rows):
                        raise ValueError(f'{backend}: missing dropdown state {prefix}{state}')
        for row in rows:
            if row['image'].startswith('skin-') and (row['stage'] != 2 or row['strength'] < .995):
                raise ValueError('Skin frame is not stable cruise')
            raw = (ROOT / row['sourceReport'] if 'sourceReport' in row else source if row['image'].startswith('skin-') else ui_source) / row.get('rawImage', row['image'])
            target = args.output / f"{backend}-{Path(row['image']).stem}.jpg"
            with Image.open(raw) as image:
                image.thumbnail((1600, 1000))
                image.convert('RGB').save(target, quality=92)
            row['file'] = target.name
            row['sha256'] = hashlib.sha256(target.read_bytes()).hexdigest()
            row['originalSha256'] = hashlib.sha256(raw.read_bytes()).hexdigest()
        summary = json.loads((source / 'dynamics-summary.json').read_text(encoding='utf-8')) if report.get('dynamics') else None
        resolved_gaps = []
        if summary:
            for gap in summary['failures']:
                skin_id, group = gap.removeprefix('Missing real moving normal-exposure photograph: ').split(' / ', 1)
                segment, view = group.split('|', 1)
                witness = next(r for r in rows if r.get('segment') == segment and r.get('view') == view
                               and r['image'].startswith(f"skin-{next(i for i,s in enumerate(skins) if s['id'] == skin_id.lower())}-"))
                resolved_gaps.append({'originalFailure': gap, 'sourceReport': witness['sourceReport'],
                                      'sourceFrame': witness['sourceFrame'], 'originalSha256': witness['originalSha256']})
        manifest['captures'][backend] = {'rows': rows, 'pointer': report['pointer'],
                                        'uiPointer': ui_report['pointer'],
                                        'fullSpeed': report.get('fullSpeed', False),
                                        'dynamics': summary, 'resolvedPhotoGaps': resolved_gaps,
                                        'sources': [{'report': p.resolve().relative_to(ROOT).as_posix(), 'verificationSha256': hashlib.sha256((p / 'verification.json').read_bytes()).hexdigest(), 'pointer': check_capture(p, allow_photo_gaps=p == source and bool(supplements))['pointer']} for p in [source] + supplements],
                                        'material': [r for r in report['rows'] if r.get('check') == 'shipping-glsl-periodicity']}
    (args.output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    html = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>化虹皮肤 · 实机对照</title>
<style>
:root{color-scheme:dark}*{box-sizing:border-box}body{margin:0;background:#101820;color:#eee7d5;font:16px/1.7 system-ui,sans-serif}main{max-width:1700px;margin:auto;padding:26px}a{color:#ecd091}h1{font-size:30px;margin:16px 0 4px}h2{font-size:23px;margin-top:30px}p{color:#bfc5ce}nav{display:flex;gap:9px;flex-wrap:wrap;margin:20px 0}button,select{font:inherit;color:inherit;border:1px solid #8b7447;background:#19232c;padding:8px 13px;border-radius:4px}button{cursor:pointer}button[aria-pressed=true]{background:#715b34;border-color:#f1cc78}.pair{display:grid;grid-template-columns:1fr 1fr;gap:18px}figure{margin:0;background:#18232d;border:1px solid #3e4b56}figcaption{padding:7px 14px;color:#e4c68b}img{display:block;width:100%;height:auto}.note{font-size:14px;color:#aab5c4}#description{min-height:30px}@media(max-width:800px){.pair{grid-template-columns:1fr}}
</style><main><a href="../index.html">← 返回总工作台</a>
<h1>化虹皮肤 · Forge 实机对照</h1>
<p>背包 → 设置 → 御剑飞行 → 化虹皮肤。点击当前皮肤按钮展开选择，选中后收起并保存，支持飞行中切换；仅改变外观。</p>
<nav id="skins"></nav><label>飞行画面 <select id="view"><option value="side">稳定直飞</option><option value="turn">掉头画面</option></select></label>
<p id="description"></p><div class="pair"><figure><figcaption>无光影</figcaption><img id="native" alt="无光影化虹实机截图"></figure><figure><figcaption>Solas Shader V3.7b</figcaption><img id="solas" alt="Solas 化虹实机截图"></figure></div>
<h2>背包设置与识海快捷入口</h2><p>复用正式金墨按钮。记录收起、展开、普通、悬停、焦点和禁用状态；检查两种窗口分辨率与 GUI 选项 3、4。识海两入口按参考恢复图案与左边框衔接。</p>
<label>窗口与状态 <select id="ui"></select></label><div class="pair"><figure><figcaption>无光影设置页</figcaption><img id="ui-native" alt="无光影设置页"></figure><figure><figcaption>Solas 设置页</figcaption><img id="ui-solas" alt="Solas 设置页"></figure></div>
<p class="note">这是实际 Forge 画面记录，按钮在此切换对照图，不控制游戏。正式速度急转、完整掉头、松键和刹车的逐帧数据保留于报告；技术通过不等于用户美术认可。以当前 Skill 的实际消费者逐族核对，虹核保留原头和前向柔光，蓝白虹流采用独立原创路径体积着色器，没有复制许可未确认的参考源码。本页可组合动作矩阵、补图和单材质专项批次；每张图的来源与哈希独立记录，不把分项证据冒称同一次全8通过。原图保留于本地 reports，此页 JPEG 等比缩小用于查看。</p>
<a href="manifest.json">查看截图、皮肤映射与验证记录</a>
</main><script>
fetch('manifest.json').then(r=>r.json()).then(m=>{let current=0;const $=s=>document.getElementById(s);function show(){for(const b of ['native','solas'])$(b).src=b+'-skin-'+current+'-'+m.skins[current].id+'-'+$('view').value+'.jpg';$('description').textContent=m.skins[current].description;[...$('skins').children].forEach((b,i)=>b.setAttribute('aria-pressed',i===current))}m.skins.forEach((s,i)=>{const b=document.createElement('button');b.textContent=s.label;b.onclick=()=>{current=i;show()};$('skins').append(b)});$('view').onchange=show;show();m.captures.native.rows.filter(r=>r.image.startsWith('settings-')||r.image.startsWith('sense-shortcuts-')).forEach(r=>{const o=document.createElement('option');o.value=r.image.slice(0,-4);o.textContent=r.image.replace('settings-','设置 · ').replace('sense-shortcuts-','识海入口 · ').replace('.png','').replace('expanded-focus-disabled','展开 · 焦点与禁用').replace('disabled','禁用').replace('focus','焦点').replace('normal','普通').replace('hover','悬停');$('ui').append(o)});function ui(){for(const b of ['native','solas'])$('ui-'+b).src=b+'-'+$('ui').value+'.jpg'}$('ui').onchange=ui;ui()});
</script></html>'''
    (args.output / 'index.html').write_text(html, encoding='utf-8')
    index = args.output.parent / 'index.html'
    text = index.read_text(encoding='utf-8')
    if 'huahong-skins/index.html' not in text:
        text = text.replace('</header>', '<p><a href="huahong-skins/index.html" style="color:#e4c68b">化虹皮肤 · 实机对照与设置</a></p>\n</header>', 1)
        index.write_text(text, encoding='utf-8')
    print(f'Generated {len(skins)} skins, {sum(len(c["rows"]) for c in manifest["captures"].values())} frames: {args.output}')


if __name__ == '__main__':
    main()
