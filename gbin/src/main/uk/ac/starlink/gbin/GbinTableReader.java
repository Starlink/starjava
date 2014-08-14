package uk.ac.starlink.gbin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;

/**
 * Does most of the work for turning a GbinObjectReader into a table.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2014
 */
public class GbinTableReader implements RowSequence {

    private final InputStream in_;
    private final GbinObjectReader reader_;
    private final Object gobj0_;
    private final int ncol_;
    private final ItemReader[] itemReaders_;
    private final Map<ItemReader,Object> itemMap_;
    private final ColumnInfo[] colInfos_;
    private boolean started_;

    /** Accessor method name prefix. */
    private static final String GET = "get";

    private static final Map<Class,Class> primitiveMap_ = createPrimitiveMap();

    /**
     * Constructor.
     *
     * @param   in   input stream containing GBIN file
     * @param  profile  configures details of table construction
     */
    public GbinTableReader( InputStream in, GbinTableProfile profile )
            throws IOException {
        in_ = in;
        reader_ = GbinObjectReader.createReader( in );
        if ( ! reader_.hasNext() ) {
            throw new IOException( "No objects in GBIN file" );
        }
        itemMap_ = new HashMap<ItemReader,Object>();

        /* Read the first object.  We need this now so we know the class
         * of all the objects in the file (assumed the same). */
        gobj0_ = reader_.next();

        /* Construct an array of readers, one for each column, based on
         * the object class. */
        itemReaders_ = createItemReaders( gobj0_.getClass(), profile );
        ncol_ = itemReaders_.length;

        /* Group readers by the leaf name of the items they read.
         * The main purpose of this is so we can tell if there are
         * duplicate names, which in turn will affect how columns
         * are named. */
        Map<String,List<ItemReader>> nameMap =
            new HashMap<String,List<ItemReader>>();
        boolean forceHier = profile.isHierarchicalNames();
        String separator = profile.getNameSeparator();
        if ( ! forceHier ) {
            for ( int ic = 0; ic < ncol_; ic++ ) {
                ItemReader irdr = itemReaders_[ ic ];
                String name = irdr.getItemName();
                if ( ! nameMap.containsKey( name ) ) {
                    nameMap.put( name, new ArrayList<ItemReader>() );
                }
                nameMap.get( name ).add( irdr );
            }
        }

        /* Construct a list of column infos based on what we have. */
        colInfos_ = new ColumnInfo[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            ItemReader irdr = itemReaders_[ ic ];
            colInfos_[ ic ] =
                new ColumnInfo( getColumnName( irdr, nameMap, forceHier,
                                               separator ),
                                irdr.getItemContentClass(), null );
        }
    }

    /**
     * Returns the column metadata representing the columns this object
     * will read.
     *
     * @return  array of metadata items, one for each column
     */
    public ColumnInfo[] getColumnInfos() {
        return colInfos_;
    }

    /**
     * Returns the class of the elements contained in the GBIN file.
     * In fact it's just the class of the first one, but it's assumed
     * the same for all.
     *
     * @return  element class
     */
    public Class getItemClass() {
        return gobj0_.getClass();
    }

    public boolean next() throws IOException {
        itemMap_.clear();
        if ( started_ ) {
            if ( reader_.hasNext() ) {
                itemMap_.put( ItemReader.ROOT, reader_.next() );
                return true;
            }
            else {
                return false;
            }
        }
        else {
            started_ = true;
            itemMap_.put( ItemReader.ROOT, gobj0_ );
            return true;
        }
    }

    public Object getCell( int icol ) throws IOException {
        return itemReaders_[ icol ].readItem( itemMap_ );
    }

