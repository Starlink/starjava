package uk.ac.starlink.votable;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 * A {@link javax.swing.table.TableModel} implementation based on a 
 * VOTable which permits random access to its cells.  This is not 
 * suitable for a table which has an arbitrarily large number of rows.
 *
 * <p>This implementation is rather simple and, because of the way in 
 * which the VOTable classes wrap array objects, a JTable built on it
 * may not be successful in diplaying cell contents in all cases,
 * though using the {@link uk.ac.starlink.table.gui.StarTableCellRenderer}
 * will improve matters.  For more sophisticated VOTable handling,
 * you are advised to use a {@link RandomVOStarTable} in conjunction
 * with a {@link uk.ac.starlink.table.gui.StarJTable}, or write your
 * own {@link javax.swing.table.TableModel}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RandomVOTableModel extends AbstractTableModel {

    private RandomVOTable rtable;

    /**
     * Construct a RandomVOTable from a VOTable <tt>Table</tt> object.
     *
     * @param  votable  the table object
     */
    public RandomVOTableModel( Table votable ) {
        this.rtable = new RandomVOTable( votable );
    }

    public int getColumnCount() {
        return rtable.getColumnCount();
    }

    public int getRowCount() {
        return rtable.getRowCount();
    }

    public Object getValueAt( int irow, int icol ) {
        return rtable.getValueAt( irow, icol );
    }

}
