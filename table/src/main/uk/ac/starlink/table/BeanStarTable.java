package uk.ac.starlink.table;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * StarTable which displays beans.
 * The table is constructed to display beans of a particular class,
 * and each of its rows displays one instance of this class.
 * It has a column for each readable property.
 * As usual, a bean is anything which has likely-looking getter methods,
 * though since this class uses the <code>java.beans</code> package 
 * any cleverer stuff held in BeanInfos will get used as well/instead.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public class BeanStarTable extends RandomStarTable {

    private final Class<?> beanClass_;
    private final Map<String,ColumnInfo> colInfos_;
    private final Map<String,PropertyDescriptor> props_;
    private PropertyDescriptor[] colProps_;
    private Object[] data_;

    private static final ValueInfo PROGNAME_INFO = 
        new DefaultValueInfo( "ProgName", String.class, "Programmatic name" );
    private static final Object[] NO_ARGS = new Object[ 0 ];

    /**
     * Constructs a new table which will hold beans which are all instances
     * of a given class.
     *
     * @param   clazz  class of which all beans held by this table are members
     */
    public BeanStarTable( Class<?> clazz ) throws IntrospectionException {
        if ( clazz.isPrimitive() ) {
            throw new IllegalArgumentException( "Can't do primitive class" );
        }
        beanClass_ = clazz;
        PropertyDescriptor[] props =
            Introspector.getBeanInfo( clazz ).getPropertyDescriptors();
        List<PropertyDescriptor> colPropList =
            new ArrayList<PropertyDescriptor>();
        colInfos_ = new HashMap<String,ColumnInfo>();
        props_ = new HashMap<String,PropertyDescriptor>();
        for ( int i = 0; i < props.length; i++ ) {
            PropertyDescriptor prop = props[ i ];
            String progName = prop.getName();
            props_.put( progName, prop );
            ColumnInfo colInfo =
                new ColumnInfo( prop.getDisplayName(),
                                getObjectType( prop.getPropertyType() ),
                                prop.getShortDescription() );
            colInfo.setAuxDatum( new DescribedValue( PROGNAME_INFO,
                                                     progName ) );
            colInfos_.put( progName, colInfo );
            if ( useProperty( prop ) ) {
                colPropList.add( prop );
            }
        }
        colProps_ = colPropList.toArray( new PropertyDescriptor[ 0 ] );
        data_ = (Object[]) Array.newInstance( clazz, 0 );
    }

    /**
     * Populates this model with items.
     *
     * @param  data  array of items, one for each row.  This array's
     *         runtime type must match that for which this model was
     *         constructed (on pain of ClassCastException)
     */
    public void setData( Object[] data ) {
        if ( ! beanClass_.isAssignableFrom( data.getClass()
                                                .getComponentType() ) ) {
            throw new ClassCastException( data.getClass().getName() +
                                          " is not " + beanClass_.getName() +
                                          "[]" );
        }
        data_ = data;
    }

    /**
     * Returns the array of objects which this model displays, one per row.
     * The runtime type of the returned array matches that of the bean class
     * this model displays.
     *
     * @return  table data
     */
    public Object[] getData() {
        return data_;
    }

    public long getRowCount() {
        return (long) data_.length;
    }

    public int getColumnCount() {
        return colProps_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_.get( colProps_[ icol ].getName() );
    }

    /**
     * Resets the metadata for a column representing a property with a
     * given name.
     *
     * @param   name  property's programmatic name
     * @param   info  new column metadata
     */
    public void setColumnInfo( String name, ValueInfo info ) {
        if ( colInfos_.containsKey( name ) ) {
            ColumnInfo cinfo = info instanceof ColumnInfo
                             ? (ColumnInfo) info
                             : new ColumnInfo( info );
            colInfos_.put( name, cinfo );
        }
        else {
            throw new IllegalArgumentException( "No such property " + name );
        }
    }

    /**
     * Returns an array of the property names which correspond to the
     * columns of this table.
     *
     * @return   array of strings giving programmatic names of bean properties,
     *           one for each table column
     */
    public String[] getColumnProperties() {
        String[] propNames = new String[ colProps_.length ];
        for ( int i = 0; i < colProps_.length; i++ ) {
            propNames[ i ] = colProps_[ i ].getName();
        }
        return propNames;
    }

    /**
     * Fixes the columns which are to be used for this table.
     * <code>propNames</code> is an array of the programmatic names of
     * each of the properties of this bean which is used to get a column
     * value.
     *
     * @param  propNames   array of programmatic names of properties
     *         to be used as columns
     */
    public void setColumnProperties( String[] propNames ) {
        PropertyDescriptor[] props = new PropertyDescriptor[ propNames.length ];
        for ( int i = 0; i < propNames.length; i++ ) {
            String name = propNames[ i ];
            if ( props_.containsKey( name ) ) {
                props[ i ] = props_.get( name );
            }
            else {
                throw new IllegalArgumentException( "No such property "
                                                  + name );
            }
        }
        colProps_ = props;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return getProperty( data_[ checkedLongToInt( irow ) ],
                            colProps_[ icol ] );
    }

    /**
     * Returns the value of a given indexed property on a given bean.
     * Any checked exceptions are rethrown as IOExceptions for convenience.
     *
     * @param  bean  the bean to interrogate
     * @param  prop  property whose value is required
     * @return   value of the <code>prop</code> in <code>bean</code>
     */
    private Object getProperty( Object bean, PropertyDescriptor prop )
            throws IOException {
        try {
            return prop.getReadMethod().invoke( bean, NO_ARGS );
        }
        catch ( IllegalAccessException e ) {
            throw (AssertionError)
                  new AssertionError( "Introspector said it would be OK" )
                 .initCause( e );
        }
        catch ( InvocationTargetException e ) {
            Throwable e2 = e.getCause();
            if ( e2 instanceof IOException ) {
                throw (IOException) e2;
            }
            else if ( e2 instanceof RuntimeException ) {
                throw (RuntimeException) e2;
            }
            else if ( e2 instanceof Error ) {
                throw (Error) e2;
            }
            else {
                throw (IOException) new IOException( e2.getMessage() )
                                   .initCause( e2 );
            }
        }
    }

    /**
     * Evaluated to determine whether a bean property will become one of
     * the columns of this method.  Since this is currently evaluated
     * in the constructor, it can't sensibly be overridden by subclasses.
     *
     * @param  prop  property to evaluate
     * @return  whether to use <code>prop<code> as a column in this model
     */
    private static boolean useProperty( PropertyDescriptor prop ) {
        Class<?> pclazz = prop.getPropertyType();
        return prop.getReadMethod() != null
            && ! prop.isHidden()
            && ( pclazz == String.class || pclazz.isPrimitive() );
    }

    /**
     * Translates primitive to wrapper class.
     */
    private static Class<?> getObjectType( Class<?> clazz ) {
        if ( clazz.isPrimitive() ) {
            if ( clazz == boolean.class ) {
                return Boolean.class;
            }
            else if ( clazz == char.class ) {
                return Character.class;
            }
            else if ( clazz == byte.class ) {
                return Byte.class;
            }
            else if ( clazz == short.class ) {
                return Short.class;
            }
            else if ( clazz == int.class ) {
                return Integer.class;
            }
            else if ( clazz == long.class ) {
                return Long.class;
            }
            else if ( clazz == float.class ) {
                return Float.class;
            }
            else if ( clazz == double.class ) {
                return Double.class;
            }
        }
        return clazz;
    }

}
