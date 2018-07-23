package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;
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
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
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

    private static final boolean IS_OPAQUE = true;
    public static final ConfigKey<Integer> THICK_KEY =
       StyleKeys.createThicknessKey( 1 );

    /**
     * Constructor.
     */
    public Line3dPlotter() {
        super( "Line3d", ResourceIcon.PLOT_LINE,
               CoordGroup.createSinglePositionCoordGroup(), false );
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
        int icPos = getCoordGroup().getPosCoordIndex( 0, geom );
        LineTracer3D tracer = style.createLineTracer3D( ptype, paper, surf );
        double[] dpos = new double[ surf.getDataDimCount() ];
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, icPos, dpos ) ) {
                tracer.addPoint( dpos );
            }
        }
    }
}
