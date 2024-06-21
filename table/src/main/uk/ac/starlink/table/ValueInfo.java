package uk.ac.starlink.table;

import java.util.List;
import java.util.Map;

/**
 * Describes a value, for instance one obtained from cells in 
 * a given table column, or from a table parameter.
 * This interface encapsulates the name, description, UCD etc, as well
 * as information about the kind of object that the value can assume, 
 * such as Java class and array shape where applicable.
 * It will normally be associated with a method which is declared to 
 * return one or more <code>Object</code>s;
 * use of the information in this class 
 * can then be used to make additional sense of the object(s) thus returned.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface ValueInfo {

    /**
     * Returns the name of this object.
     *
     * @return  the name
     */
    String getName();

    /**
     * Returns a string representing the units of the values described by
     * this object.
     * The syntax and conventions should ideally match those adopted
     * by VOTable, as defined by CDS.
     *
     * @return  a string giving the units, or <code>null</code> if units are
     *          unknown
     * @see  <a href="http://vizier.u-strasbg.fr/doc/catstd-3.2.htx">Standards
     *       for Astronomical Catalogues: Units, CDS Strasbourg</a>
     */
    String getUnitString();

    /**
     * Returns the Unified Column Descriptor string applying to the 
     * values described by this object.
     *
     * @return  the UCD, or <code>null</code> if none is known
     * @see  <a href="http://vizier.u-strasbg.fr/doc/UCD.htx">Unified 
     *       Column Descriptor scheme</a>
     * @see  UCD
     */
    String getUCD();

    /**
     * Returns the Utype string applying to the values described by
     * this object.  Utype is a string which references a data model.
     * It is used pervasively within IVOA standards; probably an official
     * IVOA definition of Utype syntax and semantics will arise one day.
     *
     * @return  the Utype, or <code>null</code> if none is known
     */
    String getUtype();

    /**
     * Returns the Xtype string applying to the values described by
     * this object.  The Xtype string is the 'extended type' information
     * characterising the data type beyond the primitive data type.
     * It is used within IVOA standards, and a number of standard
     * values are defined within the
     * <a href="http://www.ivoa.net/documents/DALI">DALI</a> standard.
     *
     * @return  the Xtype, or <code>null</code> if none is known
     */
    String getXtype();

    /**
     * Returns a description of the values described by this object.
     * It may contain a short or long textual description of the kind of
     * information represented by the value.
     *
     * @return  a textual description, or the empty string ""
     *          if there is nothing to be said
     */
    String getDescription();

    /**
     * Returns the java class of the values described by this object.
     * The intention is that any <code>Object</code> described by this 
     * <code>ValueInfo</code> will be an instance of the returned class 
     * or one of its subclasses.  Note therefore that it must <em>not</em>
     * return one of the primitive class objects
     * (<code>int.class</code> et al.);
     * the appropriate wrapper classes must be used instead 
     * (<code>Integer.class</code> etc).
     * <p>
     * The class returned should not under normal circumstances be an
     * array of arrays; to express multidimensionality of arrays you
     * should store an array of non-array objects or of primitives and
     * provide shape information via the <code>getShape</code> method.
     * There is nothing to stop you describing arrays of arrays with a 
     * <code>ValueInfo</code>, but some other elements of the tables 
     * infrastructure and applications may work on the assumption that
     * such objects are not present.
     * <p>
     * Note that to store sets of more than 2<sup>31</sup> items it
     * will be necessary to use some custom object, since java arrays
     * are indexed by type <code>int</code>.
     *
     * @return  the java class
     */
    Class<?> getContentClass();

    /**
     * Indicates whether the values described by this object are java arrays.
     * This convenience method should return the same as
     * <pre>
     *     getContentClass().getComponentType()!=null
     * </pre>
     *
     * @return  <code>true</code> iff the values described by this object are
     *          java arrays
     */
    boolean isArray();

    /**
     * Returns the shape associated with array value types.
     * If the class returned by {@link #getContentClass} is a java array type,
     * the return from this method may contain information about 
     * the shape of the rectangular array which this represents.
     * The dimensions of the array are given in the elements of 
     * the return value of this method, fastest varying first.
     * All elements of the array should be positive, except that the
     * last (slowest varying) element may be &lt;=0 (conventionally -1)
     * to indicate that the number of <code>(shape.length-1)</code>-dimensional
     * slices contained in the value is not known.
     * If nothing is known about the shape of the array values,
     * then a 1-element array whose only element is &lt;=0 should be returned.
     * <p>
     * If <code>getContentClass</code> does not give an array type 
     * (hence <code>isArray</code> returns false)
     * the return value for this method is undefined (but should probably
     * be <code>null</code>).
     *
     * @return  the shape of the array value
     */
    int[] getShape();

    /**
     * May indicate the size of a value element stored as the value of
     * this info.  The total size of the value will in this case be the
     * return value of this method multiplied by the number of elements,
     * as indicated by {@link #getShape} (or by 1, if <code>getShape</code>
     * is null).
     * <p>
     * The exact meaning of the value returned is dependent on this 
     * ValueInfo.  This method was introduced to return the maximum
     * number of characters in a <code>String</code>-class ValueInfo; 
     * this information is necessary for writing out to certain formats (FITS).
     * Other ValueInfo types however may use it for their own purposes.
     * <p>
     * ValueInfo instances which decline to supply this information 
     * should return -1 from this method.
     * 
     * @return   notional size of each element an array of values described
     *           by this info
     */
    int getElementSize();

    /**
     * Indicates whether values returned described by this object may have the
     * value <code>null</code>.
     * In general this should return <code>true</code>, 
     * which implies no assertion about the return values (they may or 
     * may not be <code>null</code>).  But if the values are known never to
     * be <code>null</code>, it may return <code>false</code>.
     *
     * @return  <code>false</code> if values are guaranteed
     *          non-<code>null</code>
     */
    boolean isNullable();

    /**
     * Returns a list of auxiliary metadata objects
     * pertaining to this info.
     * This is intended as a repository for metadata which is not
     * otherwise made available in this interface.
     *
     * @return   a List of <code>DescribedValue</code> items
     */
    List<DescribedValue> getAuxData();

    /**
     * Returns an array of objects which may be able to convert from the
     * values described by this info to a particular target value domain.
     * This can used for non-obvious representations of certain coordinates
     * such as time and angular position.  In most cases the returned array
     * will be empty, since the target domain is obvious (e.g. numeric values).
     * In the (unusual) case that the returned array contains multiple
     * entries, it should have no more than one for any given target domain,
     * and the first entry may be considered "primary" in some sense.
     * Absence of a mapper for a given target domain does not necessarily
     * indicate that the described values cannot be used in that domain.
     *
     * @return   array of domain mappers for the values described by this info
     */
    DomainMapper[] getDomainMappers();

    /**
     * Returns a string representation of a given value described by this
     * <code>ValueInfo</code>.  The returned string should be
     * no longer than a given maximum length.
     *
     * @param   value      the value to represent
     * @param   maxLength  the maximum number of characters in the returned
     *          string
     */
    String formatValue( Object value, int maxLength );

    /**
     * Returns an object of the sort described by this <code>ValueInfo</code>
     * represented by the given string <code>rep</code>.
     *
     * @param  rep  the string representation of a value described by 
     *         this <code>ValueInfo</code>
     * @return  the Object value represented by <code>rep</code>; must match
     *          this info's content class (or be null)
     */
    Object unformatString( String rep );

    /**
     * Gets an item of auxiliary metadata by its name.
     *
     * @param  name  the name of an auxiliary metadata item
     * @return  a <code>DescribedValue</code> object representing the
     *          named auxiliary metadata item for this column,
     *          or <code>null</code> if none exists
     */
    default DescribedValue getAuxDatumByName( String name ) {
        return Tables.getDescribedValueByName( getAuxData(), name );
    }

    /**
     * Adds the given DescribedValue to the list of auxiliary metadata
     * for this object.  If an item in the metadata list with the same
     * name as the supplied value already exists, it is removed from the
     * list.
     *
     * @param  dval  the new datum to add
     */
    default void setAuxDatum( DescribedValue dval ) {
        Tables.setDescribedValue( getAuxData(), dval );
    }
}
