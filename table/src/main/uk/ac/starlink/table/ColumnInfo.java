package uk.ac.starlink.table;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains information about a table column.  This includes 
 * a description of the values contained in it (as per the {@link ValueInfo}
 * interface) as well as additional miscellaneous metadata.
 * The miscellaneous, or auxiliary, metadata takes the form of a 
 * list of {@link DescribedValue} objects.  It is the intention that
 * only one object in this list exists for each value name (as returned
 * by the <tt>DescribedValue.getName</tt> method.  This restriction
 * is not guaranteed to be enforced however.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnInfo extends DefaultValueInfo {

    private List auxData = new ArrayList();

    /**
     * Constructs a <tt>ColumnInfo</tt> object.
     *
     * @param  name  the name of the column
     */
    public ColumnInfo( String name ) {
        super( name );
    }

    /**
     * Constructs a new <tt>ColumnInfo</tt> based on a <tt>ValueInfo</tt>
     * object.  All attributes are copied from the template to the new
     * object.
     *
     * @param  base  the template <tt>ValueInfo</tt>
     */
    public ColumnInfo( ValueInfo base ) {
        super( base );
    }

    /**
     * Constructs a new <tt>ColumnInfo</tt> object with a given name,
     * class and description.
     *
     * @param  name  the name applying to described values
     * @param  contentClass  the class of which described values should be
     *         instances
     * @param  description  a textual description of the described values
     */
    public ColumnInfo( String name, Class contentClass, String description ) {
        this( new DefaultValueInfo( name, contentClass, description ) );
    }

    /**
     * Constructs a <tt>ColumnInfo</tt> object which is a 
     * copy of an existing one.
     *
     * @param  base  the template <tt>ColumnInfo</tt>
     */
    public ColumnInfo( ColumnInfo base ) {
        super( base );
        this.setAuxData( new ArrayList( base.getAuxData() ) );
    }

    /**
     * Returns a list of auxiliary metadata {@link DescribedValue} objects
     * pertaining to this column.
     * This is intended as a repository for metadata which is not
     * defined in the <tt>ValueInfo</tt> interface.
     *
     * @return   a List of <tt>DescribedValue</tt> items
     */
    public List getAuxData() {
        return auxData;
    }

    /**
     * Gets an item of auxiliary metadata by its name.
     * 
     * @param  name  the name of an auxiliary metadata item
     * @return  a <tt>DescribedValue</tt> object representing the
     *          named auxiliary metadata item for this column,
     *          or <tt>null</tt> if none exists
     */
    public DescribedValue getAuxDatumByName( String name ) {
        for ( Iterator it = auxData.iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof DescribedValue ) {
                DescribedValue dv = (DescribedValue) item;
                if ( dv.getInfo().getName().equals( name ) ) {
                    return dv;
                }
            }
        }
        return null;
    }

    /**
     * Gets an item of auxiliary metadata from its specification.
     * Currently this just calls <tt>getAuxDatumByName(vinfo.getName())</tt>,
     * but may be revised in future to match on other attributes.
     *
     * @param  vinfo  the data item to match
     * @return  a <tt>DescribedValue</tt> object representing the 
     *          auxiliary metadata item matching <tt>vinfo</tt> for this column,
     *          or <tt>null</tt> if none exists
     */
    public DescribedValue getAuxDatum( ValueInfo vinfo ) {
        return getAuxDatumByName( vinfo.getName() );
    }

    /**
     * Adds the given DescribedValue to the list of auxiliary metadata
     * for this object.  If an item in the metadata list with the same
     * name as the supplied value already exists, it is removed from the
     * list.
     *
     * @param  dval  the new datum to add
     */
    public void setAuxDatum( DescribedValue dval ) {
        DescribedValue old = getAuxDatumByName( dval.getInfo().getName() );
        if ( old != null ) {
            auxData.remove( old );
        }
        auxData.add( dval );
    }

    /**
     * Sets the list of auxiliary metadata items for this column.
     * All elements of the supplied list should be 
     * {@link DescribedValue} objects.
     *
     * @param   auxData  a list of <tt>DescribedValue</tt> objects
     */
    public void setAuxData( List auxData ) {
        this.auxData = auxData;
    }
}
