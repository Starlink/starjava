package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.Collection;

/**
 * RowLink implementation for an arbitrary number of RowRefs.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2021
 */
public class RowLinkN extends RowLink {

    private final RowRef[] rows_;

    /**
     * Constructs a RowLinkN from a collection of rows.
     *
     * @param  rows  collection of row refs, copied and not retained
     */
    public RowLinkN( Collection<RowRef> rows ) {
        this( rows.toArray( new RowRef[ rows.size() ] ) );
    }

    /**
     * Constructs a RowLinkN from an array of row refs which is retained
     * and may be modified (sorted) in place.
     * Use with care.
     *
     * @param   rows   array of rows which is retained and may be modified
     */
    protected RowLinkN( RowRef[] rows ) {
        rows_ = rows;
        Arrays.sort( rows_ );
    }

    public int size() {
        return rows_.length;
    }

    public RowRef getRef( int i ) {
        return rows_[ i ];
    }

    /**
     * Constructs a RowLinkN from an array of row refs which is retained
     * and may be modified (sorted) in place.  Calling code should
     * <strong>not</strong> make subsequent modifications to this array.
     *
     * @param   rows   array of rows which is retained and may be modified
     * @return  new RowLink
     */
    public static RowLinkN fromModifiableArray( RowRef[] rows ) {
        return new RowLinkN( rows );
    }
}
