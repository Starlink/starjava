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
import java.util.Iterator;
import java.util.List;

/**
 * StarTable which displays beans.
 * The table is constructed to display beans of a particular class,
 * and each of its rows displays one instance of this class.
 * It has a column for each readable property.
 * As usual, a bean is anything which has likely-looking getter methods,
 * though since this class uses the <tt>java.beans</tt> package 
 * any cleverer stuff held in BeanInfos will get used as well/instead.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Dec 2004
 */
public class BeanStarTable extends RandomStarTable {

    private final PropertyDescriptor[] properties_;
    private final Class beanClass_;
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
    public BeanStarTable( Class clazz ) throws IntrospectionException {
        beanClass_ = clazz;
        if ( clazz.isPrimitive() ) {
            throw new IllegalArgumentException( "Can't do primitive class" );
        }
        BeanInfo info = Introspector.getBeanInfo( clazz );
        List propList =
            new ArrayList( Arrays.asList( info.getPropertyDescriptors() ) );
        for ( Iterator it = propList.iterator(); it.hasNext(); ) {
            PropertyDescriptor prop = (PropertyDescriptor) it.next();
            if ( ! useProperty( prop ) ) {
                it.remove();
            }
        }
        properties_ = (PropertyDescriptor[])
                      propList.toArray( new PropertyDescriptor[ 0 ] );
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
     * @param  table data
     */
    public Object[] getData() {
        return data_;
    }

    public long getRowCount() {
        return (long) data_.length;
    }

    public int getColumnCount() {
        return properties_.length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        PropertyDescriptor prop = properties_[ icol ];
        String name = prop.getDisplayName();
        String description = prop.getShortDescription();
        String progName = prop.getName();
        Class clazz = getObjectType( prop.getPropertyType() );
        ColumnInfo cinfo = 
            new ColumnInfo( new DefaultValueInfo( name, clazz, description ) );
        if ( progName != null && progName.trim().length() > 0 ) {
            cinfo.setAuxDatum( new DescribedValue( PROGNAME_INFO, progName ) );
        }
        return cinfo;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return getProperty( data_[ checkedLongToInt( irow ) ], icol );
    }

    /**
     * Returns the value of a given indexed property on a given bean.
     * Any checked exceptions are rethrown as IOExceptions for convenience.
     *
     * @param  bean  the bean to interrogate
     * @param  iprop  index of the property required
     * @return   value of the chosen property of <tt>bean</tt>
     */
    public Object getProperty( Object bean, int iprop ) throws IOException {
        try {
            return properties_[ iprop ].getReadMethod().invoke( bean, NO_ARGS );
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
     * @return  whether to use <tt>prop<tt> as a column in this model
     */
    private static boolean useProperty( PropertyDescriptor prop ) {
        Class pclazz = prop.getPropertyType();
        return prop.getReadMethod() != null
            && ! prop.isHidden()
            && ( pclazz == String.class || pclazz.isPrimitive() );
    }

    /**
     * Translates primitive to wrapper class.
     */
    private static Class getObjectType( Class clazz ) {
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
