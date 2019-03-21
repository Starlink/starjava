package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
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
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;


/**
 * Plotter that plots a line between data points.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class LinePlotter extends AbstractPlotter<LinePlotter.LinesStyle> {

    /** Coordinate for aux value. */
    private static final FloatingCoord AUX_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "aux", "Aux" )
           .setShortDescription( "Auxiliary colour coordinate" )
           .setXmlDescription( new String[] {
                "<p>If supplied, this controls the colouring of the line",
                "along its length according to the value of this coordinate.",
                "</p>",
            } )
        , false );

    /** Config key for point sequence pre-sorting. */
    public static final ConfigKey<AxisOpt> SORTAXIS_KEY =
        new OptionConfigKey<AxisOpt>(
            new ConfigMeta( "sortaxis", "Sort Axis" )
           .setShortDescription( "Sort order for plotted points" )
           .setStringUsage( "[" + AxisOpt.X.toString()
                          + "|" + AxisOpt.Y.toString() + "]" )
           .setXmlDescription( new String[] {
                "<p>May be set to",
                "\"<code>" + AxisOpt.X.toString() + "</code>\" or",
                "\"<code>" + AxisOpt.Y.toString() + "</code>\"",
                "to ensure that the points are plotted in ascending order",
                "of the corresponding coordinate.",
                "This will ensure that the plotted line resembles a",
                "function of the corresponding coordinate rather than",
                "a scribble.",
                "The default (null) value causes the points to be joined",
                "in the sequence in which they appear in the table.",
                "If the points already appear in the table sorted",
                "according to the corresponding coordinate,",
                "this option has no visible effect.",
                "</p>",
            } ), AxisOpt.class, new AxisOpt[] { null, AxisOpt.X, AxisOpt.Y },
                 (AxisOpt) null, true ) {
               public String valueToString( AxisOpt axis ) {
                   return axis == null ? "None" : axis.toString();
               }
               public String getXmlDescription( AxisOpt axis ) {
                   if ( axis == null ) {
                       return "No pre-sorting is performed";
                   }
                   else {
                       return "Sorting is performed on the "
                            + axis.toString() + " axis";
                   }
               }
           };

    private final boolean reportAuxKeys_ = false;
    private static final boolean IS_OPAQUE = true;
    private static final AuxScale SCALE = AuxScale.COLOR;
    private static final RampKeySet RAMP_KEYS = StyleKeys.AUX_RAMP;

    /**
     * Constructor.
     */
    public LinePlotter() {
        super( "Line", ResourceIcon.PLOT_LINE, 1, new Coord[] { AUX_COORD } );
    }

    public String getPlotterDescription() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( PlotUtil.concatLines( new String[] {
            "<p>Plots a point-to-point line joining",
            "up the positions of data points.",
            "There are additional options to pre-sort the points",
            "according to their order on the X or Y axis (using the",
            "<code>" + SORTAXIS_KEY.getMeta().getShortName() + "</code>",
            "value),",
            "and to vary the colour of the line along its length (using the",
            "<code>" + AUX_COORD.getInput().getMeta().getShortName()
                     + "</code>",
            "value).",
            "</p>",
        } ) );
        if ( ! reportAuxKeys_ ) {
            sbuf.append( PlotUtil.concatLines( new String[] {
                "<p>The options for controlling the Aux colour map",
                "are controlled at the level of the plot itself,",
                "rather than by per-layer configuration.",
                "</p>"
            } ) );
        }
        return sbuf.toString();
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( SORTAXIS_KEY );
        list.add( StyleKeys.ANTIALIAS );
        if ( reportAuxKeys_ ) {
            list.addAll( Arrays.asList( RAMP_KEYS.getKeys() ) );
        }
        list.add( StyleKeys.AUX_NULLCOLOR );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public LinesStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        RampKeySet.Ramp ramp = RAMP_KEYS.createValue( config );
        Shader shader = ramp.getShader();
        Scaling scaling = ramp.getScaling();
        Subrange dataclip = ramp.getDataClip();
        Color nullColor = config.get( StyleKeys.AUX_NULLCOLOR );
        AxisOpt sortaxis = config.get( SORTAXIS_KEY );
        return new LinesStyle( color, stroke, antialias, shader, scaling,
                               dataclip, nullColor, sortaxis );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final LinesStyle style ) {
        if ( dataSpec == null || style == null ) {
            return null;
        }
        final AxisOpt sortaxis = style.sortaxis_;
        final CoordGroup cgrp = getCoordGroup();
        final int icPos = cgrp.getPosCoordIndex( 0, geom );
        final int icAux = cgrp.getExtraCoordIndex( 0, geom );
        final boolean hasAux = ! dataSpec.isCoordBlank( icAux );
        LayerOpt opt = hasAux ? LayerOpt.OPAQUE
                              : new LayerOpt( style.getColor(), IS_OPAQUE );
        final boolean isOpaque = opt.isOpaque();
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {

            public Drawing createDrawing( final Surface surface,
                                          Map<AuxScale,Span> auxSpans,
                                          final PaperType paperType ) {
                final Span auxSpan = auxSpans.get( SCALE );
                Color baseColor = style.getColor();
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
                return new UnplannedDrawing() {
                    protected void paintData( Paper paper,
                                              final DataStore dataStore ) {
                        paperType.placeDecal( paper, new Decal() {
                            public void paintDecal( Graphics g ) {
                                paintLines( surface, dataStore, colorKit,
                                            g, paperType );
                            }
                            public boolean isOpaque() {
                                return isOpaque;
                            }
                        } );
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
                            rangeAux( surf, dataStore, ranger );
                        }
                    } );
                }
                return map;
            }

            /**
             * Does the work of painting the lines for this plotter.
             *
             * @param  surface  plot surface
             * @param  dataStore  data store
             * @param  colorKit  colouring
             * @param  g   graphics context
             * @param  paperType  paper type
             */
            private void paintLines( Surface surface, DataStore dataStore,
                                     ColorKit colorKit, Graphics g,
                                     PaperType paperType ) {
                LineTracer tracer =
                    new LineTracer( g, surface.getPlotBounds(),
                                    style.getStroke(), style.getAntialias(),
                                    10240, paperType.isBitmap() );
                AxisOpt sortaxis = style.sortaxis_;
                double[] dpos = new double[ surface.getDataDimCount() ];
                Point2D.Double gp = new Point2D.Double();
                TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
                if ( sortaxis == null ) {
                    while ( tseq.next() ) {
                        if ( geom.readDataPos( tseq, icPos, dpos ) &&
                             surface.dataToGraphics( dpos, false, gp ) &&
                             PlotUtil.isPointReal( gp ) ) {
                            Color color = colorKit.readColor( tseq );
                            tracer.addVertex( gp.x, gp.y, color );
                        }
                    }
                }
                else {
                    List<CPoint> plist = new ArrayList<CPoint>();
                    while ( tseq.next() ) {
                        if ( geom.readDataPos( tseq, icPos, dpos ) &&
                             surface.dataToGraphics( dpos, false, gp ) &&
                             PlotUtil.isPointReal( gp ) ) {
                            Color color = colorKit.readColor( tseq );
                            plist.add( new CPoint( gp.x, gp.y, color ) );
                        }
                    }
                    Collections.sort( plist, sortaxis.pointComparator() );
                    for ( CPoint p : plist ) {
                        tracer.addVertex( p.getX(), p.getY(), p.color_ );
                    }
                }
                tracer.flush();
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
            private void rangeAux( Surface surf, DataStore dataStore,
                                   Ranger ranger ) {
                final int ndim = surf.getDataDimCount();
                double[] dpos = new double[ ndim ];
                Point2D.Double gpos = new Point2D.Double();
                TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
                while ( tseq.next() ) {
                    if ( geom.readDataPos( tseq, icPos, dpos ) &&
                         surf.dataToGraphics( dpos, true, gpos ) ) {
                        ranger.submitDatum( tseq.getDoubleValue( icAux ) );
                    }
                }
            }
        };
    }

    /**
     * Point2D subclass that also contains a Color.
     */
    private static class CPoint extends Point2D.Double {
        private final Color color_;

        /**
         * Constructor.
         *
         * @param  x  graphics X coordinate
         * @param  y  graphics Y coordinate
         * @param  color  associated colour
         */
        CPoint( double x, double y, Color color ) {
            super( x, y );
            color_ = color;
        }
    }

    /**
     * Style for line plotter.
     * This decorates the LineStyle with a sort axis.
     */
    public static class LinesStyle extends AuxLineStyle {
        private final AxisOpt sortaxis_;

        /**
         * Constructor.
         *
         * @param  color  default line colour
         * @param  stroke  line stroke
         * @param  antialias  whether line is to be antialiased
         *                (only likely to make a difference on bitmapped paper)
         * @param  shader   colour ramp
         * @param  scaling  colour ramp metric
         * @param  dataclip  colour ramp input range adjustment
         * @param  nullColor  colour to use for null aux values;
         *                    if null, such segments are not plotted
         */
        public LinesStyle( Color color, Stroke stroke, boolean antialias,
                           Shader shader, Scaling scaling, Subrange dataclip,
                           Color nullColor, AxisOpt sortaxis ) {
            super( color, stroke, antialias, shader, scaling, dataclip,
                   nullColor );
            sortaxis_ = sortaxis;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof LinesStyle ) {
                LinesStyle other = (LinesStyle) o;
                return super.equals( other )
                    && PlotUtil.equals( this.sortaxis_, other.sortaxis_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 23;
            code = 23 * code + super.hashCode();
            code = 23 * code + PlotUtil.hashCode( sortaxis_ );
            return code;
        }
    }
}
