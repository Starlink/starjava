package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.BitSet;
import javax.swing.JComponent;
import javax.swing.Timer;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ColorTweaker;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PointPlacer;
import uk.ac.starlink.ttools.plot.PointSequence;
import uk.ac.starlink.util.IntList;

/**
 * Transparent component for adding additional decoration to an existing
 * plot component.
 *
 * @author   Mark Taylor
 * @since    8 Apr 2008
 */
public class AnnotationPanel extends JComponent implements ActionListener {

    private int[] activePoints_;
    private PlotData data_;
    private DataId dataId_;
    private PointPlacer placer_;
    private static final PulseColorTweaker flasher_ = new PulseColorTweaker();
    private static final MarkStyle CURSOR_STYLE = MarkStyle.targetStyle();
    private static Timer timer_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public AnnotationPanel() {
        setOpaque( false );
        activePoints_ = new int[ 0 ];
        dataId_ = new DataId( null );
        setPulsing( false );
    }

    /**
     * Sets points which should be marked as "active" by drawing a cursor
     * or something over them.
     * 
     * @param   ipoints  array of point indexes (indexes into current PlotData
     *          sequence)
     */
    public void setActivePoints( int[] ipoints ) {
        activePoints_ = ipoints.clone();
        repaint();
    }

    /**
     * Returns the current list of active points.
     *
     * @return  array of point indexes (indexes into current PlotData sequence)
     */
    public int[] getActivePoints() {
        return activePoints_.clone();
    }

    /**
     * Removes any decorations associated with this panel.
     */
    public void clear() {
        setActivePoints( new int[ 0 ] );
    }

    /**
     * Sets the data used by this panel.
     * 
     * @param   data  plot data
     */
    public void setPlotData( PlotData data ) {
        DataId id = new DataId( (PointSelection) data );
        if ( ! id.equals( dataId_ ) ) {
            dataId_ = id;
            clear();
        }
        data_ = data;
    }

    /**
     * Sets the PointPlacer which maps from PlotData coordinates to screen
     * positions.
     *
     * @param  placer  point placer
     */
    public void setPlacer( PointPlacer placer ) {
        placer_ = placer;
    }

    /**
     * Returns the PointPlacer which maps from PlotData coordinates to screen
     * positions.  This method may be overridden by subclases (in which case
     * {@link #setPlacer} might not work).
     *
     * @return  point placer
     */
    public PointPlacer getPlacer() {
        return placer_;
    }

    /**
     * Indicates whether the current point in a given point sequence is to
     * be included for the purposes of annotations.
     *
     * @param  pseq  point sequence
     * @return  true iff the current point of pseq is included
     */
    protected boolean isIncluded( PointSequence pseq ) {
        int nset = data_.getSetCount();
        for ( int is = 0; is < nset; is++ ) {
            if ( pseq.isIncluded( is ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes any indexes from the active point list which are not currently
     * visible.
     */
    public void dropInvisibles() {
        if ( activePoints_.length == 0 ) {
            return;
        }
        else if ( data_ == null ) {
            activePoints_ = new int[ 0 ];
        }
        else {
            BitSet activeMask = new BitSet();
            for ( int i = 0; i < activePoints_.length; i++ ) {
                activeMask.set( activePoints_[ i ] );
            }
            IntList visibleList = new IntList();
            PointPlacer placer = getPlacer();
            PointSequence pseq = data_.getPointSequence();
            for ( int ip = 0; pseq.next(); ip++ ) {
                if ( activeMask.get( ip ) && isIncluded( pseq ) ) {
                    if ( placer.getXY( pseq.getPoint() ) != null ) {
                        visibleList.add( ip );
                    }
                }
            }
            pseq.close();
            activePoints_ = visibleList.toIntArray();
        }
    }

    /**
     * Sets whether active points should pulse or not.
     *
     * @param  isPulsing  true  iff you want points to pulse
     */
    public void setPulsing( boolean isPulsing ) {
        if ( isPulsing ) {
            getPulseTimer().addActionListener( this );
        }
        else {
            if ( timer_ != null ) {
               timer_.removeActionListener( this );
            }
        }
    }

    public void actionPerformed( ActionEvent evt ) {
        if ( evt.getSource() == timer_ ) {
            repaint();
        }
    }

    protected void paintComponent( Graphics g ) {
        PointPlacer placer = getPlacer();
        if ( activePoints_.length == 0 || data_ == null || placer == null ) {
            return;
        }
        BitSet activeMask = new BitSet();
        for ( int i = 0; i < activePoints_.length; i++ ) {
            activeMask.set( activePoints_[ i ] );
        }
        PointSequence pseq = data_.getPointSequence();
        for ( int ip = 0; pseq.next(); ip++ ) {
            if ( activeMask.get( ip ) && isIncluded( pseq ) ) {
                Point xy = placer.getXY( pseq.getPoint() );
                if ( xy != null ) {
                    CURSOR_STYLE.drawMarker( g, xy.x, xy.y, flasher_ );
                }
            }
        }
        pseq.close();
    }

    /**
     * Returns the timer which arranges for pulsing of active point cursors.
     */
    private static Timer getPulseTimer() {
        if ( timer_ == null ) {
            timer_ = new Timer( 125, new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    flasher_.bump();
                }
            } );
            timer_.start();
        }
        return timer_;
    }

    /**
     * Characterises a data set.
     * The <code>equals</code> method is implemented in such a way that
     * two DataId objects are equal if they have the same tables and subsets.
     */
    private static class DataId {
        private final TopcatModel[] tables_;
        private final int[] isets_;

        /**
         * Constructor.
         *
         * @param  psel  point selector
         */
        DataId( PointSelection psel ) {
            if ( psel == null ) {
                tables_ = new TopcatModel[ 0 ];
                isets_ = new int[ 0 ];
            }
            else {
                SetId[] setIds = psel.getSetIds();
                int nset = setIds.length;
                tables_ = new TopcatModel[ nset ];
                isets_ = new int[ nset ];
                for ( int is = 0; is < nset; is++ ) {
                    isets_[ is ] = setIds[ is ].getSetIndex();
                    tables_[ is ] = setIds[ is ].getPointSelector().getTable();
                }
            }
        }
        public boolean equals( Object o ) {
            if ( o instanceof DataId ) {
                DataId other = (DataId) o;
                return Arrays.equals( this.tables_, other.tables_ )
                    && Arrays.equals( this.isets_, other.isets_ );
            }
            else {
                return false;
            }
        }
        public int hashCode() {
            int code = 503;
            for ( int is = 0; is < isets_.length; is++ ) {
                code = 23 * code + isets_[ is ];
                code = 23 * code + tables_[ is ].hashCode();
            }
            return code;
        }
    }

    /**
     * ColorTweaker which can pulse colours.
     */
    private static class PulseColorTweaker implements ColorTweaker {
        private byte quantum_ = (byte) 16;
        private byte phase_;
        private float[] rgba_;
        private Color color_;

        /**
         * Constructor.
         */
        PulseColorTweaker() {
            configure();
        }

        /**
         * Moves the pulse to the next state.
         */
        public void bump() {
            phase_ += quantum_;
            configure();
        }

        public Color tweakColor( Color orig ) {
            return color_;
        }

        public void tweakColor( float[] rgba ) {
            System.arraycopy( rgba_, 0, rgba, 0, rgba_.length );
        }

        /**
         * Configures this tweaker appropriately for the current pulse state.
         */
        private void configure() {
            float level = (float) Math.abs( phase_ ) / 128f;
            rgba_ = new float[] { level, level, level, 0.7f, };
            color_ = new Color( level, level, level, 0.7f );
        }
    }
}
