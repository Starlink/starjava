package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.vo.DalQuery;

/**
 * ConeSearcher implementation using a VO 
 * <a href="http://www.ivoa.net/Documents/latest/SIA.html"
 *    >Simple Image Access</a> service.
 * The matches will be available images rather than crossmatched objects,
 * but the mechanics are much the same.
 *
 * @author   Mark Taylor
 * @since    23 Sep 2009
 */
public class SiaConeSearcher extends DalConeSearcher implements ConeSearcher {

    private final String serviceUrl_;
    private final String imgFormat_;
    private final StarTableFactory tfact_;

    /**
     * Constructor.
     *
     * @param  serviceUrl  base URL for SIA service
     * @param  imgFormat   value of SIA FORMAT parameter
     *                     ("image/fits" is often a good choice)
     * @param   believeEmpty  whether empty tables are considered to
     *          contain correct metadata
     * @param   tfact   table factory
     */
    public SiaConeSearcher( String serviceUrl, String imgFormat,
                            boolean believeEmpty, StarTableFactory tfact ) {
        super( "SIA", "1.0", believeEmpty );
        serviceUrl_ = serviceUrl;
        imgFormat_ = imgFormat;
        tfact_ = tfact;
    }

    public StarTable performSearch( double ra, double dec, double sr )
            throws IOException {
        DalQuery query = new DalQuery( serviceUrl_, "SIA", ra, dec, sr * 2 );
        if ( imgFormat_ != null && imgFormat_.trim().length() > 0 ) {
            query.addArgument( "FORMAT", imgFormat_ );
        }
        StarTable table = query.execute( tfact_ );
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
