package uk.ac.starlink.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.util.MapGroup;

/**
 * A <tt>MapGroup</tt> which describes a set of 
 * {@link uk.ac.starlink.table.ValueInfo} objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ValueInfoMapGroup extends MapGroup {

    public static final String INDEX_KEY = "Index";
    public static final String NAME_KEY = "Name";
    public static final String VALUE_KEY = "Value";
    public static final String CLASS_KEY = "Class";
    public static final String SHAPE_KEY = "Shape";
    public static final String UNITS_KEY = "Units";
    public static final String DESCRIPTION_KEY = "Description";
    public static final String UCD_KEY = "UCD";
    public static final String UCD_DESCRIPTION_KEY = "UCD description";
    public static final String NULLABLE_KEY = "Nullable";
    private static final List keyOrder = Arrays.asList( new String[] {
        INDEX_KEY,
        NAME_KEY,
        CLASS_KEY,
        SHAPE_KEY,
        VALUE_KEY,
        UNITS_KEY,
        DESCRIPTION_KEY,
        UCD_KEY,
        UCD_DESCRIPTION_KEY,
        NULLABLE_KEY,
    } );

    /**
     * The longest stringified value to be stored in a Map as the result
     * of a {@link DescribedValue#getValueAsString(int)} call.
     */
    private final static int MAX_STRING_LENGTH = 1024;

    /**
     * Constructs a new ValueInfoMapGroup.
     */
    public ValueInfoMapGroup() {
        setKeyOrder( keyOrder );
    }

    /**
     * Constructs a <tt>ValueInfoMapGroup</tt> based on the column 
     * information in a <tt>StarTable</tt>.
     * This convenience constructor just calls {@link #addTableColumns}.
     *
     * @param  startab  the StarTable to base it on
     */
    public ValueInfoMapGroup( StarTable startab ) {
        this();
        addTableColumns( startab );
    }

    /**
     * Adds a new Map to the group which contains the metadata in a 
     * <tt>ValueInfo</tt> object.
     *
     * @param  info  the ValueInfo object
     */
    public void addValueInfo( ValueInfo info ) {
        addMap( makeMap( info ) );
    }

    /**
     * Adds a new Map to the group which contains the metadata and value 
     * in a DescribedValue object.
     *
     * @param  dval   the DescribedValue object
     */
    public void addDescribedValue( DescribedValue dval ) {
        addMap( makeMap( dval ) );
    }

    /**
     * Adds a Map to the group for each one of the <tt>ColumnInfo</tt> 
     * objects in a <tt>StarTable</tt>.  The proper ordering of the
     * column metadata is also incorporated into the ordering of this
     * MapGroup.
     *
     * @param   startab   the table from which to add items
     */
    public void addTableColumns( StarTable startab ) {

        /* Adjust the proper ordering of this mapgroup by adding the 
         * proper ordering for the column auxiliary metadata keys. */
        addColumnAuxDataKeys( startab );

        /* Add the map items for each column. */
        int ncol = startab.getColumnCount();
        for ( int i = 0; i < ncol; i++ ) {
            ColumnInfo colinfo = startab.getColumnInfo( i );
            Map map = makeMap( colinfo );
            map.put( INDEX_KEY, new Integer( i + 1 ) );
            addMap( map );
        }
    }

    /**
     * Adds the column auxiliary metadata keys associated with a 
     * <tt>StarTable</tt> to this <tt>MapGroup</tt>'s list of known keys.
     *
     * @param  startab  the table whose aux column metadata keys will
     *         be added
     */
    public void addColumnAuxDataKeys( StarTable startab ) {
        List order = getKeyOrder();
        for ( Iterator it = startab.getColumnAuxDataInfos().iterator();
              it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof ValueInfo ) {
                ValueInfo info = (ValueInfo) item;
                order.add( info.getName() );
            }
        }
        setKeyOrder( order );
    }

    /**
     * Returns a new Map representing a <tt>ColumnInfo</tt> object. 
     * This contains its name description etc plus any auxiliary metadata
     * items.
     *
     * @param  colinfo  the ColumnInfo to make a map from
     * @return  new map
     */
    public static Map makeMap( ColumnInfo colinfo ) {
        Map map = makeMap( (ValueInfo) colinfo );
        for ( Iterator it = colinfo.getAuxData().iterator(); it.hasNext(); ) {
            Object item = it.next();
            if ( item instanceof DescribedValue ) {
                DescribedValue dval = (DescribedValue) item;
                map.put( dval.getInfo().getName(),
                         dval.getValueAsString( MAX_STRING_LENGTH ) );
            }
        }
        return map;
    }

    /**
     * Returns a new Map representing a <tt>ValueInfo</tt> object.
     * This contains its name and description etc.
     *
     * @param  info  the ValuInfo to make a map from
     * @return  new map
     */
    public static Map makeMap( ValueInfo info ) {
        Map map = new HashMap();

        /* Name. */
        String name = info.getName();
        if ( name != null ) {
            map.put( NAME_KEY, name );
        }

        /* Class. */
        map.put( CLASS_KEY, 
                 DefaultValueInfo.formatClass( info.getContentClass() ) );

        /* Shape. */
        if ( info.isArray() ) {
            map.put( SHAPE_KEY,
                     DefaultValueInfo.formatShape( info.getShape() ) );
        }

        /* Units. */
        String units = info.getUnitString();
        if ( units != null ) {
            map.put( UNITS_KEY, units );
        }

        /* Description. */
        String description = info.getDescription();
        if ( description != null && description.length() > 0 ) {
            map.put( DESCRIPTION_KEY, description );
        }

        /* UCD etc. */
        String ucdname = info.getUCD();
        if ( ucdname != null ) {
            map.put( UCD_KEY, ucdname );
            UCD ucd = UCD.getUCD( ucdname );
            String ucdesc = ( ucd != null ) ? ucd.getDescription()
                                            : "<unknown UCD>";
            map.put( UCD_DESCRIPTION_KEY, ucdesc );
        }
        return map;
    }

    /**
     * Returns a new Map representing a <tt>DescribedValue</tt> object.
     * This contains its name and description etc as well as its
     * value.
     *
     * @param  dval the DescribedValue to make a map from
     * @return  new map
     */
    public static Map makeMap( DescribedValue dval ) {

        /* Make a map from the DescribedValue's ValueInfo. */
        Map map = makeMap( dval.getInfo() );

        /* Additionally place the value itself in.  Use the stringified
         * version of the value since it will be divorced from the
         * ValueInfo object which can make sense of it. */
        map.put( VALUE_KEY, dval.getValueAsString( MAX_STRING_LENGTH ) );

        /* Return the map. */
        return map;
    }

}
