package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Plotter that plots a line between data points.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class LinePlotter extends SimpleDecalPlotter<LineStyle> {

    /**
     * Constructor.
     */
    public LinePlotter() {
        super( "Line", ResourceIcon.PLOT_LINE,
               CoordGroup.createSinglePositionCoordGroup() );
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( StyleKeys.ANTIALIAS );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public LineStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        return new LineStyle( color, stroke, antialias );
    }

    protected LayerOpt getLayerOpt( LineStyle style ) {
        return new LayerOpt( style.getColor(), true );
    }

    protected void paintData2D( Surface surface, DataStore dataStore,
                                DataGeom geom, DataSpec dataSpec,
                                LineStyle style, Graphics g ) {
        LineTracer tracer =
            style.createLineTracer( g, surface.getPlotBounds(), 10240 );
        int icPos = getCoordGroup().getPosCoordIndex( 0, geom );
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point gp = new Point();
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, icPos, dpos ) &&
                 surface.dataToGraphics( dpos, false, gp ) ) {
                tracer.addVertex( gp.x, gp.y );
            }
        }
        tracer.flush();
    }
}
