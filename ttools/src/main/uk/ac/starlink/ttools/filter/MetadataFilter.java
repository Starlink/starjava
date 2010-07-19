package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.util.MapGroup;

/**
 * Filter for extracting column metadata.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2006
 */
public class MetadataFilter extends BasicFilter {

    /*
     * Metadata for column metadata items.
     */
    public static final ValueInfo INDEX_INFO;
    public static final ValueInfo NAME_INFO;
    public static final ValueInfo CLASS_INFO;
    public static final ValueInfo SHAPE_INFO;
    public static final ValueInfo ELSIZE_INFO;
    public static final ValueInfo UNIT_INFO;
    public static final ValueInfo DESCRIPTION_INFO;
    public static final ValueInfo UCD_INFO;
    public static final ValueInfo UCDDESC_INFO;

    /** All known metadata items. */
    public static final ValueInfo[] KNOWN_INFOS = new ValueInfo[] {
        INDEX_INFO = new DefaultValueInfo( "Index", Integer.class,
                                           "Position of column in table" ),
        NAME_INFO = new DefaultValueInfo( "Name", String.class,
                                          "Column name" ),
        CLASS_INFO = new DefaultValueInfo( "Class", String.class,
                                           "Data type of objects in column" ),
        SHAPE_INFO = new DefaultValueInfo( "Shape", int[].class,
                                           "Shape of array values" ),
        ELSIZE_INFO = new DefaultValueInfo( "ElSize", Integer.class,
                                            "Size of each element in column"
                                          + " (mostly useful for strings)" ),
        UNIT_INFO = new DefaultValueInfo( "Units", String.class,
                                          "Unit string" ),
        DESCRIPTION_INFO = new DefaultValueInfo( "Description", String.class,
                                        "Description of data in the column" ),
        UCD_INFO = new DefaultValueInfo( "UCD", String.class,
                                         "Unified Content Descriptor" ),
        UCDDESC_INFO = new DefaultValueInfo( "UCD_desc", String.class,
                                             "Textual description of UCD" ),
    };

    /** Metadata items listed by default. */
    private static final ValueInfo[] DEFAULT_INFOS = new ValueInfo[] {
        INDEX_INFO,
        NAME_INFO,
        CLASS_INFO,
        SHAPE_INFO,
        ELSIZE_INFO,
        UNIT_INFO,
        DESCRIPTION_INFO,
        UCD_INFO,
    };

    /**
     * Constructor.
     */
    public MetadataFilter() {
        super( "meta", "[<item> ...]" );
    }

    protected String[] getDescriptionLines() {
        Collection extras = new ArrayList( Arrays.asList( KNOWN_INFOS ) );
        extras.removeAll( Arrays.asList( DEFAULT_INFOS ) );
        ValueInfo[] extraKnownInfos =
            (ValueInfo[]) extras.toArray( new ValueInfo[ 0 ] );
        return new String[] {
            "<p>Provides information about the metadata for each column.",
            "This filter turns the table sideways, so that each row",
            "of the output corresponds to a column of the input.",
            "The columns of the output table contain metadata items",
            "such as column name, units, UCD etc corresponding to each",
            "column of the input table.",
            "</p>",
            "<p>By default the output table contains columns for the",
            "following items:",
            DocUtils.listInfos( DEFAULT_INFOS ),
            "as well as any table-specific column metadata items that",
            "the table contains.",
            "</p>",
            "<p>However, the output may be customised by supplying",
            "one or more <code>&lt;item&gt;</code> headings.",
            "These may be selected from the above as well as the following:",
            DocUtils.listInfos( extraKnownInfos ),
            "as well as any table-specific metadata.  It is not an error",
            "to specify an item for which no metadata exists in any of",
            "the columns (such entries will result in empty columns).",
            "</p>",
            "<p>Any table parameters of the input table are propagated",
            "to the output one.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        final String[] items;
        if ( argIt.hasNext() ) {
            List itemList = new ArrayList();
            while ( argIt.hasNext() ) {
                itemList.add( argIt.next() );
                argIt.remove();
            }
            items = (String[]) itemList.toArray( new String[ 0 ] );
        }
        else {
            items = null;
        }
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {
                MapGroup group = metadataMapGroup( base );
                group.setKeyOrder( Arrays.asList( DEFAULT_INFOS ) );
                group.setKnownKeys( Arrays.asList( getKeys( group, items ) ) );
                AbstractStarTable table = new ValueInfoMapGroupTable( group );
                table.setParameters( base.getParameters() );
                return table;
            }
        };
    }

