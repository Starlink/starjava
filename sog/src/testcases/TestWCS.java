// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    11-JUN-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.sog.test;

import com.sun.media.jai.codec.ImageCodec;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Insets;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.coords.WorldCoords;
import jsky.image.gui.ImageDisplay;
import jsky.image.gui.BasicImageDisplay;
import jsky.util.gui.BasicWindowMonitor;
import jsky.image.gui.ImageGraphicsHandler;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.jaiutil.HDXCodec;
import uk.ac.starlink.jaiutil.HDXImage;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.sog.AstTransform;
import uk.ac.starlink.ast.grf.DefaultGrf;

/**
 * Basic test of WCS functions using a the simplest JSky image display
 * component. This class adds an AST based WorldCoordinates
 * implementation and uses it to readoff coordinates and overlay an
 * astrometry grid.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class TestWCS extends JPanel implements ImageGraphicsHandler
{
    static
    {
        ImageCodec.registerCodec( new HDXCodec() );
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame( "ImageDisplay" );
        TestWCS testWCS = new TestWCS( args );

        //  Realize widgets.
        frame.getContentPane().add( testWCS, BorderLayout.CENTER );
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener( new BasicWindowMonitor() );

    }

    protected ImageDisplay display = null;
    protected JLabel label = null;

    public TestWCS( String[] args )
    {
        display = new ImageDisplay();

        //  Images are the usual astronomy orientation.
        display.getImageProcessor().setFlipY( true );
        display.setAutoCenterImage( true );

        //  This class needs to redraw the graphics when required.
        display.addImageGraphicsHandler( this );

        if ( args.length > 0 ) {
            try {
                display.setImage( JAI.create( "fileload", args[0] ) );
            }
            catch(Exception e) {
                System.out.println( "error: " + e.toString() );
                System.exit(1);
            }
        }

        //  Add button to do the basic checks and enable mouse tracking.
        JButton check = new JButton( "Check" );
        check.addActionListener
            ( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        checkWCS();
                    }
                }
            );

        //  Add a label to show mouse position.
        label = new JLabel( "Coordinates" );
        JPanel panel = new JPanel( new BorderLayout() );
        panel.add( check, BorderLayout.NORTH );
        panel.add( label, BorderLayout.SOUTH );

        //  Realize widgets.
        setLayout( new BorderLayout() );
        add( display, BorderLayout.CENTER );
        add( panel, BorderLayout.SOUTH );
        setVisible(true);
    }

    protected FrameSet frameSet = null;
    /**
     * Create the AstTransform instance and set this as the WCS system
     * to be used by JSky. Report the center of the image, set up the
     * mouse tracking and finally draw the grid overlay.
     */
    public void checkWCS()
    {
        //  Get the FrameSet....
        PlanarImage im = display.getDisplayImage();
        Object o = im.getProperty("#ndx_image");
        if ( o != null && (o instanceof HDXImage) ) {
            HDXImage hdxImage = (HDXImage) o;
            Ndx ndx = hdxImage.getCurrentNDX();
            try {
                frameSet = ndx.getAst();
                if ( frameSet != null ) {
                    addAstTransform();
                    addMouseTracker();
                    repaint();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    protected WorldCoordinateConverter convTool = null;
    protected CoordinateConverter cc = null;
    /**
     * Create and add the AstTransform object to the ImageDisplay.
     */
    protected void addAstTransform()
    {
        //  Use the FrameSet to create the AstTransform
        //  object and pass this to the ImageDisplay.
        AstTransform astTransform = 
            new AstTransform( frameSet, display.getImageWidth(),
                              display.getImageHeight() );

        display.setWCS( astTransform );
        convTool = display.getWCS();

        //  Display the center of the image. Note WorldCoords stores
        //  only in J2000.
        WorldCoords wc = new WorldCoords( convTool.getWCSCenter(),
                                          convTool.getEquinox() );
        String[] radec = wc.format(convTool.getEquinox());
        System.out.println( "Center of image: " +
                            radec[0] + ", " + radec[1] +
                            " " + convTool.getEquinox() );

        //  Hang onto the CoordinateConverter (uses our WCS and adds
        //  the canvas and screen systems).
        cc = display.getCoordinateConverter();
    }

    /** 
     * Arrange to track the mouse position and report the coordinates.
     */
    protected void addMouseTracker()
    {
        addMouseMotionListener( new MouseMotionListener()
        {
            public void mouseMoved( MouseEvent e )
            {
                //  Get action position and transform from screen
                //  coordinates to world coordinates.
                Point2D.Double p = new Point2D.Double( e.getX(),
                                                       e.getY() );
                cc.screenToWorldCoords( p, false );

                //  Display the coordinates (WorldCoords only stores
                //  in J2000).
                WorldCoords wc = new WorldCoords( p, cc.getEquinox() );
                String[] radec = wc.format( cc.getEquinox() );
                label.setText( "Coords: " + radec[0] + ", " + radec[1]
                               + ", " + cc.getEquinox() );
                }
            public void mouseDragged( MouseEvent e )
            {
                // Do nothing.
            }
        } );
    }        
        
    protected Plot astPlot = null;

    /**
     * Create an AST Plot matched to the image and draw a grid overlay.
     */
    public void doPlot()
    {
        if ( frameSet == null ) return;
        
        //  Use the limits of the image to determine the graphics position.
        double[] canvasbox = new double[4];
        Point2D.Double p = new Point2D.Double();
        
        p.setLocation( 1.0, 1.0 );
        cc.imageToScreenCoords( p, false );
        canvasbox[0] = p.x;
        canvasbox[1] = p.y;
        
        p.setLocation( display.getImageWidth(), display.getImageHeight() );
        cc.imageToScreenCoords( p, false );
        canvasbox[2] = p.x;
        canvasbox[3] = p.y;
        
        int xo = (int) Math.min( canvasbox[0], canvasbox[2] );
        int yo = (int) Math.min( canvasbox[1], canvasbox[3] );
        int dw = (int) Math.max( canvasbox[0], canvasbox[2] ) - xo;
        int dh = (int) Math.max( canvasbox[1], canvasbox[3] ) - yo;
        Rectangle graphRect = new Rectangle( xo, yo, dw, dh );
        
        //  Transform these positions back into image
        //  coordinates. These are suitably "untransformed" from the
        //  graphics position and should be the bottom-left and
        //  top-right corners.
        double[] basebox = new double[4];
        p = new Point2D.Double();
        p.setLocation( (double) xo, (double) (yo + dh) );
        cc.screenToImageCoords( p, false );
        basebox[0] = p.x;
        basebox[1] = p.y;

        p.setLocation( (double) (xo + dw), (double) yo );
        cc.screenToImageCoords( p, false );
        basebox[2] = p.x;
        basebox[3] = p.y;

        //  Now create the astPlot.
        astPlot = new Plot( frameSet, graphRect, basebox );

        astPlot.setGrid( true );
        astPlot.setDrawAxes( true );
        astPlot.setColour( "Grid", java.awt.Color.magenta.getRGB() );
        astPlot.setColour( "Border", java.awt.Color.magenta.getRGB() );
        astPlot.setColour( "NumLab", java.awt.Color.yellow.getRGB() );
        astPlot.setColour( "TextLab", java.awt.Color.yellow.getRGB() );
        astPlot.setColour( "Title", java.awt.Color.yellow.getRGB() );
        astPlot.setLabelling( "Interior" );
        astPlot.grid();
    }

    //  Called by the ImageDisplay after the image is draw. It
    //  arranges to have an AST Plot created and uses it to draw a
    //  grid overlay.
    public void drawImageGraphics( BasicImageDisplay imageDisplay,
                                   Graphics2D g)
    {
        if ( frameSet != null ) {
            doPlot();
            astPlot.paint( g );
        }
    }
}
