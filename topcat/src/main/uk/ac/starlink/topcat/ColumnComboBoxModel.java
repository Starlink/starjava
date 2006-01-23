package uk.ac.starlink.topcat;

import javax.swing.AbstractListModel;
import javax.swing.ListCellRenderer;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.util.gui.WeakTableColumnModelListener;

/**
 * Adaptor class which turns a {@link javax.swing.table.TableColumnModel}
 * into a {@link javax.swing.ComboBoxModel}.  This model is designed
 * to reflect the contents of a column model rather than the other 
 * way around, so in general you wouldn't want to add a ListDataListener
 * to this model, you'd add it to the underlying column model.
 * <p>
 * Selections in the column model are not reflected by selections in
 * this model, but columns added/moved/removed are.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnComboBoxModel extends AbstractListModel 
                                 implements TableColumnModelListener,
                                            ComboBoxModel {

    private final TableColumnModel colModel;
    private TableColumn selected;
    private boolean hasNone;
    private int dummyCount;

    private static ListCellRenderer colRenderer_ = new ColumnCellRenderer();

    public static final StarTableColumn NO_COLUMN =
        new StarTableColumn( new ColumnInfo( "" ), -1 );
    

    /**
     * Constructs a new ComboBoxModel based on a given column model,
     * optionally with a null entry at the head of the list.
     *
     * @param  colModel   the column model
     * @param  hasNone  true iff an additional null entry at the head of
     *                  the list is required
     */
    public ColumnComboBoxModel( TableColumnModel colModel, boolean hasNone ) {
        this.colModel = colModel;
        setHasNone( hasNone );
        colModel.addColumnModelListener( 
            new WeakTableColumnModelListener( this ) );
    }

    /**
     * Sets whether there should be a null entry at the head of the list.
     *
     * @param  hasNone  true iff an additional null entry at the head of
     *                  the list is required
     */
    public void setHasNone( boolean hasNone ) {
        this.hasNone = hasNone;
        this.dummyCount = hasNone ? 1 : 0;
    }

    public TableColumnModel getColumnModel() {
        return colModel;
    }

    public Object getElementAt( int index ) {
        index -= dummyCount;
        return index >= 0 ? colModel.getColumn( index ) : NO_COLUMN;
    }

    public int getSize() {
        return colModel.getColumnCount() + dummyCount;
    }

    /**
     * The returned object is guaranteed to be a 
     * {@link javax.swing.table.TableColumn} or null.
     *
     * @return  the selected <tt>TableColumn</tt>
     */
    public Object getSelectedItem() {
        return selected;
    }

    /**
     * The selected <tt>item</tt> must be a 
     * {@link javax.swing.table.TableColumn} object.
     *
     * @param  item  a table column to select
     * @throws ClassCastException  if <tt>item</tt> is not null or a 
     *         <tt>TableColumn</tt>
     */
    public void setSelectedItem( Object item ) {
        if ( selected != item ) {
            selected = (TableColumn) item;

            /* This bit of magic is copied from the J2SE1.4 
             * DefaultComboBoxModel implementation - seems to be necessary
             * to send the right events, but not otherwise documented. */
            fireContentsChanged( this, -1, -1 );
        }
    }

    /**
     * Returns a new JComboBox based on this model.
     * This convenience method, as well as installing this model into a 
     * new JComboBox instance, also installs a suitable renderer for 
     * displaying the elements. 
     *
     * @return  new combo box displaying this model
     * @see     ColumnCellRenderer
     */
    public JComboBox makeComboBox() {
        JComboBox box = new JComboBox( this );
        box.setRenderer( colRenderer_ );
        return box;
    }


    /*
     * Implementation of the TableColumnModelListener interface.
     */

    public void columnAdded( TableColumnModelEvent evt ) {
        int index = dummyCount + evt.getToIndex();
        fireIntervalAdded( this, index, index );
    }

    public void columnRemoved( TableColumnModelEvent evt ) {
        int index = dummyCount + evt.getFromIndex();
        fireIntervalRemoved( this, index, index );
    }

    public void columnMoved( TableColumnModelEvent evt ) {
        int index0 = dummyCount + evt.getFromIndex();
        int index1 = dummyCount + evt.getToIndex();
        fireContentsChanged( this, index0, index1 );
    }

    public void columnMarginChanged( ChangeEvent evt ) {}

    public void columnSelectionChanged( ListSelectionEvent evt ) {}
    
}
