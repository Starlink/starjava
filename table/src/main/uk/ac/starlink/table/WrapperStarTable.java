package uk.ac.starlink.table;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * StarTable which wraps another StarTable.  This class acts as a wrapper
 * around an existing 'base' StarTable object; almost all its methods are
 * implemented by forwarding to the corresponding methods of that base
 * table.  The exception is the {@link #getURL} method which returns
 * <code>null</code> as an indication that the actual table is not a persistent
 * one (though it may be based on, and even identical to, a persistent one).
 *
 * <p>This class is provided so that it can be extended by
 * subclasses which modify the view of the base table in useful ways.
 *
 * <p>Subclasses should take care to ensure that all the data access methods
 * are overridden in a consistent way: {@link #getCell}, {@link #getRow},
 * {@link #getRowSequence}, {@link #getRowAccess} and {@link #getRowSplittable}.
 *
 * @author   Mark Taylor (Starlink)
 * @see      WrapperRowSequence
 */
public class WrapperStarTable implements StarTable {

    protected StarTable baseTable;
    private String name;
    private boolean nameSet;
    private URL url;

    /**
     * Constructs a new <code>WrapperStarTable</code> from a given base table.
     *
     * @param  baseTable  the table to which methods invoked upon the
     *         new wrapper table will be forwarded
     */
    public WrapperStarTable( StarTable baseTable ) {
        this.baseTable = baseTable;
    }

    /**
     * Returns the base table underlying this wrapper table.
     *
     * @return  the table to which methods invoked upon this 
     *          wrappter table are forwarded
     */
    public StarTable getBaseTable() {
        return baseTable;
    }

    public int getColumnCount() {
        return baseTable.getColumnCount();
    }

    public long getRowCount() {
        return baseTable.getRowCount();
    }

    /**
     * Initially returns <code>null</code> to indicate that this table 
     * itself is not persistent.
     */
    public URL getURL() {
        return url;
    }

    public void setURL( URL url ) {
        this.url = url;
    }

    public String getName() {
        return nameSet ? name 
                       : baseTable.getName();
    }

    public void setName( String name ) {
        this.name = name;
        this.nameSet = true;
    }

    public List<DescribedValue> getParameters() {
        return baseTable.getParameters();
    }

    public DescribedValue getParameterByName( String parname ) {
        return baseTable.getParameterByName( parname );
    }

    public void setParameter( DescribedValue dval ) {
        DescribedValue old = getParameterByName( dval.getInfo().getName() );
        if ( old != null ) {
            baseTable.getParameters().remove( old );
        }
        baseTable.getParameters().add( dval );
    } 

    public ColumnInfo getColumnInfo( int icol ) {
        return baseTable.getColumnInfo( icol );
    }

    public List<ValueInfo> getColumnAuxDataInfos() {
        return baseTable.getColumnAuxDataInfos();
    }

    public RowSequence getRowSequence() throws IOException {
        return baseTable.getRowSequence();
    }

    public RowAccess getRowAccess() throws IOException {
        return baseTable.getRowAccess();
    }

    public RowSplittable getRowSplittable() throws IOException {
        return baseTable.getRowSplittable();
    }

    public boolean isRandom() {
        return baseTable.isRandom();
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return baseTable.getCell( irow, icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return baseTable.getRow( irow );
    }

    public void close() throws IOException {
        baseTable.close();
    }

    /**
     * Convenience method to get an <code>int</code>
     * value from a <code>long</code>.
     * Invokes {@link Tables#checkedLongToInt}.
     */
    public static int checkedLongToInt( long lval ) {
        return Tables.checkedLongToInt( lval );
    }

    /**
     * Returns an indication of the wrapper structure of this table.
     *
     * @return   string representation
     */
    public String toString() {
        StringBuffer sbuf = new StringBuffer( super.toString() );
        for ( StarTable table = this; table instanceof WrapperStarTable; ) {
            table = ((WrapperStarTable) table).getBaseTable();
            sbuf.append( " -> " );
            sbuf.append( table.getClass().getName() );
        }
        return sbuf.toString();
    }
}
