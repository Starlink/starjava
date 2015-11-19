/**
 * 
 */
package uk.ac.starlink.splat.util;

import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;

/**
 * Class of static members that provide utility functions for JTable.
 * 
 * @author Andresic
 * @version $Id$
 *
 */
public class JTableUtilities {
	
	// Logger.
    private static Logger logger =  Logger.getLogger( "uk.ac.starlink.splat.util.JTableUtilities" );
	
	/**
     * Class of static methods, so no construction.
     */
    private JTableUtilities()
    {
        //  Do nothing.
    }
    
    /**
     * Gets the content of currently selected JTable cell as String
     * or returns null, if the selection is invalid.
     * 
     * @param table
     * @return
     */
    public static String getCurrentCellContent(JTable table) {
    	Utilities.checkObject(table, "Table must be set.");
    	
    	int row = table.getSelectedRow();
		int col = table.getSelectedColumn();
		
		boolean validSelection = row > -1 && col > -1;
		
		if (validSelection) {
			Object value = table.getValueAt(row, col);
			String strValue = value == null ? "" : value.toString();
			
			return strValue;
		} else {
			logger.warning("Invalid selection.");
			return null;
		}
    }
    
    /**
     * Gets all the content of the given JTable String
     * or returns null, if the selection is invalid.
     * 
     * @param table
     * @param lineBreak
     * @param cellBreak
     * @return
     */
    public static String getAllContent(JTable table, String lineBreak, String cellBreak) {
    	Utilities.checkObject(table, "Table must be set.");
    	Utilities.checkObject(lineBreak, "Line break must be set.");
    	Utilities.checkObject(cellBreak, "Cell break must be set.");
    	
    	int numCols=table.getColumnCount(); 
        int numRows=table.getRowCount(); 
        int[] rowsSelected=Utilities.range(0,numRows); 
        int[] colsSelected=Utilities.range(0,numCols); 
        
        return getContent(table, lineBreak, cellBreak, numCols, numRows, 
        		rowsSelected, colsSelected);
    }
    
    /**
     * Gets all the content of the given JTable (including column names) as String
     * or returns null, if the selection is invalid.
     * 
     * @param table
     * @param lineBreak
     * @param cellBreak
     * @return
     */
    public static String getAllContentWithHeaders(JTable table, String lineBreak, String cellBreak) {
    	Utilities.checkObject(table, "Table must be set.");
    	Utilities.checkObject(lineBreak, "Line break must be set.");
    	Utilities.checkObject(cellBreak, "Cell break must be set.");
    	
    	// get column names
    	String headerColumns = getColumnNames(table, lineBreak, cellBreak); 
    	
    	// get table data
    	String content = getAllContent(table, lineBreak, cellBreak);
    	
    	return headerColumns + content;
    }
    
    /**
     * Gets the content of current JTable selection as String
     * or returns null, if the selection is invalid.
     * 
     * @param table
     * @param lineBreak
     * @param cellBreak
     * @return
     */
    public static String getCurrentSelectionContent(JTable table, String lineBreak, String cellBreak) {
    	int numCols=table.getSelectedColumnCount(); 
        int numRows=table.getSelectedRowCount(); 
        int[] rowsSelected=table.getSelectedRows(); 
        int[] colsSelected=table.getSelectedColumns();
        
        return getContent(table, lineBreak, cellBreak, numCols, numRows, 
        		rowsSelected, colsSelected);
    }
    
    //
    
    private static String getContent(JTable table, String lineBreak, String cellBreak,
    		int columnCount, int rowCount, int[] selectedRowsCount, int[] selectedColumsCount) {
    	
    	if (columnCount > 0 && rowCount > 0) {
            StringBuffer value = new StringBuffer();
            
            for (int i=0; i<rowCount; i++) { 
                    for (int j=0; j<columnCount; j++) { 
                    	value.append(escapeContentBreaks(table.getValueAt(selectedRowsCount[i], selectedColumsCount[j]), lineBreak, cellBreak)); 
                            if (j<columnCount-1) { 
                            	value.append(cellBreak); 
                            } 
                    } 
                    value.append(lineBreak); 
            }
            
			return value.toString();
        } else {
        	logger.warning("Invalid selection.");
        	return null;
        }
    }
    
    private static String getColumnNames(JTable table, String lineBreak, String cellBreak) {
    	StringBuilder headerColumnsSB = new StringBuilder();
    	for (int i = 0; i < table.getTableHeader().getColumnModel().getColumnCount(); i++) {
    		Object headerColumn = table.getColumnName(i);
    		headerColumnsSB.append(headerColumn == null ? "" : headerColumn.toString());
    		
    		if (i != table.getTableHeader().getColumnModel().getColumnCount() - 1) {
    			headerColumnsSB.append(cellBreak);
    		}
    	}
    	String headerColumns = headerColumnsSB.toString() + lineBreak;
    	
    	return headerColumns;
    }
    
    private static String escapeContentBreaks(Object cell, String lineBreak, String cellBreak) { 
        return cell == null ? "" : cell.toString().replace(lineBreak, " ").replace(cellBreak, " "); 
	}
}
