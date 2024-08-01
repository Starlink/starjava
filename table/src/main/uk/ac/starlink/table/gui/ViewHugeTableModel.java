package uk.ac.starlink.table.gui;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollBar;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * Swing TableModel implementation which provides a view on a very large
 * table model.
 *
 * <p>The point of this is for displaying tables with very large row counts
 * in a JTable.
 * Since the default height of a JTable row is 16 pixels
 * (see {@link javax.swing.JTable#getRowHeight}) a JTable component
 * with more than 2^27 rows (about 134 million) will have a height
 * in pixels which is too large to represent in a 32-bit int.
 * In that case Swing gives up and just doesn't display the thing.
 *
 * <p>This class implements a model of a fixed (large but not too large)
 * size, {@link #VIEWSIZE} rows, which at any one time gives a view of
 * a contiguous sequence of rows from the base table, starting at a
 * mutable value given by {@link #getViewBase}.  It works out the value
 * of viewBase by reference to a scrollbar which is owned by
 * this model, and assumed to control vertical scrolling of the
 * scroll panel displaying the JTable that views it.
 * The viewBase value is updated dynamically according to the current
 * state (position) of the scrollbar.  Whenever the viewBase changes,
 * listeners (most importantly the client JTable) are notified.
 *
 * <p>In principle, every time the scrollbar position, or equivalently
 * the set of currently visible rows, changes, the viewBase should change.
 * But since the scrollbar has only pixel resolution, smallish changes
 * can be accommodated without needing to take the somewhat expensive
 * step of changing the viewBase.
 *
 * <p>The underlying table model can still only have up to 2^31 rows.
 * The general approach used here would work for larger underlying
 * row data, but it would be necessary to define a new long-capable
 * TableModel interface to accommodate such data.
 *
 * <p>It is not recommended to use this class for underlying table models
 * which are not in fact huge.
 *
 * <p>Note this class will only help you for displaying a JTable within
 * a scrollpane.  If you want to display a giga-row JTable unscrolled on
 * a 100km tall VDU, you're on your own.
 *
 * @author   Mark Taylor
 * @since    16 Jul 2014
 */
public class ViewHugeTableModel implements TableModel {

    private final AdjustmentListener barListener_;
    private final TableModelListener hugeListener_;
    private final List<TableModelListener> tmListenerList_;
    private final List<PropertyChangeListener> propListenerList_;
    private TableModel hugeModel_;
    private JScrollBar vbar_;
    private int viewBase_;

    /** Name of property giving underlying value of first row of this model. */
    public static final String VIEWBASE_PROPERTY = "viewBase";

    /** Fixed number of rows displayed by this model. */
    public static final int VIEWSIZE = Integer.MAX_VALUE / 64;

    /**
     * Constructs an empty model.
     */
    public ViewHugeTableModel() {
        tmListenerList_ = new ArrayList<TableModelListener>();
        propListenerList_ = new ArrayList<PropertyChangeListener>();
        barListener_ = new ScrollListener();
        hugeListener_ = new HugeModelListener();
    }

    /**
     * Constructs a configured model.
     *
     * @param   hugeModel   table model with more than VIEWSIZE rows
     * @param   vbar   scrollbar used to control vertical scrolling of
     *                 table view
     */
    @SuppressWarnings("this-escape")
    public ViewHugeTableModel( TableModel hugeModel, JScrollBar vbar ) {
        this();
        configureModel( hugeModel, vbar );
    }

    /**
     * Sets this mode up for use with a given underlying table model and
     * scroll bar.
     *
     * @param   hugeModel   table model with more than VIEWSIZE rows
     * @param   vbar   scrollbar used to control vertical scrolling of
     *                 table view
     */
    public void configureModel( TableModel hugeModel, JScrollBar vbar ) {
        if ( vbar_ != null ) {
            vbar_.removeAdjustmentListener( barListener_ );
        }
        if ( hugeModel_ != null ) {
            hugeModel_.removeTableModelListener( hugeListener_ );
        }
        hugeModel_ = hugeModel;
        vbar_ = vbar;
        if ( vbar_ != null ) {
            vbar_.addAdjustmentListener( barListener_ );
        }
        if ( hugeModel_ != null ) {
            hugeModel_.addTableModelListener( hugeListener_ );
        }
        viewBase_ = calculateViewBase();
        fireTableChanged( new TableModelEvent( this ) );
    }

    public int getRowCount() {
        return Math.min( hugeModel_.getRowCount(), VIEWSIZE );
    }

    public Object getValueAt( int irow, int icol ) {
        return hugeModel_.getValueAt( getHugeRow( irow ), icol );
    }

    public void setValueAt( Object value, int irow, int icol ) {
        hugeModel_.setValueAt( value, getHugeRow( irow ), icol );
    }

    public boolean isCellEditable( int irow, int icol ) {
        return hugeModel_.isCellEditable( getHugeRow( irow ), icol );
    }

    public int getColumnCount() {
        return hugeModel_.getColumnCount();
    }

    public String getColumnName( int icol ) {
        return hugeModel_.getColumnName( icol );
    }

    public Class<?> getColumnClass( int icol ) {
        return hugeModel_.getColumnClass( icol );
    }

    public void addTableModelListener( TableModelListener lnr ) {
        tmListenerList_.add( lnr );
    }

    public void removeTableModelListener( TableModelListener lnr ) {
        tmListenerList_.remove( lnr );
    }

    /**
     * Returns the offset from which this view views the underlying
     * table model.  Changes in this value will be notified to registered
     * PropertyChangeListeners.
     *
     * @return   view base
     */
    public int getViewBase() {
        return viewBase_;
    }

    /**
     * Adds a listener that will be notified about ViewBase changes.
     * These have the property name {@link #VIEWBASE_PROPERTY}.
     *
     * @param  lnr  listener to add
     */
    public void addPropertyChangeListener( PropertyChangeListener lnr ) {
        propListenerList_.add( lnr );
    }

    /**
     * Removes a listener added earlier.
     *
     * @param  lnr  listener to remove
     */
    public void removePropertyChangeListener( PropertyChangeListener lnr ) {
        propListenerList_.remove( lnr );
    }

    /**
     * Notifies table model listeners of a table model event.
     *
     * @param  evt  event
     */
    protected void fireTableChanged( TableModelEvent evt ) {
        for ( TableModelListener lnr : tmListenerList_ ) {
            lnr.tableChanged( evt );
        }
    }

    /**
     * Notifies property change listeners of a property change.
     *
     * @param   evt   event
     */
    protected void firePropertyChanged( PropertyChangeEvent evt ) {
        for ( PropertyChangeListener lnr : propListenerList_ ) {
            lnr.propertyChange( evt );
        }
    }

    /**
     * Returns the row in the underlying huge model corresponding to a given
     * row in this view.
     *
     * @return   <code>iViewRow + viewBase</code>
     */
    public int getHugeRow( int iViewRow ) {
        return iViewRow + viewBase_;
    }

    /**
     * Determines the optimal value of the viewBase value for the current
     * state of this model's scrollbar.
     *
     * @return  optimal viewBase
     */
    private int calculateViewBase() {
        int vrange = hugeModel_.getRowCount() - VIEWSIZE;
        if ( vrange <= 0 ) {
            return 0;
        }
        else {
            int vmin = vbar_.getMinimum();
            int vmax = vbar_.getMaximum();
            int vval = vbar_.getValue();
            int vext = vbar_.getVisibleAmount();
            double vscale = vval / (double) ( vmax - vmin - vext );
            return (int) ( vscale * vrange );
        }
    }

    /**
     * Called if it's a possible time to reset the viewBase.
     * Resetting is somewhat expensive (listeners must be notified and may
     * need to update their views wholesale) so only actually reset it
     * if the effect on the scrollbar is big enough to be noticeable.
     */
    private void maybeReconfigure() {
        int vbase = calculateViewBase();
        int npix = Math.max( vbar_.getHeight(), 400 );
        int rowsPerPixel = hugeModel_.getRowCount() / npix;
        int diff = Math.abs( vbase - viewBase_ );
        if ( diff > rowsPerPixel / 2 ) {
            resetViewBase( vbase );
        }
    }

    /**
     * Unconditionally resets the viewBase to a given value, notifying
     * listeners etc.
     *
     * @param  viewBase  new offset value
     */
    private void resetViewBase( int viewBase ) {
        int oldViewBase = viewBase_;
        viewBase_ = viewBase;

        /* All table rows of this model will change, so notify the table model
         * listeners accordingly. */
        TableModelEvent tmEvt =
            new TableModelEvent( this, 0, Math.min( VIEWSIZE, getRowCount() ) );
        fireTableChanged( tmEvt );

        /* Inform interested listeners that the viewbase has changed as well. */
        PropertyChangeEvent propEvt = 
            new PropertyChangeEvent( this, VIEWBASE_PROPERTY,
                                     Integer.valueOf( oldViewBase ),
                                     Integer.valueOf( viewBase ) );
        firePropertyChanged( propEvt );
    }

    /**
     * Listens to the scrollbar, and reconfigures this model's viewbase
     * according to changes.
     */
    private class ScrollListener implements AdjustmentListener {
        public void adjustmentValueChanged( AdjustmentEvent evt ) {

            /* If the change is the result of somebody leaning on the
             * up/down arrows (line or page) we don't really want to 
             * reset the viewBase, because it is likely to lead to
             * un-smooth scrolling.  If your underlying model is big enough
             * to make it worthwhile using this class, you'd have to lean
             * on those buttons for a really long time before the position
             * of the scrollbar gets noticeably out of step with where
             * it should be, so ignore that possibility. */
            int atype = evt.getAdjustmentType();
            boolean isRelative = atype == AdjustmentEvent.UNIT_INCREMENT
                              || atype == AdjustmentEvent.UNIT_DECREMENT
                              || atype == AdjustmentEvent.BLOCK_INCREMENT
                              || atype == AdjustmentEvent.BLOCK_DECREMENT;

            /* If somebody has dragged the scrollbar, or otherwise moved
             * the viewport in a non-relative way, it's no problem to 
             * reconfigure. */
            if ( ! isRelative ) {
                maybeReconfigure();
            }
        }
    }

    /**
     * Listens to the underlying table model and forwards events as required.
     */
    private class HugeModelListener implements TableModelListener {
        public void tableChanged( TableModelEvent evt ) {
            Object src = evt.getSource();
            TableModel tmodelSrc = src instanceof TableModel
                                 ? (TableModel) src
                                 : null;
            int hugeRow0 = evt.getFirstRow();
            int hugeRow1 = evt.getLastRow();
            int type = evt.getType();
            int icol = evt.getColumn();
            final TableModelEvent viewEvt;
            if ( type == TableModelEvent.UPDATE &&
                 hugeRow1 < hugeModel_.getRowCount() ) {
                viewEvt = new TableModelEvent( tmodelSrc,
                                               hugeToView( hugeRow0 ),
                                               hugeToView( hugeRow1 ),
                                               icol, type );
            }
            else {
                viewEvt = new TableModelEvent( tmodelSrc );
            }
            fireTableChanged( viewEvt ); 
            maybeReconfigure();
        }

        /**
         * Converts row indices from the underlying model to this one,
         * preserving special values.
         *
         * @param  hugeRow   underlying model row index for events
         * @return   this model row index for events
         */
        private int hugeToView( int hugeRow ) {
            return hugeRow == TableModelEvent.HEADER_ROW
                 ? TableModelEvent.HEADER_ROW
                 : hugeRow - viewBase_;
        }
    }
}
