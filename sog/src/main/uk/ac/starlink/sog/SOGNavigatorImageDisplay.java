/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-JUN-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.sog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;

import javax.media.jai.PlanarImage;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.navigator.NavigatorImageDisplay;
import jsky.util.gui.DialogUtil;
import jsky.image.ImageProcessor;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
//import uk.ac.starlink.hdx.HdxException;
import uk.ac.starlink.jaiutil.HDXImage;
import uk.ac.starlink.jaiutil.HDXImageProcessor;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;

/**
 * Extends NavigatorImageDisplay (and DivaMainImageDisplay) to add
 * support for reading HDX files.
 *
 * @author Peter W. Draper
 * @version $Id$
 */

public class SOGNavigatorImageDisplay
    extends NavigatorImageDisplay
{
    /**
     * Reference to the HDXImage displaying the NDX, if any.
     */
    protected HDXImage hdxImage = null;

    /**
     * Whether we're draw a grid or not.
     */
    protected boolean drawGrid = false;

    /**
     * Simple counter for generating unique names.
     */
    private int counter= 0;

    /**
     * True when a NDX is loading. Used to enable certain events that
     * are only performed for files or urls (which an NDX may not have).
     */
    private boolean ndxLoading = false;

    //  Repeat all constructors.
    public SOGNavigatorImageDisplay( Component parent )
    {
        //  Use an ImageProcessor with HDX support.
        super( parent, new HDXImageProcessor() );
    }

    /**
     * Accept an DOM element that contains an NDX for display. NDX
     * equivalent of setFilename.
     */
    public void setNDX( Element element )
    {
        //  Make sure currently displayed image is saved and added to
        //  history.
        if ( !checkSave() ) {
            return;
        }
        addToHistory();

        //  Create and load the PlanarImage that wraps the HDXImage,
        //  that accepts the DOM NDX!
        try {
            PlanarImage im =
                PlanarImage.wrapRenderedImage( new HDXImage( element, 0 ) );
            ndxLoading = true;
            setImage(im);
            ndxLoading = false;
        }
        catch( Exception e ) {
            DialogUtil.error( e );
            clear();
        }
        updateTitle();

        // Set the cut levels. TODO: Still not great.
        ImageProcessor ip = getImageProcessor();
        //ip.setCutLevels( ip.getMinValue(), ip.getMaxValue() );
        ip.autoSetCutLevels( getVisibleArea() );
        ip.update();
    }

    /**
     * Update the enabled states of some menu/toolbar actions.
     */
    protected void updateEnabledStates()
    {
        super.updateEnabledStates();
        if ( ndxLoading ) {
            getCutLevelsAction().setEnabled( true );
            getColorsAction().setEnabled( true );
        }
        if ( gridAction != null ) {
            gridAction.setEnabled( getCutLevelsAction().isEnabled() );
        }
    }


    /**
     * This method is called before and after a new image is loaded,
     * each time with a different argument.
     *
     * @param before set to true before the image is loaded and false
     *               afterwards
     */
    protected void newImage( boolean before )
    {
        if ( before ) {
            if ( hdxImage != null ) {
                hdxImage.clearTileCache();
            }
            hdxImage = null;
            setWCS( null );
        }
        else if ( getFitsImage() == null ) {

            // Check if it is a HDX, and if so, get the HDXImage
            // object which is needed to initialize WCS.
            PlanarImage im = getImage();
            if ( im != null ) {
                Object o = im.getProperty("#ndx_image");
                if ( o != null && (o instanceof HDXImage) ) {
                    hdxImage = (HDXImage) o;
                    initWCS();
                }
            }
        }

        // Do this afterwards so graphics are not re-drawn until WCS is
        // available.
        super.newImage( before );
    }

    /**
     * Initialize the world coordinate system, if the image properties
     * (keywords) support it
     */
    protected void initWCS()
    {
        if ( getFitsImage() != null ) {
            super.initWCS();
            return;
        }

        // If not FITS and not HDX nothing to do.
        if ( hdxImage == null ) {
            return;
        }

        // Only initialize once per image.
        if ( getWCS() != null ) {
            return;
        }

        //  Access the current NDX and see if it has an AST
        //  FrameSet. This can be used to construct an AstTransform
        //  that can be used instead of a WCSTransform.
        Ndx ndx = hdxImage.getCurrentNDX();
        try {
            FrameSet frameSet = Ndxs.getAst( ndx );
            if ( frameSet != null ) {
                setWCS( new AstTransform( frameSet, getImageWidth(),
                                          getImageHeight() ) );
            }
        }
        catch( Exception e ) {
            e.printStackTrace();
            setWCS( null );
            return;
        }
        if ( ! getWCS().isWCS() ) {
            setWCS( null );
        }
    }

    /**
     * Add an action to draw or remove a grid overlay.
     */
    private AbstractAction gridAction =
        new AbstractAction( "Grid" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                AbstractButton b = (AbstractButton) evt.getSource();
                if ( b.isSelected() ) {
                    drawGrid();
                }
                else {
                    eraseGrid();
                }
            }
        };
    public AbstractAction getGridAction()
    {
        return gridAction;
    }

    /**
     * Draw a grid over the currently displayed image.
     */
    public void drawGrid()
    {
        drawGrid = true;
        repaint();
    }

    /**
     * Erase the grid, if drawn.
     */
    public void eraseGrid()
    {
        drawGrid = false;
        if ( astPlot != null ) {
            astPlot.clear();
            repaint();
        }
    }

    protected Plot astPlot = null;

    /**
     * Create an AST Plot matched to the image and draw a grid overlay.
     */
    public void doPlot()
    {
        // Check we have a transform and it understands AST (codecs
        // for non-NDX types will in general not).
        WorldCoordinateConverter transform = getWCS();
        if ( transform == null ||
             ! getWCS().isWCS() ||
             ! ( transform instanceof AstTransform ) ) {
            System.out.println( "Not an AstTransform" );
            astPlot = null;
            return;
        }
        System.out.println( "Is an AstTransform" );
        CoordinateConverter cc = getCoordinateConverter();
        FrameSet frameSet = ((AstTransform)transform).getFrameSet();

        //  Use the limits of the image to determine the graphics position.
        double[] canvasbox = new double[4];
        Point2D.Double p = new Point2D.Double();

        p.setLocation( 1.0, 1.0 );
        cc.imageToScreenCoords( p, false );
        canvasbox[0] = p.x;
        canvasbox[1] = p.y;

        p.setLocation( getImageWidth(), getImageHeight() );
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
        //astPlot.setLabelling( "Interior" );
        astPlot.grid();
    }

    //  Called when image needs repainting...
    public synchronized void paintLayer(Graphics2D g2D, Rectangle2D region)
    {
        super.paintLayer( g2D, region );

        //  Redraw a grid if required.
        if ( drawGrid ) {
            doPlot();
            if ( astPlot != null ) {
                astPlot.paint( g2D );
            }
        }
    }

    //  The exit method needs finer control. Do not close the whole
    //  application when being tested.
    public void exit()
    {
        if ( doExit ) {
            super.exit();
        }
        else {
            Window w = SwingUtilities.getWindowAncestor( this );
            if ( w != null ) {
                w.setVisible( false );
            }
            else {
                setVisible( false );
            }
        }
    }

    private boolean doExit = true;
    public void setDoExit( boolean doExit )
    {
        this.doExit = doExit;
    }
    public boolean isDoExit()
    {
        return doExit;
    }

    /**
     * Return true if the given filename has a suffix that indicates
     * that it is not a FITS file and is one of the standard JAI
     * supported image types. Need to override this so that XML files
     * are re-directed.
     */
    public boolean isJAIImageType( String filename ) 
    {
        System.out.println( "isJAIImageType: " + filename );
        if ( filename.endsWith("xml") ) {
            return true;
        }
        return super.isJAIImageType( filename );
    }
}
