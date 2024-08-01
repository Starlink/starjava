package uk.ac.starlink.topcat.plot2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.plot2.LabelledLine;
import uk.ac.starlink.ttools.plot2.PlotMetric;
import uk.ac.starlink.ttools.plot2.Surface;

/**
 * Overlay panel that can display annotated distance measurements
 * between two user-specified points on the plot.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2019
 */
public class MeasurePanel extends JPanel {

    private final PlotMetric metric_;
    private final PlotPanel<?,?> plotPanel_;
    private final ToggleButtonModel measureModel_;
    private final MeasureListener listener_;
    private final Color fg_;
    private final Color bg_;
    private final Stroke bgStroke_;
    private Surface surf_;
    private Point gpos0_;
    private Point gpos1_;

    /**
     * Constructor.
     *
     * @param   metric   plot metric appropriate for surfaces that
     *                   this panel will be presented with
     * @param   plotPanel   factory for surfaces
     */
    @SuppressWarnings("this-escape")
    public MeasurePanel( PlotMetric metric, PlotPanel<?,?> plotPanel ) {
        metric_ = metric;
        plotPanel_ = plotPanel;
        listener_ = new MeasureListener();
        measureModel_ = new ToggleButtonModel( "Measure Distance",
                                               ResourceIcon.MEASURE,
                                               "Measure the distance between "
                                             + "two points on the plot" );
        measureModel_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                setActive( measureModel_.isSelected() );
            }
        } );
        fg_ = Color.BLACK;
        bg_ = new Color( 0xa0ffffff, true );
        bgStroke_ =
            new BasicStroke( 3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
        setOpaque( false );

        /* It would be nice to set a cursor here; all that's required is a
         * call to java.awt.Cursor.setCursor.  However the J2SE doesn't
         * supply a suitable cursor image for this purpose. */
    }

    /**
     * Returns a button model that will initiate a measurement gesture
     * when selected.  When the measurement is finished, it will
     * unselect itself.
     *
     * @return   measure button model
     */
    public ToggleButtonModel getModel() {
        return measureModel_;
    }

    /**
     * Configures whether this measurement panel is currently measuring
     * or dormant.  If set active, it will become inactive next time
     * the user releases the mouse.
     *
     * @param  isActive  true for active, false for inactive
     */
    private void setActive( boolean isActive ) {
        gpos0_ = null;
        gpos1_ = null;
        surf_ = null;
        if ( isActive ) {
            addMouseListener( listener_ );
            addMouseMotionListener( listener_ );
        }
        else {
            removeMouseListener( listener_ );
            removeMouseMotionListener( listener_ );
        }
        setVisible( isActive );
        repaint();
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        if ( surf_ != null && gpos0_ != null && gpos1_ != null ) {
            LabelledLine[] lines = metric_.getMeasures( surf_, gpos0_, gpos1_ );

            /* Plot line backgrounds. */
            Graphics2D g2 = (Graphics2D) g.create();
            Stroke stroke0 = g2.getStroke();
            g2.setColor( bg_ );
            g2.setStroke( bgStroke_ );
            for ( LabelledLine line : lines ) {
                line.drawLine( g2 );
            }
            g2.setStroke( stroke0 );

            /* Plot line foregrounds and text annotations. */
            g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
            for ( LabelledLine line : lines ) {
                g2.setColor( fg_ );
                line.drawLine( g2 );
                line.drawLabel( g2, bg_ );
            }
        }
    }

    /**
     * Mouse listener.
     */
    private class MeasureListener extends MouseAdapter {
        public void mousePressed( MouseEvent evt ) {
            Point gpos = evt.getPoint();
            int iz = plotPanel_.getZoneIndex( gpos );
            if ( iz >= 0 ) {
                surf_ = plotPanel_.getSurface( iz );
                gpos0_ = gpos;
            }
            else {
                measureModel_.setSelected( false );
            }
        }
        public void mouseReleased( MouseEvent evt ) {
            measureModel_.setSelected( false );
        }
        public void mouseDragged( MouseEvent evt ) {
            gpos1_ = evt.getPoint();
            repaint();
        }
    }
}
