/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     15-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.canvas.JCanvas;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * {@link DrawActions} interface for a Diva canvas.  Typically this
 * will be part of component that is extending a {@link JCanvas}.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see JCanvas
 * @see DrawActions
 */      
public interface Draw
{
    /** 
     * Return the instance of DrawGraphicsPane that is being used by
     * this Canvas (a type of GraphicsPane) 
     */
    public DrawGraphicsPane getGraphicsPane();

    /** 
     * Adds the specified mouse listener to receive mouse events from
     * this component. 
     */
    public void addMouseListener( MouseListener l );
    
    /** 
     * Adds the specified mouse motion listener to receive mouse
     * motion events from this component. 
     */
    public void addMouseMotionListener( MouseMotionListener l );

    /** 
     * Return a reference to the component that implements this
     * interface, usually "this". Only used for dialog parents, so
     * may be return null. 
     */
    public Component getComponent();
}

