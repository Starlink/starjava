package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.vo.ConeSearch;

/**
 * ConeSearcher implementation using a VO
 * <a href="http://www.ivoa.net/Documents/latest/ConeSearch.html"
 *    >Cone Search</a> service.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2009
 */
public class ServiceConeSearcher extends DalConeSearcher
                                 implements ConeSearcher {
    private final int verb_;
    private final ConeSearch csearch_;
    private final StarTableFactory tfact_;

    /**
     * Constructor.
     *
     * @param   csearch  cone search service specification object
     * @param   verb  verbosity parameter
     * @param   believeEmpty  whether empty tables are considered to
     *          contain correct metadata
     * @param   tfact  table factory
     */
    public ServiceConeSearcher( ConeSearch csearch, int verb,
                                boolean believeEmpty, StarTableFactory tfact ) {
        super( "Cone Search", "1.02", believeEmpty );
        verb_ = verb;
        csearch_ = csearch;
        tfact_ = tfact;
    }

    public StarTable performSearch( double ra, double dec, double sr )
            throws IOException {
        StarTable table = csearch_.performSearch( ra, dec, sr, verb_, tfact_ );
        return getConsistentTable( table );
    }

    public int getRaIndex( StarTable result ) {
        return getUcd1RaIndex( result );
    }

    public int getDecIndex( StarTable result ) {
        return getUcd1DecIndex( result );
    }

    public void close() {
    }
}
