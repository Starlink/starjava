package nom.tam.util;

/* Copyright: Thomas McGlynn 1997-1998.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */


/** This interface defines the properties that
  * a generic table should have.
  */

public interface DataTable {

    public abstract void   setRow(int row, Object newRow)
        throws TableException;
    public abstract Object getRow(int row);

    public abstract void   setColumn(int column, Object newColumn)
        throws TableException;
    public abstract Object getColumn(int column);

    public abstract void   setElement(int row, int col, Object newElement)
        throws TableException;
    public abstract Object getElement(int row, int col);

    public abstract int getNRows();
    public abstract int getNCols();

}
