package nom.tam.fits;


/** This class allows FITS binary and ASCII tables to
 *  be accessed via a common interface.
 */

public interface TableData {
    
    public abstract Object[] getRow  (int row) throws FitsException;
    public abstract Object getColumn (int col) throws FitsException;
    public abstract Object getElement(int row, int col) throws FitsException;
    
    public abstract void setRow      (int row, Object[] newRow) throws FitsException;
    public abstract void setColumn   (int col, Object newCol) throws FitsException;
    public abstract void setElement  (int row, int col, Object element) throws FitsException;
    
    public abstract int addRow   (Object[] newRow) throws FitsException;
    public abstract int addColumn(Object newCol) throws FitsException;
    
    public abstract int getNCols();
    public abstract int getNRows();

}
