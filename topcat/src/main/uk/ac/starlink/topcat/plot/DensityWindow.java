package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.filechooser.FileFilter;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.SuffixFileFilter;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.func.Times;

/**
 * Graphics window which displays a density plot, that is a 2-dimensional
 * histogram.  Each screen pixel corresponds to a bin of the 2-d histogram,
 * and is coloured according to how many items fall into it.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2005
 */
public class DensityWindow extends GraphicsWindow {

    private final DensityPlot plot_;
    private final BlobPanel blobPanel_;
    private final Action blobAction_;
    private final CountsLabel plotStatus_;
    private final ToggleButtonModel rgbModel_;
    private final CutChooser cutter_;
    private final PixelSizeAction pixIncAction_;
    private final PixelSizeAction pixDecAction_;
    private final Action fitsAction_;
    private int pixelSize_ = 1;

    private static FileFilter fitsFilter_ =
        new SuffixFileFilter( new String[] { ".fits", ".fit", ".fts", } );
    private static FileFilter jpegFilter_ =
        new SuffixFileFilter( new String[] { ".jpeg", ".jpg", } );

    /**
     * Constructs a new DensityWindow.
     *
     * @param   parent   parent component (may be used for positioning)
     */
    public DensityWindow( Component parent ) {
        super( "Density Plot", new String[] { "X", "Y" }, parent );

        /* Construct a plotting surface to receive the graphics. */
        final PlotSurface surface = new PtPlotSurface( this );
        ((PtPlotSurface) surface).setPadPixels( 0 );

        /* No grid.  There are currently problems with displaying it
         * over the top of the plot. */
        getGridModel().setSelected( false );

        /* Construct and populate the plot panel with the 2d histogram
         * itself and a transparent layer for doodling blobs on. */
        plot_ = new DensityPlot( surface ) {
            protected void reportCounts( int nPoint, int nInc, int nVis ) {
                plotStatus_.setValues( new int[] { nPoint, nInc, nVis } );
            }
        };
        JPanel plotPanel = new JPanel();
        blobPanel_ = new BlobPanel() {
            protected void blobCompleted( Shape blob ) {
                addNewSubsets( plot_.getContainedMask( blob ) );
            }
        };
        blobPanel_.setColors( new Color( 0x80a0a0a0, true ),
                              new Color( 0xc0a0a0a0, true ) );
        blobAction_ = blobPanel_.getBlobAction();
        plotPanel.setLayout( new OverlayLayout( plotPanel ) );
        plotPanel.add( blobPanel_ );
        plotPanel.add( plot_ );
        getMainArea().add( plotPanel, BorderLayout.CENTER );

        /* Construct and add a status line. */
        plotStatus_ = new CountsLabel( new String[] {
            "Potential", "Included", "Visible",
        } );
        PositionLabel posStatus = new PositionLabel( surface );
        posStatus.setMaximumSize( new Dimension( Integer.MAX_VALUE,
                                                 posStatus.getMaximumSize()
                                                          .height ) );
        getStatusBox().add( plotStatus_ );
        getStatusBox().add( Box.createHorizontalStrut( 5 ) );
        getStatusBox().add( posStatus );

        /* Action for resizing the plot. */
        Action resizeAction = new BasicAction( "Rescale", ResourceIcon.RESIZE,
                                               "Rescale the plot to show " +
                                               "all data" ) {
            public void actionPerformed( ActionEvent evt ) {
                plot_.rescale();
                forceReplot();
            }
        };

        /* Action for rgb/greyscale toggle. */
        rgbModel_ = new ToggleButtonModel( "Colour", ResourceIcon.COLOR,
                                           "Select red/green/blue or " +
                                           "greyscale rendering" );
        rgbModel_.setSelected( true );
        rgbModel_.addActionListener( getReplotListener() );

        /* Actions for altering pixel size. */
        pixIncAction_ =
            new PixelSizeAction( "Bigger Pixels", ResourceIcon.ROUGH,
                                 "Increase number of screen pixels per bin",
                                 +1 );
        pixDecAction_ =
            new PixelSizeAction( "Smaller Pixels", ResourceIcon.FINE,
                                 "Decrease number of screen pixels per bin",
                                 -1 );

        /* Action for exporting image. */
        fitsAction_ = new ExportAction( "FITS", ResourceIcon.FITS, 
                                        "Save image as FITS array",
                                        fitsFilter_ ) {
            public void exportTo( OutputStream out ) throws IOException {
                try {
                    exportFits( out );
                }
                catch ( FitsException e ) {
                    throw (IOException) new IOException( e.getMessage() )
                                       .initCause( e );
                }
            }
        };
        Action jpegAction = new ImageIOExportAction( "JPEG", jpegFilter_ );
        getExportMenu().add( fitsAction_ );
        getExportMenu().add( jpegAction );

        /* Cut level adjuster widgets. */
        cutter_ = new CutChooser(); 
        cutter_.setLowValue( 0.1 );
        cutter_.setHighValue( 0.9 );
        cutter_.setBorder( makeTitledBorder( "Cut Percentile Levels" ) );
        cutter_.addChangeListener( getReplotListener() );
        getMainArea().add( cutter_, BorderLayout.SOUTH );

        /* General plot operation menu. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( resizeAction );
        plotMenu.add( getReplotAction() );
        getJMenuBar().add( plotMenu );

        /* Axis operation menu. */
        JMenu axisMenu = new JMenu( "Axes" );
        axisMenu.setMnemonic( KeyEvent.VK_A );
        axisMenu.add( getFlipModels()[ 0 ].createMenuItem() );
        axisMenu.add( getFlipModels()[ 1 ].createMenuItem() );
        axisMenu.addSeparator();
        axisMenu.add( getLogModels()[ 0 ].createMenuItem() );
        axisMenu.add( getLogModels()[ 1 ].createMenuItem() );
        getJMenuBar().add( axisMenu );

        /* View menu. */
        JMenu viewMenu = new JMenu( "View" );
        viewMenu.setMnemonic( KeyEvent.VK_V );
        axisMenu.add( rgbModel_.createMenuItem() );
        axisMenu.add( pixIncAction_ );
        axisMenu.add( pixDecAction_ );

        /* Subset operation menu. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        Action fromVisibleAction = new BasicAction( "New subset from visible",
                                                    ResourceIcon.VISIBLE_SUBSET,
                                                    "Define a new row subset " +
                                                    "containing only " +
                                                    "currently visible data" ) {
            public void actionPerformed( ActionEvent evt ) {
                addNewSubsets( plot_.getVisibleMask() );
            }
        };
        subsetMenu.add( blobAction_ );
        subsetMenu.add( fromVisibleAction );
        getJMenuBar().add( subsetMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( resizeAction );
        getToolBar().add( getFlipModels()[ 0 ].createToolbarButton() );
        getToolBar().add( getFlipModels()[ 1 ].createToolbarButton() );
        getToolBar().add( getLogModels()[ 0 ].createToolbarButton() );
        getToolBar().add( getLogModels()[ 1 ].createToolbarButton() );
        getToolBar().add( getReplotAction() );
        getToolBar().add( rgbModel_.createToolbarButton() );
        getToolBar().add( pixIncAction_ );
        getToolBar().add( pixDecAction_ );
        getToolBar().add( blobAction_ );
        getToolBar().add( fromVisibleAction );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "DensityWindow" );

        /* Perform an initial plot. */
        replot();

        /* Render this component visible. */
        pack();
        setVisible( true );
    }

