package uk.ac.starlink.splat.vo;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableModel;

/**
 * ServerPopupTable
 * An extension of JTable with row popup support
 * 
 * @author Margarida Castro Neves
 *
 */
class RowPopupTable extends JTable {
           
    public RowPopupTable() {        
        super();
    }

    public Point getPopupLocation(MouseEvent event) {
        setPopupTriggerLocation(event);
      
        return super.getPopupLocation(event);
    }
    protected void setPopupTriggerLocation(MouseEvent event) {
        putClientProperty("popupTriggerLocation", 
                event != null ? event.getPoint() : null);
    }

    public int getPopupRow () {
        int row = rowAtPoint( (Point) getClientProperty("popupTriggerLocation") );
        return convertRowIndexToModel(row);
    }    
 
    public boolean isCellEditable(int row, int col) {
        return false; //Disable cell editing
    }    

}
