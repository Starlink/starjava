package uk.ac.starlink.frog.plot;

import java.util.EventListener;

/**
 * FigureListener defines an interface used when listening for the
 * creation, removal and changes of Figures on a Plot.
 *
 * @since $Date$
 * @since 08-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public interface FigureListener extends EventListener 
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
     *  Send when a figure is changed (i.e. moved or transformed).
     */
    public void figureChanged( FigureChangedEvent e );
}
