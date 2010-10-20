/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.starlink.topcat.contrib.basti;

import java.util.Hashtable;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author molinaro
 * 
 * generates the table model for result displaying
 */
class ResultsTableModel extends AbstractTableModel {
    
    /**
     * Results rows part of total results rows.
     * The 2 rows discarded are: a descrition and the column names
     */
    public int getRowCount() {
        return BaSTIPOSTMessage.SQLresults.length - 2;
    }

    /**
     * Results columns are 2 less the total output columns 
     * since ID_OUT is not considered and PATH and FILENAME are concatenated
     */
    public int getColumnCount() {
        return (BaSTIPOSTMessage.SQLresults[1].split(":")).length - 2;
    }

    /**
     * Single cells are re-arranged from results full table
     * concatenating URL and discarding description lines
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = new Object();
        // split sql result array
        String[] rowArray = BaSTIPOSTMessage.SQLresults[rowIndex+2].split(":");
        /* return element */
        if ( columnIndex == 0) {
            //value = "http://albione.oa-teramo.inaf.it/POSTQuery/getVOTable.php?" + rowArray[1] + rowArray[2];
            value = rowArray[2];
            return value;
        } else {
            if ( rowArray.length == getColumnCount()-1 ) {
                value = rowArray[columnIndex+2];
            } else {
                value = ( columnIndex+2 < rowArray.length )? rowArray[columnIndex+2] : "";
            }
            return value;
        }
    }

    /**
     * simply converts DB column names to Query Panel diplayed ones
     */
    @Override
    public String getColumnName(int col) {
        /* table for column names conversion */
        Hashtable DBtoDisplay = new Hashtable();
        DBtoDisplay.put("FILENAME", "BaSTI Table");
        DBtoDisplay.put("FILE_TYPE", "Data Type");
        DBtoDisplay.put("SCENARIO_TYPE", "Scenario");
        DBtoDisplay.put("TYPE", "Type");
        DBtoDisplay.put("MASS_LOSS", "Mass Loss");
        DBtoDisplay.put("PHOT_SYSTEM", "Photometry");
        DBtoDisplay.put("HED_TYPE", "Mixture");
        DBtoDisplay.put("AGE", "Age [Gyr]");
        DBtoDisplay.put("MASS", "Mass [MSun]");
        DBtoDisplay.put("Z", "Z");
        DBtoDisplay.put("Y", "Y");
        DBtoDisplay.put("FE_H", "[Fe/H]");
        DBtoDisplay.put("M_H", "[M/H]");
        // split column names line from result
        String[] names = BaSTIPOSTMessage.SQLresults[1].split(":");
        // add 2 to col number
        // ID and PATH will not be directly displayed in result
        col += 2;
        /* return correct column name */
        return DBtoDisplay.get(names[col]).toString();
    }

    /**
     * result cells will not be editable
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        return false; 
    }

}
