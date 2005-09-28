package uk.ac.starlink.table.join;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Match engine which considers two rows matched if they contain objects
 * which are non-blank and equal 
 * (in the sense of {@link java.lang.Object#equals}).  These will typically
 * be strings, but could equally be something else.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Mar 2004
 */
public class EqualsMatchEngine implements MatchEngine {

    public double matchScore( Object[] tuple1, Object[] tuple2 ) {
        Object o1 = tuple1[ 0 ];
        Object o2 = tuple2[ 0 ];
        return ( ! Tables.isBlank( o1 ) && 
                 ! Tables.isBlank( o2 ) &&
                 o1.equals( o2 ) ) ? 0.0 : -1.0; 
    }

    /**
     * The match score is uninteresting, since it's either -1 or 0.
     * We flag this by returning <code>null</code> here.
     *
     * @return  null
     */
    public ValueInfo getMatchScoreInfo() {
        return null;
    }

    public Object[] getBins( Object[] tuple ) {
        Object obj = tuple[ 0 ];
        return Tables.isBlank( obj ) ? NO_BINS : new Object[] { obj };
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

    public boolean canBoundMatch() {
        return true;
    }

    public Comparable[][] getMatchBounds( Comparable[] min, Comparable[] max ) {
        return new Comparable[][] { min, max };
    }

    public String toString() {
        return "Exact Value";
    }
}
