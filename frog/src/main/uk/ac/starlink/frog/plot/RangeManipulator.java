package uk.ac.starlink.frog.plot;

import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.Site;
import diva.canvas.connector.CenterSite;
import diva.canvas.interactor.BoundsGeometry;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.GrabHandleFactory;
import diva.canvas.interactor.MoveHandle;

import java.util.Iterator;

import javax.swing.SwingConstants;

/**
 * A manipulator which attaches grab handles to the vertical or
 * horizontal bounds of a figure. The natural figure movements
 * supported are vertical or horizontal movement and resizing.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class RangeManipulator extends BoundsManipulator
{
    /**
     * Construct a new manipulator that uses rectangular grab-handles.
     */
    public RangeManipulator()
    {
        super();
    }

    /**
     * Construct a new manipulator using the given grab-handle factory.
     */
    public RangeManipulator( GrabHandleFactory f )
    {
        super( f );
    }

    /** Create a new instance of this manipulator. The new
     * instance will have the same grab handle, and interaction role
     * for grab-handles, as this one.
     */
    public FigureDecorator newInstance (Figure f) {
        RangeManipulator m = new RangeManipulator();
        m.setGrabHandleFactory(this.getGrabHandleFactory());
        m.setHandleInteractor(this.getHandleInteractor());
        //m.setDragInteractor(_dragInteractor);
        return m;
    }

  
}
