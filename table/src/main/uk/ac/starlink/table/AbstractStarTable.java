package uk.ac.starlink.table;

import java.io.IOException;
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
 * the <tt>isRandom</tt>, <tt>getCell</tt> and <tt>getRow</tt> methods.
 *
 * @author   Mark Taylor (Starlink)
 */
public abstract class AbstractStarTable implements StarTable {

    private List parameters = new ArrayList();

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
        return parameters;
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
        this.parameters = parameters;
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
     * If the supplied long integer <tt>lval</tt> is out of the range
     * which can be represented in an <tt>int</tt>, then unlike a
     * typecast, this method will throw an <tt>IllegalArgumentException</tt>.
     *
     * @param  the <tt>long</tt> value to convert
     * @return an <tt>int</tt> value which has the same value as <tt>lval</tt>
     * @throws IllegalArgumentException  if the conversion cannot be done
     */
    public static int checkedLongToInt( long lval ) {
        int ival = (int) lval;
        if ( (long) ival == lval ) {
            return ival;
        }
        else {
            if ( ival < Integer.MIN_VALUE ) {
                throw new IllegalArgumentException( "Out of supported range: "
                    + ival + " < Integer.MIN_VALUE" );
            }
            else if ( ival > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException( "Out of supported range: "
                    + ival + " > Integer.MAX_VALUE" );
            }
            else {
                throw new AssertionError();
            }
        }
    }

    public boolean isRandom() {
        return false;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        throw new UnsupportedOperationException( "No random access available" );
    }

    public Object[] getRow( long irow ) throws IOException {
        throw new UnsupportedOperationException( "No random access available" );
    }

    abstract public ColumnInfo getColumnInfo( int icol );
    abstract public int getColumnCount();
    abstract public long getRowCount();
    abstract public RowSequence getRowSequence() throws IOException;

}
