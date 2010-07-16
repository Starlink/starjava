package uk.ac.starlink.table;

import java.util.Map;
import javax.swing.table.TableCellRenderer;

/**
 * Describes a value, for instance one obtained from cells in 
 * a given table column, or from a table parameter.
 * This interface encapsulates the name, description, UCD etc, as well
 * as information about the kind of object that the value can assume, 
 * such as Java class and array shape where applicable.
 * It will normally be associated with a method which is declared to 
 * return one or more <tt>Object</tt>s; use of the information in this class 
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
     * @return  a string giving the units, or <tt>null</tt> if units are
     *          unknown
     * @see  <a href="http://vizier.u-strasbg.fr/doc/catstd-3.2.htx">Standards
     *       for Astronomical Catalogues: Units, CDS Strasbourg</a>
     */
    String getUnitString();

    /**
     * Returns the Unified Column Descriptor string applying to the 
     * values described by this object.
     *
     * @return  the UCD, or <tt>null</tt> if none is known
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
     * The intention is that any <tt>Object</tt> described by this 
     * <tt>ValueInfo</tt> will be an instance of the returned class 
     * or one of its subclasses.  Note therefore that it must <em>not</em>
     * return one of the primitive class objects (<tt>int.class</tt> et al.);
     * the appropriate wrapper classes must be used instead 
     * (<tt>Integer.class</tt> etc).
     * <p>
     * The class returned should not under normal circumstances be an
     * array of arrays; to express multidimensionality of arrays you
     * should store an array of non-array objects or of primitives and
     * provide shape information via the <tt>getShape</tt> method.
     * There is nothing to stop you describing arrays of arrays with a 
     * <tt>ValueInfo</tt>, but some other elements of the tables 
     * infrastructure and applications may work on the assumption that
     * such objects are not present.
     * <p>
     * Note that to store sets of more than 2<sup>31</sup> items it
     * will be necessary to use some custom object, since java arrays
     * are indexed by type <tt>int</tt>.
     *
     * @return  the java class
     */
    Class getContentClass();

    /**
     * Indicates whether the values described by this object are java arrays.
     * This convenience method should return the same as
     * <pre>
     *     getContentClass().getComponentType()!=null
     * </pre>
     *
     * @return  <tt>true</tt> iff the values described by this object are
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
     * to indicate that the number of <tt>(shape.length-1)</tt>-dimensional
     * slices contained in the value is not known.
     * If nothing is known about the shape of the array values,
     * then a 1-element array whose only element is &lt;=0 should be returned.
     * <p>
     * If <tt>getContentClass</tt> does not give an array type 
     * (hence <tt>isArray</tt> returns false)
     * the return value for this method is undefined (but should probably
     * be <tt>null</tt>).
     *
     * @return  the shape of the array value
     */
    int[] getShape();

    /**
     * May indicate the size of a value element stored as the value of
     * this info.  The total size of the value will in this case be the
     * return value of this method multiplied by the number of elements,
     * as indicated by {@link #getShape} (or by 1, if <tt>getShape</tt>
     * is null).
     * <p>
     * The exact meaning of the value returned is dependent on this 
     * ValueInfo.  This method was introduced to return the maximum
     * number of characters in a <tt>String</tt>-class ValueInfo; 
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
     * value <tt>null</tt>.  In general this should return <tt>true</tt>, 
     * which implies no assertion about the return values (they may or 
     * may not be <tt>null</tt>).  But if the values are known never to
     * be <tt>null</tt>, it may return <tt>false</tt>.
     *
     * @return  <tt>false</tt> if values are guaranteed non-<tt>null</tt>
     */
    boolean isNullable();

    /**
     * Returns a string representation of a given value described by this
     * <tt>ValueInfo</tt>.  The returned string should be
     * no longer than a given maximum length.
     *
     * @param   value      the value to represent
     * @param   maxLength  the maximum number of characters in the returned
     *          string
     */
    String formatValue( Object value, int maxLength );

    /**
     * Returns an object of the sort described by this <tt>ValueInfo</tt>
     * represented by the given string <tt>rep</tt>.
     *
     * @param  rep  the string representation of a value described by 
     *         this <tt>ValueInfo</tt>
     * @return  the Object value represented by <tt>rep</tt>; must match
     *          this info's content class (or be null)
     */
    Object unformatString( String rep );

    /**
     * Returns a renderer suitable for rendering values described by 
     * this object in a <tt>JTable</tt> cell.
     * The renderer should ideally render objects in such a way that
     * a long column of them looks tidy.
     * A <tt>null</tt> value may be returned to indicate that smoe
     * default renderer will be used, in which case the text of the
     * rendered cell will be as provided by the {@link #formatValue}
     * method.
     *
     * @return   a custom table cell renderer, or <tt>null</tt>
     */
    TableCellRenderer getCellRenderer();

}
