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
 * straightforward parts of the <tt>StarTable</tt> interface.
 * This implementation assumes that random access is not available;
 * subclasses which provide random access should override 
 * the <tt>isRandom</tt>, <tt>getCell</tt> and perhaps <tt>getRow</tt> methods.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class AbstractStarTable implements StarTable {

    private List parameters_ = new ArrayList();
    private String name_;
    private URL url_;

    /**
     * Goes through the table columns (<tt>ColumnInfo</tt> objects) 
     * and picks out all the AuxData items which exist, generalising
     * where necessary and returning a union of them in 
     * alphabetical order by name.
     * Subclasses should override this if they can do better, for instance
     * providing an order for the keys.
     *
     * @return  a list of all the auxiliary metadata <tt>ValueInfo</tt> items
     *          which in fact crop up in column metadata
     */
    public List getColumnAuxDataInfos() {
        Map auxMap = new TreeMap();  // order alphabetically
        for ( int i = 0; i < getColumnCount(); i++ ) {
           for ( Iterator it = getColumnInfo( i ).getAuxData().iterator();
                 it.hasNext(); ) {

               /* Construct a ValueInfo based on this DescribedValue. */
               DescribedValue dval = (DescribedValue) it.next();
               ValueInfo info = dval.getInfo();
               String name = info.getName();

               /* We already have one by this name, if necessary generalise
                * the stored ValueInfo so that it is consistent with this 
                * one too. */
               if ( auxMap.containsKey( name ) ) {
                   ValueInfo oldInfo = (ValueInfo) auxMap.get( name );
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
        return Collections.unmodifiableList( new ArrayList( auxMap.values() ) );
    }

    public List getParameters() {
        return parameters_;
    }

    /**
     * Sets the list of table parameters, that is items which pertain
     * to the entire table.  Each element of the provided list 
     * <tt>parameters</tt> should be a {@link DescribedValue} object.
     *
     * @param  parameters   a List of <tt>DescribedValue</tt>s pertaining
     *         to this table
     */
    public void setParameters( List parameters ) {
        parameters_ = parameters;
    }

    public void setParameter( DescribedValue dval ) {
        DescribedValue old = getParameterByName( dval.getInfo().getName() );
        List parameters = getParameters();
        if ( old != null ) {
            parameters.remove( old );
        }
        parameters.add( dval );
    }

    public String getName() {
        return name_;
    }

    /**
     * Sets the name for this table. 
     *
     * @param  name  the table name - may be <tt>null</tt>
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
     * @param  url  the URL where this table lives - may be <tt>null</tt>
     */
    public void setURL( URL url ) {
        url_ = url;
    }

    public DescribedValue getParameterByName( String parname ) {
        for ( Iterator it = getParameters().iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof DescribedValue ) {
                DescribedValue dval = (DescribedValue) item;
                if ( parname.equals( dval.getInfo().getName() ) ) {
                    return dval;
                }
            }
        }
        return null;
    }

    /**
     * Convenience method to get an <tt>int</tt> value from a <tt>long</tt>.
     * Invokes {@link Tables#checkedLongToInt}.
     */
    public static int checkedLongToInt( long lval ) {
        return Tables.checkedLongToInt( lval );
    }

    /**
     * The <tt>AbstractStarTable</tt> implementation of this method 
     * returns <tt>false</tt>.
     */
    public boolean isRandom() {
        return false;
    }

    /**
     * The <tt>AbstractStarTable</tt> implementation of this method throws an
     * <tt>UnsupportedOperationException</tt>, since unless otherwise
     * provided there is no random access.
     */
    public Object getCell( long irow, int icol ) throws IOException {
        throw new UnsupportedOperationException( "No random access available" );
    }

    /**
     * The <tt>AbstractStarTable</tt> implementation of this method 
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

    abstract public ColumnInfo getColumnInfo( int icol );
    abstract public int getColumnCount();
    abstract public long getRowCount();
    abstract public RowSequence getRowSequence() throws IOException;

}
