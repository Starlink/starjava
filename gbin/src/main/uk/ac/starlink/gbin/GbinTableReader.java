package uk.ac.starlink.gbin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;

public class GbinTableReader implements RowSequence {

    private final InputStream in_;
    private final GbinObjectReader reader_;
    private final Object gobj0_;
    private final int ncol_;
    private final ItemReader[] itemReaders_;
    private final Map<ItemReader,Object> itemMap_;
    private final ColumnInfo[] colInfos_;
    private boolean started_;

    private static final String NAME_SEP = "_";
    private static final Map<Class,Class> primitiveMap_ = createPrimitiveMap();
    private static final Set<String> boringGetters_ = createBoringGetters();

    public GbinTableReader( InputStream in ) throws IOException {
        in_ = in;
        reader_ = GbinObjectReader.createReader( in );
        if ( ! reader_.hasNext() ) {
            throw new IOException( "No objects in GBIN file" );
        }
        gobj0_ = reader_.next();
        itemReaders_ = createItemReaders( gobj0_.getClass() );
        ncol_ = itemReaders_.length;
        colInfos_ = new ColumnInfo[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            ItemReader irdr = itemReaders_[ ic ];
            colInfos_[ ic ] =
                new ColumnInfo( irdr.getItemName(), irdr.getItemContentClass(),
                                null );
        }
        itemMap_ = new HashMap<ItemReader,Object>();
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

    private static ItemReader[] createItemReaders( Class rootClazz ) {
        List<ItemReader> rdrList = new ArrayList<ItemReader>();
        addItemReaders( rootClazz, ItemReader.ROOT, rdrList );
        return rdrList.toArray( new ItemReader[ 0 ] );
    }

    private static void addItemReaders( Class parentClazz, ItemReader parentRdr,
                                        List<ItemReader> rdrList ) {
        Method[] methods = parentClazz.getMethods();
        for ( int i = 0; i < methods.length; i++ ) {
            Method method = methods[ i ];
            if ( isGetterMethod( method ) ) {
                ItemReader rdr = new ItemReader( parentRdr, method );
                Class clazz = rdr.getItemContentClass();
                if ( isColumnType( clazz ) ) {
                    rdrList.add( rdr );
                }
                else {
                    addItemReaders( clazz, rdr, rdrList );
                }
            }
        }
    }

    private static boolean isGetterMethod( Method method ) {
        String name = method.getName();
        int mods = method.getModifiers();
        return name.startsWith( "get" )
            && ! boringGetters_.contains( name )
            && Modifier.isPublic( mods )
            && ! Modifier.isStatic( mods )
            && method.getParameterTypes().length == 0;
    }

    private static boolean isColumnType( Class clazz ) {
        return primitiveMap_.values().contains( clazz )
            || clazz.equals( String.class )

    // Arrays of non-primitive objects are not decomposed into primitive
    // values.  Hard to see how they could be, but it means you may end
    // up with column content types that are arrays of non-primitive
    // objects, which most STIL output handlers will not cope well with.
            || clazz.getComponentType() != null;
    }

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
        return map;
    }

    private static Set<String> createBoringGetters() {
        return new HashSet<String>( Arrays.asList( new String[] {
            "getClass",
            "getField",
            "getStringValue",
            "getGTDescription",
            "getParamMaxValues",
            "getParamMinValues",
            "getParamOutOfRangeValues",
        } ) );
    }

    private static class ItemReader {

        private final ItemReader parentReader_;
        private final Method method_;
        public static final ItemReader ROOT = new ItemReader( null, null );

        public ItemReader( ItemReader parentReader, Method method ) {
            parentReader_ = parentReader;
            method_ = method;
        }
        public String getItemName() {
            StringBuffer sbuf = new StringBuffer();
            if ( parentReader_ != ROOT ) {
                sbuf.append( parentReader_.getItemName() )
                    .append( NAME_SEP );
            }
            sbuf.append( method_.getName().substring( 3 ) );
            return sbuf.toString();
        }
        public Class getItemContentClass() {
            Class clazz = method_.getReturnType();
            return primitiveMap_.containsKey( clazz )
                 ? primitiveMap_.get( clazz )
                 : clazz;
        }
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
