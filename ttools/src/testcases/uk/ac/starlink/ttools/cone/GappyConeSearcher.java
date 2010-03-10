package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.EmptyStarTable;
import uk.ac.starlink.table.StarTable;

public abstract class GappyConeSearcher implements ConeSearcher {

    private final ConeSearcher base_;
    private final boolean gapsAreNulls_;
    private int count_;

    public GappyConeSearcher( ConeSearcher base, boolean gapsAreNulls ) {
        base_ = base;
        gapsAreNulls_ = gapsAreNulls;
    }

    protected abstract boolean isGap( int irow );

    public StarTable performSearch( double ra, double dec, double sr )
            throws IOException {
        StarTable result = base_.performSearch( ra, dec, sr );
        if ( isGap( count_++ ) ) {
            if ( gapsAreNulls_ ) {
                result = null;
            }
            else {
                result = new EmptyStarTable( result );
            }
        }
        return result;
    }

    public int getRaIndex( StarTable result ) {
        return base_.getRaIndex( result );
    }

    public int getDecIndex( StarTable result ) {
        return base_.getDecIndex( result );
    }

    public void close() {
        base_.close();
    }
}
