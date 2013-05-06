package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Plotter that plots a line between data points.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2013
 */
public class LinePlotter extends SimpleDecalPlotter<MarkStyle> {

    /**
     * Constructor.
     */
    public LinePlotter() {
        super( "Line", ResourceIcon.PLOT_LINE, new Coord[ 0 ] );
    }

    public ConfigKey[] getStyleKeys() {
        return new ConfigKey[] {
            StyleKeys.COLOR,
            StyleKeys.DASH,
            StyleKeys.THICKNESS,
        };
    }

    public MarkStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        int thickness = config.get( StyleKeys.THICKNESS );
        float[] dash = config.get( StyleKeys.DASH );
        MarkStyle mstyle = MarkShape.POINT.getStyle( color, 0 );
        mstyle.setHidePoints( true );
        mstyle.setLine( MarkStyle.DOT_TO_DOT );
        mstyle.setLineWidth( thickness );
        mstyle.setDash( dash );
        return mstyle;
    }

    protected LayerOpt getLayerOpt( MarkStyle style ) {
        return new LayerOpt( style.getColor(), true );
    }

    protected void paintData2D( Surface surface, DataStore dataStore,
                                DataGeom geom, DataSpec dataSpec,
                                MarkStyle style, Graphics g ) {
        Rectangle bounds = surface.getPlotBounds();
        int huge = Math.max( bounds.width, bounds.height ) * 100;
        Graphics2D g2 = (Graphics2D) g;
        Color color0 = g2.getColor();
        Stroke stroke0 = g2.getStroke();
        g2.setColor( style.getColor() );
        g2.setStroke( style.getStroke( BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_ROUND ) );
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point gp = new Point();
        TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
        boolean notFirst = false;
        int lastxp = 0;
        int lastyp = 0;
        while ( tseq.next() ) {
            if ( geom.readDataPos( tseq, 0, dpos ) &&
                 surface.dataToGraphics( dpos, false, gp ) ) {

                /* Limit plotted/recorded positions to be not too far off
                 * the plotted area.  Failing to do this can result in
                 * attempts to draw lines kilometres long, which can have
                 * an adverse effect on the graphics system. */
                int xp = Math.max( -huge, Math.min( huge, gp.x ) );
                int yp = Math.max( -huge, Math.min( huge, gp.y ) );
                if ( notFirst ) {
                    g2.drawLine( lastxp, lastyp, xp, yp );
                }
                notFirst = true;
                lastxp = xp;
                lastyp = yp;
            }
        }
        g2.setColor( color0 );
        g2.setStroke( stroke0 );
    }
}
