package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * Match engine which considers two rows matched if they contain objects
 * which are non-null and equal 
 * (in the sense of {@link java.lang.Object#equals}).  These will typically
 * be strings, but could equally be something else.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Mar 2004
 */
public class EqualsMatchEngine implements MatchEngine {

    private static Object[] NO_BINS = new Object[ 0 ];

    public boolean matches( Object[] tuple1, Object[] tuple2 ) {
        Object o1 = tuple1[ 0 ];
        Object o2 = tuple2[ 0 ];
        return o1 != null && o2 != null && o1.equals( o2 );
    }

    public Object[] getBins( Object[] tuple ) {
        Object obj = tuple[ 0 ];
        return obj == null ? NO_BINS : new Object[] { obj };
    }

    public ValueInfo[] getTupleInfos() {
        DefaultValueInfo vinfo = 
            new DefaultValueInfo( "Matched Value", Object.class,
                                  "Value for exact match" );
        vinfo.setNullable( false );
        return new ValueInfo[] { vinfo };
    }

    public DescribedValue[] getMatchParameters() {
        return new DescribedValue[ 0 ];
    }

    public String toString() {
        return "Exact Value";
    }
}
