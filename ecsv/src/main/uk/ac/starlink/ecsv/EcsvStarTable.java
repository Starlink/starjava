package uk.ac.starlink.ecsv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Partial StarTable implementation for ECSV tables.
 * This abstract class provides table and column metadata;
 * concrete subclasses have to implment the data access methods.
 *
 * @author   Mark Taylor
 * @since    29 Apr 2020
 */
public abstract class EcsvStarTable extends AbstractStarTable {

    private final ColumnInfo[] colInfos_;

    /** Metadata for ECSV format item. */
    public static final ValueInfo CFORMAT_INFO =
        new DefaultValueInfo( "C_FORMAT", String.class,
                              "Printf-style format string" );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ecsv" );

    /**
     * Constructor.
     *
     * @param  meta  ECSV metadata object
     */
    protected EcsvStarTable( EcsvMeta meta ) {
        EcsvColumn<?>[] ecols = meta.getColumns();
        int ncol = ecols.length;
        colInfos_ = new ColumnInfo[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            colInfos_[ ic ] = toColumnInfo( ecols[ ic ] );
        }
        Map<?,?> tableMeta = meta.getTableMeta();
        if ( tableMeta != null ) {
            for ( Map.Entry<?,?> entry : tableMeta.entrySet() ) {
                DescribedValue dval = toDescribedValue( entry );
                if ( dval != null ) {
                    setParameter( dval );
                }
            }
        }
    }

