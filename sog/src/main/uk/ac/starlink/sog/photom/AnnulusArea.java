/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     04-NOV-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import java.awt.geom.Area;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;

/**
 * Implements a Shape that consists of three ellipses that are
 * supposed to represent an aperture plus an associated annular region.
 * <p>
 * Limitations, just circular, could do with elliptical with
 * rotation angle. May want to remove the aperture if dealing with
 * non-annular sky regions.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AnnulusArea
    implements Shape
{
    // The three basic ellipse shapes.
    private Ellipse2D.Double aperture = new Ellipse2D.Double();
    private Ellipse2D.Double inner = new Ellipse2D.Double();
    private Ellipse2D.Double outer = new Ellipse2D.Double();

    // The Area object used to encapsulate the three ellipses.
    private Area mainArea = new Area();

    // The scales of the radii of the annulus ellipses.
    private double innerscale = 2.0;
    private double outerscale = 3.0;

    // Whether the anulus is drawn in an inverted sense.
    private boolean invert = false;

    /**
     * Create an instance with the given radius.
     */
    public AnnulusArea( double radius )
    {
        super();
        setRadius( radius );
    }

    /**
     * Create an annulus with no default values.
     */
    public AnnulusArea( double radius, double xcentre, double ycentre,
                        double innerscale, double outerscale )
    {
        super();
        setValues( radius, xcentre, ycentre, innerscale, outerscale );
    }

    /**
     * Update the shape to match a new radius etc. in the various
     * components.
     */
    protected void updateShapes( double radius, double xcentre,
                                 double ycentre )
    {
        centreEllipse( aperture, radius, xcentre, ycentre );
        centreEllipse( inner, radius * innerscale, xcentre, ycentre );
        centreEllipse( outer, radius * outerscale, xcentre, ycentre );

        // Clear out the area and re-add. Only way to see the changes
        // propagated (tried just updating ellipses but those changes
        // were not seem in the Area).
        mainArea.reset();

        if ( invert ) {
            //  Annulus area is the region between annulus and
            //  aperture (i.e. the part not used).
            mainArea.add( new Area( outer ) );

            // Decrease outer and subtract from self to leave an
            // outline.
            centreEllipse( outer, radius * outerscale * 0.999,
                           xcentre, ycentre ); 
            mainArea.subtract( new Area( outer ) );
            mainArea.add( new Area( inner ) );
            mainArea.subtract( new Area( aperture ) );
        }
        else {
            //  Annulus effective area is annulus plus the aperture,
            //  (i.e. the parts used.)
            mainArea.add( new Area( outer ) );
            mainArea.subtract( new Area( inner ) );
            mainArea.add( new Area( aperture ) );
        }
    }
    protected void updateShapes()
    {
        updateShapes( getRadius(), aperture.getCenterX(),
                      aperture.getCenterY() );
    }
    protected void updateShapes( double radius )
    {
        updateShapes( radius, aperture.getCenterX(),
                      aperture.getCenterY() );
    }
    protected void updateShapes( double xcentre, double ycentre )
    {
        updateShapes( getRadius(), xcentre, ycentre );
    }

    /**
     * Centre an ellipse to the current position. Use the given radius
     * which is different for the various ellipses.
     */
    protected void centreEllipse( Ellipse2D.Double ell, double radius,
                                  double xcentre, double ycentre )
    {
        double xo = xcentre - radius;
        double yo = ycentre - radius;
        double dr = radius * 2.0;
        ell.setFrame( xo, yo, dr, dr );
    }

    /**
     * Set all values at same time.
     */
    public void setValues( double radius, double xcentre,
                           double ycentre, double innerscale,
                           double outerscale )
    {
        this.innerscale = innerscale;
        this.outerscale = outerscale;
        updateShapes( radius, xcentre, ycentre );
    }

    /**
     * Set the centre of the shape.
     */
    public void setPosition( Point2D.Double pt )
    {
        updateShapes( pt.x, pt.y );
    }

    /**
     * Set the centre of the shape.
     */
    public void setPosition( double xcentre, double ycentre )
    {
        updateShapes( xcentre, ycentre );
    }

    /**
     * Get the centre of the shape.
     */
    public Point2D.Double getPosition()
    {
        return new Point2D.Double( aperture.getCenterX(),
                                   aperture.getCenterY() );
    }

    /**
     * Get the radius
     */
    public double getRadius()
    {
        return 0.5 * aperture.width;
    }

    /**
     * Set the radius.
     */
    public void setRadius( double radius )
    {
        updateShapes( radius );
    }

    /**
     * Get the innerscale
     */
    public double getInnerscale()
    {
        return innerscale;
    }

    /**
     * Set the innerscale
     */
    public void setInnerscale( double innerscale )
    {
        this.innerscale = innerscale;
        updateShapes();
    }

    /**
     * Get the outerscale
     */
    public double getOuterscale()
    {
        return outerscale;
    }

    /**
     * Set the outerscale
     */
    public void setOuterscale( double outerscale )
    {
        this.outerscale = outerscale;
        updateShapes();
    }

    /**
     * Set whether an "inverted" annulus is drawn.
     */
    public void setInverted( boolean invert )
    {
        this.invert = invert;
    }

    /**
     * Get whether an "inverted" annulus is drawn.
     */
    public boolean getInverted()
    {
        return invert;
    }

    private double[] srcPts = new double[4];
    private double[] dstPts = new double[4];
    /**
     * Apply a generalized transformation to the annulus. Used to
     * track changes made to a view of the object.
     */
    public void transform( AffineTransform at )
    {
        // Don't just transform the Shapes as these can loose the
        // elliptical context (a Shape becomes a PathIterator, which
        // doesn't seem to keep any genuine Shape context). Use a
        // representative line shape instead. 
        srcPts[0] = aperture.getCenterX();
        srcPts[1] = aperture.getCenterY();
        double radius = getRadius();
        srcPts[2] = srcPts[0] + radius;
        srcPts[3] = srcPts[1] + radius;
        at.transform( srcPts, 0, dstPts, 0, 2 );
        updateShapes( dstPts[2] - dstPts[0], dstPts[0], dstPts[1] );
    }

    /**
     * Apply a translation to the centre of the shape (see {@link transform}).
     */
    public void translate( double x, double y )
    {
        updateShapes( aperture.getCenterX() + x, aperture.getCenterY() + y );
    }

//
// Implement the Shape interface. These just proxy to the
// underlying Area. XXX May want access to the components...
//
    public Rectangle getBounds()
    {
        return mainArea.getBounds();
    }

    public Rectangle2D getBounds2D()
    {
        return mainArea.getBounds2D();
    }

    public boolean contains( double x, double y )
    {
        return mainArea.contains( x, y );
    }

    public boolean contains( Point2D p )
    {
        return mainArea.contains( p );
    }

    public boolean intersects( double x, double y, double w, double h )
    {
        return mainArea.intersects( x, y, w, h );
    }

    public boolean intersects( Rectangle2D r )
    {
        return mainArea.intersects( r );
    }

    public boolean contains( double x, double y, double w, double h )
    {
        return mainArea.contains( x, y, w, h );
    }

    public boolean contains( Rectangle2D r )
    {
        return mainArea.contains( r );
    }

    public PathIterator getPathIterator( AffineTransform at )
    {
        return mainArea.getPathIterator( at );
    }

    public PathIterator getPathIterator( AffineTransform at, double flatness )
    {
        return mainArea.getPathIterator( at, flatness );
    }
}
