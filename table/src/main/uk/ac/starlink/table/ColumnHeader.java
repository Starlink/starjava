package uk.ac.starlink.table;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Contains information about a column in a table. 
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnHeader {

    private String name;
    private String unitString = null;
    private String ucd = null;
    private String description = "";
    private Class contentClass = Object.class;
    private Map metadata = new HashMap();

    /**
     * Constructs a new ColumnHeader object with a given name.
     *
     * @param  name  this column's name
     */
    public ColumnHeader( String name ) {
        this.name = name;
    }

    /**
     * The name of this column.  Should be a short string suitable
     * for presentation in the heading of a graphical table.
     *
     * @return  column name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representing the units of the cells in this column.
     * The syntax and conventions should ideally match those adopted
     * by VOTable, as defined by CDS.
     *
     * @return  a string giving the units, or <tt>null</tt> if units are
     *          unknown
     * @see  <a href="http://vizier.u-strasbg.fr/doc/catstd-3.2.htx">Standards
     *       for Astronomical Catalogues: Units, CDS Strasbourg</a>
     */
    public String getUnitString() {
        return unitString;
    }

    /**
     * Sets a string representing the units of the cells in this column.
     *
     * @param  unitString  a string giving the units, or <tt>null</tt> if
     *         units are unknown
     */
    public void setUnitString( String unitString ) {
        this.unitString = unitString;
    }

    /**
     * Returns the Unified Column Descriptor string applying to this column.
     *
     * @return  the UCD, or <tt>null</tt> if none is known
     * @see  <a href="http://vizier.u-strasbg.fr/doc/UCD.htx">Unified 
     *       Column Descriptor scheme</a>
     * @see  UCD
     */
    public String getUCD() {
        return ucd;
    }

    /**
     * Sets the Unified Column Descriptor string applying to this column
     *
     * @param  ucd  the UCD, or <tt>null</tt> if none is known
     */
    public void setUCD( String ucd ) {
        this.ucd = ucd;
    }

    /**
     * Returns a description of the column.  
     * It may contain a short or long textual description of the kind of
     * information held by this column.
     *
     * @return  a textual description of this column, or the empty string ""
     *          if there is nothing to be said
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets a textual description of this column.
     *
     * @param  description  a texttual description of this column, 
     *         or the empty string "" if there is nothing to be said
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * Returns the java class of objects contained in this column.
     * The intention is that any object retrieved from this column using
     * the {@link StarTable#getValueAt} method will be in instance of this
     * class or one of its subclasses.
     *
     * @return  the class of items in this column
     */
    public Class getContentClass() {
        return contentClass;
    }

    /**
     * Sets the java class of objects contained in this column.
     *
     * @param  contentClass  the class of items in this column
     */
    public void setContentClass( Class contentClass ) {
        this.contentClass = contentClass;
    }

    /**
     * Returns the map holding auxiliary metadata for this column.
     * The metadata is simply a Map intended to hold key,value pairs
     * in which each key is a String.  It is intended to hold information
     * about this column not covered by the other methods.
     *
     * @return  the Map containing key,value pairs characterising auxiliary
     *          metadata for this column.  May be written to
     */
    public Map getMetadata() {
        return metadata;
    }

    /**
     * Sets the map holding auxiliary metadata for this column.
     * The metadata is simply a Map intended to hold key,value pairs
     * in which each key is a String.  It is intended to hold information
     * about this column not covered by the other methods.
     *
     * @param  the Map containing key,value pairs characterising auxiliary
     *         metadata for this column
     */
    public void setMetadata( Map metadata ) {
        this.metadata = metadata;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer( name );
        sb.append( ":" );
        if ( unitString != null ) {
            sb.append( " units=" )
              .append( unitString );
        }
        if ( ucd != null ) {
            sb.append( " UCD=" )
              .append( ucd );
        }
        if ( contentClass != Object.class ) {
            sb.append( " class=" )
              .append( contentClass );
        }
        for ( Iterator it = metadata.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sb.append( " " )
              .append( entry.getKey() )
              .append( "=" )
              .append( entry.getValue() );
        }
        return sb.toString();
    }

}
