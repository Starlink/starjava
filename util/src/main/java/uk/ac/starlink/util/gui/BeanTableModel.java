package uk.ac.starlink.util.gui;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;

/**
 * TableModel which displays beans.  
 * The table is constructed to display beans of a particular class,
 * and each of its row displays one instance of this class.
 * It has one column for each readable property.
 * As usual, a bean is anything which has likely-looking getter methods,
 * though by using the <code>java.beans</code> package any cleverer stuff
 * held in BeanInfos will get used as well/instead.
 * What a neat idea!
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Dec 2004
 */
public class BeanTableModel extends AbstractTableModel {

    private final PropertyDescriptor[] properties_;
    private final Class<?> beanClass_;
    private Object[] data_;
    private boolean readErrorReported_;
    private boolean writeErrorReported_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );
    private static final Object[] NO_ARGS = new Object[ 0 ];

    /**
     * Constructs a new table model for displaying beans of a given class.
     *
     * @param  clazz  class of items which will be displayed in this table
     */
    public BeanTableModel( Class<?> clazz ) throws IntrospectionException {
        beanClass_ = clazz;
        if ( clazz.isPrimitive() ) {
            throw new IllegalArgumentException( "Can't do primitive class" );
        }
        BeanInfo info = Introspector.getBeanInfo( clazz );
        List<PropertyDescriptor> propList =
            new ArrayList<PropertyDescriptor>(
                Arrays.asList( info.getPropertyDescriptors() ) );
        for ( Iterator<PropertyDescriptor> it = propList.iterator();
              it.hasNext(); ) {
            PropertyDescriptor prop = it.next();
            if ( ! useProperty( prop ) ) {
                it.remove();
            }
        }
        properties_ = propList.toArray( new PropertyDescriptor[ 0 ] );
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
        fireTableDataChanged();
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

    /**
     * Returns a sorter which can be used to sort rows of this table
     * (data array elements).
     *
     * @param  propertyName  name of readable bean property to sort on
     * @return   a comparator that sorts on <code>propertyName</code>, or
     *           null if <code>propertyName</code> does not name a suitable 
     *           property
     */
    public Comparator<?> propertySorter( String propertyName ) {
        PropertyDescriptor prop = getPropertyByName( propertyName );
        if ( prop != null && 
             Comparable.class.isAssignableFrom( prop.getPropertyType() ) ) {
            final Method getter = prop.getReadMethod();
            if ( getter != null ) {
                return new Comparator<Object>() {
                    public int compare( Object o1, Object o2 ) {
                        Object v1;
                        Object v2;
                        try {
                            v1 = getter.invoke( o1, NO_ARGS );
                            v2 = getter.invoke( o2, NO_ARGS );
                        }
                        catch ( IllegalAccessException e ) {
                            throw (AssertionError)
                                  new AssertionError( "Introspector said " +
                                                      "it would be OK" )
                                 .initCause( e );
                        }
                        catch ( InvocationTargetException e ) {
                            if ( ! readErrorReported_ ) {
                                readErrorReported_ = true;
                                Throwable e2 = e.getCause();
                                logger_.log( Level.WARNING, e2.getMessage(),
                                             e2 );
                            }
                            v1 = Integer.valueOf( o1.hashCode() );
                            v2 = Integer.valueOf( o2.hashCode() );
                        }
                        if ( v1 != null && v2 != null ) {
                            @SuppressWarnings("unchecked")
                            Comparable<Object> cv1 = (Comparable<Object>) v1;
                            return cv1.compareTo( v2 );
                        }
                        else if ( v1 == null && v2 == null ) {
                            return 0;
                        }
                        else if ( v1 == null ) {
                            assert v2 != null;
                            return -1;
                        }
                        else if ( v2 == null ) {
                            assert v1 != null;
                            return +1;
                        }
                        throw new AssertionError();
                    }
                };
            }
        }
        return null;
    }

    /** 
     * Evaluated to determine whether a bean property will become one of
     * the columns of this method.  Since this is currently evaluated
     * in the constructor, it can't sensible be overridden by subclasses.
     *
     * @param  prop  property to evaluate
     * @return  whether to use <code>prop<code> as a column in this model
     */
    private boolean useProperty( PropertyDescriptor prop ) {
        Class<?> pclazz = prop.getPropertyType();
        return prop.getReadMethod() != null
            && ( pclazz == String.class || pclazz.isPrimitive() );
    }

    public String getColumnName( int icol ) {
        return properties_[ icol ].getDisplayName();
    }

    public Class<?> getColumnClass( int icol ) {
        Class<?> clazz = properties_[ icol ].getPropertyType();
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

    public int getColumnCount() {
        return properties_.length;
    }

    public int getRowCount() {
        return data_.length;
    }

    public Object getValueAt( int irow, int icol ) {
        try {
            return properties_[ icol ].getReadMethod()
                                      .invoke( data_[ irow ], NO_ARGS );
        }
        catch ( IllegalAccessException e ) {
            throw (AssertionError)
                  new AssertionError( "Introspector said it would be OK" )
                 .initCause( e );
        }
        catch ( InvocationTargetException e ) {
            if ( ! readErrorReported_ ) {
                readErrorReported_ = true;
                Throwable e2 = e.getCause();
                logger_.log( Level.WARNING, e2.getMessage(), e2 );
            }
            return "ERROR";
        }
    }

    public boolean isCellEditable( int irow, int icol ) {
        return properties_[ icol ].getWriteMethod() != null;
    }

    public void setValueAt( Object value, int irow, int icol ) {
        try {
            properties_[ icol ].getWriteMethod()
                               .invoke( data_[ irow ], new Object[] { value } );
        }
        catch ( IllegalAccessException e ) {
            throw (AssertionError)
                  new AssertionError( "Introspector said it would be OK" )
                 .initCause( e );
        }
        catch ( InvocationTargetException e ) {
            if ( ! writeErrorReported_ ) {
                writeErrorReported_ = true;
                Throwable e2 = e.getCause();
                logger_.log( Level.WARNING, e2.getMessage(), e2 );
            }
        }
    }

    private PropertyDescriptor getPropertyByName( String propName ) {
        for ( int i = 0; i < properties_.length; i++ ) {
            PropertyDescriptor prop = properties_[ i ];
            if ( propName.equals( prop.getDisplayName() ) ||
                 propName.equals( prop.getName() ) ) {
                return prop;
            }
        }
        return null;
    }

}
