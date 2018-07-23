package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
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
public class Line3dPlotter extends AbstractPlotter<LineStyle> {

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
    private static final boolean IS_OPAQUE = true;
    public static final ConfigKey<Integer> THICK_KEY =
       StyleKeys.createThicknessKey( 1 );

    /**
     * Constructor.
     */
    public Line3dPlotter() {
        super( "Line3d", ResourceIcon.PLOT_LINE, 1,
               new Coord[] { SORT_COORD } );
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a point-to-point line joining",
            "up the positions of data points in three dimensions.",
            "</p>",
        } );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( THICK_KEY );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public LineStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        int thick = config.get( THICK_KEY );
        Stroke stroke = new BasicStroke( thick, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = false;
        return new LineStyle( color, stroke, antialias );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final LineStyle style ) {
        LayerOpt opt = new LayerOpt( style.getColor(), IS_OPAQUE );
        return new AbstractPlotLayer( this, geom, dataSpec, style, opt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          final PaperType paperType ) {
                final CubeSurface surf = (CubeSurface) surface;
                final PaperType3D ptype = (PaperType3D) paperType;
                return new UnplannedDrawing() {
                     protected void paintData( Paper paper,
                                               DataStore dataStore ) {
                         paintLines3d( style, surf, geom, dataSpec, dataStore,
                                       ptype, paper );
                     }
                };
            }
        };
    }

    /**
     * Does the work of joining the dots.
     *
     * @param  style  style
     * @param  surf   plotting surface
     * @param  geom   data geom
     * @param  dataSpec   data spec
     * @param  dataStore   data store
     * @param  ptype   paper type
     * @param  paper   paper object
     */
    private void paintLines3d( LineStyle style, CubeSurface surf, DataGeom geom,
                               DataSpec dataSpec, DataStore dataStore,
                               PaperType3D ptype, Paper paper ) {
        CoordGroup cgrp = getCoordGroup();
        int icPos = cgrp.getPosCoordIndex( 0, geom );
        int icSort = cgrp.getExtraCoordIndex( 0, geom );
        LineTracer3D tracer = style.createLineTracer3D( ptype, paper, surf );
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        final int ndim = surf.getDataDimCount();
        assert ndim == 3;

        /* No sorting, natural order. */
        if ( dataSpec.isCoordBlank( icSort ) ) {
            double[] dpos = new double[ ndim ];
            while ( tseq.next() ) {
                if ( geom.readDataPos( tseq, icPos, dpos ) ) {
                    tracer.addPoint( dpos );
                }
            }
        }

        /* Sort coordinate is specified. */
        else {

            /* First acquire the 3d data position and sort coordinate
             * for each valid point. */
            List<double[]> plist = new ArrayList<double[]>();
            double[] dpos = new double[ ndim ];
            while ( tseq.next() ) {
                double dsort = tseq.getDoubleValue( icSort );
                if ( PlotUtil.isFinite( dsort ) &&
                     geom.readDataPos( tseq, icPos, dpos ) ) {
                    double[] dpos1 = new double[ ndim + 1 ];
                    System.arraycopy( dpos, 0, dpos1, 0, ndim );
                    dpos1[ ndim ] = dsort;
                    plist.add( dpos1 );
                }
            }

            /* Then sort them by the chosen coordinate.
             * Note that Collections.sort is, according to its documentation,
             * less efficient than explicitly converting to an array here. */
            double[][] parray = plist.toArray( new double[ 0 ][ 0 ] );
            Arrays.sort( parray, new Comparator<double[]>() {
                public int compare( double[] p1, double[] p2 ) {
                    return Double.compare( p1[ ndim ], p2[ ndim ] );
                }
            } );

            /* Finally hand the points off in order to the line tracer. */
            for ( double[] dpos1 : parray ) {
                tracer.addPoint( dpos1 );
            }
        }
    }
}
