package uk.ac.starlink.datanode.nodes;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * A MetamapGroup which describes a set of 
 * {@link uk.ac.starlink.table.ValueInfo} objects.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ValueInfoMetamapGroup extends MetamapGroup {

    public static final String INDEX_KEY = "Index";
    public static final String NAME_KEY = "Name";
    public static final String CLASS_KEY = "Class";
    public static final String SHAPE_KEY = "Shape";
    public static final String VALUE_KEY = "Value";
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
     * Constructs a ValueInfoMetamapGroup from a List value metadata objects.
     * Each element of the supplied list may be either a ValueInfo or
     * a DescribedValue.
     */
    public ValueInfoMetamapGroup( List infos ) {
        super( infos.size() );

        setKeyOrder( keyOrder );

        int i = 0;
        for ( Iterator it = infos.iterator(); it.hasNext(); i++ ) {
            Object item = it.next();
            ValueInfo info;
            if ( item instanceof ValueInfo ) {
                info = (ValueInfo) item;
            }
            else if ( item instanceof DescribedValue ) {
                DescribedValue dval = (DescribedValue) item;
                info = dval.getInfo();
                addEntry( i, VALUE_KEY, dval.getValueAsString( 600 ) );
            }
            else {
                throw new IllegalArgumentException( 
                    item + " is not ValueInfo or DescribedValue" );
            }

            /* Name. */
            addEntry( i, NAME_KEY, info.getName() );

            /* Class. */
            addEntry( i, CLASS_KEY, DefaultValueInfo
                                   .formatClass( info.getContentClass() ) );

            /* Shape. */
            if ( info.isArray() ) {
                addEntry( i, SHAPE_KEY,
                          DefaultValueInfo.formatShape( info.getShape() ) );
            }

            /* Units. */
            addEntry( i, UNITS_KEY, info.getUnitString() );

            /* Description. */
            addEntry( i, DESCRIPTION_KEY, info.getDescription() );

            /* UCD & description. */
            addEntry( i, UCD_KEY, info.getUCD() );
            if ( hasEntry( i, UCD_KEY ) ) {
                UCD ucd = UCD.getUCD( (String) getEntry( i, UCD_KEY ) );
                String desc = ( ucd != null ) ? ucd.getDescription()
                                              : "<unknown UCD>";
                addEntry( i, UCD_DESCRIPTION_KEY, desc );
            }
        }
    }

}
