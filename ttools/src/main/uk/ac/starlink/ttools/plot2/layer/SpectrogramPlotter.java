package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.table.ValueInfo;
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
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
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
    private final CoordGroup spectroCoordGrp_;
    private final int icX_;
    private final int icExtent_;
    private final int icSpectrum_;
    private final boolean reportAuxKeys_;

    private static final AuxScale SPECTRO_SCALE = AuxScale.COLOR;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final ConfigKey<Color> NULLCOLOR_KEY =
        StyleKeys.AUX_NULLCOLOR;
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
            FloatingArrayCoord.createCoord(
                new InputMeta( "spectrum", "Spectrum" )
               .setShortDescription( "Array of spectrum channel values" )
               .setXmlDescription( new String[] {
                    "<p>Provides an array of spectral samples at each",
                    "data point.",
                    "The value must be a numeric array",
                    "(e.g. the value of an array-valued column).",
                    "</p>",
                } )
               .setValueUsage( "array" )
            , true );

        /* X extent is the width of each spectrum. */
        InputMeta xMeta = xCoord.getInput().getMeta();
        String xName = xMeta.getLongName();
        xExtentCoord_ = 
            FloatingCoord.createCoord(
                new InputMeta( xMeta.getShortName() + "width",
                               xMeta.getLongName() + " Width" )
               .setShortDescription( xName + " extent of spectrum" )
               .setXmlDescription( new String[] {
                    "<p>Range on the " + xName + " axis",
                    "over which the spectrum is plotted.",
                    "If no value is supplied, an attempt will be made",
                    "to determine it automatically by looking at the",
                    "spacing of the " + xName + " coordinates",
                    "plotted in the spectrogram.",
                    "</p>",
                } )
            , false );

        /* Maps each row to an X position, but not to a point, since it
         * covers a vertical line. */
        spectroDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "Time" );

        Coord[] coords = new Coord[] { xCoord_, spectrumCoord_, xExtentCoord_ };
        boolean[] rangeFlags = new boolean[] { true, true, false };
        spectroCoordGrp_ =
            CoordGroup.createPartialCoordGroup( coords, rangeFlags );

        /* For this plot type, coordinate indices are not sensitive to
         * plot-time geom (the CoordGroup has no point positions),
         * so we can calculate them here. */
        icX_ = spectroCoordGrp_.getExtraCoordIndex( 0, null );
        icSpectrum_ = spectroCoordGrp_.getExtraCoordIndex( 1, null );
        icExtent_ = spectroCoordGrp_.getExtraCoordIndex( 2, null );

        /* Set reportAuxKeys false, since the colour ramp config will
         * usually be controlled globally at the level of the plot. */
        reportAuxKeys_ = false;
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

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Plots spectrograms.\n" )
            .append( "A spectrogram is a sequence of spectra " )
            .append( "plotted as vertical 1-d images, each one\n" )
            .append( "plotted at a different horizontal coordinate.\n" )
            .append( "</p>\n" )
            .append( "<p>This specialised layer is only available for\n" )
            .append( "<ref id='plot2time'><code>time</code></ref> plots.\n" )
            .append( "</p>\n" );
        sbuf.append( "<p>" );
        if ( reportAuxKeys_ ) {
            sbuf.append( "There are additional options to adjust\n" )
                .append( "the way data values are mapped to colours.\n" );
        }
        else {
            sbuf.append( "The way that data values are mapped\n" )
                .append( "to colours is usually controlled by options\n" )
                .append( "at the level of the plot itself,\n" )
                .append( "rather than by per-layer configuration.\n" );
        }
        sbuf.append( "</p>\n" );
        return sbuf.toString();
    }

    public CoordGroup getCoordGroup() {
        return spectroCoordGrp_;
    }

    public ConfigKey<?>[] getStyleKeys() {
        List<ConfigKey<?>> keyList = new ArrayList<ConfigKey<?>>();
        if ( reportAuxKeys_ ) {
            keyList.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
            keyList.add( NULLCOLOR_KEY );
        }
        return keyList.toArray( new ConfigKey<?>[ 0 ] );
    }

    public SpectroStyle createStyle( ConfigMap config ) {
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        Shader shader = ramp.getShader();
        Scaling scaling = ramp.getScaling();
        Subrange dataclip = ramp.getDataClip();
        Color nullColor = config.get( NULLCOLOR_KEY );
        ChannelGrid grid = DEFAULT_CHANGRID;
        return new SpectroStyle( shader, scaling, dataclip, nullColor, grid );
    }

    public Object getRangeStyleKey( SpectroStyle style ) {
        return null;
    }

    public boolean hasReports() {
        return false;
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
                                              Map<AuxScale,Span> auxSpans,
                                              final PaperType paperType ) {
                    final Span spectroSpan = auxSpans.get( SPECTRO_SCALE );
                    return new UnplannedDrawing() {
                        protected void paintData( Paper paper,
                                                  final DataStore dataStore ) {
                            paperType.placeDecal( paper, new Decal() {
                                public void paintDecal( Graphics g ) {
                                    paintSpectrogram( surface, dataStore,
                                                      dataSpec, style,
                                                      spectroSpan, g );
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
                        public int getCoordIndex() {
                            return icSpectrum_;
                        }
                        public Scaling getScaling() {
                            return style.scaling_;
                        }
                        public ValueInfo getAxisInfo( DataSpec dataSpec ) {
                            ValueInfo[] infos =
                                dataSpec.getUserCoordInfos( icSpectrum_ );
                            return infos != null && infos.length == 1
                                 ? infos[ 0 ]
                                 : null;
                        }
                        public void adjustAuxRange( Surface surface,
                                                    DataSpec dataSpec,
                                                    DataStore dataStore,
                                                    Object[] plans,
                                                    Ranger ranger ) {
                            dataStore.getTupleRunner()
                                     .rangeData( this::fillRange, ranger,
                                                 dataSpec, dataStore );
                        }
                        private void fillRange( TupleSequence tseq,
                                                Ranger ranger ) {
                            while ( tseq.next() ) {
                                double[] spectrum =
                                    spectrumCoord_
                                   .readArrayCoord( tseq, icSpectrum_ );
                                int nchan = spectrum.length;
                                for ( int ic = 0; ic < nchan; ic++ ) {
                                    ranger.submitDatum( spectrum[ ic ] );
                                }
                            }
                        }
                    } );
                    return map;
                }
                @Override
                public void extendCoordinateRanges( Range[] ranges,
                                                    boolean[] logFlags,
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
     * @param   spectroSpan   the range of spectral values
     * @param   g   output graphics context
     */
    private void paintSpectrogram( Surface surface, DataStore dataStore,
                                   DataSpec dataSpec, SpectroStyle style,
                                   Span spectroSpan, Graphics g ) {
        ChannelGrid grid = style.grid_;
        Shader shader = style.shader_;
        Scaler specScaler =
            spectroSpan.createScaler( style.scaling_, style.dataclip_ );

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
        Point2D.Double gp0 = new Point2D.Double();
        Point2D.Double gp3 = new Point2D.Double();
        Point gp0i = new Point();
        Point gp3i = new Point();
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
                            PlotUtil.quantisePoint( gp0, gp0i );
                            PlotUtil.quantisePoint( gp3, gp3i );
                            double sval =
                                specScaler.scaleValue( chanVector[ ic ] );

                            /* This could be made more efficient by setting up
                             * a lookup table of colours at the start and
                             * indexing into it rather than creating a new
                             * Color object each time. */
                            shader.adjustRgba( rgba, (float) sval );
                            g.setColor( new Color( rgba[ 0 ], rgba[ 1 ],
                                                   rgba[ 2 ] ) );
                            int x03 = gp3i.x - gp0i.x;
                            int y03 = gp3i.y - gp0i.y;
                            final int px;
                            final int pwidth;
                            final int py;
                            final int pheight;
                            if ( x03 > 0 ) {
                                px = gp0i.x;
                                pwidth = x03;
                            }
                            else {
                                px = gp3i.x;
                                pwidth = -x03;
                            }
                            if ( y03 > 0 ) {
                                py = gp0i.y;
                                pheight = y03;
                            }
                            else {
                                py = gp3i.y;
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
        private final Scaling scaling_;
        private final Subrange dataclip_;
        private final Color nullColor_;
        private final ChannelGrid grid_;

        /**
         * Constructor.
         *
         * @param   shader  shader
         * @param   scaling   maps data values to shader ramp
         * @param   dataclip  scaling range adjustment
         * @param   nullColor  colour to use for blank spectral values
         * @param   grid    channel bounds grid
         */
        public SpectroStyle( Shader shader, Scaling scaling, Subrange dataclip,
                             Color nullColor, ChannelGrid grid ) {
            shader_ = shader;
            scaling_ = scaling;
            dataclip_ = dataclip;
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
            code = 23 * code + scaling_.hashCode();
            code = 23 * code + dataclip_.hashCode();
            code = 23 * code + PlotUtil.hashCode( nullColor_ );
            code = 23 * code + PlotUtil.hashCode( grid_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SpectroStyle ) {
                SpectroStyle other = (SpectroStyle) o;
                return this.shader_.equals( other.shader_ )
                    && this.scaling_ == other.scaling_
                    && this.dataclip_ == other.dataclip_
                    && PlotUtil.equals( this.nullColor_, other.nullColor_ )
                    && PlotUtil.equals( this.grid_, other.grid_ );
            }
            else {
                return false;
            }
        }
    }
}
