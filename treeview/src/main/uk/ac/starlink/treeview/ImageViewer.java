package uk.ac.starlink.treeview;

import java.nio.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import javax.swing.*;
import javax.media.jai.PlanarImage;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.hdx.array.NDArray;
import jsky.image.ImageProcessor;
import jsky.image.gui.ImageDisplay;

/**
 * Displays the pixels of a 2-d array, optionally with an AST grid plotted
 * over the top.
 * There is currently no provision for scaling, modifying the colour map,
 * or anything else flashy.
 */
class ImageViewer extends JPanel {

    /**
     * Construct an image view from an NIO buffer without coordinate grids.
     *
     * @param  niobuf  a buffer containing the data
     * @param  shape   the shape of the data to be displayed - must be 2-d
     */
    public ImageViewer( Buffer niobuf, Cartesian shape ) {
        this( niobuf, shape, null, null );
    }
  
    /**
     * Construct an image view which may have a non-zero origin and optionally
     * displays coordinate grids.
     *
     * @param  niobuf  a buffer containing the array data
     * @param  shape   the shape of the data to be displayed - must be 2-d
     * @param  origin  a 2-element array giving the pixel coords of the 
     *                 data origin
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     */
    public ImageViewer( Buffer niobuf, Cartesian shape, long[] origin,
                        FrameSet wcs ) {
        this( niobuf, shape, origin, wcs, 0, niobuf.limit() );
    }

    /**
     * Construct an image view from part of an nio buffer; it may have
     * a non-zero origin and optionall displays coordinate grids.
     *
     * @param  niobuf  a buffer containing the array data
     * @param  shape   the shape of the data to be displayed - must be 2-d
     * @param  origin  a 2-element array giving the pixel coords of the 
     *                 data origin
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     * @param  start   the first element of the buffer to use
     * @param  size    the number of elements of the buffer to use
     */
    public ImageViewer( Buffer niobuf, Cartesian shape, long[] origin, 
                        FrameSet wcs, int start, int size ) {
        this( new NioImage( niobuf, new int[] { (int) shape.getCoord( 0 ),
                                                (int) shape.getCoord( 1 ) },
                            start, size ),
              origin, wcs );
    }

    /**
     * Construct an image view from an NDArray.
     *
     * @param  nda   a 2-dimensional readable NDArray with random access.
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     */
    public ImageViewer( NDArray nda, FrameSet wcs ) {
        this( new NDArrayImage( nda ), nda.getShape().getOrigin(), wcs );
    }


    /**
     * Construct an image view given a RenderedImage.  It may have a 
     * non-zero origin and which optionally displays coordinate grids. 
     *
     * @param  im      a RenderedImage on which to base the display
     * @param  origin  a 2-element array giving the pixel coords of the 
     *                 data origin
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     */
    private ImageViewer( RenderedImage im, long[] origin, FrameSet wcs ) {

        /* Turn it into a PlanarImage and do more setup. */
        PlanarImage pim = PlanarImage.wrapRenderedImage( im );
        Dimension picsize = new Dimension( pim.getWidth(), pim.getHeight() );
        final ImageDisplay disp = new ImageDisplay();
        disp.setPrescaled( true );
        disp.setImmediateMode( true );
        disp.setPreferredSize( picsize );

        /* Arrange coordinate plotting if necessary. */
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
            basebox[ 0 ] = origin[ 0 ] + 0.5;
            basebox[ 1 ] = origin[ 1 ] + 0.5;
            basebox[ 2 ] = origin[ 0 ] + 0.5 + pim.getWidth();
            basebox[ 3 ] = origin[ 1 ] + 0.5 + pim.getHeight();
            final Plot plot = new Plot( wcs, outRect, basebox, 
                                        lgap, rgap, bgap, tgap );

            /* Configure the Plot. */
            plot.setGrid( true );
            plot.setColour( "grid", 0x8000ff00 );
            plot.setColour( "ticks", Color.GREEN.getRGB() );
            plot.setColour( "text", Color.BLACK.getRGB() );
            plot.grid();

            /* Make a panel which will cope with redrawing it as required. */
            JPanel plotpan = new JPanel() {
                protected void paintComponent( Graphics g ) {
                    super.paintComponent( g );
                    plot.paint( g );
                }
            };
            plotpan.setOpaque( false );

            /* Put both the image display and the plot panel into a new 
             * conainer which has no LayoutManager, doing placement manually
             * so they line up properly (tried to do this with an OverlayLayout
             * but couldn't get it to work). */
            JPanel holder = new JPanel();
            holder.setLayout( null );
            holder.setPreferredSize( holdersize );
            holder.add( plotpan );
            holder.add( disp );
            plotpan.setBounds( outRect );
            disp.setBounds( inRect );

            /* Add the whole lot to this panel. */
            this.add( holder );
        }

        /* No coordinates - just stick the PlanarImage into this panel. */
        else {
            this.add( disp );
        }

        /* Configure the image display panel. */
        // This part should be done out of the Event Dispatcher thread
        // really, but whenever I try to put it in a SwingWorker it ends
        // up failing to set the data properly (get a blank screen).
        int tx = Math.min( pim.getTileWidth(), pim.getWidth() );
        int ty = Math.min( pim.getTileHeight(), pim.getHeight() );
        Rectangle2D.Double sample = new Rectangle2D.Double( 0.0, 0.0, tx, ty );
        final ImageProcessor ip = new ImageProcessor( pim, sample );
        ip.setReverseY( true );
        boolean done = false;
        for ( int ix = 0; ix < pim.getWidth() / tx && ! done; ix++ ) {
            for ( int iy = 0; iy < pim.getHeight() / ty && ! done; iy++ ) {
                Rectangle2D.Double samp = 
                    new Rectangle2D.Double( (double) ix * tx,
                                            (double) iy * tx, tx, ty );
                try {
                    ip.autoSetCutLevels( 95.0, samp );
                    if ( ip.getLowCut() < ip.getHighCut() ) {
                        done = true;
                    }
                }
                catch ( ArrayIndexOutOfBoundsException e ) {
                    // this happens when there are no good pixels
                    // - I think.  Ignore it and progress to the
                    // next tile
                }
            }
        }
        ip.update();
        disp.setImageProcessor( ip );
    }

} 
