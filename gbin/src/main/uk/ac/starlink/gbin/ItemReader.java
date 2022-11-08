package uk.ac.starlink.gbin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Object which can read the value of a given table column from the
 * (GaiaRoot) data object representing a table row.
 * ItemReaders are arranged hierarchically, since objects may contain
 * objects, and it is in general the leaf nodes that get turned into
 * columns.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2017
 */
public class ItemReader {

    private final ItemReader parentReader_;
    private final Method method_;
    private final String itemName_;
    private final Representation<?> repr_;

    /** Special instance used as the reader root. */
    public static final ItemReader ROOT =
        new ItemReader( null, null, null, null );

    /* Accessor method name prefixes. */
    private static final String GET = "get";
    private static final String IS = "is";

    private static final Logger logger_ =
        Logger.getLogger( ItemReader.class.getName() );

    /**
     * Constructor.
     *
     * @param  parentReader  parent instance
     * @param  method  the no-arg public instance method whose return
     *                 value when applied to the parent reader's object
     *                 gives this column's value
     * @param  itemName  basic name for this item
     * @param  repr    value representation
     */
    public ItemReader( ItemReader parentReader, Method method,
                       String itemName, Representation<?> repr ) {
        parentReader_ = parentReader;
        method_ = method;
        itemName_ = itemName;
        repr_ = repr;
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
     * @return  the Xxx for method getXxx() or isXxx()
     */
    public String getItemName() {
        return itemName_;
    }

    /**
     * Returns the class of objects returned by <code>readItem</code>
     *
     * @return  item content class
     */
    public Class<?> getItemContentClass() {
        return repr_.getContentClass();
    }

    /**
     * Reads the value for this reader in the context of a given
     * row object.  The row object is represented as a map which
     * caches ItemReader-&gt;value pairs.  The map will be updated
     * by this method, not only by inserting its own value, but
     * also by inserting the values of any missing parent objects
     * as required.  Thus each method only needs to be invoked
     * reflectively once per row even for hierarchically nested
     * data items.  The only precondition is that the map
     * must contain the entry ROOT-&gt;rowObject.
     *
     * @param  itemMap  reader-&gt;value map, may be modified
     * @return  value associated with this reader for the row
     *          represented by the submitted map
     */
    public Object readItem( Map<ItemReader,Object> itemMap )
            throws IOException {
        if ( ! itemMap.containsKey( this ) ) {
            Object parentItem = parentReader_ == null
                              ? null
                              : parentReader_.readItem( itemMap );
            Object item =
                  parentItem == null
                ? null
                : repr_.representValue( invokeMethod( parentItem ) );
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

    /**
     * Constructs a list of objects that can each read a field from
     * an object.  There is one ItemReader returned for each column
     * in the output table.
     *
     * @param   rootClazz   class of per-row element
     * @param   profile  table configuration
     */
    public static ItemReader[]
            createItemReaders( Class<?> rootClazz, GbinTableProfile profile ) {
        List<ItemReader> rdrList = new ArrayList<ItemReader>();
        addItemReaders( rootClazz, ItemReader.ROOT, rdrList, 0, profile );
        return rdrList.toArray( new ItemReader[ 0 ] );
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
    public static String
            getColumnName( ItemReader rdr, Map<String,List<ItemReader>> nameMap,
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
     * Recursive method to populate the list of ItemReaders for this
     * table reader.
     *
     * @param  parentClazz  content class of parent reader
     * @param  parentRdr    parent item reader
     * @param  rdrList      list of item readers so far assembled
     * @param  iLevel       current recursion depth
     * @param  profile      configures details of table construction
     */
    private static void addItemReaders( Class<?> parentClazz,
                                        ItemReader parentRdr,
                                        List<ItemReader> rdrList, int iLevel,
                                        GbinTableProfile profile ) {
 
        /* Logging indentation. */
        StringBuffer pbuf = new StringBuffer();
        for ( int i = 0; i < iLevel; i++ ) {
            pbuf.append( "  " );
        }
        String prefix = pbuf.toString();
        if ( ! parentRdr.isRoot() ) {
            logger_.config( new StringBuffer()
                           .append( "GBIN obj: " )
                           .append( prefix )
                           .append( "+ " )
                           .append( parentRdr.getItemName() )
                           .append( "  (" )
                           .append( parentRdr.getItemContentClass()
                                             .getSimpleName() )
                           .append( ")" )
                           .toString() );
        }
        Collection<String> ignoreNames =
            new HashSet<String>( Arrays
                                .asList( profile.getIgnoreMethodNames() ) );
        Collection<String> ignoreMethodDeclaringClasses =
            new HashSet<String>( Arrays
                                .asList( profile
                                        .getIgnoreMethodDeclaringClasses() ) );

        /* Get all methods provided by the parent class. */
        Method[] methods = parentClazz.getMethods();

        /* Sort them alphabetically if required. */
        if ( profile.isSortedMethods() ) {
            Arrays.sort( methods, new Comparator<Method>() {
                public int compare( Method m1, Method m2 ) {
                    return m1.getName().compareTo( m2.getName() );
                }
            } );
        }

        /* For each method, if it looks like a data accessor, turn it into
         * an ItemReader or a hierarchy of them. */
        for ( Method method : methods ) {
            if ( ! ignoreMethodDeclaringClasses
                  .contains( method.getDeclaringClass().getName() ) ) {
                String itemName = getItemName( method, ignoreNames );
                if ( itemName != null ) {
                    Representation<?> repr =
                        profile.createRepresentation( method.getReturnType() );
                    if ( repr == null ) {
                        logger_.info( "Skip GBIN column " + itemName
                                    + ", return type "
                                    + method.getReturnType().getSimpleName()
                                    + " blocked by profile" );
                    }
                    else {
                        Class<?> clazz = repr.getContentClass();
                        ItemReader rdr =
                            new ItemReader( parentRdr, method, itemName, repr );
                        if ( repr.isColumn() ) {
                            rdrList.add( rdr );
                            logger_.config( new StringBuffer()
                                          .append( "GBIN col: " )
                                          .append( prefix )
                                          .append( "  - " )
                                          .append( rdr.getItemName() )
                                          .append( "  (" )
                                          .append( clazz.getSimpleName() )
                                          .append( ")" )
                                          .toString() );
                        }
                        else {

                            /* We have to be a bit careful here.
                             * Don't add any object whose types have already
                             * appeared higher up in the hierarchy,
                             * since that will lead to infinite recursion. */
                            if ( ! hasAncestorType( rdr.getParentReader(),
                                                    clazz ) ) {
                                addItemReaders( clazz, rdr, rdrList, iLevel + 1,
                                                profile );
                            }
                            else {
                                logger_.warning( "Skip GBIN column " + rdr
                                               + " (" + clazz.getSimpleName()
                                               + ") to avoid"
                                               + " infinite recursion" );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines whether a given method should be used to supply table data,
     * and if so under what name that data should be referenced.
     * The methods we're after are basically public instance no-arg
     * accessor methods, of the form getXxx() or isXxx(),
     * where Xxx is treated as (at least a component of) the component name.
     * If the supplied method does not look like a suitable accessor method,
     * null is returned.
     * Return type is not much checked here, except to see that it's not null.
     * 
     * @param   method   method on an object
     * @param   ignoreNames  list of method names we don't want to use,
     *                       even if they satisfy other criteria
     * @return   name of data item accessed by the given method,
     *           or null if it's not an accessor method
     */
    private static String getItemName( Method method,
                                       Collection<String> ignoreNames ) { 
        String methodName = method.getName();
        Class<?> clazz = method.getReturnType();
        int mods = method.getModifiers();
        if ( ! ignoreNames.contains( methodName ) &&
             Modifier.isPublic( mods ) &&
             ! Modifier.isStatic( mods ) &&
             method.getParameterTypes().length == 0 ) {
            if ( methodName.matches( "^" + GET + "[A-Z0-9_].*$" ) &&
                 ! void.class.equals( clazz ) ) {
                return methodName.substring( GET.length() );
            }
            else if ( methodName.matches( "^" + IS + "[A-Z0-9_].*$" ) &&
                 ( boolean.class.equals( clazz ) ||
                   Boolean.class.equals( clazz ) ) ) {
                return methodName.substring( IS.length() );
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Determines whether the content class of an item reader or any of
     * its ancestors matches a given type.
     *
     * @param   rdr  reader to test
     * @param   clazz  type to check against
     * @return  true iff rdr or any ancestor has a content class of clazz
     */
    private static boolean hasAncestorType( ItemReader rdr, Class<?> clazz ) {
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
}
