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
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.image.ImageProcessor;
import jsky.image.fits.codec.FITSImage;
import jsky.image.gui.ImageHistoryItem;
import jsky.navigator.NavigatorImageDisplay;
import jsky.util.FileUtil;
import jsky.util.gui.DialogUtil;

import org.w3c.dom.Element;

import nom.tam.fits.Header;

import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.AstPackage;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.FitsChan;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.util.gui.StoreConfiguration;
import uk.ac.starlink.ast.gui.GraphicsHints;
import uk.ac.starlink.ast.gui.GraphicsHintsControls;
import uk.ac.starlink.ast.gui.PlotConfiguration;
import uk.ac.starlink.ast.gui.PlotConfigurator;
import uk.ac.starlink.ast.gui.PlotController;
import uk.ac.starlink.jaiutil.HDXImage;
import uk.ac.starlink.jaiutil.HDXImageProcessor;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.Ndxs;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;

import uk.ac.starlink.sog.photom.SOGCanvasDraw;
import uk.ac.starlink.sog.photom.AperturePhotometryFrame;

import uk.ac.starlink.ast.gui.AstFigureStore;
import uk.ac.starlink.ast.gui.AstPlotSource;
import uk.ac.starlink.diva.Draw;
import uk.ac.starlink.diva.DrawGraphicsMenu;

