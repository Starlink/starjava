/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import java.util.EventListener;

/**
 * FigureListener defines an interface used when listening for the
 * creation, removal and changes of Figures on a {@link Draw} instance.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface FigureListener 
    extends EventListener 
{
    /**
     *  Sent when a figure is created.
     */
    public void figureCreated( FigureChangedEvent e );

    /**
     *  Send when a figure is removed.
     */
    public void figureRemoved( FigureChangedEvent e );

    /**
     *  Send when a figure is changed (i.e.&nbsp;moved or transformed).
     */
    public void figureChanged( FigureChangedEvent e );
}
