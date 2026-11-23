import sys, numpy as np
from PIL import Image, ImageDraw
D=sys.argv[1]; OUT=sys.argv[2]
z=np.load(f'{D}/seg.npz'); al=z['alpha']; col=z['col']
src=np.asarray(Image.open(f'{D}/src.webp').convert('RGB')).astype(np.float32)
H,W=al.shape
r,g,b=src[...,0],src[...,1],src[...,2]
YY,XX=np.mgrid[0:H,0:W]
def shift(m,dx,dy): return np.roll(np.roll(m,dy,0),dx,1)
def dilate(m,n=1):
    for _ in range(n): m=m|shift(m,1,0)|shift(m,-1,0)|shift(m,0,1)|shift(m,0,-1)
    return m
def comp_at(mask,x,y):
    img=Image.fromarray((mask*255).astype(np.uint8)).convert('RGB')
    ImageDraw.floodfill(img,(x,y),(128,7,9),thresh=0)
    q=np.asarray(img); return (q[...,0]==128)&(q[...,1]==7)
present=al>0.02
grow=dilate(present,0)
def region(x,y):  # component of the soft mask containing (x,y)
    return comp_at(al>0.25,x,y)
mark=dilate(region(250,600),2)&present
upper=dilate(region(1100,330),2)&present
starY=dilate(region(610,330),2)&present
starB=dilate(region(535,420),2)&present
textrow=(al>0.04)&(YY>900)
lineL=textrow&(XX<370); lineR=textrow&(XX>870); letters=textrow&~lineL&~lineR

# G outer left edge (x by row), measured from the image where the swoosh does not cross it.
ey=np.array([400,560,600,700,740,770,800,830,860,880,920]); ex=np.array([700,664,650,648,655,668,689,720,768,812,890])
gEdge=np.interp(YY,ey,ex)
left=np.where(YY<700,XX<gEdge-9,XX<gEdge-0.5)
# colour class inside the mark: blue (N) vs warm (G + swoosh); weak pixels take the nearest strong class
hue=b-r
cls=np.zeros((H,W),np.int8)   # 1 blue, 2 warm
cls[mark&(hue>25)]=1; cls[mark&(hue<-25)]=2
for _ in range(12):
    todo=mark&(cls==0)
    if not todo.any(): break
    for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
        n=shift(cls,dx,dy); upd=todo&(n>0)&(cls==0); cls[upd]=n[upd]
cls[mark&(cls==0)]=1
warm=mark&(cls==2); blu=mark&(cls==1)
lo=warm&left&(YY>640)
# pale highlight/edge pixels of the swoosh (low saturation) belong to it too, not to the N
sat=src.max(-1)-src.min(-1)
lo=lo|(dilate(lo,4)&mark&left&(YY>640)&(sat<90))
G=warm&~left
N=blu
def layer(mask,name, fillholes=None):
    A=al*mask
    C=col.copy()
    if fillholes is not None:
        # diffuse premultiplied colour+alpha of N into the holes the swoosh leaves
        known=mask.astype(np.float32); hole=fillholes
        P=np.concatenate([C*A[...,None],A[...,None]],-1)*known[...,None]
        K=known.copy()
        for it in range(400):
            s=sum(shift(P,dx,dy) for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)))
            k=sum(shift(K,dx,dy) for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)))
            upd=hole&(k>0)
            P[upd]=s[upd]/k[upd][:,None]
            K=np.where(upd,1.0,K)
        # background next to the hole counts as known alpha 0, so the shape edge is preserved
        a_h=np.clip((P[...,3]-0.5)*3+0.5,0,1)
        C=np.where(hole[...,None],P[...,:3]/np.maximum(P[...,3:4],1e-3),C)
        A=np.where(hole,a_h,A)
        mask=mask|hole
    ys,xs=np.nonzero(A>0.004)
    x0,y0,x1,y1=xs.min(),ys.min(),xs.max()+1,ys.max()+1
    rgba=np.dstack([np.clip(C,0,255),np.clip(A*255,0,255)])[y0:y1,x0:x1].astype(np.uint8)
    im=Image.fromarray(rgba,'RGBA')
    sc=0.8
    im=im.resize((max(1,round((x1-x0)*sc)),max(1,round((y1-y0)*sc))),Image.LANCZOS)
    im.save(f'{OUT}/{name}.png',optimize=True)
    return (name,int(x0),int(y0),int(x1-x0),int(y1-y0))

