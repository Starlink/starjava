/*
 * Copyright (C) 2006-2007 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     01-DEC-2006 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.plot;

import diva.canvas.AbstractFigure;
import diva.canvas.interactor.Interactor;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.text.DecimalFormat;

import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nom.tam.fits.FitsDate;

import uk.ac.starlink.ast.DSBSpecFrame;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.diva.DrawLabelFigure;
import uk.ac.starlink.diva.DrawRectangleFigure;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.mjDate;
import uk.ac.starlink.pal.palTime;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A Figure that describes some of the meta-data properties of a
 * spectrum. These are modelled on those liked by the JCMT observers
 * at the JAC (basically those displayed by Specx). The Figure is
 * really a {@link DrawLabelFigure}, but one which retains a fixed position
 * inside a {@link JViewport}, so that it remains visible.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class JACSynopsisFigure
    extends DrawLabelFigure
{
    /** The SpecData instance */
    private SpecData specData = null;

    /** The JViewport we're at a fixed position within */
    private JViewport viewport = null;

    /** The underlying location within viewport */
    private Point anchor = null;

    /** The default font */
    private static Font defaultFont = new Font( null, Font.PLAIN, 10 );

    /**
     * Create an instance.
     *
     * @param specData the {@link SpecData} instance.
     * @param viewport the {@link JViewport} that this figure is being
     *                 displayed within.
     * @param anchor the initial position within the {@link JViewport}.
     *               This will change as the figure is dragged around.
     */
    public JACSynopsisFigure( SpecData specData, JViewport viewport,
                              Point anchor )
    {
        super( "JACSynopsisFigure", defaultFont, 4.0,
               SwingConstants.NORTH_WEST );
        this.anchor = anchor.getLocation();
        setSpecData( specData );
        setViewport( viewport );
        reAnchor();
    }

    /**
     * Set the SpecData instance.
     */
    public void setSpecData( SpecData specData )
    {
        this.specData = specData;
        updateProperties();
        fireChanged();
    }

    /**
     * Set the {@link JViewport} instance. The position of our synopsis text
     * is initially fixed within the visible portion of this.
     */
    public void setViewport( JViewport viewport )
    {
        this.viewport = viewport;

        // We want to know when this changes so we can redraw the text at the
        // correct position so register for any changes.
        viewport.addChangeListener( new ChangeListener()
            {
                public void stateChanged( ChangeEvent e )
                {
                    reAnchor();
                }
            });

        fireChanged();
    }

    /**
     * Return a copy of the current local anchor position.
     */
    public Point getLocalAnchor()
    {
        return anchor.getLocation();
    }

    //  Create the String that represents the synopsis.
    protected void updateProperties()
    {
        if ( specData == null ) {
            return;
        }

        ASTJ astj = specData.getAst();
        Frame specAxis = astj.pickAxis( 1 );

        StringBuffer b = new StringBuffer();

        //  SPLAT name.
        b.append( "Name: " + specData.getShortName() + "\n" );

        //  Telescope/instrument/backend.
        String prop = specData.getProperty( "TELESCOP" );
        String telescope = prop;
        if ( ! "".equals( prop ) ) {
            b.append( "Telescope: " + prop );
            prop = specData.getProperty( "INSTRUME" );
            if ( ! "".equals( prop ) ) {
                b.append( " / " + prop );
                prop = specData.getProperty( "BACKEND" );
                if ( ! "".equals( prop ) ) {
                    b.append( " / " + prop );
                }
            }
            b.append( "\n" );
        }

        //  Target of observation.
        prop = specData.getProperty( "OBJECT" );
        if ( ! "".equals( prop ) ) {
            b.append( "Object: " + prop );

            //  ACSIS specific.
            prop = specData.getProperty( "MOLECULE" );
            if ( ! "".equals( prop ) && ! "No Line".equals( prop ) ) {
                b.append( " / " + prop );
            }
            prop = specData.getProperty( "TRANSITI" );
            if ( ! "".equals( prop ) && ! "No Line".equals( prop ) ) {
                b.append( " / " + prop );
            }
            b.append( "\n" );
        }

        //  Prefer DATE-OBS human readable date of observation. Epoch will
        //  be MJD-OBS converted into a Julian epoch, which requires more work.
        prop = specData.getProperty( "DATE-OBS" );
        if ( "".equals( prop ) ) {
            if ( specAxis.test( "Epoch" ) ) {

                //  Use Epoch value, that's decimal years (TDB), so convert
                //  into human readable form. Get epoch in TAI (/UTC).
                double epoch = specAxis.getEpoch() -
                    ( 32.184 / ( 60.0 * 60.0 * 24.0 * 365.25 ) );
                Pal pal = new Pal();
                double mjd = pal.Epj2d( epoch );
                try {
                    mjDate date = pal.Djcl( mjd );
                    palTime dayfrac = pal.Dd2tf( date.getFraction() );
                    prop =
                        date.getYear() + "-" + date.getMonth() + "-" +
                        date.getDay() + "T" + dayfrac.toString();
                }
                catch (Exception e) {
                    //  Formatting or domain error.
                    prop = e.getMessage();
                }
            }
        }
        if ( ! "".equals( prop ) ) {
            b.append( "Date obs: " + prop + " (UTC)\n" );
        }

        //  Elevation.
        prop = specData.getProperty( "ELSTART" );
        if ( ! "".equals( prop ) ) {
            double elstart = Double.parseDouble( prop );
            prop = specData.getProperty( "ELEND" );
            if ( ! "".equals( prop ) ) {
                double elend = Double.parseDouble( prop );
                b.append( "Elevation: " + (0.5*(elstart+elend)) + "\n" );
            }
            else {
                b.append( "Elevation: " + elstart + "\n" );
            }
        }

        //  Exposure time. Prefer EXTIME from SPLAT, that's the time for just
        //  this spectrum (ton+toff).
        prop = specData.getProperty( "EXTIME" );
        if ( ! "".equals( prop ) ) {
            b.append( "Exposure: " + prop + " (sec)\n" );
        }
        else {

            //  EXP_TIME is median for cube.
            prop = specData.getProperty( "EXP_TIME" );
            if ( ! "".equals( prop ) ) {
                b.append( "Exposure (median): " + prop + " (sec)\n" );
            }
            else {
                
                //  Try elapsed time.
                prop = specData.getProperty( "INT_TIME" );
                if ( ! "".equals( prop ) ) {
                    b.append( "Exposure (elapsed): " + prop + " (sec)\n" );
                }
                else {

                    //  These are human readable dates so need to use FITS
                    //  parsing.
                    prop = specData.getProperty( "DATE-OBS" );
                    if ( ! "".equals( prop ) ) {
                        try {
                            Date dstart = new FitsDate( prop ).toDate();
                            prop = specData.getProperty( "DATE-END" );
                            if ( ! "".equals( prop ) ) {
                                Date dend = new FitsDate( prop ).toDate();
                                
                                //  Get milliseconds in UNIX time.
                                long istart = dstart.getTime();
                                long iend = dend.getTime();
                                b.append( "Exposure (elapsed): "
                                          +( (iend-istart)/1000.0 )+ " (sec)\n" );
                            }
                        }
                        catch (Exception e) {
                            //  Unparsable time. Just give up.
                        }
                    }
                }
            }
        }
            
        b.append( "Coord Sys: " + specAxis.getSystem() + "\n" );
        //b.append( "ObsLat: " + specAxis.getC( "ObsLat" ) + "\n" );
        //b.append( "ObsLon: " + specAxis.getC( "ObsLon" ) + "\n" );

        if ( specAxis instanceof SpecFrame ) {

            //  Extraction position.
            prop = specData.getProperty( "EXRAX" );
            if ( ! "".equals( prop ) ) {
                b.append( "Spec Position: " +
                          prop + ", " +
                          specData.getProperty( "EXDECX" ) + "\n" );
            }

            //  RA and Dec of image centre are ideally from GAIA.
            //  Prefer EXRRA over EXRA, as EXRRA should be the source
            //  position, EXRA is just the image centre.
            prop = specData.getProperty( "EXRRA" );
            if ( ! "".equals( prop ) ) {
                b.append( "Src Position: " +
                          prop + ", " +
                          specData.getProperty( "EXRDEC" ) + "\n" );

                //  Offset from this position.
                prop = specData.getProperty( "EXRRAOF" );
                if ( ! "".equals( prop ) ) {
                    b.append( "Offset: " +
                              prop + ", " +
                              specData.getProperty( "EXRDECOF" ) +
                              " (arcsec)\n" );
                }
            }
            else {
                //  Try for image centre.
                prop = specData.getProperty( "EXRA" );
                if ( ! "".equals( prop ) ) {
                    b.append( "Img Centre: " +
                              prop + ", " +
                              specData.getProperty( "EXDEC" ) + "\n" );

                    //  Extraction offsets from centre.
                    prop = specData.getProperty( "EXRAOF" );
                    if ( ! "".equals( prop ) ) {
                        b.append( "Offset: " +
                                  prop + ", " +
                                  specData.getProperty( "EXDECOF" ) +
                                  " (arcsec)\n" );
                    }
                }
                else {
                    //  No image centre, may have an offset anyway (from
                    //  JCMT GAPPT observation).
                    prop = specData.getProperty( "EXRRAOF" );
                    if ( ! "".equals( prop ) ) {
                        b.append( "Offset: " +
                                  prop + ", " +
                                  specData.getProperty( "EXRDECOF" ) +
                                  " (arcsec)\n" );
                    }
                }
            }

            //  Report SpecFrame reference position. This should be
            //  also for the source in general. The actual use is for doppler
            //  corrections.
            if ( specAxis.test( "RefRA" ) ) {
                b.append( "Doppler RA, Dec: " + specAxis.getC( "RefRA" ) );
                b.append( ", " + specAxis.getC( "RefDec" ) + "\n" );
            }

            //  Source velocity and rest frame. Only report if set.
            if ( specAxis.test( "SourceVel" ) ) {
                b.append( "SourceVel: " + specAxis.getC("SourceVel" ) + "\n");
                if ( specAxis.test( "SourceVRF" ) ) {
                    b.append( "SourceVRF: " +
                    specAxis.getC("SourceVRF")+"\n");
                }
            }
            if ( specAxis.test( "StdOfRest" ) ) {
                b.append( "StdOfRest: "+specAxis.getC( "StdOfRest" )+"\n" );
            }
            if ( specAxis.test( "RestFreq" ) ) {
                b.append( "RestFreq: "+specAxis.getC( "RestFreq" )+" (GHz)\n");
            }
        }
        if ( specAxis instanceof DSBSpecFrame ) {
            b.append( "ImagFreq: " + specAxis.getC( "ImagFreq" ) + " (GHz)\n");
            //b.append( "DSBCentre: " + specAxis.getC( "DSBCentre" ) + "\n" );
            //b.append( "IF: " + specAxis.getC( "IF" ) + " (GHz)\n" );
        }

        //  Channel spacing.
        prop = specData.getProperty( "IFCHANSP" );
        if ( specAxis instanceof DSBSpecFrame && ! "".equals( prop ) ) {
            //  JCMT value, should only be seen for DSBSpecFrames.
            double inc = Double.parseDouble( prop ) / 1.0E6;
            b.append( "Channel spacing: " + inc + " (MHz)\n" );
        }
        else {
            //  Work out the channel spacing from the coordinates.
            //  These are in the axis units.
            Mapping mapping = astj.get1DMapping( 1 );
            double xin[] = new double[]{1.0, 2.0};
            double xout[] = mapping.tran1( 2, xin, true );
            double inc = xout[1] - xout[0];
            String u = specAxis.getUnit( 1 );
            b.append( "Channel spacing: " + inc );
            if ( ! "".equals( u ) ) {
                b.append( " (" + u + ")\n" );
            }
            else {
                b.append( "\n" );
            }
        }

        //  TSYS and TRX. TRX only for ACSIS timeseries cubes.
        prop = specData.getProperty( "TSYS" );
        if ( ! "".equals( prop ) ) {
            b.append( "TSYS: " + prop + " (K)\n" );
        }
        else {
            //  Maybe we have a median TSYS for cube.
            prop = specData.getProperty( "MEDTSYS" );
            if ( ! "".equals( prop ) ) {
                b.append( "TSYS (median): " + prop + " (K)\n" );
            }
        }

        prop = specData.getProperty( "TRX" );
        if ( ! "".equals( prop ) ) {
            b.append( "TRX: " + prop + " (K)\n" );
        }

        setString( b.toString() );
    }

    /**
     * Reset the text anchor position so that the position within the viewport
     * is retained.
     */
    protected void reAnchor()
    {
        Point p = viewport.getViewPosition();
        p.translate( anchor.x, anchor.y );

        Point2D a = getAnchorPoint();
        translate( p.getX() - a.getX(), p.getY() - a.getY(), false );
    }

    // Don't want this to transform, needs to retain its size, not grow in
    // scale with the other DrawFigure instances.
    public void transform( AffineTransform at )
    {
        // Do nothing.
    }

    //  Like translate, but with optional reset of local anchor position
    //  (do after interactive drag, but not when viewport updates).
    public void translate( double x, double y, boolean reset )
    {
        //  Cheat big time. Use transform() of super-class, not translate.
        //  That avoids having a transform in this class, which is where the
        //  translation request end up.
        super.transform( AffineTransform.getTranslateInstance( x, y ) );

        if ( reset ) {
            //  Reset local anchor wrt to viewport.
            Point2D a = getAnchorPoint();
            Point p = viewport.getViewPosition();
            anchor.setLocation( (int)(a.getX() - p.getX()),
                                (int)(a.getY() - p.getY()) );
        }
    }

    //  Normal translate.
    public void translate( double x, double y )
    {
        //  Reset anchor as should be from an interactive movement.
        translate( x, y, true );
    }
}
