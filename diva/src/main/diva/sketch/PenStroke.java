/*
 * $Id: PenStroke.java,v 1.4 2001/07/22 22:01:41 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;

import diva.util.java2d.Polyline2D;
import diva.util.java2d.Polygon2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.BasicStroke;
import java.awt.Shape;


/**
 * A first cut at making pen-sketched drawings look
 * like they were actually sketched by a pen.  This
 * class special-cases Polyline2D objects and hands
 * everything else off to its superclass.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class PenStroke extends BasicStroke implements Stroke {
    /**
     * Constructs a solid <code>BasicStroke</code> with the specified 
     * line width and with default values for the cap and join 
     * styles.
     * @param width the width of the <code>BasicStroke</code>
     * @throws IllegalArgumentException if <code>width</code> is negative
     */
    public PenStroke(float width) {
        super(width);
    }

    /**
     * Returns a <code>Shape</code> whose interior defines the 
     * stroked outline of a specified <code>Shape</code>.
     * @param s the <code>Shape</code> boundary be stroked
     * @return the <code>Shape</code> of the stroked outline.
     */
    public final Shape createStrokedShape(Shape s) {
        if(s instanceof Polyline2D) {
            Polyline2D poly = (Polyline2D)s;
            float lenSq = 0;
            final int vertexCount = poly.getVertexCount();
            float x1 = (float)poly.getX(0);
            float y1 = (float)poly.getY(0);
            float x2, y2, dx, dy;
            for(int i = 1; i < vertexCount; i++) {
                x2 = (float)poly.getX(i);
                y2 = (float)poly.getY(i);
                dx = x2-x1;
                dy = y2-y1;
                lenSq += (dx*dx + dy*dy);
                x1 = x2;
                y1 = y2;
            }
            x1 = (float)poly.getX(0);
            y1 = (float)poly.getY(0);
            final float[] widths = new float[vertexCount];
            final float maxWidth = getLineWidth();
            final float minWidth = 0.5f;
            final float deltaWidth = maxWidth-minWidth;
            float width;
            float len;
            float prevWidth = maxWidth;
            widths[0] = maxWidth;
            for(int i = 1; i < vertexCount; i++) {
                x2 = (float)poly.getX(i);
                y2 = (float)poly.getY(i);
                dx = x2-x1;
                dy = y2-y1;
                float ratio = (float)Math.pow((dx*dx + dy*dy)/lenSq, .1);
                width = maxWidth -
                    (ratio*deltaWidth);
                widths[i] = (width+prevWidth)/2;
                System.out.println("Width: " + widths[i]);
                prevWidth = width;
                x1 = x2;
                y1 = y2;
            }

            final int polyCount = 2*vertexCount;
            final float[] out = new float[2*polyCount];
            x1 = (float)poly.getX(0);
            y1 = (float)poly.getY(0);
            x2 = (float)poly.getX(1);
            y2 = (float)poly.getY(1);
            dx = x2-x1;
            dy = y2-y1;
            len = dx*dx+dy*dy;
            setX(out, 0, x1+(maxWidth*dy/len));
            setY(out, 0, y1-(maxWidth*dx/len));
            setX(out, polyCount-1, x1-(maxWidth*dy/len));
            setY(out, polyCount-1, y1+(maxWidth*dx/len));
            x1 = x2;
            y1 = y2;
            for(int i = 2; i < vertexCount; i++) {
                x2 = (float)poly.getX(i);
                y2 = (float)poly.getY(i);
                dx = x2-x1;
                dy = y2-y1;
                width = widths[i-1];
                len = dx*dx+dy*dy;
                setX(out, i-1, x1+(width*dy/len));
                setY(out, i-1, y1-(width*dx/len));
                //                System.out.println("i-1: " + (i-1));
                //                System.out.println("Setting: " + 2*(polyCount-i));
                setX(out, polyCount-i, x1-(width*dy/len));
                setY(out, polyCount-i, y1+(width*dx/len));
                if(i == vertexCount-1) {
                    width = widths[i];
                    len = dx*dx+dy*dy;
                    setX(out, i, x2+(width*dy/len));
                    setY(out, i, y2-(width*dx/len));
                    setX(out, polyCount-i-1, x2-(width*dy/len));
                    setY(out, polyCount-i-1, y2+(width*dx/len));
                }
                x1 = x2;
                y1 = y2;
            }
            for(int i = 3; i < vertexCount; i++) {
                if(Line2D.linesIntersect(getX(out, i-3), getY(out, i-3),
                        getX(out, i-2), getY(out, i-2),
                        getX(out, i-1), getY(out, i-1),
                        getX(out, i), getY(out, i))) {
                    float tmp;
                    tmp = getX(out, i-2);
                    setX(out,i-2,getX(out,i-1));
                    setX(out,i-1,tmp);
                    tmp = getY(out, i-2);
                    setY(out,i-2,getY(out,i-1));
                    setY(out,i-1,tmp);
                }
            }
            Polygon2D outPoly = new Polygon2D.Float(out);
            outPoly.closePath();
            return outPoly;
        }
        else {
            return super.createStrokedShape(s);
        }
    }

    private static final void setX(float[] a, int i, float x) {
        a[2*i] = x;
    }
    private static final void setY(float[] a, int i, float y) {
        a[2*i+1] = y;
    }
    private static final float getX(float[] a, int i) {
        return a[2*i];
    }
    private static final float getY(float[] a, int i) {
        return a[2*i+1];
    }
}