/**
 * Main class that creates windows for displaying an image. This is ultimately
 * used by both versions of SoG, that is with and without internal-frames.
 * <p>
 * The class extends NavigatorImageDisplay (and DivaMainImageDisplay) to add
 * any extra features provided by SoG.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SOGNavigatorImageDisplay
    extends NavigatorImageDisplay
    implements PlotController
    //,Draw
{
    /**
     * Reference to the HDXImage displaying the NDX, if any.
     */
    protected HDXImage hdxImage = null;

    /**
     * Whether we're drawing a grid or not.
     */
    protected boolean drawGrid = false;

    /**
     * Simple counter for generating unique names.
     */
    private int counter= 0;

    /**
     * Specialized CanvasDraw.
     */
    protected SOGCanvasDraw sogCanvasDraw = null;

    /**
     * True when a NDX is loading. Used to enable certain events that
     * are only performed for files or urls (which an NDX may not have).
     */
    private boolean ndxLoading = false;

    /**
     * Should the photometry Action be available?
     */
    private static boolean photomEnabled = false;

    //  Repeat all constructors.
    public SOGNavigatorImageDisplay( Component parent )
    {
        //  Use an ImageProcessor with HDX support.
        super( parent, new HDXImageProcessor() );

        //  Add our CanvasDraw.
        sogCanvasDraw = new SOGCanvasDraw( this );
        setCanvasDraw( sogCanvasDraw );

        // The window will accept drop events to display a new image.
        setTransferHandler( new SOGTransferHandler( this ) );
    }

    /**
     * Open up another window like this one and return a reference to it.
     * <p>
     * Note: derived classes should redefine this to return an instance of the
     * correct class, which should be a sublclass of JFrame or JInternalFrame.
     */
    public Component newWindow()
    {
        JDesktopPane desktop = getDesktop();
        if ( desktop != null ) {

            SOGNavigatorImageDisplayInternalFrame f =
                new SOGNavigatorImageDisplayInternalFrame( desktop );
            f.getImageDisplayControl().getImageDisplay().setTitle(getTitle());
            f.setVisible( true );
            desktop.add( f, JLayeredPane.DEFAULT_LAYER );
            desktop.moveToFront( f );
            f.setVisible(true);
            return f;
        }
        else {

            SOGNavigatorImageDisplayFrame f =
                new SOGNavigatorImageDisplayFrame();
            f.getImageDisplayControl().getImageDisplay().setTitle(getTitle());
            f.setVisible( true );
            return f;
        }
    }

    /**
     * Set the filename.
     */
    public void setFilename( String fileOrUrl )
    {
        try {
            setFilename( fileOrUrl, true );
        }
        catch (Exception e) {
            // Should never happen, errors are handled in 
            // setFilename( String, boolean ).
            e.printStackTrace();
        }
    }

    /**
     * Set the filename.... Whole method lifted from DivaMainImageDisplay as
     * we need to control creation differently to how JSky works... 
     * Also modified to throw an exception if asked. This is for remote
     * services that want to handle the error themselves.
     * XXX need to track any changes.
     */
    public void setFilename( String fileOrUrl, boolean handle )
        throws Exception
    {
        if ( fileOrUrl.startsWith( "http:" ) ) {
            setURL( FileUtil.makeURL( null, fileOrUrl ) );
            return;
        }
        if ( !checkSave() ) {
            return;
        }

        addToHistory();
        _filename = fileOrUrl;
        _url = _origURL = FileUtil.makeURL( null, fileOrUrl );

        // free up any previously opened FITS images
        FITSImage fitsImage = getFitsImage();
        if (fitsImage != null) {
            fitsImage.close();
            fitsImage.clearTileCache();
            fitsImage = null;
        }

        // Do same for the HdxImage?
        if ( hdxImage != null ) {
            try {
                hdxImage.close();
            }
            catch (IOException e) {
                e.printStackTrace(); // Otherwise, lets assume harmless.
            }
            hdxImage.clearTileCache();
            hdxImage = null;
        }

        // load non FITS and HDX images with JAI, others use more
        // efficienct implementations.

        if ( isJAIImageType( _filename ) ) {
            try {
                setImage( JAI.create( "fileload", _filename ) );
            }
            catch (Exception exception) {
                _filename = null;
                _url = _origURL = null;
                clear();
                if ( handle ) {
                    DialogUtil.error( exception );
                }
                else {
                    throw exception;
                }
            }
        }
        else if ( _filename.endsWith( "xml" ) ||
                  _filename.endsWith( "sdf" )  ) {
            System.out.println( "Loading NDX: " + _filename );
            //  HDX/NDF.
            try {
                hdxImage = new HDXImage( _filename );
                initHDXImage( hdxImage );

                //  HDX's are pre-flipped in Y, but still need their
                //  coordinates flipping.
                getImageProcessor().setInvertedYAxis( false );
                setImage( PlanarImage.wrapRenderedImage( hdxImage ) );
            }
            catch (IOException exception) {
                _filename = null;
                _url = _origURL = null;
                clear();
                if ( handle ) {
                    DialogUtil.error( exception );
                }
                else {
                    throw exception;
                }
            }
        }
        else {
            //  FITS Image. 13/01/03 is much faster than HDX FITS access...
            try {
                fitsImage = new FITSImage( _filename );
                initFITSImage( fitsImage );
                setImage( fitsImage );
            }
            catch ( Exception e ) {
                // fall back to JAI method
                try {
                    setImage( JAI.create( "fileload", _filename ) );
                }
                catch ( Exception exception ) {
                    _filename = null;
                    _url = _origURL = null;
                    clear();

                    //  Use first error, it's the most specific.
                    if ( handle ) {
                        DialogUtil.error( e );
                    }
                    else {
                        throw e;
                    }
                }
            }
        }
        updateTitle();
    }

    /**
     * Accept an HDXImage ready for use.
     */
    public void setHDXImage( HDXImage hdxImage )
    {
        //  Make sure currently displayed image is saved and added to
        //  history.
        if ( !checkSave() ) {
            return;
        }
        addToHistory();

        //  Create and load the PlanarImage that wraps the HDXImage.
        try {
            PlanarImage im = PlanarImage.wrapRenderedImage( hdxImage );
            ndxLoading = true;
            setImage( im );
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
     * Accept an DOM element that contains an NDX for display. NDX
     * equivalent of setFilename. This version is intended for use by the
     * remote interface and passes on the error for remote handling.
     */
    public void setRemoteNDX( Element element )
        throws IOException
    {
        HDXImage hdxImage = new HDXImage( element, 0 );
        setHDXImage( hdxImage );
    }

    /**
     * Accept an DOM element that contains an NDX for display. NDX
     * equivalent of setFilename.
     */
    public void setNDX( Element element )
    {
        try {
            HDXImage hdxImage = new HDXImage( element, 0 );
            setHDXImage( hdxImage );
        }
        catch (IOException e) {
            DialogUtil.error( e );
            clear();
        }
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
                Object o = im.getProperty( "#ndx_image" );
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
     * Called after a new HDXImage object was created to do HDX
     * specific initialization.
     */
    protected void initHDXImage( HDXImage hdxImage)
        throws IOException
    {
        // If the user previously set the image scale, restore it here
        // to avoid doing it twice
        ImageHistoryItem hi = getImageHistoryItem( new File(_filename) );
        float scale;
        if ( hi != null ) {
            scale = hi.getScale();
        }
        else {
            scale = getScale();
        }
        if ( scale != 1.0F ) {
            hdxImage.setScale( scale );
        }
    }

    /**
     * Initialize the world coordinate system, if the image properties
     * (keywords) support it
     */
    protected void initWCS()
    {
        if ( getFitsImage() != null ) {
            // If we have JNIAST available might as well use it's superior
            // facilities for WCS.
            if ( AstPackage.isAvailable() ) {
                try {
                    initWCSFromFits();
                }
                catch ( AstException e ) {
                    System.out.println( e.getMessage() );
                    super.initWCS();
                }
            }
            else {
                //  Fallback to JSky support.
                super.initWCS();
            }
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

        //  Access the current NDX and see if it has an AST FrameSet. This can
        //  be used to construct an AstTransform that can be used instead of a
        //  WCSTransform.
        Ndx ndx = hdxImage.getCurrentNDX();
        if ( AstPackage.isAvailable() ) {
            try {
                FrameSet frameSet = Ndxs.getAst( ndx );
                if ( frameSet != null ) {
                    setWCS( new AstTransform( frameSet, getImageWidth(),
                                              getImageHeight() ) );
                }
            }
            catch( Exception e ) {
                System.out.println( e.getMessage() );
                setWCS( null );
                return;
            }
        }
        else {
            System.err.println( "No WCS support available" );
            setWCS( null );
            return;
        }
        if ( ! getWCS().isWCS() ) {
            setWCS( null );
        }
    }

    /**
     * Initialise the WCS using the FITS header in a FITSImage.
     */
    protected void initWCSFromFits()
        throws AstException
    {
        //  Access the header.
        Header header = getFitsImage().getHeader();

        //  Arrange to read the headers into a FITS channel. Note we do not
        //  use the Iterator constructor of FitsChan as this doesn't allow the
        //  trapping of minor errors when reading the cards (like formatting
        //  problems).
        Iterator iter = header.iterator();
        FitsChan fitschan = new FitsChan();
        while ( iter.hasNext() ) {
            try {
                fitschan.putFits( iter.next().toString(), false );
            }
            catch (AstException e) {
                //  Ignore. Real failure is during full read, if any.
                System.out.println( e.getMessage() );
            }
        }
        fitschan.setCard( 0 );
        AstObject astObject = fitschan.read();

        //  If we have a FrameSet, use this as the WCS.
        if ( astObject != null && astObject instanceof FrameSet ) {
            setWCS( new AstTransform( (FrameSet) astObject, 
                                      getImageWidth(),
                                      getImageHeight() ) );
        }
        else {
            throw new AstException( "Failed to read FITS WCS using JNIAST" );
        }
    }

    /**
     * Get the current NDX being displayed.
     */
    public Ndx getCurrentNdx()
    {
        return hdxImage.getCurrentNDX();
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
                    showGridControls();
                }
                else {
                    withdrawGridControls();
                }
            }
        };
    public AbstractAction getGridAction()
    {
        return gridAction;
    }

    protected PlotConfigurator plotConfigurator = null;
    protected PlotConfiguration plotConfiguration = null;
    protected GraphicsHints graphicsHints = new GraphicsHints();

    /**
     * Display a window for controlling the appearance of the grid.
     */
    protected void showGridControls()
    {
        if ( plotConfiguration == null ) {
            if ( AstPackage.isAvailable() ) {
                plotConfiguration = new PlotConfiguration();
            }
            else {
                System.err.println("Cannot display WCS grids, no AST support");
                return;
            }
        }

        if ( plotConfigurator == null ) {
            plotConfigurator =
                new PlotConfigurator( "Grid Overlay Configuration",
                                      this, plotConfiguration,
                                      "jsky", "PlotConfig.xml" );

            //  Add in extra graphics hints controls (anti-aliasing).
            plotConfiguration.add( graphicsHints );
            plotConfigurator.addExtraControls
                ( new GraphicsHintsControls( graphicsHints ), true );

            //  Set the default configuration of the controls.
            InputStream defaultConfig =
                getClass().getResourceAsStream( "defaultgrid.xml" );
            StoreConfiguration store = new StoreConfiguration( defaultConfig );
            Element defaultElement = store.getState( 0 );
            plotConfigurator.restoreState( defaultElement );
            plotConfigurator.reset();
            try {
                defaultConfig.close();
            }
            catch (IOException e) {
                // Do nothing.
            }
        }
        plotConfigurator.setVisible( true );
    }

    /**
     * Erase the grid, if drawn.
     */
    public void withdrawGridControls()
    {
        drawGrid = false;
        if ( astPlot != null ) {
            astPlot.clear();
            repaint();
        }
        if ( plotConfigurator != null ) {
            plotConfigurator.setVisible( false );
        }
    }

    protected Plot astPlot = null;

    /**
     * Create a {@link Plot} matched to the image and draw a grid overlay.
     */
    public void doPlot()
    {
        // Check we have a transform and it understands AST (codecs
        // for non-NDX types will in general not).
        WorldCoordinateConverter transform = getWCS();
        if ( transform == null ||
             ! getWCS().isWCS() ||
             ! ( transform instanceof AstTransform ) ) {
            astPlot = null;
            return;
        }
        CoordinateConverter cc = getCoordinateConverter();
        FrameSet frameSet = ((AstTransform)transform).getFrameSet();

        //  Use the limits of the image to determine the graphics
        //  position.
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

        //  Transform these positions back into image coordinates. These are
        //  suitably "untransformed" from the graphics position and should be
        //  the bottom-left and top-right corners.
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

        //  Now create the astPlot, clearing existing graphics first.
        if ( astPlot != null ) astPlot.clear();
        astPlot = new Plot( frameSet, graphRect, basebox );

        String options = plotConfiguration.getAst();
        astPlot.set( options );
        astPlot.grid();
    }

    //  Called when image needs repainting...
    public synchronized void paintLayer(Graphics2D g2D, Rectangle2D region)
    {
        super.paintLayer( g2D, region );

        //  Redraw a grid if required.
        if ( drawGrid ) {

            //  Draw the plot.
            doPlot();
            if ( astPlot != null ) {

                //  Apply rendering hints.
                graphicsHints.applyRenderingHints( g2D );

                // And paint.
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

    //
    // PlotController interface.
    //

    /**
     * Draw a grid over the currently displayed image.
     */
    public void updatePlot()
    {
        drawGrid = true;
        repaint();
    }

    /**
     * Set the image background colour.
     */
    public void setPlotColour( Color color )
    {
        setBackground( color );
    }

    /**
     * Get the image background colour.
     */
    public Color getPlotColour()
    {
        return getBackground();
    }

    /**
     * Return a reference to the Frame used to define the
     * current coordinates. Using the WCS FrameSet.
     */
    public Frame getPlotCurrentFrame()
    {
        WorldCoordinateConverter transform = getWCS();
        if ( transform == null ||
             ! getWCS().isWCS() ||
             ! ( transform instanceof AstTransform ) ) {
            return new Frame( 2 ); // Cannot return a null.
        }
        return (Frame) ((AstTransform)transform).getFrameSet();
    }

    //
    // Aperture photometry toolbox
    //
    /**
     * Add an action to draw or remove a grid overlay.
     */
    private AbstractAction photometryAction =
        new AbstractAction( "Photometry" )
        {
            public void actionPerformed( ActionEvent evt )
            {
                AbstractButton b = (AbstractButton) evt.getSource();
                if ( b.isSelected() ) {
                    showPhotomControls();
                }
                else {
                    withdrawPhotomControls();
                }
            }
        };
    public AbstractAction getPhotomAction()
    {
        if ( photomEnabled ) {
            return photometryAction;
        }
        return null;
    }

    /**
     * Withdraw the photometry toolbox.
     */
    public void withdrawPhotomControls()
    {
        if ( photometryWindow != null ) {
            photometryWindow.setVisible( false );
        }
    }

    /**
     * Display a window for performing aperture photometry.
     */
    protected void showPhotomControls()
    {
        if ( photometryWindow == null ) {
            photometryWindow = new AperturePhotometryFrame( this );
        }
        photometryWindow.setVisible( true );
    }
    private AperturePhotometryFrame photometryWindow = null;

    /**
     * Set the scale (zoom factor) for the image.
     * This also adjusts the origin so that the center of the image
     * remains about the same.
     */
    public void setScale( float scale )
    {
        super.setScale( scale );

        //  HDX speed ups? Done after super-class, which should take
        //  care of setting new scale and centering.
        if ( hdxImage != null ) {
            boolean needsUpdate = false;
            try {
                needsUpdate = hdxImage.setScale( scale );
                setPrescaled(  hdxImage.getSubsample() != 1 );
            }
            catch(IOException e) {
                DialogUtil.error(e);
            }
            if ( ! needsUpdate && scale == getScale() ) {
                return;
            }
            if ( needsUpdate ) {
                ImageProcessor ip = getImageProcessor();
                ip.setSourceImage( PlanarImage.wrapRenderedImage( hdxImage ),
                                   ip );
                ip.update();
            }
        }
    }

    protected double _getPixelValue( PlanarImage im, int ix, int iy,
                                     int w, int h, int band )
    {
        if ( hdxImage == null ) {
            return super._getPixelValue( im, ix, iy, w, h, band );
        }

        // Handle HDX I/O optimizations
        int subsample = hdxImage.getSubsample();
        ix = ix / subsample;
        iy = iy / subsample;

        if ( ! getImageProcessor().isInvertedYAxis() ) {
            iy = h - 1 - iy;
        }

        if ( ix < 0 || ix >= w || iy < 0 || iy >= h ) {
            return 0.0;
        }

        int x = (int) ( (double) ix / hdxImage.getTileWidth() );
        int y = (int) ( (double) iy / hdxImage.getTileHeight() );
        if ( x < 0 || y < 0 ) {
            return 0.0;
        }

        Raster tile = im.getTile( x, y );
        if ( tile != null ) {
            try {
                return tile.getSampleDouble( ix, iy, band );
            }
            catch(Exception e) {
            }
        }
        return 0.0;
    }

    /**
     * Return the width of the source image in pixels
     */
    public int getImageWidth()
    {
        if ( hdxImage == null ) {
            return super.getImageWidth();
        }
        return hdxImage.getRealWidth();
    }


    /**
     * Return the height of the source image in pixels
     */
    public int getImageHeight()
    {
        if ( hdxImage == null ) {
            return super.getImageHeight();
        }
        return hdxImage.getRealHeight();
    }

    /**
     * Display a file chooser to select a filename to
     * display. Overridden so that we can use our overridden
     * makeImageFileChooser.
     */
    public void open() {
        if ( fileChooser == null ) {
            fileChooser = makeImageFileChooser();
            setFileChooser( fileChooser );
        }
        super.open();
    }
    private JFileChooser fileChooser = null;

    /**
     * Create and return a new file chooser to be used to select an image file
     * to display. Overridden to add HDS and HDX file types and use a
     * chooser that sometimes works with windows shortcuts.
     */
    public static JFileChooser makeImageFileChooser() 
    {
         JFileChooser plainChooser =
             NavigatorImageDisplay.makeImageFileChooser();

         FileFilter currentFilter = plainChooser.getFileFilter();
         FileFilter defaultFilters[] = plainChooser.getChoosableFileFilters();

         BasicFileChooser chooser = new BasicFileChooser();
         for ( int i = 0; i < defaultFilters.length; i++ ) {
             chooser.setFileFilter( defaultFilters[i] );
         }

         BasicFileFilter hdsFilter =
             new BasicFileFilter( "sdf", "HDS container files" );
         chooser.addChoosableFileFilter( hdsFilter );

         BasicFileFilter hdxFilter =
             new BasicFileFilter( "xml", "HDX/NDX XML files");
         chooser.addChoosableFileFilter( hdxFilter );

         //  Restore the default filter.
         chooser.setFileFilter( currentFilter );

         return chooser;
     }

    /**
     * Set if the photometry action should be enabled.
     */
    public static void setPhotomEnabled( boolean photomEnabled )
    {
        SOGNavigatorImageDisplay.photomEnabled = photomEnabled;
    }


}
