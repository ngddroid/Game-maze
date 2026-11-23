import javax.imageio.ImageIO; import java.awt.*; import java.awt.image.BufferedImage; import java.io.File; import java.util.*;
/** Removes a baked-in checkerboard "transparency" background: flood-fills the light-grey/white checker from the
 *  edges, also clears enclosed checker patches, softens the edge, crops and scales. */
// Usage: java CutLogo.java docs/store/ng_studio_logo.png app/src/main/assets/legal/ng_studio_logo.png 480
public class CutLogo {
  static int W,H; static int[] px;
  static boolean light(int c){int r=(c>>16)&255,g=(c>>8)&255,b=c&255;int mx=Math.max(r,Math.max(g,b)),mn=Math.min(r,Math.min(g,b));return mn>=214&&mx-mn<=14;}
  public static void main(String[] a) throws Exception {
    BufferedImage src=ImageIO.read(new File(a[0])); W=src.getWidth();H=src.getHeight(); px=src.getRGB(0,0,W,H,null,0,W);
    int[] comp=new int[W*H]; Arrays.fill(comp,-1); boolean[] bg=new boolean[W*H];
    int id=0; ArrayDeque<Integer> q=new ArrayDeque<>();
    for(int s=0;s<W*H;s++){ if(comp[s]!=-1||!light(px[s])) continue;
      java.util.List<Integer> cells=new ArrayList<>(); boolean edge=false; int grey=0,white=0;
      comp[s]=id; q.add(s);
      while(!q.isEmpty()){int p=q.poll(); cells.add(p); int x=p%W,y=p/W; if(x==0||y==0||x==W-1||y==H-1) edge=true;
        int v=px[p]&255; if(v<=238) grey++; else if(v>=248) white++;
        int[] nb={p-1,p+1,p-W,p+W}; for(int n:nb){ if(n<0||n>=W*H) continue; if((n%W==0&&p%W==W-1)||(p%W==0&&n%W==W-1)) continue; if(comp[n]==-1&&light(px[n])){comp[n]=id;q.add(n);} } }
      int sz=cells.size(); double fg=grey/(double)sz, fw=white/(double)sz;
      boolean isBg = edge || (sz>300 && fg>0.15 && fw>0.15);
      if(isBg) for(int p:cells) bg[p]=true;
      id++; }
    // alpha: 0 for background, soft fade for light pixels touching it
    int[] out=new int[W*H]; int minX=W,minY=H,maxX=0,maxY=0;
    for(int p=0;p<W*H;p++){ int c=px[p]; int al;
      if(bg[p]) al=0; else { al=255; int x=p%W,y=p/W; boolean near=false;
        for(int dy=-2;dy<=2&&!near;dy++)for(int dx=-2;dx<=2;dx++){int xx=x+dx,yy=y+dy; if(xx>=0&&yy>=0&&xx<W&&yy<H&&bg[yy*W+xx]){near=true;break;}}
        if(near){int r=(c>>16)&255,g=(c>>8)&255,b=c&255;int mn=Math.min(r,Math.min(g,b)),mx=Math.max(r,Math.max(g,b)); if(mx-mn<40&&mn>150) al=(int)Math.max(0,Math.min(255,(250-mn)*255.0/(250-150)));} }
      out[p]=(al<<24)|(c&0xFFFFFF);
      if(al>40){int x=p%W,y=p/W; minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);} }
    BufferedImage full=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB); full.setRGB(0,0,W,H,out,0,W);
    int m=12; minX=Math.max(0,minX-m);minY=Math.max(0,minY-m);maxX=Math.min(W-1,maxX+m);maxY=Math.min(H-1,maxY+m);
    BufferedImage crop=full.getSubimage(minX,minY,maxX-minX+1,maxY-minY+1);
    int tw=Integer.parseInt(a[2]); int th=(int)Math.round(crop.getHeight()*tw/(double)crop.getWidth());
    // progressive halving for a clean downscale
    BufferedImage cur=crop; int cw=cur.getWidth(), ch=cur.getHeight();
    while(cw/2>=tw){ cw/=2; ch/=2; cur=scale(cur,cw,ch);} cur=scale(cur,tw,th);
    ImageIO.write(cur,"png",new File(a[1]));
    // preview on a coloured background
    BufferedImage pv=new BufferedImage(tw*2+30,th+20,BufferedImage.TYPE_INT_RGB); Graphics2D g=pv.createGraphics();
    g.setColor(new Color(0x7A4A2A)); g.fillRect(0,0,pv.getWidth(),pv.getHeight()); g.setColor(new Color(0xFFFAF3)); g.fillRect(tw+20,0,tw+10,th+20);
    g.drawImage(cur,10,10,null); g.drawImage(cur,tw+25,10,null); g.dispose(); ImageIO.write(pv,"png",new File(a[1]+".preview.png"));
    System.out.println("components="+id+" crop="+crop.getWidth()+"x"+crop.getHeight()+" -> "+tw+"x"+th);
  }
  static BufferedImage scale(BufferedImage s,int w,int h){BufferedImage d=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);Graphics2D g=d.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    g.drawImage(s,0,0,w,h,null);g.dispose();return d;}
}