    protected JComponent getPlot() {
        return plot_;
    }

    protected PlotState createPlotState() {
        return new DensityPlotState();
    }

    public PlotState getPlotState() {
        DensityPlotState state = (DensityPlotState) super.getPlotState();

        rgbModel_.setEnabled( state.getValid() &&
                              state.getPointSelection()
                                   .getStyles().length <= 3 );
        state.setRgb( rgbModel_.isEnabled() && rgbModel_.isSelected() );

        state.setLoCut( cutter_.getLowValue() );
        state.setHiCut( cutter_.getHighValue() );
        state.setPixelSize( pixelSize_ );
        pixIncAction_.configureEnabledness();
        pixDecAction_.configureEnabledness();

        /* Manipulate the style choices directly.  The default handling done
         * in the superclass uses a pool of styles which is shared around
         * between the plotted subsets, new ones being assigned as required.
         * This isn't suitable here, since there are only 4 possible ones;
         * if we've got three or less styles we can use red, green, blue,
         * otherwise we can only use a single greyscale set. */
        if ( state.getValid() ) {
            Style[] styles = state.getPointSelection().getStyles();
            assert styles == state.getPointSelection()
                            .getStyles();  // check we're not getting a clone
            boolean rgb = state.getRgb() && styles.length <= 3;
            fitsAction_.setEnabled( ! rgb || styles.length == 1 );
            for ( int is = 0; is < styles.length; is++ ) {
                styles[ is ] = rgb ? DensityStyle.RGB.getStyle( is )
                                   : DensityStyle.WHITE;
            }
        }
        return state;
    }

    protected void doReplot( PlotState state, Points points ) {

        /* Cancel any current blob drawing. */
        blobPanel_.setActive( false );

        /* Send the plot component the most up to date plotting state. */
        PlotState lastState = plot_.getState();
        plot_.setPoints( points );
        plot_.setState( state );

        /* If the axes are different from the last time we plotted, 
         * rescale so that all the points are visible. */
        if ( ! state.sameAxes( lastState ) || ! state.sameData( lastState ) ) {
            plotStatus_.setValues( null );
            plot_.rescale();
        }

        /* Schedule for repainting so changes can take effect. */
        plot_.repaint();
    }

    public StyleSet getDefaultStyles( int npoint ) {
        return ((DensityPlotState) getPlotState()).getRgb() ? DensityStyle.RGB
                                                            : DensityStyle.MONO;
    }

