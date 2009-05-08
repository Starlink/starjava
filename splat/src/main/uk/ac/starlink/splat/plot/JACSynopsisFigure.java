/*
 * Copyright (C) 2006-2007 Particle Physics and Astronomy Research Council
 * Copyright (C) 2007-2008 Science and Technology Facilities Council
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
import uk.ac.starlink.ast.TimeFrame;
import uk.ac.starlink.diva.DrawLabelFigure;
import uk.ac.starlink.diva.DrawRectangleFigure;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.mjDate;
import uk.ac.starlink.pal.palTime;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.util.JACUtilities;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A Figure that describes some of the meta-data properties of a
 * spectrum. These are modelled on those liked by the JCMT observers
 * at the JAC (basically those displayed by Specx) and makes a lot of use of 
 * FITS values either written with JAC data or created by GAIA, but retains
 * some use for other data sources.
 *
 * The Figure is really a {@link DrawLabelFigure}, but one which retains a
 * fixed position inside a {@link JViewport}, so that it remains visible.
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
     * Set the local anchor position.
     */
    public void setLocalAnchor( Point anchor )
    {
        this.anchor = anchor.getLocation();
        reAnchor();
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
        Frame specAxis = specData.getAst().pickAxis( 1 );

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
                //  into human readable form in UTC.

                //  Method using AST to convert timescales.
                //  XXX Requires up to date JNIAST with latest leap second.
//                 TimeFrame tf = new TimeFrame();
//                 tf.set( "TimeScale=TDB" );
//                 Pal pp = new Pal();
//                 double ep = pp.Epj2d( specAxis.getEpoch() );
//                 tf.set( "TimeOrigin= MJD" + ep );
//                 tf.set( "TimeScale=UTC" );
                
//                 String testprop = null;
//                 try {
//                     Pal pal = new Pal();
//                     mjDate date = pal.Djcl( tf.getTimeOrigin() );
//                     palTime dayfrac = pal.Dd2tf( date.getFraction() );
//                     testprop =
//                         date.getYear() + "-" + date.getMonth() + "-" +
//                         date.getDay() + "T" + dayfrac.toString();
//                 }
//                 catch (Exception e) {
//                     //  Formatting or domain error.
//                     testprop = e.getMessage();
//                 }
//                 b.append( "Date-obs: " + testprop + "\n" );
                
                //  Without AST method.
                //  Get epoch in TDB.
                double epoch = specAxis.getEpoch();
                
                //  To an MJD.
                Pal pal = new Pal();
                double mjd = pal.Epj2d( epoch );

                //  To TAI (-32.184 seconds).
                mjd -= ( 32.184 / ( 60.0 * 60.0 * 24.0 ) );
                
                //  To UTC (subtract leap seconds );
                mjd -= ( pal.Dat( mjd ) / ( 60.0 * 60.0 * 24.0 ) );

                //  Format as FITS date.
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

        //  Exposure time. Prefer EXTIME from GAIA, that's the time for just
        //  this spectrum (ton+toff).
        boolean needatime = true;
        prop = specData.getProperty( "EXTIME" );
        if ( ! "".equals( prop ) ) {
            b.append( "Exposure: " + prop + " (sec)\n" );
            needatime = false;
        }
        else {
            //  EXP_TIME is median for all the spectra in a cube.
            prop = specData.getProperty( "EXP_TIME" );
            if ( ! "".equals( prop ) ) {
                b.append( "Exposure (median): " + prop + " (sec)\n" );
                needatime = false;
            }
        }

        //  Effective exposure time, this will be written only by
        //  GAIA.
        prop = specData.getProperty( "EXEFFT" );
        if ( ! "".equals( prop ) ) {
            b.append( "Exposure (effective): " + prop + " (sec)\n" );
            needatime = false;
        }

        //  Elapsed time for the whole observation, only if no other times
        //  have been shown.
        if ( needatime ) {
            prop = specData.getProperty( "INT_TIME" );
            if ( ! "".equals( prop ) ) {
                b.append( "Exposure (elapsed): " + prop + " (sec)\n" );
            }
            else {
                //  These are human readable dates so need to use FITS parsing.
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

        //  The coordinate system. AST code.
        b.append( "Coord sys: " + specAxis.getSystem() + "\n" );

        if ( specAxis instanceof SpecFrame ) {

            //  If no extraction position is available keep the other
            //  reports suppressed as people find that confusing.
            boolean havepos = false;

            //  Extraction position.
            prop = specData.getProperty( "EXRAX" );
            if ( ! "".equals( prop ) ) {
                b.append( "Spec position: " +
                          prop + ", " +
                          specData.getProperty( "EXDECX" ) + "\n" );
                havepos = true;
            }

            //  RA and Dec of image centre are ideally from GAIA.
            //  Prefer EXRRA over EXRA, as EXRRA should be the source
            //  position, EXRA is just the image centre.
            prop = specData.getProperty( "EXRRA" );
            if ( ! "".equals( prop ) ) {
                b.append( "Src position: " +
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
                havepos = true;
            }
            else {
                //  Try for image centre.
                prop = specData.getProperty( "EXRA" );
                if ( ! "".equals( prop ) ) {
                    b.append( "Img centre: " +
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
                    havepos = true;
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
            if ( havepos && specAxis.test( "RefRA" ) ) {
                b.append( "Doppler RA, Dec: " + specAxis.getC( "RefRA" ) );
                b.append( ", " + specAxis.getC( "RefDec" ) + "\n" );
            }

            //  Source velocity and rest frame. Only report if set.
            if ( specAxis.test( "SourceVel" ) ) {
                b.append( "SourceVel: " + specAxis.getC( "SourceVel" ) +"\n" );
                if ( specAxis.test( "SourceVRF" ) ) {
                    b.append( "SourceVRF: " +
                              specAxis.getC( "SourceVRF" ) + "\n" );
                }
                if ( specAxis.test( "SourceSys" ) ) {
                    b.append( "SourceSys: " +
                              specAxis.getC( "SourceSys") + "\n" );
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
        }

        //  Channel spacing. Work this out from AST, but report the value
        //  in MHz, if IFCHANSP is present, and we have a SpecFrame.
        prop = specData.getProperty( "IFCHANSP" );
        double inc;
        if ( specAxis instanceof SpecFrame && ! "".equals( prop ) ) {
            inc = specData.channelSpacing( "System=FREQ,Unit=MHz" );
            b.append( "Channel spacing: " + inc + " (MHz)\n" );
        }
        else {
            inc = specData.channelSpacing( "" );
            b.append( "Channel spacing: " + inc );
            String u = specAxis.getUnit( 1 );
            if ( ! "".equals( u ) ) {
                b.append( " (" + u + ")\n" );
            }
            else {
                b.append( "\n" );
            }
        }

        //  Number of channels in spectrum.
        int nchan = specData.size();
        b.append( "Number of channels: " + nchan + "\n" );

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

        //  Estimated TSYS from variance, if possible.
        if ( specAxis instanceof SpecFrame && "JCMT".equals( telescope ) ) {
            prop = JACUtilities.calculateTSYS( specData );
            if ( ! "".equals( prop ) ) {
                b.append( "TSYS (est): " + prop + " (K)\n" );
            }
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

    
    public void paint (Graphics2D g) 
    {
        // XXX strange exception from Ducus library in Java 1.6. Just handle
        // this for now.
        try {
            super.paint( g );
        }
        catch (Exception e) {
            System.out.println( e.getMessage() );
        }
    }
}
