package uk.ac.starlink.table.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * ListSelectionModel to use in conjunction with a ViewHugeTableModel
 * for tracking row selection of very large tables.
 * Since the rows of the ViewHugeTableModel keep changing which rows
 * of the underlying huge model they are talking about, the selection
 * model has to understand how that happens if you want to have
 * row selection that persists as the scroll bar is moved around.
 * This is the model which should be installed on the JTable.
 *
 * @author   Mark Taylor
 * @since    16 Jul 2014
 * @see   ViewHugeTableModel
 */
public class ViewHugeSelectionModel implements ListSelectionModel {

    private final ListSelectionModel hugeModel_;
    private final Map<ListSelectionListener,HugeListener> listenerMap_;
    private int viewBase_;

    /**
     * Constructor.
     *
     * @param  hugeModel  list selection model corresonding to the
     *                    table model underlying the supplied
     *                    <code>tmodel</code>
     * @param  tmodel     ViewHugeTableModel presenting an underlying
     *                    huge table to a JTable
     */
    public ViewHugeSelectionModel( ListSelectionModel hugeModel,
                                   final ViewHugeTableModel tmodel ) {
        hugeModel_ = hugeModel;
        listenerMap_ = new LinkedHashMap<ListSelectionListener,HugeListener>();

        /* Take action if the viewBase property of the tracked table model
         * changes. */
        tmodel.addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( ViewHugeTableModel.VIEWBASE_PROPERTY
                                       .equals( evt.getPropertyName() ) ) {
                    int vbase = tmodel.getViewBase();
                    assert viewBase_ == ((Number) evt.getOldValue()).intValue();
                    assert vbase == ((Number) evt.getNewValue()).intValue();
                    assert vbase != viewBase_;

                    /* Update this object's viewBase. */
                    viewBase_ = vbase;

                    /* Notify listeners to this model (most importantly
                     * the JTable) that all the selections have changed. */
                    ListSelectionEvent sevt =
                        new ListSelectionEvent( ViewHugeSelectionModel.this, 0,
                                                tmodel.getRowCount(), false );
                    for ( ListSelectionListener lnr : listenerMap_.keySet() ) {
                        lnr.valueChanged( sevt );
                    }
                }
            }
        } );
    }

    public void setSelectionInterval( int index0, int index1 ) {
        hugeModel_.setSelectionInterval( toHuge( index0 ), toHuge( index1 ) );
    }

    public void addSelectionInterval( int index0, int index1 ) {
        hugeModel_.addSelectionInterval( toHuge( index0 ), toHuge( index1 ) );
    }

    public void removeSelectionInterval( int index0, int index1 ) {
        hugeModel_.removeSelectionInterval( toHuge( index0 ),
                                            toHuge( index1 ) );
    }

    public int getMinSelectionIndex() {
        return fromHuge( hugeModel_.getMinSelectionIndex() );
    }

    public int getMaxSelectionIndex() {
        return fromHuge( hugeModel_.getMaxSelectionIndex() );
    }

    public boolean isSelectedIndex( int index ) {
        return hugeModel_.isSelectedIndex( toHuge( index ) );
    }

    public int getAnchorSelectionIndex() {
        return fromHuge( hugeModel_.getAnchorSelectionIndex() );
    }

    public void setAnchorSelectionIndex( int index ) {
        hugeModel_.setAnchorSelectionIndex( toHuge( index ) );
    }

    public int getLeadSelectionIndex() {
        return fromHuge( hugeModel_.getLeadSelectionIndex() );
    }

    public void setLeadSelectionIndex( int index ) {
        hugeModel_.setLeadSelectionIndex( toHuge( index ) );
    }

    public void clearSelection() {
        hugeModel_.clearSelection();
    }

    public boolean isSelectionEmpty() {
        return hugeModel_.isSelectionEmpty();
    }

    public void insertIndexInterval( int index, int length, boolean before ) {
        hugeModel_.insertIndexInterval( toHuge( index ), length, before );
    }

    public void removeIndexInterval( int index0, int index1 ) {
        hugeModel_.removeIndexInterval( toHuge( index0 ), toHuge( index1 ) );
    }

    public void setValueIsAdjusting( boolean valueIsAdjusting ) {
        hugeModel_.setValueIsAdjusting( valueIsAdjusting );
    }

    public boolean getValueIsAdjusting() {
        return hugeModel_.getValueIsAdjusting();
    }

    public void setSelectionMode( int selectionMode ) {
        hugeModel_.setSelectionMode( selectionMode );
    }

    public int getSelectionMode() {
        return hugeModel_.getSelectionMode();
    }

    public void addListSelectionListener( ListSelectionListener viewLnr ) {
        HugeListener hugeLnr = new HugeListener( viewLnr );
        listenerMap_.put( viewLnr, hugeLnr );
        hugeModel_.addListSelectionListener( hugeLnr );
    }

    public void removeListSelectionListener( ListSelectionListener viewLnr ) {
        HugeListener hugeLnr = listenerMap_.remove( viewLnr );
        hugeModel_.removeListSelectionListener( hugeLnr );
    }

    /**
     * Returns the row index in the underlying huge model corresponding to
     * a row index in this model.
     * Special values (&lt;0) are preserved.
     *
     * @param   viewIndex  index in this model
     * @return  index in base model
     */
    private int toHuge( int viewIndex ) {
        return viewIndex >=0 ? viewIndex + viewBase_
                             : viewIndex;
    }

    /**
     * Returns the row index in this model corresponding to
     * a row index in the underlying huge model.
     * Special values (&lt;0) are preserved.
     *
     * @param  hugeIndex  index in base model
     * @return   index in this model
     */
    private int fromHuge( int hugeIndex ) {
        return hugeIndex >= 0 ? hugeIndex - viewBase_
                              : hugeIndex;
    }

    /**
     * ListSelectionListener that translates events from the underlying
     * huge model to ones corresponding to this model.
     */
    private class HugeListener implements ListSelectionListener {
        final ListSelectionListener viewListener_;

        /**
         * Constructor.
         *
         * @param  listener to the view model
         */
        HugeListener( ListSelectionListener viewListener ) {
            viewListener_ = viewListener;
        }
        public void valueChanged( ListSelectionEvent hugeEvt ) {
            ListSelectionEvent viewEvt =
                new ListSelectionEvent( this,
                                        fromHuge( hugeEvt.getFirstIndex() ),
                                        fromHuge( hugeEvt.getLastIndex() ),
                                        hugeEvt.getValueIsAdjusting() );
            viewListener_.valueChanged( viewEvt );
        }
    }
}