# Hole region for N: swoosh pixels plus background pixels enclosed with them (the known set
# includes transparent background so diffusion keeps N's notches open).
holeN=((lo|(dilate(lo,6)&N&((b-r)<90)))&(al>0.02))|(dilate(lo,3)&(al<0.9)&left&(YY>640)&(YY<905))
rects=[]
# Treat transparent background around N as known alpha=0 by giving N's mask the full non-hole area
Nmask_known=(N|(~present))&~holeN&(XX<gEdge+40)&(YY>380)&(YY<905)
def layerN():
    hole=holeN.copy()
    Aknown=np.where(N,al,0.0).astype(np.float32)
    A=Aknown.copy()
    # fill each vertical run of the hole by interpolating the coverage just above and below it
    # (the swoosh crosses N at a shallow angle). Pixels hidden by the G count as unknown.
    A=Aknown.copy()
    Gm=G
    for x in range(W):
        colh=hole[:,x]
        if not colh.any(): continue
        ys=np.nonzero(colh)[0]
        runs=np.split(ys,np.where(np.diff(ys)>1)[0]+1)
        for rr in runs:
            y0,y1=rr[0]-1,rr[-1]+1
            top=None if (y0<0 or Gm[y0,x]) else float(Aknown[y0,x])
            bot=None if (y1>=H or Gm[y1,x]) else float(Aknown[y1,x])
            if top is None and bot is None: top=bot=0.0
            if top is None: top=bot
            if bot is None: bot=top
            t=(rr-y0)/(y1-y0)
            A[rr,x]=top*(1-t)+bot*t
    for it in range(6):   # light horizontal smoothing to hide column steps
        A=np.where(hole,(shift(A,1,0)+shift(A,-1,0)+2*A)/4,A)
    A=np.where(hole,np.clip((A-0.4)/0.2,0,1),Aknown)
    # colour: per column, blend the solid N colour a few pixels beyond each side of the hole
    C=col.copy()
    solidN=N&(al>0.95)
    K=10
    for x in range(W):
        colh=hole[:,x]
        if not colh.any(): continue
        ys=np.nonzero(dilate(hole,3)[:,x])[0] if False else np.nonzero(colh)[0]
        runs=np.split(ys,np.where(np.diff(ys)>1)[0]+1)
        for rr in runs:
            y0=rr[0]-K; y1=rr[-1]+K
            ext=np.arange(rr[0]-3,rr[-1]+4)
            def pick(yy,step):
                for _ in range(12):
                    if 0<=yy<H and solidN[yy,x]: return col[yy,x]
                    yy+=step
                return None
            top=pick(y0,-1); bot=pick(y1,1)
            if top is None and bot is None: continue
            if top is None: top=bot
            if bot is None: bot=top
            t=((ext-(rr[0]-3))/max(1,len(ext)-1))[:,None]
            fill=top*(1-t)+bot*t
            keep=(ext>=0)&(ext<H)
            ext=ext[keep]; fill=fill[keep]
            m=hole[ext,x]|N[ext,x]
            C[ext[m],x]=fill[m]
    for it in range(4):   # soften column steps
        sm=(shift(C,1,0)+shift(C,-1,0)+2*C)/4
        C=np.where(dilate(hole,3)[...,None],sm,C)
    # knock out bright specks left along the seam (swoosh highlight bleeding into N)
    Rg=dilate(hole,8)
    lum=C.mean(-1)
    dark=(lum<170).astype(np.float32)
    Pd=C*dark[...,None]; Kd=dark.copy()
    for it in range(10):
        Pd=(shift(Pd,1,0)+shift(Pd,-1,0)+shift(Pd,0,1)+shift(Pd,0,-1)+Pd)/5
        Kd=(shift(Kd,1,0)+shift(Kd,-1,0)+shift(Kd,0,1)+shift(Kd,0,-1)+Kd)/5
    blur=Pd/np.maximum(Kd,1e-3)[...,None]
    bright=Rg&(lum>blur.mean(-1)+35)&(Kd>0.05)
    C=np.where(bright[...,None],blur,C)
    return A,C
A,C=layerN()
al_save=al.copy(); col_save=col.copy()
al=np.where(N|holeN,A,0); col=C
rects.append(layer(al>0,'ngi_n')); al=al_save; col=col_save
for m,n in [(G,'ngi_g'),(lo,'ngi_swoosh_lo'),(upper,'ngi_swoosh_hi'),(starB,'ngi_star_b'),(starY,'ngi_star_y'),
            (letters,'ngi_studio'),(lineL,'ngi_line_l'),(lineR,'ngi_line_r')]:
    rects.append(layer(m,n))
for t in rects: print(t)
# letter x ranges
cols=np.nonzero(letters.any(0))[0]
runs=np.split(cols,np.where(np.diff(cols)>3)[0]+1)
print('letters',[(int(r_[0]),int(r_[-1]+1)) for r_ in runs])
# orbit path: swoosh pixels by angle around a centre
cx,cy=640,600
sw=(lo|upper)&(al>0.5)
ang=np.degrees(np.arctan2(YY-cy,XX-cx))
pts=[]
for a0 in range(-112,176,4):
    m=sw&(ang>=a0)&(ang<a0+4)
    if m.sum()>20: pts.append((a0+2,round(float(XX[m].mean()),1),round(float(YY[m].mean()),1)))
    else: pts.append((a0+2,None,None))
print('orbit',pts)
ys,xs=np.nonzero(present); print('all bbox',xs.min(),ys.min(),xs.max(),ys.max())
