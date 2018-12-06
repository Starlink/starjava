package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.table.StarTable;

/**
 * Typed key for use in a ReportMap.
 * Instances of this class identify an item of data generated when
 * plotting a layer.
 * They are classed as "general interest" or not.
 * General interest keys represent data that clients could consider
 * passing on to a human user, while non-general-interest ones are
 * generally intended for consumption by parts of the implementation
 * that understand the details of particular report types.
 * 
 * @author   Mark Taylor
 * @since    9 Dec 2014
 */
public abstract class ReportKey<T> {

    private final ReportMeta meta_;
    private final Class<T> clazz_;
    private final boolean isGeneralInterest_;

    /**
     * Constructor.
     *
     * @param   meta   metadata describing this key
     * @param   clazz  type of data item described by this key
     * @param   isGeneralInterest  indicates whether this key represents
     *          a general purpose report
     */
    public ReportKey( ReportMeta meta, Class<T> clazz,
                      boolean isGeneralInterest ) {
        meta_ = meta;
        clazz_ = clazz;
        isGeneralInterest_ = isGeneralInterest;
    }

    /**
     * Returns this key's metadata.
     *
     * @return  descriptive metadata
     */
    public ReportMeta getMeta() {
        return meta_;
    }

    /**
     * Returns the type of object identified by this key.
     *
     * @return   value class
     */
    public Class<T> getValueClass() {
        return clazz_;
    }

    /**
     * Indicates whether this key represents a key of general interest.
     * General interest reports can/should be presented to the user by a
     * general purpose UI as plot feedback and the corresponding values
     * should have a sensible toString implemenatation.
     * If the return value is false, the corresponding report is only
     * intended for plotter-specific code that understands what it's getting.
     *
     * @return   true  if general purpose code should present report items
     *                 to the user in their stringified form
     */
    public boolean isGeneralInterest() {
        return isGeneralInterest_;
    }

    /**
     * Serializes a value associated with this key in a way that
     * can be presented to a human user.
     *
     * @param  value   value for this key
     * @return   short text representation
     */
    public abstract String toText( T value );

    /**
     * Constructs a string-valued key.
     *
     * @param   meta   metadata describing the key
     * @param   isGeneralInterest  indicates whether the key represents
     *          a general purpose report
     * @return   new key
     */
    public static ReportKey<String>
            createStringKey( ReportMeta meta, boolean isGeneralInterest ) {
        return new ReportKey<String>( meta, String.class, isGeneralInterest ) {
            public String toText( String value ) {
                return value;
            }
        };
    }

    /**
     * Constructs a double-precision-valued key.
     *
     * @param   meta   metadata describing the key
     * @param   isGeneralInterest  indicates whether the key represents
     *          a general purpose report
     * @return   new key
     */
    public static ReportKey<Double>
            createDoubleKey( ReportMeta meta, boolean isGeneralInterest ) {
        return new ReportKey<Double>( meta, Double.class, isGeneralInterest ) {
            public String toText( Double value ) {
                if ( value == null ) {
                    return null;
                }
                else {
                    double dval = value.doubleValue();
                    return ( Math.abs( dval ) < Float.MAX_VALUE &&
                             Math.abs( dval ) > Float.MIN_NORMAL )
                         ? Float.toString( (float) dval )
                         : Double.toString( dval );
                }
            }
        };
    }

    /**
     * Constructs an integer-valued key.
     *
     * @param   meta   metadata describing the key
     * @param   isGeneralInterest  indicates whether the key represents
     *          a general purpose report
     * @return   new key
     */
    public static ReportKey<Integer>
            createIntegerKey( ReportMeta meta, boolean isGeneralInterest ) {
        return new ReportKey<Integer>( meta, Integer.class,
                                       isGeneralInterest ) {
            public String toText( Integer value ) {
                return value == null ? null : value.toString();
            }
        };
    }

    /**
     * Constructs a typed key with default stringification.
     *
     * @param   meta   metadata describing this key
     * @param   clazz  type of data item described by this key
     * @param   isGeneralInterest  indicates whether this key represents
     *          a general purpose report
     * @return   new report key
     */
    public static <T> ReportKey<T>
            createObjectKey( ReportMeta meta, Class<T> clazz,
                             boolean isGeneralInterest ) {
        return new ReportKey<T>( meta, clazz, isGeneralInterest ) {
            public String toText( T value ) {
                return value == null ? null : value.toString();
            }
        };
    }

    /**
     * Constructs a StarTable-valued key.
     *
     * @param   meta   metadata describing the key
     * @param   isGeneralInterest  indicates whether the key represents
     *          a general purpose report
     * @return   new key
     */
    public static ReportKey<StarTable>
            createTableKey( ReportMeta meta, boolean isGeneralInterest ) {
        return new ReportKey<StarTable>( meta, StarTable.class,
                                         isGeneralInterest ) {
            public String toText( StarTable table ) {
                return table == null
                     ? "null"
                     : ( "table " + table.getColumnCount()
                       + " x " + table.getRowCount() );
            }
        };
    }

    /**
     * Constructs a non-general-interest key with no useful
     * text serialization.
     *
     * @param   meta   metadata describing the key
     * @param   clazz  type of data item described by this key
     * @return   new key
     */
    public static <T> ReportKey<T>
            createUnprintableKey( ReportMeta meta, Class<T> clazz ) {
        return new ReportKey<T>( meta, clazz, false ) {
            public String toText( T value ) {
                return "[unprintable]";
            }
        };
    }
}
