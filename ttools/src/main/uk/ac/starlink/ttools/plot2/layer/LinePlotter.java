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
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter that plots a line between data points.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class LinePlotter extends SimpleDecalPlotter<LinePlotter.LinesStyle> {

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

    /**
     * Constructor.
     */
    public LinePlotter() {
        super( "Line", ResourceIcon.PLOT_LINE,
               CoordGroup.createSinglePositionCoordGroup(), false );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a point-to-point line joining",
            "up the positions of data points.",
            "Note that for a large and unordered data set",
            "this can lead to a big scribble on the screen.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( SORTAXIS_KEY );
        list.add( StyleKeys.ANTIALIAS );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public LinesStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        AxisOpt sortaxis = config.get( SORTAXIS_KEY );
        return new LinesStyle( color, stroke, antialias, sortaxis );
    }

    protected LayerOpt getLayerOpt( LinesStyle style ) {
        return new LayerOpt( style.getColor(), true );
    }

    protected void paintData2D( Surface surface, DataStore dataStore,
                                DataGeom geom, DataSpec dataSpec,
                                LinesStyle style, Graphics g,
                                PaperType paperType ) {
        LineTracer tracer =
            style.createLineTracer( g, surface.getPlotBounds(), 10240,
                                    paperType.isBitmap() );
        Color color = style.getColor();
        AxisOpt sortaxis = style.sortaxis_;
        int icPos = getCoordGroup().getPosCoordIndex( 0, geom );
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point2D.Double gp = new Point2D.Double();
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        if ( sortaxis == null ) {
            while ( tseq.next() ) {
                if ( geom.readDataPos( tseq, icPos, dpos ) &&
                     surface.dataToGraphics( dpos, false, gp ) &&
                     PlotUtil.isPointReal( gp ) ) {
                    tracer.addVertex( gp.x, gp.y, color );
                }
            }
        }
        else {
            List<Point2D.Double> plist = new ArrayList<Point2D.Double>();
            while ( tseq.next() ) {
                if ( geom.readDataPos( tseq, icPos, dpos ) &&
                     surface.dataToGraphics( dpos, false, gp ) &&
                     PlotUtil.isPointReal( gp ) ) {
                    plist.add( new Point2D.Double( gp.x, gp.y ) );
                }
            }
            Collections.sort( plist, sortaxis.pointComparator() );
            for ( Point2D point : plist ) {
                tracer.addVertex( point.getX(), point.getY(), color );
            }
        }
        tracer.flush();
    }

    /**
     * Style for line plotter.
     * This decorates the LineStyle with a sort axis.
     */
    public static class LinesStyle extends LineStyle {
        private final AxisOpt sortaxis_;

        /**
         * Constructor.
         *
         * @param  color   line colour
         * @param  stroke  line stroke
         * @param  antialias  true to draw line antialiased
         * @param  sortaxis   axis along which points are sorted before the plot
         */
        public LinesStyle( Color color, Stroke stroke, boolean antialias,
                           AxisOpt sortaxis ) {
            super( color, stroke, antialias );
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
