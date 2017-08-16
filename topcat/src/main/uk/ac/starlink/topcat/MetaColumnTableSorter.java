package uk.ac.starlink.topcat;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.util.gui.SortingHeaderRenderer;

/**
 * Handles the GUI aspects of sorting a MetaColumnTableModel.
 * It provides the mouse listeners that set up sorting and
 * ensures that the sorting methods are called on the table model
 * as required to keep the row order up to date.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2017
 */
public class MetaColumnTableSorter {

    private final MetaColumnTableModel model_;
    private final MouseListener headerListener_;
    private final JLabel unsortLabel_;
    private JTableHeader header_;
    private int icolSort_;
    private boolean isDescending_;

    /**
     * Constructor.
     *
     * @param   model   the model whose sorting this sorter orchestrates
     */
    public MetaColumnTableSorter( MetaColumnTableModel model ) {
        model_ = model;
        icolSort_ = -1;
        headerListener_ = new MouseAdapter() {
            public void mouseClicked( MouseEvent evt ) {
                JTableHeader header = (JTableHeader) evt.getSource();
                TableColumnModel colModel = header.getColumnModel();
                int iViewcol = header.columnAtPoint( evt.getPoint() );
                int icol = colModel.getColumn( iViewcol ).getModelIndex();
                if ( icol > -1 ) {
                    if ( icolSort_ == icol ) {
                        if ( isDescending_ ) { 
                            setSorting( -1, false );
                        }
                        else {
                            setSorting( icol, true );
                        }
                    }
                    else {
                        setSorting( icol, false );
                    }   
                }
            }
        };
        unsortLabel_ = new JLabel( (Icon) null );
        unsortLabel_.addMouseListener( new MouseAdapter() {
            public void mouseClicked( MouseEvent evt ) {
                if ( icolSort_ >= 0 ) {
                    setSorting( -1, false );
                }
            }
        } );
        model.addTableModelListener( new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                reSort();
            }
        } );
    }

    /**
     * Sets this object up to allow sorting its model by clicking on a
     * given JTable header.  Currently only one header can be installed
     * at a time.  You can uninstall it by calling this method with a
     * null argument.
     *
     * @param  header   table header component
     */
    public void install( JTableHeader header ) {

        /* Uninstall any previous header. */
        if ( header_ != null ) {
            header_.removeMouseListener( headerListener_ );
            TableCellRenderer rend1 = header_.getDefaultRenderer();
            if ( rend1 instanceof SortingHeaderRenderer ) {
                TableCellRenderer rend0 =
                    ((SortingHeaderRenderer) rend1).getBaseRenderer();
                header_.setDefaultRenderer( rend0 );
            }
        }

        /* Install new renderer. */
        TableCellRenderer rend0 = header.getDefaultRenderer();
        header_ = header;
        header_.setDefaultRenderer( new SortingHeaderRenderer( rend0 ) {
            public int getSortColumnIndex() {
                return icolSort_;
            }
            public boolean isSortDescending() {
                return isDescending_;
            }
        } );
        header_.addMouseListener( headerListener_ );
        repaintComponents();
    }

    /**
     * Returns a component that will display an indication of when
     * the natural (unsorted) sequence is in use.  Clicking on this
     * component will cause natural order to resume.
     *
     * @return  small label indicating unsortedness
     */
    public JComponent getUnsortLabel() {
        return unsortLabel_;
    }

    /**
     * Instructs this sorter to keep the table model sorted
     * according to a given column.
     * If no sort order is defined for the requested column,
     * nothing happens.
     *
     * @param  icol  sort column index, or -1 for no sorting
     * @param  descending  true for descending sequence, false for ascending;
     *         ignored for no sort column
     */
    public void setSorting( int icol, boolean descending ) {
        if ( icol < 0 || model_.canSort( getColumn( icol ) ) ) {
            icolSort_ = icol;
            isDescending_ = descending;
            repaintComponents();
            if ( header_ != null ) {
                header_.repaint();
                Icon unsortIcon =
                      icolSort_ < 0
                    ? SortingHeaderRenderer
                     .createArrowIcon( descending, header_.getFont().getSize() )
                    : null;
                unsortLabel_.setIcon( unsortIcon );
            }
            reSort();
        }
    }

    /**
     * Returns the index of the column on which sorting is in effect.
     *
     * @return  index of column in this sorter's table model on which
     *          sorting is being performed, or -1 for natural order
     */
    public int getSortIndex() {
        return icolSort_;
    }

    /**
     * Returns sort sense.
     * Irrelevant if no sort column.
     *
     * @return  true for descending, false for ascending
     */
    public boolean isDescending() {
        return isDescending_;
    }

    /**
     * Returns the MetaColumn object for a given column index in this model.
     *
     * @param  icol  column index
     * @return  column object, or null if icol is out of range
     */
    private MetaColumn getColumn( int icol ) {
        return icol >= 0 ? model_.getColumnList().get( icol ) : null;
    }

    /**
     * Performs a sort on the rows of the underlying table model,
     * and informs listeners if it may have changed the table data.
     */
    private void reSort() {
        if ( model_.sortRows( getColumn( icolSort_ ), isDescending_ ) ) {
            model_.fireTableDataChanged();
        }
    }

    /**
     * Must be called to update the appearance of the components
     * affected by this sorter if the sort order may have changed.
     */
    private void repaintComponents() {
        int size = 12;
        if ( header_ != null ) {
            header_.repaint();
            size = header_.getFont().getSize();
        }
        Icon unsortIcon =
              icolSort_ < 0
            ? SortingHeaderRenderer.createArrowIcon( isDescending_, size )
            : null;
        unsortLabel_.setIcon( unsortIcon );
    }
}
