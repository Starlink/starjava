package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
import java.awt.Point;
import java.util.Arrays;
import java.util.BitSet;
import javax.swing.JComponent;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.IntList;

import uk.ac.starlink.tplot.*;

/**
 * Transparent component for adding additional decoration to an existing
 * plot component.
 *
 * @author   Mark Taylor
 * @since    8 Apr 2008
 */
public class AnnotationPanel extends JComponent {

    private int[] activePoints_;
    private PlotData data_;
    private DataId dataId_;
    private PointPlacer placer_;
    private static final MarkStyle CURSOR_STYLE = MarkStyle.targetStyle();

    /**
     * Constructor.
     */
    public AnnotationPanel() {
        setOpaque( false );
        activePoints_ = new int[ 0 ];
        dataId_ = new DataId( null );
    }

    /**
     * Sets points which should be marked as "active" by drawing a cursor
     * or something over them.
     * 
     * @param   ipoints  array of point indexes (indexes into current PlotData
     *          sequence)
     */
    public void setActivePoints( int[] ipoints ) {
        activePoints_ = (int[]) ipoints.clone();
        repaint();
    }

    /**
     * Returns the current list of active points.
     *
     * @return  array of point indexes (indexes into current PlotData sequence)
     */
    public int[] getActivePoints() {
        return (int[]) activePoints_.clone();
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
                    CURSOR_STYLE.drawMarker( g, xy.x, xy.y );
                }
            }
        }
        pseq.close();
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
}