    public Object[] getRow() throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            row[ ic ] = itemReaders_[ ic ].readItem( itemMap_ );
        }
        return row;
    }

    public void close() throws IOException {
        in_.close();
    }

    /**
     * Constructs a list of objects that can each read a field from
     * an object.  There is one ItemReader returned for each column
     * in the output table.
     *
     * @param   rootClazz   class of per-row element
     * @param   profile  table configuration
     */
    private static ItemReader[] createItemReaders( Class rootClazz,
                                                   GbinTableProfile profile ) {
        List<ItemReader> rdrList = new ArrayList<ItemReader>();
        addItemReaders( rootClazz, ItemReader.ROOT, rdrList,
                        profile.isSortedMethods(),
                        new HashSet<String>(
                            Arrays.asList( profile.getIgnoreMethodNames() ) ) );
        return rdrList.toArray( new ItemReader[ 0 ] );
    }

    /**
     * Recursive method to populate the list of ItemReaders for this
     * table reader.
     *
     * @param  parentClazz  content class of parent reader
     * @param  parentRdr    parent item reader
     * @param  rdrList      list of item readers so far assembled
     * @param  sortMethods  true iff object methods are to be sorted
     *                      alphabetically before being added to the list
     * @param  ignoreNames  method names that should not be used for columns
     */
    private static void addItemReaders( Class parentClazz, ItemReader parentRdr,
                                        List<ItemReader> rdrList,
                                        boolean sortMethods,
                                        Collection<String> ignoreNames ) {

        /* Get all methods provided by the parent class. */
        Method[] methods = parentClazz.getMethods();

        /* Sort them alphabetically if required. */
        if ( sortMethods ) {
            Arrays.sort( methods, new Comparator<Method>() {
                public int compare( Method m1, Method m2 ) {
                    return m1.getName().compareTo( m2.getName() );
                }
            } );
        }

        /* For each method, test whether it looks like a data accessor.
         * If so, turn it into an ItemReader or a hierarchy of them. */
        for ( int i = 0; i < methods.length; i++ ) {
            Method method = methods[ i ];
            if ( isDataMethod( method, ignoreNames ) ) {
                ItemReader rdr = new ItemReader( parentRdr, method );
                Class clazz = rdr.getItemContentClass();

                /* We have to be a bit careful here.  In particular don't
                 * add any object whose types have already appeared
                 * higher up in the hierarchy, since that will lead to
                 * infinite recursion. */
                if ( isColumnType( clazz ) ||
                     hasAncestorType( rdr.getParentReader(), clazz ) ) {
                    rdrList.add( rdr );
                }
                else {
                    addItemReaders( clazz, rdr, rdrList,
                                    sortMethods, ignoreNames );
                }
            }
        }
    }

    /**
     * Determines whether a given method should be used to supply
     * table data.  The methods we're after are basically public
     * instance no-arg methods of the form getXxx(),
     * where Xxx is treated as (at least a component of) the column name.
     *
     * <p>This method does not consider return type, except to exclude
     * void returns.
     * 
     * @param   method  method to test
     * @param   ignoreNames  method names that should be excluded from
     *                       consideration
     * @return  true  iff the method should be used to generate a data column
     */
    private static boolean isDataMethod( Method method,
                                         Collection<String> ignoreNames ) {
        String name = method.getName();
        int mods = method.getModifiers();
        return

            /* Check method name. */
            ( name.startsWith( GET ) &&
              ! ignoreNames.contains( name ) ) &&

            /* Check modifiers. */
            ( Modifier.isPublic( mods ) &&
              ! Modifier.isStatic( mods ) ) &&

            /* Check parameter list. */
            ( method.getParameterTypes().length == 0 ) &&

            /* Check it returns something. */
            ( method.getReturnType() != void.class );
    }

    /**
     * Tests whether a method return type is suitable for use as a single
     * column.  If not, the class will recursively be broken up into
     * components which are themselves columns.
     *
     * <p>Primitives (and their associated wrapper classes) and Strings
     * are considered suitable for columns.
     * Note also that any array value is reported suitable for a column.
     * For arrays of primitives and Strings, that makes good sense.
     * For arrays of non-primitive values it will probably result
     * in columns that most STIL output handlers won't cope with,
     * but it's hard to see what else you can do with them,
     * since there is no obvious way to turn a variable-length
     * array of composed objects into a fixed-length list of columns.
     *
     * @param  clazz  return type
     * @return  true iff clazz should be made into a column
     */
    private static boolean isColumnType( Class clazz ) {
        return primitiveMap_.values().contains( clazz )
            || clazz.equals( String.class )
            || clazz.getComponentType() != null;
    }

    /**
     * Determines whether the content class of an item reader or any of
     * its ancestors matches a given type.
     *
     * @param   rdr  reader to test
     * @param   clazz  type to check against
     * @return  true iff rdr or any ancestor has a content class of clazz
     */
    private static boolean hasAncestorType( ItemReader rdr, Class clazz ) {
        if ( rdr.isRoot() ) {
            return false;
        }
        else if ( rdr.getItemContentClass().equals( clazz ) ) {
            return true;
        }
        else {
            return hasAncestorType( rdr.getParentReader(), clazz );
        }
    }

    /**
     * Constructs a map of all existing primtive classes to their
     * corresponding wrapper classes.
     *
     * @return  primitive->wrapper class map
     */
    private static Map<Class,Class> createPrimitiveMap() {
        Map<Class,Class> map = new LinkedHashMap<Class,Class>();
        map.put( boolean.class, Boolean.class );
        map.put( char.class, Character.class );
        map.put( byte.class, Byte.class );
        map.put( short.class, Short.class );
        map.put( int.class, Integer.class );
        map.put( long.class, Long.class );
        map.put( float.class, Float.class );
        map.put( double.class, Double.class );
        return Collections.unmodifiableMap( map );
    }

    /**
     * Works out what name to use for the table column using data from a
     * given item reader.
     *
     * @param  rdr  item reader supplying data
     * @param  nameMap  map of all item reader names to a list of the
     *         item readers having that name; it can be used to detect
     *         name duplication
     * @param  forceHier  true iff hierarchical names should always be used
     * @param  separator  separator string for components of a hierarchical
     *                    column name
     */
    private static String getColumnName( ItemReader rdr,
                                         Map<String,List<ItemReader>> nameMap,
                                         boolean forceHier, String separator ) {
        String itemName = rdr.getItemName();
        StringBuffer sbuf = new StringBuffer();

        /* The name is hierarchical if we have been explicitly instructed so,
         * or if a non-hierarchical one would result in duplicate column
         * names in the table.  If the name is hierarchical, construct it
         * recursively. */
        if ( forceHier || ( nameMap.containsKey( itemName ) &&
                            nameMap.get( itemName ).size() > 1 ) ) {
            ItemReader parentRdr = rdr.getParentReader();
            if ( ! parentRdr.isRoot() ) {
                sbuf.append( getColumnName( parentRdr, nameMap,
                                            forceHier, separator ) )
                    .append( separator );
            }
        }

        /* Add the item (leaf) name in any case. */
        sbuf.append( itemName );
        return sbuf.toString();
    }

    /**
     * Object which can read the value of a given table column from the
     * (GaiaRoot) data object representing a table row.
     * ItemReaders are arranged hierarchically, since objects may contain
     * objects, and it is in general the leaf nodes that get turned into
     * columns.
     */
    private static class ItemReader {

        private final ItemReader parentReader_;
        private final Method method_;

        /** Special instance used as the reader root. */
        public static final ItemReader ROOT = new ItemReader( null, null );

        /**
         * Constructor.
         *
         * @param  parentReader  parent instance
         * @param  method  the no-arg public instance method whose return
         *                 value when applied to the parent reader's object
         *                 gives this column's value
         */
        public ItemReader( ItemReader parentReader, Method method ) {
            parentReader_ = parentReader;
            method_ = method;
        }

        /**
         * Determines whether this instance is the tree root.
         *
         * @return  true iff this == ROOT
         */
        public boolean isRoot() {
            return this == ROOT;
        }

        /**
         * Returns the parent of this reader.
         *
         * @return  parent, it's null only for ROOT
         */
        public ItemReader getParentReader() {
            return parentReader_;
        }

        /**
         * Returns the (non-hierarchical) name of the data item represented
         * by this reader.
         *
         * @return  the Xxx for method getXxx
         */
        public String getItemName() {
            return method_.getName().substring( GET.length() );
        }

        /**
         * Returns the class of objects returned by <code>readItem</code>
         *
         * @return  item content class
         */
        public Class getItemContentClass() {
            Class rclazz = method_.getReturnType();
            return primitiveMap_.containsKey( rclazz )
                 ? primitiveMap_.get( rclazz )
                 : rclazz;
        }

        /**
         * Reads the value for this reader in the context of a given
         * row object.  The row object is represented as a map which
         * caches ItemReader->value pairs.  The map will be updated
         * by this method, not only by inserting its own value, but
         * also by inserting the values of any missing parent objects
         * as required.  Thus each method only needs to be invoked
         * reflectively once per row even for hierarchically nested
         * data items.  The only precondition is that the map
         * must contain the entry ROOT->rowObject.
         *
         * @param  itemMap  reader->value map, may be modified
         * @return  value associated with this reader for the row
         *          represented by the submitted map
         */
        public Object readItem( Map<ItemReader,Object> itemMap )
                throws IOException {
            if ( ! itemMap.containsKey( this ) ) {
                Object parentItem = parentReader_.readItem( itemMap );
                Object item = parentItem == null
                            ? null
                            : invokeMethod( parentItem );
                itemMap.put( this, item );
            }
            assert itemMap.containsKey( this );
            return itemMap.get( this );
        }

        /**
         * Invokes this reader's method on a given parent item,
         * translating various throwables to IOExceptions with
         * informative messages.
         *
         * @param  parentItem  data item associated with this reader's parent
         * @return  method return value
         */
        private Object invokeMethod( Object parentItem ) throws IOException {
            try {
                return method_.invoke( parentItem );
            }
            catch ( IllegalAccessException e ) {
                throw (IOException) new IOException( "Reflection trouble" )
                                   .initCause( e );
            }
            catch ( IllegalArgumentException e ) {
                throw (IOException) new IOException( "Reflection trouble" )
                                   .initCause( e );
            }
            catch ( InvocationTargetException e ) {
                throw (IOException)
                      new IOException( e.getTargetException().getMessage() )
                     .initCause( e );
            }
        }
    }
}
