package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter for spectrograms.
 * A spectrogram is a spectrum represented as a vertical line of pixels
 * at successive positions in a time series (time axis horizontal,
 * spectral axis vertical).
 *
 * @author   Mark Taylor
 * @since    16 Jul 2013
 */
public class SpectrogramPlotter
        implements Plotter<SpectrogramPlotter.SpectroStyle> {

    private final FloatingCoord xCoord_;
    private final FloatingCoord xExtentCoord_;
    private final FloatingArrayCoord spectrumCoord_;
    private final SliceDataGeom spectroDataGeom_;
    private final int icX_;
    private final int icExtent_;
    private final int icSpectrum_;

    private static final AuxScale SPECTRO_SCALE = new AuxScale( "Spectral" );
    private static final ChannelGrid DEFAULT_CHANGRID =
        new AssumedChannelGrid();
    private static final int MAX_SAMPLE = 100;

    /**
     * Constructor.
     *
     * @param  xCoord  horizontal axis coordinate
     */
    public SpectrogramPlotter( FloatingCoord xCoord ) {
        xCoord_ = xCoord;

        /* Spectral coordinate, containing the spectrum at each X position. */
        spectrumCoord_ =
            FloatingArrayCoord.createCoord( "Spectrum",
                                            "Array of spectrum channel values",
                                            true );

        /* X extent is the width of each spectrum.  If not supplied,
         * an attempt is made to determine it automatically by looking at
         * the separations of the X coordinates. */
        xExtentCoord_ = 
            FloatingCoord.createCoord( xCoord.getUserInfo().getName() + "Width",
                                       "Extent of samples in X direction",
                                       false );

        /* Maps each row to an X position, but not to a point, since it
         * covers a vertical line. */
        spectroDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "Time" );

        /* Record which coordinate is where in the tuples. */
        int icol = 0;
        icX_ = icol++;
        icSpectrum_ = icol++;
        icExtent_ = icol++;
    }

    /**
     * Returns the coordinate index for the spectral coordinate.
     *
     * @return  spectrum coordinate index
     */
    public int getSpectrumCoordIndex() {
        return icSpectrum_;
    }

    /**
     * Returns the coordinate index for the time extent coordinate.
     *
     * @return  time extent coordinate index
     */
    public int getExtentCoordIndex() {
        return icExtent_;
    }

    public String getPlotterName() {
        return "Spectrogram";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.PLOT_SPECTRO;
    }

    /**
     * Returns false, since rows do not correspond to a point-like position.
     */
    public int getPositionCount() {
        return 0;
    }

    public Coord[] getExtraCoords() {
        return new Coord[] {
            xCoord_,
            spectrumCoord_,
            xExtentCoord_,
        };
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            StyleKeys.AUX_SHADER,
            StyleKeys.SHADE_LOG,
            StyleKeys.SHADE_FLIP,
            StyleKeys.SHADE_NULL_COLOR,
        };
    }

    public SpectroStyle createStyle( ConfigMap config ) {
        Shader shader = config.get( StyleKeys.AUX_SHADER );
        boolean shadeLog = config.get( StyleKeys.SHADE_LOG );
        boolean shadeFlip = config.get( StyleKeys.SHADE_FLIP );
        Color nullColor = config.get( StyleKeys.SHADE_NULL_COLOR );
        ChannelGrid grid = DEFAULT_CHANGRID;
        return new SpectroStyle( shader, shadeLog, shadeFlip, nullColor, grid );
    }

    /**
     * The supplied <code>geom</code> is ignored.
     */
    public PlotLayer createLayer( DataGeom geom, final DataSpec dataSpec,
                                  final SpectroStyle style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        else {
            LayerOpt layerOpt = LayerOpt.OPAQUE;
            return new AbstractPlotLayer( this, spectroDataGeom_, dataSpec,
                                          style, layerOpt ) {
                public Drawing createDrawing( final Surface surface,
                                              Map<AuxScale,Range> auxRanges,
                                              final PaperType paperType ) {
                    final Range spectroRange = auxRanges.get( SPECTRO_SCALE );
                    return new UnplannedDrawing() {
                        protected void paintData( Paper paper,
                                                  final DataStore dataStore ) {
                            paperType.placeDecal( paper, new Decal() {
                                public void paintDecal( Graphics g ) {
                                    paintSpectrogram( surface, dataStore,
                                                      dataSpec, style,
                                                      spectroRange, g );
                                }
                                public boolean isOpaque() {
                                    return true;
                                }
                            } );
                        }
                    };
                }
                @Override
                public Map<AuxScale,AuxReader> getAuxRangers() {
                    Map<AuxScale,AuxReader> map =
                        new HashMap<AuxScale,AuxReader>();
                    map.put( SPECTRO_SCALE, new AuxReader() {
                        public void updateAuxRange( Surface surface,
                                                    TupleSequence tseq,
                                                    Range range ) {
                            double[] spectrum =
                                spectrumCoord_
                               .readArrayCoord( tseq, icSpectrum_ );
                            int nchan = spectrum.length;
                            for ( int ic = 0; ic < nchan; ic++ ) {
                                range.submit( spectrum[ ic ] );
                            }
                        }
                    } );
                    return map;
                }
                @Override
                public void extendCoordinateRanges( Range[] ranges,
                                                    DataStore dataStore ) {
                    Range specRange = ranges[ 1 ];
                    ChannelGrid grid = style.grid_;
                    int nchan = grid.getChannelCount();
                    if ( nchan < 0 ) {
                        TupleSequence tseq =
                            dataStore.getTupleSequence( dataSpec );
                        while ( tseq.next() ) {
                            int nc = spectrumCoord_
                                    .getArrayCoordLength( tseq, icSpectrum_ );
                            nchan = Math.max( nc, nchan );
                        }
                    }
                    double[] bounds = new double[ 2 ];
                    for ( int ic = 0; ic < nchan; ic++ ) {
                        grid.getChannelBounds( ic, bounds );
                        specRange.submit( bounds[ 0 ] );
                        specRange.submit( bounds[ 1 ] );
                    }
                }
            };
        }
    }

    /**
     * Does the drawing.
     *
     * @param   surface  plot surface
     * @param   dataStore  data repository
     * @param   dataSpec  data specifier
     * @param   style   spectrogram style
     * @param   spectroRange   the range of spectral values
     * @param   g   output graphics context
     */
    private void paintSpectrogram( Surface surface, DataStore dataStore,
                                   DataSpec dataSpec, SpectroStyle style,
                                   Range spectroRange, Graphics g ) {
        ChannelGrid grid = style.grid_;
        Shader shader = style.shader_;
        RangeScaler specScaler =
            RangeScaler.createScaler( style.shadeLog_, style.shadeFlip_,
                                      spectroRange );

        /* Work out the data bounds of the plotting surface. */
        Rectangle plotBounds = surface.getPlotBounds();
        Point gCorner0 = new Point( plotBounds.x, plotBounds.y );
        Point gCorner3 = new Point( plotBounds.x + plotBounds.width,
                                    plotBounds.y + plotBounds.height );
        double[] dCorner0 = surface.graphicsToData( gCorner0, null );
        double[] dCorner3 = surface.graphicsToData( gCorner3, null );
        double dxmin = Math.min( dCorner0[ 0 ], dCorner3[ 0 ] );
        double dxmax = Math.max( dCorner0[ 0 ], dCorner3[ 0 ] );
        double dymin = Math.min( dCorner0[ 1 ], dCorner3[ 1 ] );
        double dymax = Math.max( dCorner0[ 1 ], dCorner3[ 1 ] );

        /* Get the default width of each X value by acquiring some kind of
         * median of sample separations.  This will only be used if the
         * extent parameter has missing values. */
        TupleSequence tseq0 = dataStore.getTupleSequence( dataSpec );
        double[] diffs = new double[ MAX_SAMPLE ];
        int idiff = 0;
        double lastx = Double.NaN;
        while ( tseq0.next() && idiff < MAX_SAMPLE ) {
            double x = xCoord_.readDoubleCoord( tseq0, icX_ );
            if ( ! Double.isNaN( x ) ) {
                if ( ! Double.isNaN( lastx ) ) {
                    diffs[ idiff++ ] = x - lastx;
                }
                lastx = x;
            }
        }
        if ( idiff == 0 ) {
            return;
        }
        Arrays.sort( diffs );
        double median = diffs[ idiff / 2 ];
        double dfltExtent = Double.isNaN( median ) ? 1 : median;

        /* Work out which channels are visible. */
        int[] chanRange = grid.getChannelRange( dymin, dymax );
        int ichanLo = chanRange[ 0 ];
        int ichanHi = chanRange[ 1 ];

        /* Draw pixels. */
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        double[] dxs = new double[ 2 ];
        Point gp0 = new Point();
        Point gp3 = new Point();
        double[] dpos0 = new double[ 2 ];
        double[] dpos3 = new double[ 2 ];
        double[] chanBounds = new double[ 2 ];
        float[] rgba = new float[ 4 ];
        Color color0 = g.getColor();
        while ( tseq.next() ) {
            double x = xCoord_.readDoubleCoord( tseq, icX_ );
            if ( ! Double.isNaN( x ) ) {
                double xExtent = xExtentCoord_
                                .readDoubleCoord( tseq, icExtent_ );
                if ( Double.isNaN( xExtent ) ) {
                    xExtent = dfltExtent;
                }
                double xlo = x;
                double xhi = x + xExtent;
                if ( xhi > dxmin || xlo < dxmax ) {
                    dpos0[ 0 ] = xlo;
                    dpos3[ 0 ] = xhi;
                    double[] chanVector =
                        spectrumCoord_.readArrayCoord( tseq, icSpectrum_ );
                    int nchan = Math.min( chanVector.length, ichanHi );
                    for ( int ic = ichanLo; ic < nchan; ic++ ) {
                        grid.getChannelBounds( ic, chanBounds );
                        dpos0[ 1 ] = chanBounds[ 0 ];
                        dpos3[ 1 ] = chanBounds[ 1 ];
                        if ( surface.dataToGraphics( dpos0, false, gp0 ) &&
                             surface.dataToGraphics( dpos3, false, gp3 ) ) {
                            double sval = specScaler.scale( chanVector[ ic ] );

                            /* This could be made more efficient by setting up
                             * a lookup table of colours at the start and
                             * indexing into it rather than creating a new
                             * Color object each time. */
                            shader.adjustRgba( rgba, (float) sval );
                            g.setColor( new Color( rgba[ 0 ], rgba[ 1 ],
                                                   rgba[ 2 ] ) );
                            int x03 = gp3.x - gp0.x;
                            int y03 = gp3.y - gp0.y;
                            final int px;
                            final int pwidth;
                            final int py;
                            final int pheight;
                            if ( x03 > 0 ) {
                                px = gp0.x;
                                pwidth = x03;
                            }
                            else {
                                px = gp3.x;
                                pwidth = -x03;
                            }
                            if ( y03 > 0 ) {
                                py = gp0.y;
                                pheight = y03;
                            }
                            else {
                                py = gp3.y;
                                pheight = -y03;
                            }
                            assert pwidth >= 0;
                            assert pheight >= 0;
                            g.fillRect( px, py, Math.max( pwidth, 1 ),
                                                Math.max( pheight, 1 ) );
                        }
                    }
                }
            }
        }
        g.setColor( color0 );
    }

    /**
     * Defines the spectrum frequency channels.
     * These are assumed the same for every spectrum in the spectrogram
     * (every X coordinate).
     * Each channel has an index (0..channelCount-1) and a lower and
     * upper Y (spectral) axis bound.
     * Typically the upper bound of channel <em>i</em>
     * will be the lower bound of channel <em>i+1</em>, but this is
     * not enforced.
     * It is expected, though not enforced, that channel bound pairs
     * are monotonically increasing with channel index.
     */
    @Equality
    public interface ChannelGrid {

        /**
         * Returns the number of channels if known.
         *
         * @return   number of channels in this grid, or -1 if not known
         */
        int getChannelCount();

        /**
         * Returns the range of channel indices which are completely or
         * partially covered in a given range of Y (spectral) values.
         *
         * @param   ylo  lower bound of Y value
         * @param   yhi  upper bound of Y value
         * @return  2-element array giving (lower, upper+1) index of
         *          channels visible in the given Y range
         */
        int[] getChannelRange( double ylo, double yhi );

        /**
         * Reports the upper and lower Y (spectral) bounds for a given channel.
         *
         * @param   ichan  channel index
         * @param   ybounds  2-element array, on return contains (lower,upper)
         *          bounds of channel on the Y (spectral) axis
         */
        void getChannelBounds( int ichan, double[] ybounds );
    }

    /**
     * ChannelGrid implementation for an indeterminate number of channels
     * each with a Y extent of unity.
     */
    private static class AssumedChannelGrid implements ChannelGrid {
        public int getChannelCount() {
            return -1;
        }
        public int[] getChannelRange( double ylo, double yhi ) {
            return new int[] { Math.max( 0, (int) Math.floor( ylo ) ),
                               (int) Math.ceil( yhi ) };
        }
        public void getChannelBounds( int ichan, double[] bounds ) {
            bounds[ 0 ] = ichan;
            bounds[ 1 ] = ichan + 1;
        }
    }

    /**
     * ChannelGrid implementation populated by explicitly supplied
     * lower and upper bounds for each channel.
     */
    private static class DataChannelGrid implements ChannelGrid {
        private final int count_;
        private final double[] lows_;
        private final double[] highs_;

        /**
         * Constructor.
         *
         * @param   count  number of channels
         * @param   lows   count-element array of channel lower bounds
         * @parma   highs  count-element array of channel upper bounds
         */
        DataChannelGrid( int count, double[] lows, double[] highs ) {
            count_ = count;
            lows_ = lows;
            highs_ = highs;
        }

        public int getChannelCount() {
            return count_;
        }

        public int[] getChannelRange( double ylo, double yhi ) {
            int ilo = count_;
            int ihi = 0;
            for ( int ic = 0; ic < count_; ic++ ) {
                if ( highs_[ ic ] > ylo ) {
                    ilo = Math.min( ilo, ic );
                }
                if ( lows_[ ic ] < yhi ) {
                    ihi = Math.max( ihi, ic + 1 );
                }
            }
            return new int[] { ilo, ihi };
        }

        public void getChannelBounds( int ichan, double[] bounds ) {
            bounds[ 0 ] = lows_[ ichan ];
            bounds[ 1 ] = highs_[ ichan ];
        }

        @Override
        public int hashCode() {
            int code = 5501;
            code = 23 * code + count_;
            code = 23 * code + Arrays.hashCode( lows_ );
            code = 23 * code + Arrays.hashCode( highs_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DataChannelGrid ) {
                DataChannelGrid other = (DataChannelGrid) o;
                return this.count_ == other.count_
                    && Arrays.equals( this.lows_, other.lows_ )
                    && Arrays.equals( this.highs_, other.highs_ );
            }
            else {
                return false;
            }
        }
    }
   
    /**
     * Style subclass for SpectrogramPlotter.
     */
    public static class SpectroStyle implements Style {
        private final Shader shader_;
        private final boolean shadeLog_;
        private final boolean shadeFlip_;
        private final Color nullColor_;
        private final ChannelGrid grid_;

        /**
         * Constructor.
         *
         * @param   shader  shader
         * @param   shadeLog  true for logarithmic shading scale,
         *                    false for linear
         * @param   shadeFlip  true to invert shading scale
         * @param   nullColor  colour to use for blank spectral values
         * @param   grid    channel bounds grid
         */
        public SpectroStyle( Shader shader, boolean shadeLog, boolean shadeFlip,
                             Color nullColor, ChannelGrid grid ) {
            shader_ = shader;
            shadeLog_ = shadeLog;
            shadeFlip_ = shadeFlip;
            nullColor_ = nullColor;
            grid_ = grid;
        }

        public Icon getLegendIcon() {
            return ResourceIcon.PLOT_SPECTRO;
        }

        @Override
        public int hashCode() {
            int code = 9703;
            code = 23 * code + shader_.hashCode();
            code = 23 * code + ( shadeLog_ ? 1 : 3 );
            code = 23 * code + ( shadeFlip_ ? 5 : 7 );
            code = 23 * code + PlotUtil.hashCode( nullColor_ );
            code = 23 * code + PlotUtil.hashCode( grid_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SpectroStyle ) {
                SpectroStyle other = (SpectroStyle) o;
                return this.shader_.equals( other.shader_ )
                    && this.shadeLog_ == other.shadeLog_
                    && this.shadeFlip_ == other.shadeFlip_
                    && PlotUtil.equals( this.nullColor_, other.nullColor_ )
                    && PlotUtil.equals( this.grid_, other.grid_ );
            }
            else {
                return false;
            }
        }
    }
}
