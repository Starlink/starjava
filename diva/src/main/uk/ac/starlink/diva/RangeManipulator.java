/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

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
 * @author Peter W. Draper
 * @version $Id$
 */
public class RangeManipulator 
   extends BoundsManipulator
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
    public FigureDecorator newInstance( Figure f ) 
    {
        RangeManipulator m = new RangeManipulator();
        m.setGrabHandleFactory(this.getGrabHandleFactory());
        m.setHandleInteractor(this.getHandleInteractor());
        m.setDragInteractor(_dragInteractor);
        return m;
    }

    /**
     * Set the child figure. If we have any grab-handles, lose them.
     * Then get a rectangle geometry object and create grab-handles on
     * its sites. Override to modify Geometry object, so we can match
     * "range" rather than "bounds" handles.
     */
    public void setChild( Figure child )
    {
        super.setChild( child );
        clearGrabHandles();

        // Process new child
        if ( child != null ) {

            // Create the geometry defining the sites
            _geometry = new BoundsGeometry( this, getChild().getBounds() );
            Iterator i = _geometry.sites();
            GrabHandle g = null;
            while ( i.hasNext() ) {
                // Create a grab handle and set up the interaction role
                Site site = (Site) i.next();

                // Skip the sites we're not interesed in.
                if ( site.getID() != SwingConstants.NORTH && 
                     site.getID() != SwingConstants.SOUTH ) {
                    g = getGrabHandleFactory().createGrabHandle( site );
                    g.setParent( this );
                    g.setInteractor( getHandleInteractor() );
                    addGrabHandle( g );
                }
            }

            // Add a center handle for dragging
            if ( _dragInteractor != null ) {
                CenterSite center = new CenterSite( getChild() );
                GrabHandle mover = new MoveHandle( center );
                mover.setParent( this );
                mover.setInteractor( _dragInteractor );
                addGrabHandle( mover );
            }

            // Move them where they should be - ?
            relocateGrabHandles();
        }
    }
}
