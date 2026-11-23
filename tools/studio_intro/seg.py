import sys, numpy as np
from PIL import Image, ImageDraw
D=sys.argv[1]
im=Image.open(f'{D}/src.webp').convert('RGB'); W,H=im.size
a=np.asarray(im).astype(np.float32)
mx=a.max(-1); mn=a.min(-1)

def shift(m,dx,dy):
    o=np.zeros_like(m)
    ys=slice(max(dy,0),H+min(dy,0)); yd=slice(max(-dy,0),H+min(-dy,0))
    xs=slice(max(dx,0),W+min(dx,0)); xd=slice(max(-dx,0),W+min(-dx,0))
    o[ys,xs]=m[yd,xd]; return o
def dilate(m,n=1):
    for _ in range(n):
        m=m|shift(m,1,0)|shift(m,-1,0)|shift(m,0,1)|shift(m,0,-1)
    return m
def erode(m,n=1): return ~dilate(~m,n)

def label(mask, minsize=1):
    """Connected components via PIL flood fill. Returns list of boolean masks."""
    img=Image.fromarray((mask*255).astype(np.uint8)).convert('RGB')
    arr=np.asarray(img); comps=[]
    todo=mask.copy(); k=0
    while todo.any():
        ys,xs=np.nonzero(todo); y,x=ys[0],xs[0]
        k+=1; col=(k%251+1, (k//251)%251+1, 7)
        ImageDraw.floodfill(img,(int(x),int(y)),col,thresh=0)
        arr=np.asarray(img)
        c=(arr[...,0]==col[0])&(arr[...,1]==col[1])&(arr[...,2]==7)
        todo&=~c
        if c.sum()>=minsize: comps.append(c)
    return comps

# --- background: outside the card (black corners) + white card + enclosed white pockets
whiteish=(mn>226)&((mx-mn)<30)
black=mx<40
cand=whiteish|black
bg=np.zeros((H,W),bool)
for c in label(cand, 1):
    ys,xs=np.nonzero(c)
    if c.sum()>120 or c[0].any() or c[:,0].any(): bg|=c
fg=~bg
# drop specks
fgc=[c for c in label(fg,1)]
fg=np.zeros_like(fg)
for c in fgc:
    if c.sum()>=60: fg|=c
bg=~fg
print('components fg:', sum(1 for c in fgc if c.sum()>=60))

# --- alpha by unmixing from white in a band around the boundary
band=dilate(fg,2)&~erode(fg,3)
interior=fg&~band
F=np.where(interior[...,None],a,0); Wt=interior.astype(np.float32)
Fs=F.copy(); Ws=Wt.copy()
for _ in range(8):  # spread interior colour outwards
    Fn=Fs.copy(); Wn=Ws.copy()
    for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
        Fn+= np.stack([shift_f for shift_f in [np.roll(np.roll(Fs,dy,0),dx,1)]])[0]
        Wn+= np.roll(np.roll(Ws,dy,0),dx,1)
    Fs,Ws=Fn,Wn
est=Fs/np.maximum(Ws,1e-3)[...,None]
d=255-est; c=255-a
alpha_b=np.clip((c*d).sum(-1)/np.maximum((d*d).sum(-1),1.0),0,1)
alpha=np.where(interior,1.0,np.where(band,alpha_b,0.0)).astype(np.float32)
col=np.where(band[...,None]&(Ws[...,None]>0),est,a)
# where unmixed alpha is small the colour estimate is what matters; keep original where solid
col=np.where((alpha>0.98)[...,None],a,col)

# --- classes
r,g,b=a[...,0],a[...,1],a[...,2]
solid=alpha>0.5
YY,XX=np.mgrid[0:H,0:W]
comps=[c&solid for c in label(solid,40)]
info=[]
for i,c in enumerate(comps):
    ys,xs=np.nonzero(c); info.append((i,c.sum(),xs.min(),ys.min(),xs.max(),ys.max()))
for t in sorted(info,key=lambda t:-t[1])[:20]: print(t)
np.savez_compressed(f'{D}/seg.npz',alpha=alpha,col=col.astype(np.float32),fg=fg)
