package nom.tam.fits;


/** This class allows FITS binary and ASCII tables to
 *  be accessed via a common interface.
 * 
 *  Bug Fix: 3/28/01 to findColumn.
 */

public abstract class TableHDU extends BasicHDU {
    
    private TableData      table;
    private int            currentColumn;
    
    
    TableHDU(TableData td) {
	table = td;
    }
    
    public Object[] getRow(int row) throws FitsException {
        return table.getRow(row);
    }
    
    public Object getColumn(String colName) throws FitsException {
	return getColumn(findColumn(colName));
    }
    
    public Object getColumn(int col) throws FitsException {
        return table.getColumn(col);
    }
    
    public Object getElement(int row, int col) throws FitsException {
        return table.getElement(row, col);
    }
    
    public void setRow(int row, Object[] newRow) throws FitsException {
        table.setRow(row, newRow);
    }
    
    public void setColumn(String colName, Object newCol) throws FitsException {
	setColumn(findColumn(colName), newCol);
    }

    public void setColumn(int col, Object newCol) throws FitsException {
        table.setColumn(col, newCol);
    }
  
    public void setElement(int row, int col, Object element) throws FitsException {
        table.setElement(row, col, element);
    }
    
    public int addRow(Object[] newRow) throws FitsException {
	
        int row = table.addRow(newRow);
	myHeader.addValue("NAXIS2", row, null);
	return row;
    }
    
    public int findColumn(String colName) {
	
	for (int i=0; i < getNCols(); i += 1) {

	    String val = myHeader.getStringValue("TTYPE"+(i+1));
	    if (val != null  && val.trim().equals(colName)) {
		return i;
	    }
	}
	return -1;
    }
    
    public abstract int addColumn(Object data) throws FitsException;

    /** Get the number of columns for this table
      * @return The number of columns in the table.
      */
    public int getNCols()
    {
        return table.getNCols();
    }

    /** Get the number of rows for this table
      * @return The number of rows in the table.
      */
    public int getNRows()
    {
        return table.getNRows();
    }

    /** Get the name of a column in the table.
      * @param index The 0-based column index.
      * @return The column name.
      * @exception FitsException if an invalid index was requested.
      */
    public String getColumnName(int index) {

        String ttype = myHeader.getStringValue("TTYPE"+(index+1));
        if (ttype != null) {
	    ttype = ttype.trim();
        }
        return ttype;
    }
    
    public void setColumnName(int index, String name, String comment)
      throws FitsException {
	if (getNCols() > index && index >= 0) {
	    myHeader.positionAfterIndex("TFORM", index+1);
	    myHeader.addValue("TTYPE"+(index+1), name, comment);
	}
    }
    
    /** Get the FITS type of a column in the table.
      * @return The FITS type.
      * @exception FitsException if an invalid index was requested.
      */
    public String getColumnFormat(int index)
	throws FitsException
    {
        int flds = myHeader.getIntValue("TFIELDS", 0);
        if (index < 0 || index >= flds) {
            throw new FitsException("Bad column index " + index + " (only " + flds +
			      " columns)");
        }

        return myHeader.getStringValue("TFORM" + (index + 1)).trim();
    }
    
    public void setCurrentColumn(int col) {
	myHeader.positionAfterIndex("TFORM", (col+1));
    }
    
}
