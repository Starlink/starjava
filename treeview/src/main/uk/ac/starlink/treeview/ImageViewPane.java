package uk.ac.starlink.treeview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.IOException;
import javax.media.jai.PlanarImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import jsky.image.ImageProcessor;
import jsky.image.gui.ImageDisplay;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;

/**
 * This is the business end of an ImageViewer - it displays
 * an image in an unadorned window, and generates a corresponding 
 * Plot object if WCS info is supplied.
 * <p>
 * You can choose whether the display is updated asynchronously or not. 
 * If so, display updates are done in a thread out of the event
 * dispatcher and the display is updated tile by tile.  This looks kind
 * of messy but you can see what's going on.   If not, the GUI can
 * hang while the image is calculated.  In general you should go for
 * async updates if the calculation time may be long.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ImageViewPane extends JPanel {

    private Plot plot;
    private JPanel plotPanel;
    private boolean doPlot = true;

    /**
     * Construct an image view from an NDArray.
     *
     * @param  nda   a 2-dimensional readable NDArray with random access.
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     * @param  async   true if the display may be updated asynchronously
     * @throws  IOException  if there is an error in data access
     */
    public ImageViewPane( NDArray nda, FrameSet wcs,
                          boolean async ) throws IOException {
        this( new NDArrayImage( nda ), nda.getShape().getOrigin(),
              nda.getBadHandler().getBadValue(), wcs, async );
    }

    /**
     * Construct an image view given a RenderedImage.  It may have a
     * non-zero origin and which optionally displays coordinate grids.
     *
     * @param  im      a RenderedImage on which to base the display
     * @param  origin  a 2-element array giving the pixel coords of the
     *                 data origin
     * @param  badval  magic bad value as a Number
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     * @param  async   true if the display may be updated asynchronously
     */
    private ImageViewPane( RenderedImage im, long[] origin, Number badValue,
                           FrameSet wcs, boolean async ) {

        /* Get the bad value as a double. */
        final double badval = ( badValue == null ) ? Double.NaN
                                                   : badValue.doubleValue();

        /* Turn it into a PlanarImage and do more setup. */
        PlanarImage pim = PlanarImage.wrapRenderedImage( im );
        Dimension picsize = new Dimension( pim.getWidth(), pim.getHeight() );
        final ImageDisplay disp = async ? new AsynchronousImageDisplay()
                                        : new ImageDisplay();

        disp.setPrescaled( true );
        disp.setImmediateMode( true );
        disp.setPreferredSize( picsize );
        disp.setOpaque( false );

        /* Arrange coordinate plotting if necessary. */
        JComponent mainPanel;
        if ( wcs != null ) {

            /* Create a Plot with enough space round it to do axis labelling. */
            int lgap = 70;
            int rgap = 20;
            int tgap = 40;
            int bgap = 50;
            Dimension holdersize =
                new Dimension( pim.getWidth() + lgap + rgap,
                               pim.getHeight() + tgap + bgap );
            Rectangle outRect = new Rectangle( holdersize );
            Rectangle inRect = new Rectangle( lgap, tgap,
                                              pim.getWidth(), pim.getHeight() );
            double[] basebox = new double[ 4 ];
            basebox[ 0 ] = 0.5;
            basebox[ 1 ] = 0.5;
            basebox[ 2 ] = 0.5 + pim.getWidth();
            basebox[ 3 ] = 0.5 + pim.getHeight();
            plot = new Plot( wcs, outRect, basebox, lgap, rgap, bgap, tgap );

            /* Configure the Plot. */
            plot.setGrid( true );
            plot.setColour( "grid", 0x8000ff00 );
            plot.setColour( "ticks", Color.GREEN.getRGB() );
            plot.setColour( "textlab", Color.BLACK.getRGB() );
            plot.grid();

            /* Make a panel which will cope with redrawing it as required. */
            plotPanel = new JPanel() {
                protected void paintComponent( Graphics g ) {
                    super.paintComponent( g );
                    plot.paint( g );
                }
            };
            plotPanel.setOpaque( false );

            /* Put both the image display and the plot panel into a new
             * conainer which has no LayoutManager, doing placement manually
             * so they line up properly (tried to do this with an OverlayLayout
             * but couldn't get it to work). */
            /* There is a problem with placing the Plot window over the
             * image display window: since the image display window is 
             * underneath its choice of cursor is not shown by the AWT.
             * I don't know how to get round this, though some complicated
             * fiddling with glassPanes might help. */
            JPanel holder = new JPanel();
            holder.setOpaque( false );
            holder.setLayout( null );
            holder.setPreferredSize( holdersize );
            holder.add( plotPanel );
            holder.add( disp );
            plotPanel.setBounds( outRect );
            disp.setBounds( inRect );
            mainPanel = holder;
        }

        /* No coordinates - just stick the PlanarImage into this panel. */
        else {
            disp.setBorder( BorderFactory.createLineBorder( Color.BLACK, 1 ) );
            mainPanel = disp;
        }
        add( mainPanel );

        /* Configure the image display panel. */
        // This part should be done out of the Event Dispatcher thread
        // really, but whenever I try to put it in a SwingWorker it ends
        // up failing to set the data properly (get a blank screen).
        int tx = Math.min( pim.getTileWidth(), pim.getWidth() );
        int ty = Math.min( pim.getTileHeight(), pim.getHeight() );
        Rectangle2D.Double sample = new Rectangle2D.Double( 0.0, 0.0, tx, ty );
        final ImageProcessor ip = new ImageProcessor( pim, sample ) {
            {
                setBlank( badval );
            }
        };
        ip.setReverseY( true );
        boolean done = false;
        for ( int ix = 0; ix < pim.getWidth() / tx && ! done; ix++ ) {
            for ( int iy = 0; iy < pim.getHeight() / ty && ! done; iy++ ) {
                Rectangle2D.Double samp =
                    new Rectangle2D.Double( (double) ix * tx, (double) iy * ty,
                                            tx, ty );
                ip.autoSetCutLevels( 98.0, samp );
                if ( ip.getLowCut() < ip.getHighCut() ) {
                    done = true;
                }
            }
        }
        ip.update();
        disp.setImageProcessor( ip );
    }

    /**
     * Returns the Plot object which is used to plot a grid over the
     * image.  May be null if no WCS was supplied.
     *
     * @return plot the plot
     */
    public Plot getPlot() {
        return plot;
    }

    /**
     * Determines whether the plot graphics will actually be written over
     * the image at the next rePlot call.
     *
     * @param  doPlot  whether to draw anything
     */
    public void setDoPlot( boolean doPlot ) {
        this.doPlot = doPlot;
    }

    /**
     * Redraws the Plot object, taking into account any reconfiguration that
     * has been done on it.
     */
    public void rePlot() {
        plot.clear();
        if ( doPlot ) {
            plot.grid();
        }
        plotPanel.repaint();
    }
}