    /**
     * Constructs a MapGroup containing column metadata of a given table.
     *
     * @param  table  the table for which to extract metadata
     * @return  mapgroup containing column metadata
     */
    public static MapGroup metadataMapGroup( StarTable table ) {

        /* Initialise table with a sensible key order for standard metadata
         * items. */
        MapGroup group = new MapGroup();

        /* Count columns in the original table. */
        int ncol = table.getColumnCount();

        /* Compile a name->ValueInfo map of auxiliary metadata items which
         * appear in any of the columns. */
        Map auxInfos = new HashMap();
        for ( int icol = 0; icol < ncol; icol++ ) {
            for ( Iterator it = table.getColumnInfo( icol ).getAuxData()
                                                           .iterator();
                  it.hasNext(); ) {
                Object item = it.next();
                if ( item instanceof DescribedValue ) {
                    DescribedValue dval = (DescribedValue) item;
                    ValueInfo info = dval.getInfo();
                    String name = info.getName();
                    if ( auxInfos.containsKey( name ) ) {
                        info = DefaultValueInfo
                              .generalise( (ValueInfo) auxInfos.get( name ),
                                           info );
                    }
                    auxInfos.put( name, info );
                }
            }
        }

        /* Prepare a metadata map for each column of the input table and
         * add it to the group. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            Map map = new HashMap();

            /* Add standard metadata items. */
            map.put( INDEX_INFO, new Integer( icol + 1 ) );
            map.put( NAME_INFO, info.getName() );
            map.put( CLASS_INFO,
                     DefaultValueInfo.formatClass( info.getContentClass() ) );
            map.put( UNIT_INFO, info.getUnitString() );
            map.put( SHAPE_INFO, info.getShape() );
            int elsize = info.getElementSize();
            if ( elsize >= 0 ) {
                map.put( ELSIZE_INFO, info.getElementSize() );
            }
            map.put( DESCRIPTION_INFO, info.getDescription() );
            String ucd = info.getUCD();
            map.put( UCD_INFO, ucd );
            if ( ucd != null ) {
                UCD u = UCD.getUCD( ucd );
                if ( u != null ) {
                    map.put( UCDDESC_INFO, u.getDescription() );
                }
            }

            /* Add auxiliary items if there are any. */
            if ( ! auxInfos.isEmpty() ) {
                for ( Iterator it = info.getAuxData().iterator();
                      it.hasNext(); ) {
                    Object item = it.next();
                    if ( item instanceof DescribedValue ) {
                        DescribedValue dval = (DescribedValue) item;
                        ValueInfo auxInfo = (ValueInfo)
                            auxInfos.get( dval.getInfo().getName() );
                        map.put( auxInfo, dval.getValue() );
                    }
                }
            }
            group.addMap( map );
        }

        /* Return the group. */
        return group;
    }

    /**
     * Returns a list of keys corresponding to a requested list of item names
     * for a ValueInfo-keyed MapGroup.
     *
     * @param   group  map group containing column metadata
     * @param   itemNames  names of metadata items for display;
     *          may be null for default items
     * @return  array of ValueInfo keys corresponding to <code>itemNames</code>
     */
    private static ValueInfo[] getKeys( MapGroup group, String[] itemNames ) {
        Map[] maps = (Map[]) group.getMaps().toArray( new Map[ 0 ] );
        ValueInfo[] keys;

        /* For a null list of names, ascertain the columns from the non-empty
         * known keys of the map group. */
        if ( itemNames == null ) {
            List keyList = new ArrayList();
            for ( Iterator it = group.getKnownKeys().iterator();
                  it.hasNext(); ) {
                ValueInfo info = (ValueInfo) it.next();
                boolean hasSome = false;
                for ( int imap = 0; ! hasSome && imap < maps.length;
                      imap++ ) {
                    hasSome =
                        hasSome ||
                        ( ! Tables.isBlank( maps[ imap ].get( info ) ) );
                }
                if ( hasSome ) {
                    keyList.add( info );
                }
            }
            keys = (ValueInfo[]) keyList.toArray( new ValueInfo[ 0 ] );
        }

        /* For a non-null list of names, construct the list of columns
         * accordingly. */
        else {
            keys = new ValueInfo[ itemNames.length ];
            for ( int i = 0; i < itemNames.length; i++ ) {
                String item = itemNames[ i ];
                ValueInfo itemInfo = null;

                /* Try to find a ValueInfo in the group keys which corresponds
                 * to the given item name. */
                for ( Iterator it = group.getKnownKeys().iterator();
                      it.hasNext() && itemInfo == null; ) {
                    ValueInfo info = (ValueInfo) it.next();
                    if ( info.getName().equalsIgnoreCase( item ) ) {
                        itemInfo = info;
                    }
                }

                /* If there isn't one, fake an empty column with the right
                 * name. */
                if ( itemInfo == null ) {
                    itemInfo = new DefaultValueInfo( item, String.class );
                }
                keys[ i ] = itemInfo;
            }
        }
        return keys;
    }
}
