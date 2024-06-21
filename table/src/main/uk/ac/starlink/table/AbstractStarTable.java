package uk.ac.starlink.table;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Abstract base class providing an implementation of the generic and
 * straightforward parts of the <code>StarTable</code> interface.
 * This implementation assumes that random access is not available;
 * subclasses which provide random access should override 
 * the <code>isRandom</code>, <code>getCell</code>
 * and perhaps <code>getRow</code> methods.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class AbstractStarTable implements StarTable {

    private List<DescribedValue> parameters_ = new ArrayList<DescribedValue>();
    private String name_;
    private URL url_;

    /**
     * Goes through the table columns (<code>ColumnInfo</code> objects) 
     * and picks out all the AuxData items which exist, generalising
     * where necessary and returning a union of them in 
     * alphabetical order by name.
     * Subclasses should override this if they can do better, for instance
     * providing an order for the keys.
     *
     * @return  a list of all the auxiliary metadata <code>ValueInfo</code>
     *          items which in fact crop up in column metadata
     */
    public List<ValueInfo> getColumnAuxDataInfos() {
        Map<String,ValueInfo> auxMap = new TreeMap<String,ValueInfo>();
        for ( int i = 0; i < getColumnCount(); i++ ) {
            for ( DescribedValue dval : getColumnInfo( i ).getAuxData() ) {

                /* Construct a ValueInfo based on this DescribedValue. */
                ValueInfo info = dval.getInfo();
                String name = info.getName();

                /* We already have one by this name, if necessary generalise
                 * the stored ValueInfo so that it is consistent with this 
                 * one too. */
                if ( auxMap.containsKey( name ) ) {
                    ValueInfo oldInfo = auxMap.get( name );
                    auxMap.put( name, 
                                DefaultValueInfo.generalise( oldInfo, info ) );
                }

                /* Not encountered one with this name before, put it 
                 * straight in the pool. */
                else {
                    auxMap.put( name, info );
                }
            }
        }
        return Collections
              .unmodifiableList( new ArrayList<ValueInfo>( auxMap.values() ) );
    }

    public List<DescribedValue> getParameters() {
        return parameters_;
    }

    /**
     * Sets the list of table parameters, that is items which pertain
     * to the entire table.  Each element of the provided list 
     * <code>parameters</code> should be a {@link DescribedValue} object.
     *
     * @param  parameters   a List of <code>DescribedValue</code>s pertaining
     *         to this table
     */
    public void setParameters( List<DescribedValue> parameters ) {
        parameters_ = parameters;
    }

    public String getName() {
        return name_;
    }

    /**
     * Sets the name for this table. 
     *
     * @param  name  the table name - may be <code>null</code>
     */
    public void setName( String name ) {
        name_ = name;
    }

    public URL getURL() {
        return url_;
    }

    /**
     * Sets the URL for this table.
     *
     * @param  url  the URL where this table lives - may be <code>null</code>
     */
    public void setURL( URL url ) {
        url_ = url;
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
     * The <code>AbstractStarTable</code> implementation of this method 
     * returns <code>false</code>.
     */
    public boolean isRandom() {
        return false;
    }

    public RowAccess getRowAccess() throws IOException {
        throw new UnsupportedOperationException( "No random access available" );
    }

    /**
     * Returns a default splittable which relies on table random access
     * if available, or otherwise provides only sequential access (no splits).
     *
     * <p>It is often possible to provide a better implementation than this.
     *
     * @return  {@link Tables#getDefaultRowSplittable
     *                 Tables.getDefaultRowSplittable(this)}
     */
    public RowSplittable getRowSplittable() throws IOException {
        return Tables.getDefaultRowSplittable( this );
    }

    /**
     * The <code>AbstractStarTable</code> implementation of this method
     * throws an <code>UnsupportedOperationException</code>,
     * since unless otherwise provided there is no random access.
     */
    public Object getCell( long irow, int icol ) throws IOException {
        throw new UnsupportedOperationException( "No random access available" );
    }

    /**
     * The <code>AbstractStarTable</code> implementation of this method 
     * constructs a row by repeated invocation of {@link #getCell}.
     */
    public Object[] getRow( long irow ) throws IOException {
        int ncol = getColumnCount();
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = getCell( irow, icol );
        }
        return row;
    }

    /**
     * The <code>AbstractStarTable</code> implementation of this method
     * does nothing.
     */
    public void close() throws IOException {
    }

    abstract public ColumnInfo getColumnInfo( int icol );
    abstract public int getColumnCount();
    abstract public long getRowCount();
    abstract public RowSequence getRowSequence() throws IOException;

}
