"""万界仙途工作台统一入口；可通过 --catalog 接入现有总目录。"""
from pathlib import Path
from functools import partial
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
import argparse, importlib.util, threading, webbrowser

ROOT=Path(__file__).resolve().parent

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--catalog',type=Path,help='现有启动全部工作台.py 路径')
    ap.add_argument('--open',dest='selected',help='名称关键词或编号')
    ap.add_argument('--no-browser',action='store_true')
    args=ap.parse_args()
    external=None
    entries=[]
    if args.catalog:
        spec=importlib.util.spec_from_file_location('existing_workbench_catalog',args.catalog)
        external=importlib.util.module_from_spec(spec);spec.loader.exec_module(external)
        entries=[(item[0],item) for item in external.ENTRIES]
    entries += [('紫曜背轮 · 分层调色工作台','lucifer-workbench'),('储物袋 UI · 原版重置预览','bag-ui'),('阵法总工作台 · 全部预览','formations'),('阵法刻画核心 · 流程预览','formation-carving'),('炼丹重塑 · 年份与九品参数工作台','alchemy-rework'),('灵石矿 · 贴图展示工作台','ore-gallery')]
    for i,(name,_) in enumerate(entries,1): print(f'{i}. {name}',flush=True)
    choice=args.selected or input('输入编号或名称：').strip()
    selected=int(choice)-1 if choice.isdecimal() else next((i for i,(name,_) in enumerate(entries) if choice in name),-1)
    if not 0<=selected<len(entries):raise SystemExit('未找到工作台')
    name,item=entries[selected]
    if item in ('formations','formation-carving'):
        spec=importlib.util.spec_from_file_location('formations_gallery',ROOT/'formations/server.py')
        gallery=importlib.util.module_from_spec(spec);spec.loader.exec_module(gallery)
        server,url=gallery.create_server()
        if item=='formation-carving':url=url.replace('/formations/index.html','/formation-carving/index.html')
    elif not isinstance(item,str): server,url=external.create_server(item)
    else:
        class Handler(SimpleHTTPRequestHandler):
            def translate_path(self, path):
                if item == 'ore-gallery' and path in ('/spirit_vein_ore.png', '/spirit_ore.png'):
                    textures = ROOT.parents[1] / 'src/main/resources/assets/wuxingxiantu/textures/block'
                    return str(textures / path[1:])
                return super().translate_path(path)

            def log_message(self,*args):pass
            def end_headers(self):
                self.send_header('Cache-Control','no-store');super().end_headers()
        server=ThreadingHTTPServer(('127.0.0.1',0),partial(Handler,directory=str(ROOT/item)))
        url=f'http://127.0.0.1:{server.server_port}/index.html'
    print(f'WORKBENCH_URL={url}',flush=True)
    if not args.no_browser:webbrowser.open_new_tab(url)
    try:server.serve_forever()
    except KeyboardInterrupt:pass
    finally:server.server_close()

if __name__=='__main__':main()