    public int getColumnCount() {
        return colInfos_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public long getRowCount() {
        return -1;
    }

    /**
     * Adapts an EcsvColumn to a ColumnInfo.
     *
     * @param  ecol  ECSV column metadata
     * @return  STIL column metadata
     */
    private static ColumnInfo toColumnInfo( EcsvColumn<?> ecol ) {
        EcsvDecoder<?> decoder = ecol.getDecoder();
        Class<?> clazz = decoder.getContentClass();
        ColumnInfo cinfo =
            new ColumnInfo( ecol.getName(), clazz, ecol.getDescription() );
        String unit = ecol.getUnit();
        if ( unit != null && unit.trim().length() > 0 ) {
            cinfo.setUnitString( unit );
        }
        String format = ecol.getFormat();
        if ( format != null && format.trim().length() > 0 ) {
            cinfo.setAuxDatum( new DescribedValue( CFORMAT_INFO, format ) );
        }
        if ( "uint8".equals( ecol.getDatatype() ) &&
             Short.class.equals( clazz ) ) {
            cinfo.setAuxDatum( new DescribedValue( Tables.UBYTE_FLAG_INFO,
                                                   Boolean.TRUE ) );
        }
        Map<?,?> meta = ecol.getMeta();
        if ( meta != null ) {
            meta = new LinkedHashMap<Object,Object>( meta );
            Object ucdObj = meta.remove( EcsvTableWriter.UCD_METAKEY );
            if ( ucdObj instanceof String ) {
                cinfo.setUCD( (String) ucdObj );
            }
            Object utypeObj = meta.remove( EcsvTableWriter.UTYPE_METAKEY );
            if ( utypeObj instanceof String ) {
                cinfo.setUtype( (String) utypeObj );
            }
            Object xtypeObj = meta.remove( EcsvTableWriter.XTYPE_METAKEY );
            if ( xtypeObj instanceof String ) {
                cinfo.setXtype( (String) xtypeObj );
            }
            for ( Map.Entry<?,?> entry : meta.entrySet() ) {
                DescribedValue dval = toDescribedValue( entry );
                if ( dval != null ) {
                    cinfo.setAuxDatum( dval );
                }
            }
        }
        return cinfo;
    }

    /**
     * Converts a Map entry to a DescribedValue, if possible.
     *
     * @param  entry  map entry
     * @return  DescribedValue suitable for insertion in table or column
     *          metadata, or null if not possible or not appropriate
     */
    private static DescribedValue toDescribedValue( Map.Entry<?,?> metaEntry ) {
        Object key = metaEntry.getKey();
        Object rawValue = metaEntry.getValue();
        if ( key instanceof String && rawValue != null ) {
            String name = (String) key;
            Object value = toMetaValue( rawValue );
            if ( value != null ) {
                Class<?> clazz = value.getClass();
                String descrip = name + " value from ECSV meta structure";
                ValueInfo info = new DefaultValueInfo( name, clazz, descrip );
                return new DescribedValue( info, value );
            }
            else {
                logger_.info( "Ignore metadata item " + name
                            + " of unsupported type " + rawValue.getClass() );
            }
        }
        return null;
    }

    /**
     * Attempts to turn an object acquired from YAML parsing
     * into an object suitable for storage in a STIL DescribedValue.
     * If it can't be done, null is returned.
     *
     * @param   rawValue  value acquired from YAML parsing
     * @return   best-efforts conversion to STIL-friendly value, or null
     */
    private static Object toMetaValue( Object rawValue ) {
        if ( rawValue == null ) {
            return null;
        }
        if ( rawValue instanceof Number ||
             rawValue instanceof Boolean ||
             rawValue instanceof String ) {
            return rawValue;
        }

        /* I don't think that SnakeYaml ever provides these. */
        if ( rawValue instanceof byte[] ||
             rawValue instanceof short[] ||
             rawValue instanceof int[] ||
             rawValue instanceof long[] ||
             rawValue instanceof float[] ||
             rawValue instanceof double[] ||
             rawValue instanceof String[] ) {
            return rawValue;
        }

        /* If it's a List, process it as an Object array. */
        if ( rawValue instanceof List ) {
            return toMetaArray( ((List) rawValue).toArray() );
        }

        /* If it's an array of non-primitives, process it as an Object array. */
        Class<?> componentClass = rawValue.getClass().getComponentType();
        if ( componentClass != null &&
             Object.class.isAssignableFrom( componentClass ) ) {
            return toMetaArray( (Object[]) rawValue );
        }
        return null;
    }

    /**
     * Attempts to turn an Object array as acquired from YAML parsing
     * into an object suitable for storage in a STIL DescribedValue.
     * The input array is likely to contain primitive wrapper objects.
     * For the output we want a typed array of one of the primitive types
     * or String.
     * If it can't be done, null is returned.
     *
     * @param   array  Object array
     * @return   best-efforts conversion to STIL-friendly value, or null
     */
    private static Object toMetaArray( Object[] array ) {
        if ( array == null || array.length == 0 ) {
            return null;
        }
        int n = array.length;

        /* If all the elements are Strings, return a String array. */
        if ( isAllType( array, new Class<?>[] { String.class } ) ) {
            String[] strArray = new String[ n ];
            for ( int i = 0; i < n; i++ ) {
                strArray[ i ] = (String) array[ i ];
            }
            return strArray;
        }

        /* If all the elements are integer-like, return an int[] array. */
        else if ( isAllType( array,
                             new Class<?>[] { Integer.class, Short.class,
                                              Byte.class } ) ) {
            int[] iArray = new int[ n ];
            for ( int i = 0; i < n; i++ ) {
                iArray[ i ] = ((Number) array[ i ]).intValue();
            }
            return iArray;
        }

        /* If all the elements are numeric, return a double[] array. */
        else if ( isAllType( array, new Class<?>[] { Number.class } ) ) {
            double[] dArray = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                dArray[ i ] = ((Number) array[ i ]).doubleValue();
            }
            return dArray;
        }

        /* Inhomogeneous or unsuitable - give up. */
        return null;
    }

    /**
     * Indicates whether all the elements of a given array are
     * instances of one of a given list of classes.
     *
     * @param   array  input array
     * @param   clazzes  list of acceptable classes
     * @return   true iff all array elements are instances of one of the
     *           given classes
     */
    private static boolean isAllType( Object[] array, Class<?>[] clazzes ) {
        for ( Object el : array ) {
            boolean isType = false;
            if ( el != null ) {
                Class<?> elc = el.getClass();
                for ( Class<?> clazz : clazzes ) {
                    if ( clazz.isAssignableFrom( elc ) ) {
                        isType = true;
                    }
                }
            }
            if ( ! isType ) {
                return false;
            }
        }
        return true;
    }
}
