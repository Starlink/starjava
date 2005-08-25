package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Wrapper table which looks at only every n'th row.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class EveryTable extends WrapperStarTable {

    private final long step_;

    /**
     * Constructor.
     *
     * @param   base  base table
     * @param   step  number of rows of base table per single row of this one
     */
    public EveryTable( StarTable base, long step ) {
        super( base );
        step_ = step;
    }

    public long getRowCount() {
        long baseCount = super.getRowCount();
        if ( baseCount >= 0 ) {
            return ( ( baseCount - 1 ) / step_ ) + 1;
        }
        else {
            return baseCount;
        }
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return super.getCell( irow * step_, icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return super.getRow( irow * step_ );
    }

    public RowSequence getRowSequence() throws IOException {
        return new WrapperRowSequence( super.getRowSequence() ) {
            boolean started;
            public boolean next() throws IOException {
                if ( started ) {
                    for ( int i = 0; i < step_; i++ ) {
                        if ( ! super.next() ) {
                            return false;
                        }
                    }
                }
                else {
                    started = true;
                    return super.next();
                }
                return true;
            }
        };
    }
}
