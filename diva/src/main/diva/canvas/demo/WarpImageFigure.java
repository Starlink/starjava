/* 
 * $Id: WarpImageFigure.java,v 1.7 2001/07/22 22:00:34 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.canvas.demo;

import diva.canvas.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;
import javax.swing.Timer;

/**
 * Warps a image on a CubicCurve2D flattened path.
 *
 * @version $Revision: 1.7 $
 * @author John Reekie
 */
public class WarpImageFigure extends AbstractFigure {
    private static int iw, ih, iw2, ih2;
    private static Image img;
    private static final int FORWARD = 0;
    private static final int BACK = 1;
    private Point2D pts[];
    private int direction = FORWARD;
    private int pNum = 0;
    private int x, y;
    private int _x = 0;
    private int _y = 0;


    public WarpImageFigure(Image img) {
        this.img = img;
        iw = img.getWidth(null);
        ih = img.getHeight(null);
        iw2 = iw/2;
        ih2 = ih/2;
        reset(iw, ih);

        (new Timer(20, new LocalActionListener())).start();
    }

    private class LocalActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            step(iw, ih);
            repaint();
        }
    }


    public void reset(int w, int h) {
        CubicCurve2D cc = new CubicCurve2D.Float(
                        w*.2f, h*.5f, w*.4f,0, w*.6f,h,w*.8f,h*.5f);
        PathIterator pi = cc.getPathIterator(null, 0.1);
        Point2D tmp[] = new Point2D[200];
        int i = 0;
        while ( !pi.isDone() ) {
            float[] coords = new float[6];
            switch ( pi.currentSegment(coords) ) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                        tmp[i] = new Point2D.Float(coords[0], coords[1]);
            }
            i++;
            pi.next();
        }
        pts = new Point2D[i];
        System.arraycopy(tmp,0,pts,0,i);
    }


    public void step(int w, int h) {
        if (pts == null) {
            return;
        }
        x = (int) pts[pNum].getX();
        y = (int) pts[pNum].getY();
        if (direction == FORWARD)
            if (++pNum == pts.length)
                direction = BACK;
        if (direction == BACK)
            if (--pNum == 0)
                direction = FORWARD;
    }


    public void paint(Graphics2D g2) {
        int w = iw;
        int h = ih;
        g2.drawImage(img,
                        _x,              _y,              _x+x,              _y+y,
                        0,              0,              iw2,            ih2,
                        null);
        g2.drawImage(img,
                        _x+x,              _y,              _x+w,              _y+y,
                        iw2,            0,              iw,             ih2,
                        null);
        g2.drawImage(img,
                        _x,              _y+y,              _x+x,              _y+h,
                        0,              ih2,            iw2,            ih,
                        null);
        g2.drawImage(img,
                        _x+x,              _y+y,              _x+w,              _y+h,
                        iw2,            ih2,            iw,             ih,
                        null);
    }

    public Shape getShape() {
        return new Rectangle2D.Double(_x, _y, iw, ih);
    }

    public void transform(AffineTransform t) {
        throw new RuntimeException("operation not supported!");
    }

    public void translate(double dx, double dy) {
        repaint();
        _x += (int)dx;
        _y += (int)dy;
        repaint();
    }
}


