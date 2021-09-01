package uk.ac.starlink.table.join;

/**
 * RowLink subclass which contains a single RowRef.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2021
 */
public class RowLink1 extends RowLink {

    private final RowRef ref_;

    /**
     * Constructor.
     *
     * @param   ref  sole row reference
     */
    public RowLink1( RowRef ref ) {
        ref_ = ref;
    }

    public int size() {
        return 1;
    }

    public RowRef getRef( int i ) {
        if ( i == 0 ) {
            return ref_;
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
