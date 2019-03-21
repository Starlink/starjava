package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
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
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Plots lines joining data points in three dimensions.
 *
 * @author   Mark Taylor
 * @since    19 Jul 2018
 */
public class Line3dPlotter extends AbstractPlotter<AuxLineStyle> {

    private static final FloatingCoord AUX_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "aux", "Aux" )
           .setShortDescription( "Auxiliary colour coordinate" )
           .setXmlDescription( new String[] {
                "<p>If supplied, this adjusts the colouring of the line",
                "along its length according to the value of this coordinate.",
                "</p>",
            } )
        , false );
    private static final FloatingCoord SORT_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "sort", "Sort" )
           .setShortDescription( "Sorting sequence for plotted lines" )
           .setXmlDescription( new String[] {
                "<p>If supplied, this gives a value to define in what order",
                "points are joined together.",
                "If no value is given, the natural order is used,",
                "i.e. the sequence of rows in the table.",
                "</p>",
                "<p>Note that if the required order is in fact the natural",
                "order of the table, it is better to leave this value blank,",
                "since sorting is a potentially expensive step.",
                "</p>",
            } )
        , false );
    private final boolean reportAuxKeys_ = false;
    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;
    private static final boolean IS_OPAQUE = true;
    public static final ConfigKey<Integer> THICK_KEY =
       StyleKeys.createThicknessKey( 1 );

    /**
     * Constructor.
     */
    public Line3dPlotter() {
        super( "Line3d", ResourceIcon.PLOT_LINE, 1,
               new Coord[] { AUX_COORD, SORT_COORD } );
    }

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer();
	sbuf.append( PlotUtil.concatLines( new String[] {
            "<p>Plots a point-to-point line joining",
            "up the positions of data points in three dimensions.",
            "There are additional options to pre-sort the points",
            "by a given quantity before drawing the lines",
            "(using the <code>" + SORT_COORD.getInput().getMeta().getShortName()
                                + "</code> value),",
            "and to vary the colour of the line along its length",
            "(using the <code>" + AUX_COORD.getInput().getMeta().getShortName()
                                + "</code> value).",
        } ) );
        if ( ! reportAuxKeys_ ) {
            sbuf.append( PlotUtil.concatLines( new String[] {
                "The options for controlling the Aux colour map",
                "are controlled at the level of the plot itself,",
                "rather than by per-layer configuration.",
            } ) );
        }
        sbuf.append( "</p>" );
        sbuf.append( PlotUtil.concatLines( new String[] {
            "<p>Note that the line positioning in 3d and the line segment",
            "aux colouring is somewhat approximate.",
            "In most cases it is good enough for visual inspection,",
            "but pixel-level examination may reveal discrepancies.",
            "</p>",
        } ) );
        return sbuf.toString();
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( THICK_KEY );
        if ( reportAuxKeys_ ) {
            list.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public AuxLineStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        int thick = config.get( THICK_KEY );
        Stroke stroke = new BasicStroke( thick, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = false;
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        Shader shader = ramp.getShader();
        Scaling scaling = ramp.getScaling();
        Subrange dataclip = ramp.getDataClip();
        Color nullColor = config.get( StyleKeys.AUX_NULLCOLOR );
        return new AuxLineStyle( color, stroke, antialias, shader, scaling,
                                 dataclip, nullColor );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final AuxLineStyle style ) {
        CoordGroup cgrp = getCoordGroup();
        final int icPos = cgrp.getPosCoordIndex( 0, geom );
        final int icAux = cgrp.getExtraCoordIndex( 0, geom );
        final int icSort = cgrp.getExtraCoordIndex( 1, geom );
        final boolean hasAux = ! dataSpec.isCoordBlank( icAux );
        final boolean hasSort = ! dataSpec.isCoordBlank( icSort );
        LayerOpt opt = hasAux ? LayerOpt.OPAQUE
                              : new LayerOpt( style.getColor(), IS_OPAQUE );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {

            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          final PaperType paperType ) {
                final CubeSurface surf = (CubeSurface) surface;
                final PaperType3D ptype = (PaperType3D) paperType;
                final Span auxSpan = auxSpans.get( SCALE );
                return new UnplannedDrawing() {
                     protected void paintData( Paper paper,
                                               DataStore dataStore ) {
                         paintLines3d( style, surf, dataStore, auxSpan,
                                       ptype, paper );
                     }
                };
            }

            @Override
            public Map<AuxScale,AuxReader> getAuxRangers() {
                Map<AuxScale,AuxReader> map = super.getAuxRangers();
                if ( hasAux ) {
                    map.put( SCALE, new AuxReader() {
                        public int getCoordIndex() {
                            return icAux;
                        }
                        public ValueInfo getAxisInfo( DataSpec dataSpec ) {
                            ValueInfo[] infos =
                                dataSpec.getUserCoordInfos( icAux );
                            return infos.length == 1 ? infos[ 0 ] : null;
                        }
                        public Scaling getScaling() {
                            return style.getScaling();
                        }
                        public void adjustAuxRange( Surface surf,
                                                    DataSpec dataSpec,
                                                    DataStore dataStore,
                                                    Object[] plans,
                                                    Ranger ranger ) {
                            rangeAux3d( (CubeSurface) surf, dataStore, ranger );
                        }
                    } );
                }
                return map;
            }

            /**
             * Does the work of joining the dots.
             *
             * @param  style  style
             * @param  surf   plotting surface
             * @param  dataStore   data store
             * @param  auxSpan    aux value range
             * @param  ptype   paper type
             * @param  paper   paper object
             */
            private void paintLines3d( AuxLineStyle style, CubeSurface surf,
                                       DataStore dataStore, Span auxSpan,
                                       PaperType3D ptype, Paper paper ) {
                Color baseColor = style.getColor();
                Stroke stroke = style.getStroke();
                LineTracer3D tracer =
                    LineTracer3D.createTracer( ptype, paper, surf, stroke );
                TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
                final int ndim = surf.getDataDimCount();
                assert ndim == 3;
                final ColorKit colorKit;
                if ( hasAux ) {
                    Shader shader = style.getShader();
                    Scaling scaling = style.getScaling();
                    Subrange dataclip = style.getDataClip();
                    Scaler scaler = auxSpan.createScaler( scaling, dataclip );
                    Color nullColor = style.getNullColor();
                    float scaleAlpha = 1;
                    colorKit = new AuxColorKit( icAux, shader, scaler,
                                                baseColor, nullColor,
                                                scaleAlpha );
                }
                else {
                    colorKit = new FixedColorKit( baseColor );
                }

                /* No sorting, natural order. */
                if ( ! hasSort ) {
                    double[] dpos = new double[ ndim ];
                    while ( tseq.next() ) {
                        if ( geom.readDataPos( tseq, icPos, dpos ) ) {
                            Color color = colorKit.readColor( tseq );
                            tracer.addPoint( dpos, color );
                        }
                    }
                }

                /* Sort coordinate is specified. */
                else {

                    /* First acquire the 3d data position and sort coordinate
                     * for each valid point. */
                    List<Vertex> vlist = new ArrayList<Vertex>();
                    double[] dpos = new double[ ndim ];
                    while ( tseq.next() ) {
                        double dsort = tseq.getDoubleValue( icSort );
                        if ( PlotUtil.isFinite( dsort ) &&
                             geom.readDataPos( tseq, icPos, dpos ) ) {
                            Color color = colorKit.readColor( tseq );
                            if ( color != null ) {
                                Vertex vertex =
                                    new Vertex( dpos[ 0 ], dpos[ 1 ], dpos[ 2 ],
                                                color, dsort );
                                vlist.add( vertex );
                            }
                        }
                    }

                    /* Then sort them by the chosen coordinate.
                     * Note that Collections.sort is, according to its
                     * documentation, less efficient than explicitly
                     * converting to an array here. */
                    Vertex[] varray = vlist.toArray( new Vertex[ 0 ] );
                    Arrays.sort( varray );

                    /* Finally hand the points off in order to the tracer. */
                    for ( Vertex v : varray ) {
                        dpos[ 0 ] = v.dx_;
                        dpos[ 1 ] = v.dy_;
                        dpos[ 2 ] = v.dz_;
                        tracer.addPoint( dpos, v.color_ );
                    }
                }
            }

            /**
             * Extends a given range to accommodate the values of the Aux
             * coordinate for this layer.  This cheats a bit; it uses
             * the range values for the points that would be visible,
             * which is not actually the values that will be used to
             * colour the line segments that are visible.
             * But it's less complicated to do it this way, and the result
             * is likely to be visually quite similar.
             *
             * @param  surf   plotting surface
             * @param  dataStore   data store
             * @param  ranger   ranger object to update with aux values
             */
            private void rangeAux3d( CubeSurface surf, DataStore dataStore,
                                     Ranger ranger ) {
                final int ndim = surf.getDataDimCount();
                double[] dpos = new double[ ndim ];
                TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
                while ( tseq.next() ) {
                    if ( geom.readDataPos( tseq, icPos, dpos ) &&
                         surf.inRange( dpos ) ) {
                        ranger.submitDatum( tseq.getDoubleValue( icAux ) );
                    }
                }
            }
        };
    }

    /**
     * Stores information about a line vertex to plot.
     * Only used in sorting mode.
     */
    private static class Vertex implements Comparable<Vertex> {
        final double dx_;
        final double dy_;
        final double dz_;
        final Color color_;
        final double seq_;

        /**
         * Constructor.
         *
         * @param  dx  data X coordinate
         * @param  dy  data Y coordinate
         * @param  dz  data Z coordinate
         * @param  color  plotting colour
         * @param  seq   sorting sequence value
         */
        Vertex( double dx, double dy, double dz, Color color, double seq ) {
            dx_ = dx;
            dy_ = dy;
            dz_ = dz;
            color_ = color;
            seq_ = seq;
        }

        public int compareTo( Vertex other ) {
            return Double.compare( this.seq_, other.seq_ );
        }
    }
}
