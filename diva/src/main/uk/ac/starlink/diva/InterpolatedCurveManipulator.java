/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import diva.canvas.interactor.DragInteractor;
import diva.canvas.Figure;
import diva.canvas.interactor.GrabHandle;
import diva.canvas.interactor.GrabHandleFactory;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.BasicGrabHandleFactory;
import diva.canvas.FigureDecorator;
import diva.canvas.AbstractSite;
import diva.canvas.event.LayerEvent;

import java.awt.geom.Point2D;
import java.awt.Graphics2D;

import uk.ac.starlink.diva.geom.InterpolatedCurve2D;
import uk.ac.starlink.diva.interp.Interpolator;;

/**
 * A manipulator for InterpolatedCurveFigure figures. This attaches
 * handles to the vertices of the curve and allows them to be moved,
 * the corresponding InterpolatedCurveFigure should then re-draw
 * itself with a new interpolation.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class InterpolatedCurveManipulator
    extends Manipulator
{
    /**
     * The InterpolatedCurveFigure.
     */
    protected InterpolatedCurveFigure curve = null;

    /**
     * The InterpolatedCurve2D.
     */
    protected InterpolatedCurve2D curve2d = null;

    /**
     * Set when a refresh is required.
     */
    private boolean revalidate = true;

    /**
     * The Vertex instances.
     */
    private Vertex[] vertices = null;

    /**
     * Construct a new manipulator that uses rectangular grab-handles.
     */
    public InterpolatedCurveManipulator()
    {
        this( new BasicGrabHandleFactory() );
    }

    /**
     * Construct a new manipulator using the given grab-handle factory.
     */
    public InterpolatedCurveManipulator( GrabHandleFactory f )
    {
        setGrabHandleFactory( f );
	setHandleInteractor( new Reshaper() );
    }

    /**
     * Create a new instance of this manipulator. The new instance
     * will have the same grab handle, and interactor for
     * grab-handles, as this one.
     */
    public FigureDecorator newInstance( Figure f )
    {
        InterpolatedCurveManipulator m = new InterpolatedCurveManipulator();
        m.setGrabHandleFactory( getGrabHandleFactory() );
        m.setHandleInteractor( getHandleInteractor() );
        return m;
    }

    /**
     * Refresh the underlying InterpolatedCurveFigure.
     */
    public void refresh()
    {
        if ( curve != null && revalidate ) {
            revalidate = false;
            repaint();
        }
    }

    public void paint( Graphics2D g2d )
    {
        // Force an update of the interpolated positions and draw them.
        curve2d.setInterpolator( curve2d.getInterpolator() );
        super.paint( g2d );
    }

    /**
     * Set the child figure. If we have any grab-handles, lose them
     * and create new ones.
     */
    public void setChild( Figure f )
    {
        super.setChild( f );
        clearGrabHandles();
        curve = null;

        // Process new child
        Figure child = getChild();
        if ( child != null ) {
            // Check it's an InterpolatedCurveFigure.
            if ( ! ( child instanceof InterpolatedCurveFigure ) ) {
                throw new IllegalArgumentException
                    ( "InterpolatedCurveManipulator can only decorate " +
                      "an InterpolatedCurveFigure");
            }

            //  Get the underlying InterpolatedCurveFigure.
            curve = (InterpolatedCurveFigure) child;
            curve2d = (InterpolatedCurve2D) curve.getShape();

            //  Create the handles for each vertex.
            int count = curve2d.getVertexCount();
            vertices = new Vertex[count];
            if ( count > 0 ) {
                GrabHandle g = null;
                for ( int i = 0; i < count; i++ ) {
                    vertices[i] = new Vertex( i );
                    g = getGrabHandleFactory().createGrabHandle( vertices[i] );
                    g.setParent( this );
                    g.setInteractor( getHandleInteractor() );
                    addGrabHandle( g );
                }
            }
        }
    }

    /**
     * Make the X vertex coordinates monotonic and re-order the
     * Vertex instances as appropriate.
     */
    protected synchronized void orderVertices()
    {
        // Re-order vertices so that X coordinates are monotonic.
        int[] remap = curve2d.orderVertices();

        int size = remap.length;
        Vertex[] newVertices = new Vertex[vertices.length];

        //  Re-order vertices so that they follow the monotonic order.
        //  They are the same grab handles/sites, but now refer to a
        //  different vertex.
        if ( curve2d.isIncreasing() ) {
            for ( int i = 0; i< size; i++ ) {
                newVertices[i] = vertices[ remap[i] ];
                newVertices[i].setIndex( remap[i] );
            }
        }
        else {
            int j = size - 1;
            for ( int i = 0; i < size; i++, j-- ) {
                newVertices[i] = vertices[ remap[j] ];
                newVertices[i].setIndex( remap[j] );
            }
        }
        vertices = newVertices;
    }

    //
    // Reshaper
    //

    /**
     * An interactor class that changes a vertex of the child figure
     *  and triggers a repaint.
     */
    private class Reshaper
        extends DragInteractor
    {
        /**
         * Translate the grab-handle
         */
        public void translate( LayerEvent e, double x, double y )
        {
            // Translate the grab-handle.
            GrabHandle g = (GrabHandle) e.getFigureSource();
            g.translate( x, y );
        }
    }

    //
    // Vertex
    //

    /**
     * Vertex is the site that represents a single vertex of the
     * interpolated curve.
     */
    private class Vertex
        extends AbstractSite
    {
        private double x = 0.0;
        private double y = 0.0;
        private int index = 0;
        private int id = 0;

        /**
         * Create a new site using a given vertex.
         */
        Vertex( int index )
        {
            this.index = index;
            id = index;
        }

        /**
         * Return the grab handle ID.
         */
        public int getID()
        {
            return id;
        }

        /**
         * Set the grab handle ID.
         */
        public void setID( int id )
        {
            this.id = id;
        }

        /**
         * Set the index.
         */
        public void setIndex( int index )
        {
            this.index = index;
        }

        /**
         * Get the index.
         */
        public int getIndex()
        {
            return index;
        }

        /**
         * Get the figure to which this site is attached.
         */
        public Figure getFigure()
        {
            return curve;
        }

        /**
         * Get the x-coordinate of the site.
         */
        public double getX()
        {
            return curve2d.getXVertex( index );
	}

        /**
         * Get the y-coordinate of the site.
         */
        public double getY()
        {
            return curve2d.getYVertex( index );
        }

        /** Set the point location of the site
         */
        public void setPoint( Point2D point )
        {
            translate( point.getX() - getX(), point.getY() - getY() );
        }

        /**
         * Translate the site by the indicated distance.
         */
        public void translate( double dx, double dy )
        {
            // Just move the point, do not re-calculate the full
            // interpolation, let the figure defer that until re-painting.

            // Make any changes to the ordering before applying the
            // shift (makes sure that the shift doesn't appear on
            // another handle). XXX yes we do need this mess,
            // otherwise handles receive translation meant for other
            // handles, when moving part each other.
            orderVertices();
            curve2d.translateVertex( index, dx, dy, false );

            //  Apply the shifts to the handles.
            revalidate = true;
            refresh();

            //  And now reorder to make the shift apparent in the
            //  vertices for next time.
            orderVertices();
	}
    }
}
