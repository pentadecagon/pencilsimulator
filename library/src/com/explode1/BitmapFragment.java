package com.explode1;

import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Region;
import android.os.SystemClock;

public class BitmapFragment{
    Bitmap bmp;
    int sourceX;
    int sourceY;
    int destX;
    int destY;
    int totalW;
    int totalH;
    Region.Op op;
    Path triangle;
    private static Random rnd;
    public static int SLICES_WIDTH=2;
    public static int SLICES_HEIGHT=3;

    public BitmapFragment(Bitmap bmp, int sourceX, int sourceY, int totalW, int totalH, Region.Op op, Path tri) {
        this.bmp = bmp;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.op=op;
        this.triangle=tri;
        this.totalH=totalH;
        this.totalW=totalW;
        if(rnd==null)rnd=new Random(SystemClock.uptimeMillis());
    }
    public void prepare(int xOrigin, int yOrigin){
        final int x=sourceX;
        final int y=sourceY;
        final int h=totalH;
        final int w=totalW;
        final int sliceW=bmp.getWidth();
        final int sliceH=bmp.getHeight();
        int distH = (x + (SLICES_WIDTH / 2)) - (xOrigin);
        int distV = (y + (SLICES_HEIGHT / 2))- (yOrigin);

        Path tri=new Path();
        tri.moveTo(0,0);
        for(float jj=0;jj<=1;jj+=.2){
            tri.lineTo(rnd.nextInt(sliceW), sliceH * jj);
        }
        tri.lineTo(0,sliceH);
        tri.close();

        //ADD 2 times each part for 2 pieces
        destX = distH>0?(x + (rnd.nextInt(w-xOrigin))):(x - (rnd.nextInt(xOrigin)));
        destY = distV>0?(y + (rnd.nextInt(h-yOrigin))):(y - (rnd.nextInt(yOrigin)));
    }
}