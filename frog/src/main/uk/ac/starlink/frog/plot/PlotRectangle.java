package uk.ac.starlink.frog.plot;
 
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/** 
 * A figure that displays as a rectangle. This is a convenience class
 * for creating rectangles. It inherits from BasicFigure, and so contains
 * a single Rectangle2D as its shape. It provides a useful set of
 * constructors.
 *
 * Changed by Peter W. Draper to inherit from BasicPlotFigure so we
 * can use it on Plots.
 *
 * @version	$Revision$
 * @author 	John Reekie
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file DIVA COPYRIGHT for details.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see Plot, PlotConfigurator
 */
public class PlotRectangle extends BasicPlotFigure 
{
    /** Create a new rectangle with the given rectangle shape, a
     * unit-width continuous stroke and no paint pattern.
     */
    public PlotRectangle (Rectangle2D rect) 
    {
        super(rect);
    }
    
    /** Create a new rectangle with the given origin and size, a
     * unit-width continuous stroke and no paint pattern.
     */
    public PlotRectangle (double x, double y, double width, 
                          double height) 
    {
        super(new Rectangle2D.Double(x,y,width,height));
    }
    
    /** Create a new rectangle with the given origin, size, and
     * fill paint. It has no outline.
     */
    public PlotRectangle ( double x, double y, double width, double height,
                           Paint fill) 
    {
        super(new Rectangle2D.Double(x,y,width,height), fill);
    }
    
    /** Create a new rectangle with the given origin, size, and
     * outline width. It has no fill.
     */
    public PlotRectangle ( double x, double y, double width, double height,
                            float lineWidth) 
    {
        super(new Rectangle2D.Double(x,y,width,height), lineWidth);
    }
    
    /** Create a new rectangle with the given origin, size, fill, and
     * outline width.
     */
    public PlotRectangle ( double x, double y, double width, double height,
                            Paint fill, float lineWidth) 
    {
        super(new Rectangle2D.Double(x,y,width,height), fill, lineWidth);
    }
    
    /** 
     * Translate the rectangle the given distance
     */
    public void translate (double x, double y) 
    {
        Shape s = getShape();
        if (s instanceof Rectangle2D) {
            Rectangle2D r = (Rectangle2D)s;
            repaint();
            r.setFrame(r.getX()+x, r.getY()+y, r.getWidth(), r.getHeight());
            repaint();
            fireChanged();
        } else {
            super.translate(x,y);
        }
    }
}