    private void exportFits( OutputStream ostrm )
            throws IOException, FitsException {
        final DataOutputStream out = new DataOutputStream( ostrm );

        BinGrid grid = plot_.getBinnedData()[ 0 ];
        int max = grid.getMaxCount();
        int bitpix;
        abstract class IntWriter {
            abstract void writeInt( int value ) throws IOException;
            abstract int size();
        };
        IntWriter intWriter;
        if ( max < Math.pow( 2, 7 ) ) {
            bitpix = BasicHDU.BITPIX_BYTE;
            intWriter = new IntWriter() {
                void writeInt( int value ) throws IOException {
                    out.writeByte( value );
                }
                int size() {
                    return 1;
                }
            };
        }
        else if ( max < Math.pow( 2, 15 ) ) {
            bitpix = BasicHDU.BITPIX_SHORT;
            intWriter = new IntWriter() {
                void writeInt( int value ) throws IOException {
                    out.writeShort( value );
                }
                int size() {
                    return 2;
                }
            };
        }
        else {
            bitpix = BasicHDU.BITPIX_INT;
            intWriter = new IntWriter() {
                void writeInt( int value ) throws IOException {
                    out.writeInt( value );
                }
                int size() {
                    return 4;
                }
            };
        }

        int nx = grid.getSizeX();
        int ny = grid.getSizeY();
        DensityPlotState state = (DensityPlotState) plot_.getState();
        int psize = state.getPixelSize();
        ValueInfo[] axes = state.getAxes();
        PlotSurface surface = plot_.getSurface();
        Rectangle bbox = surface.getClip().getBounds();
        int x0 = bbox.x;
        int y0 = bbox.y;
        double[] p0 = surface.graphicsToData( x0, y0, false );
        double[] p1 = surface.graphicsToData( x0 + psize, y0 + psize, false );
        Header hdr = new Header();
        hdr.addValue( "SIMPLE", true, "" );
        hdr.addValue( "BITPIX", bitpix, "Data type" );
        hdr.addValue( "NAXIS", 2, "Number of axes" );
        hdr.addValue( "NAXIS1", grid.getSizeX(), "X dimension" );
        hdr.addValue( "NAXIS2", grid.getSizeY(), "Y dimension" );
        hdr.addValue( "DATE", Times.mjdToIso( Times.unixMillisToMjd( 
                                           System.currentTimeMillis() ) ),
                      "HDU creation date" );
        hdr.addValue( "CTYPE1", axes[ 0 ].getName(),
                                axes[ 0 ].getDescription() );
        hdr.addValue( "CTYPE2", axes[ 1 ].getName(),
                                axes[ 1 ].getDescription() );
        hdr.addValue( "BUNIT", "COUNTS", "Number of points per pixel (bin)" );
        hdr.addValue( "DATAMIN", 0.0, "Minimum value" );
        hdr.addValue( "DATAMAX", (double) grid.getMaxCount(), "Maximum value" );
        hdr.addValue( "CRPIX1", 0.0, "Reference pixel X index" );
        hdr.addValue( "CRPIX2", (double) ny, "Reference pixel Y index" );
        hdr.addValue( "CRVAL1", p0[ 0 ], "Reference pixel X position" );
        hdr.addValue( "CRVAL2", p0[ 1 ], "Reference pixel Y position" );
        hdr.addValue( "CDELT1", p1[ 0 ] - p0[ 0 ],
                                "X extent of reference pixel" );
        hdr.addValue( "CDELT2", p0[ 1 ] - p1[ 1 ],
                                "Y extent of reference pixel" );
        hdr.addValue( "ORIGIN", "TOPCAT " + TopcatUtils.getVersion() + 
                      " (" + getClass().getName() + ")", null );
        FitsConstants.writeHeader( out, hdr );

        int[] data = grid.getCounts();
        for ( int iy = 0; iy < ny; iy++ ) {
            int yoff = ( ny - 1 - iy ) * nx;
            for ( int ix = 0; ix < nx; ix++ ) {
                intWriter.writeInt( data[ yoff + ix ] );
            }
        }
        int nbyte = nx * ny * intWriter.size();
        int over = nbyte % FitsConstants.FITS_BLOCK;
        if ( over > 0 ) {
            out.write( new byte[ FitsConstants.FITS_BLOCK - over ] );
        }
        out.flush();
    }

    /**
     * Action for incrementing the grid pixel size.
     */
    private class PixelSizeAction extends BasicAction {
        final int MAX_SIZE = 20;
        final int MIN_SIZE = 1;
        final int inc_;

        /**
         * Constructs a new PixelSizeAction.
         *
         * @param  name  action name
         * @param  icon  action icon
         * @param  desc  short description (tool tip)
         * @param  inc   amount to increment the pixsize when the action is
         *               invoked
         */
        PixelSizeAction( String name, Icon icon, String desc, int inc ) {
            super( name, icon, desc );
            inc_ = inc;
        }

        public void actionPerformed( ActionEvent evt ) {
            pixelSize_ = Math.min( Math.max( pixelSize_ + inc_, MIN_SIZE ),
                                             MAX_SIZE );
            configureEnabledness();
            replot();
        }

        /**
         * Configures this action according to whether it would have any
         * effect or not.
         */
        void configureEnabledness() {
            setEnabled( pixelSize_ + inc_ >= MIN_SIZE &&
                        pixelSize_ + inc_ <= MAX_SIZE );
        }
    }
}
