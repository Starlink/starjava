// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    11-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.sog;

import java.awt.geom.Point2D;

import jsky.coords.WorldCoordinateConverter;

import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.AstException;

/**
 * Ast Implementation the WorldCoordinateConverter interface of
 * JSky. All world coordinate transformations are defined in this
 * class. <tt>Ast version of WCSTransform</tt>
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class AstTransform implements WorldCoordinateConverter 
{
    /**
     * The Ast FrameSet that defines the world coordinate
     * transformations.
     */
    private FrameSet frameSet = null;

    /**
     * The RA axis index.
     */
    protected int raIndex = 1;

    /**
     * The DEC axis index.
     */
    protected int decIndex = 2;
    
    /**
     * X image dimension.
     */
    protected int nxpix = 0;

    /**
     * Y image dimension.
     */
    protected int nypix = 0;

    /**
     * Image scale at center (degress/pixel).
     */
    protected Point2D.Double degPerPixel = null;

    /**
     * Conversion factor for radians to degrees.
     */
    public static final double R2D = 180.0 / Math.PI;

    /**
     * Conversion factor for degrees to radians.
     */
    public static final double D2R = Math.PI / 180.0;

    /**
     * Smallest value between 1.0 and next double precision value.
     */
    protected static final double EPSILON = 2.2204460492503131e-16;

    /**
     * Construct an instance using a given Ast FrameSet.
     *
     * @param frameSet the Ast FrameSet.
     * @param nxpix the X dimension of the image
     * @param nypix the Y dimension of the image
     */
    public AstTransform( FrameSet frameSet, int nxpix, int nypix )
    {
        setFrameSet( frameSet, nxpix, nypix );
    }

   /**
     * Constructs a new instance using a simple sky coordinate system.
     *
     * @param cra       Center right ascension in degrees 
     * @param cdec      Center declination in degrees 
     * @param xsecpix   Number of arcseconds per pixel along x-axis
     * @param ysecpix   Number of arcseconds per pixel along y-axis
     * @param xrpix     Reference pixel X coordinate 
     * @param yrpix     Reference pixel X coordinate 
     * @param nxpix     Number of pixels along x-axis 
     * @param nypix     Number of pixels along y-axis 
     * @param rotate    Rotation angle (clockwise positive) in degrees 
     * @param equinox   Equinox of coordinates, 1950 and 2000 supported 
     * @param epoch     Epoch of coordinates, used for FK4/FK5
     *                  conversion no effect if 0 
     * @param proj      Projection 
     */
    public AstTransform( double cra,
                         double cdec,   
                         double xsecpix,        
                         double ysecpix,        
                         double xrpix,  
                         double yrpix,  
                         int    nxpix,  
                         int    nypix,  
                         double rotate, 
                         int    equinox,
                         double epoch,  
                         String proj ) 
    {
        //  Create a FITS channel to which we will send our header cards.
        FitsChan fitsChan = new FitsChan();
        fitsChan.putFits( "NAXIS  = 2", false );
        fitsChan.putFits( "NAXIS1 = " + nxpix, false );
        fitsChan.putFits( "NAXIS2 = " + nypix, false );
        fitsChan.putFits( "EQUINOX= " + equinox, false );
        if ( epoch != 0 ) {
            fitsChan.putFits( "EPOCH  = " + epoch, false );
        }
        fitsChan.putFits( "CTYPE1 = RA---" + proj, false );
        fitsChan.putFits( "CTYPE2 = DEC--" + proj, false );
        fitsChan.putFits( "CRVAL1 = " + cra, false );
        fitsChan.putFits( "CRVAL2 = " + cdec, false );
        fitsChan.putFits( "CDELT1 = " + (-xsecpix / 3600.0), false );
        fitsChan.putFits( "CDELT2 = " + (-ysecpix / 3600.0), false );
        fitsChan.putFits( "CRPIX1 = " + xrpix, false );
        fitsChan.putFits( "CRPIX2 = " + yrpix, false );
        fitsChan.putFits( "CROTA1 = " + rotate, false );

        //  Now read the headers back as a suitable frameset.
        fitsChan.clear( "Card" );
        setFrameSet ( (FrameSet) fitsChan.read(), nxpix, nypix );
    }

    /**
     * Set the Ast FrameSet that is used to perform the actual
     * transformations. 
     *
     * @param frameSet the Ast FrameSet.
     * @param nxpix the X dimension of the image
     * @param nypix the Y dimension of the image
     */
    public void setFrameSet( FrameSet frameSet, int nxpix, int nypix )
    {
        this.frameSet = frameSet;

        //  Need to know which axis is time, if any.
        try {
            int astime2 = frameSet.getI( "astime(2)" );
            if ( astime2 == 1 ) {
                raIndex  = 2;
                decIndex = 1;
            } 
            else {
                raIndex  = 1;
                decIndex = 2;
            }
        }
        catch (Exception e) {
            // Not celestial coordinates. Maybe OK.
        }

        //  Record the nominal image dimensions.
        this.nxpix = nxpix;
        this.nypix = nypix;

        //  Need to determine the image scales.
        setSecPix();
    }

    /**
     * Get a reference to the Ast FrameSet that we are using.
     */
    public FrameSet getFrameSet()
    {
        return frameSet;
    }

    //
    // Implementation of the WorldCoordinateConverter interface
    //

    //  Get the image height.
    public double getHeight()
    {
        return (double) nypix;
    }

    //  Get the image width.
    public double getWidth()
    {
        return (double) nxpix;
    }

    //  Return if world coordinate conversion is available. Must be by
    //  definition?
    public boolean isWCS()
    {
        return (frameSet != null);
    }

    //  Return the equinox
    public double getEquinox()
    {
        try {
            return frameSet.getD( "Equinox" );
        }
        catch (AstException e) {
            // No equinox.
        }
        return -1.0;
    }


    // Convert the given image coordinates to world coordinates
    // degrees.
    public void imageToWorldCoords( Point2D.Double p, boolean isDistance )
    {
        if ( isDistance ) {
            p.x = Math.abs( p.x * degPerPixel.x );
            p.y = Math.abs( p.y * degPerPixel.y );
        }
        else {
            //  Transform the position.
            double[] oldx = new double[1];
            double[] oldy = new double[1];
            oldx[0] = p.x;
            oldy[0] = p.y;
            double[][] newp = frameSet.tran2( 1, oldx, oldy, true );
            
            //  Normalize the result into the correct range.
            double[] point = new double[2];
            point[0] = newp[0][0];
            point[1] = newp[1][0];
            frameSet.norm( point );
            
            if ( raIndex == 1 ) {
                p.x = point[0] * R2D; // Convert to degrees
                p.y = point[1] * R2D;
            } 
            else {
                p.x = point[1] * R2D;
                p.y = point[0] * R2D;
            }
        }
    }

    // Convert the given world coordinates.
    public void worldToImageCoords( Point2D.Double p, boolean isDistance )
    {
        if ( isDistance ) {
            p.x = Math.abs( p.x / degPerPixel.x );
            p.y = Math.abs( p.y / degPerPixel.y );
        }
        else {

            double[] oldx = new double[1];
            double[] oldy = new double[1];
            if ( raIndex == 1 ) {
                oldx[0] = p.x * D2R;  // Convert into radians.
                oldy[0] = p.y * D2R;
            } 
            else {
                oldy[0] = p.x * D2R;
                oldx[0] = p.y * D2R;
            }

            double[][] newp = frameSet.tran2( 1, oldx, oldy, false );
            p.x = newp[0][0];
            p.y = newp[1][0];
        }
    }

    // Return the center RA, Dec coordinates in degrees.
    public Point2D.Double getWCSCenter()
    {
        Point2D.Double point = new Point2D.Double( 0.5 * (double) nxpix,
                                                   0.5 * (double) nypix );
        imageToWorldCoords( point, false );
        return point;
    }

    // Return the width in degrees.
    public double getWidthInDeg() 
    {
        // Compute the image width as a distance 1.0 -> nxpix about
        // the centre of the image, so first set up image coordinates
        // describing this position.
        double[] xin = new double[2];
        xin[0] = 1.0;
        xin[1] = (double) nxpix;
        double[] yin = new double[2];
        yin[0] = yin[1] = 0.5 * (double) nypix;

        // Transform these image positions into sky coordinates.
        double[][] xyout = frameSet.tran2( 2, xin, yin, true );

        // And now get the distance between these positions in degrees.
        double[] point1 = new double[2];
        double[] point2 = new double[2];
        if ( raIndex == 1 ) {
            point1[0] = xyout[0][0];
            point1[1] = xyout[1][0];
            point2[0] = xyout[0][1];
            point2[1] = xyout[1][1];
        } 
        else {
            point1[1] = xyout[0][0];
            point1[0] = xyout[1][0];
            point2[1] = xyout[0][1];
            point2[0] = xyout[1][1];
        }
        double dist = frameSet.distance( point1, point2 );

        //  Check that distance isn't 0 or very small, this indicates
        //  that edge of image is same coordinate. If so use arcsec
        //  per pixel estimate.
        if ( dist == 0.0 || dist < EPSILON ) {
            dist = degPerPixel.x * nxpix;
        } 
        else {
            dist *= R2D;
        }
        return dist;
    }

    // Return the height in degrees.
    public double getHeightInDeg()
    {
        // Compute the image width as a distance 1.0 -> nypix about
        // the centre of the image, so first set up image coordinates
        // describing this position.
        double[] xin = new double[2];
        xin[0] = xin[1] = 0.5 * (double) nxpix;
        double[] yin = new double[2];
        yin[0] = 1.0;
        yin[1] = (double) nypix;

        // Transform these image positions into sky coordinates.
        double[][] xyout = frameSet.tran2( 2, xin, yin, true );

        // And now get the distance between these positions in degrees.
        double[] point1 = new double[2];
        double[] point2 = new double[2];
        if ( raIndex == 1 ) {
            point1[0] = xyout[0][0];
            point1[1] = xyout[1][0];
            point2[0] = xyout[0][1];
            point2[1] = xyout[1][1];
        } 
        else {
            point1[1] = xyout[0][0];
            point1[0] = xyout[1][0];
            point2[1] = xyout[0][1];
            point2[0] = xyout[1][1];
        }
        double dist = frameSet.distance( point1, point2 );

        //  Check that distance isn't 0 or very small, this indicates
        //  that edge of image is same coordinate. If so use arcsec
        //  per pixel estimate.
        if ( dist == 0.0 || dist < EPSILON ) {
            dist = degPerPixel.y * nypix;
        } 
        else {
            dist *= R2D;
        }
        return dist;
    }

    // Return the image center coordinates in pixels.
    public Point2D.Double getImageCenter()
    {
        return new Point2D.Double( 0.5 * nxpix, 0.5 * nypix );
    }

    // 
    // Utility methods.
    //

    /**
     * Find values for the number of degrees per pixel. These are
     * used to transform distances given by a single point (which is
     * taken to mean "from the centre").
     */
    protected void setSecPix()
    {
        double[] point1 = new double[2];
        double[] point2 = new double[2];
        double[] xin = new double[2];
        double[] yin = new double[2];

        //  Compute the scales the the sizes of a pixel near the
        //  centre of the image.
        double xcen = 0.5 * ( (double) nxpix );
        double ycen = 0.5 * ( (double) nypix );
        xin[0] = xcen - 0.5;
        xin[1] = xcen + 0.5;
        yin[0] = yin[1] = ycen;

        // Transform these image positions into sky coordinates.
        double[][] xyout = frameSet.tran2( 2, xin, yin, true );

        // And now get the distance between these positions in degrees.
        if ( raIndex == 1 ) {
            point1[0] = xyout[0][0];
            point1[1] = xyout[1][0];
            point2[0] = xyout[0][1];
            point2[1] = xyout[1][1];
        } 
        else {
            point1[1] = xyout[0][0];
            point1[0] = xyout[1][0];
            point2[1] = xyout[0][1];
            point2[0] = xyout[1][1];
        }
        double dist = frameSet.distance( point1, point2 );

        double xDegPix = 0.0;
        if ( dist != AstObject.AST__BAD ) {
            xDegPix = dist * R2D;
        }

        //  Same procedure for Y.
        xin[0] = xin[1] = xcen;
        yin[0] = ycen - 0.5;
        yin[1] = ycen + 0.5;

        // Transform these image positions into sky coordinates.
        xyout = frameSet.tran2( 2, xin, yin, true );
        if ( decIndex == 1 ) {
            point1[0] = xyout[0][0];
            point1[1] = xyout[1][0];
            point2[0] = xyout[0][1];
            point2[1] = xyout[1][1];
        } 
        else {
            point1[1] = xyout[0][0];
            point1[0] = xyout[1][0];
            point2[1] = xyout[0][1];
            point2[0] = xyout[1][1];
        }
        dist = frameSet.distance( point1, point2 );

        double yDegPix = 0.0;
        if ( dist != AstObject.AST__BAD ) {
            yDegPix = dist * R2D;
        }

        degPerPixel = new Point2D.Double( xDegPix, yDegPix );
    }
}
