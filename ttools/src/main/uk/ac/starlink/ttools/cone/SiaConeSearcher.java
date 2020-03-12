package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.DalQuery;
import uk.ac.starlink.vo.SiaFormatOption;
import uk.ac.starlink.vo.SiaVersion;

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
    private final SiaVersion siaVersion_;
    private final SiaFormatOption format_;
    private final StarTableFactory tfact_;
    private final ContentCoding coding_;

    /**
     * Constructor.
     *
     * @param  serviceUrl  base URL for SIA service
     * @param  format   value of SIA FORMAT parameter
     * @param   believeEmpty  whether empty tables are considered to
     *          contain correct metadata
     * @param   tfact   table factory
     * @param   coding  controls HTTP-level byte stream encoding
     */
    public SiaConeSearcher( String serviceUrl, SiaVersion siaVersion,
                            SiaFormatOption format,
                            boolean believeEmpty, StarTableFactory tfact,
                            ContentCoding coding ) {
        super( "SIA", Integer.toString( siaVersion.getMajorVersion() ),
               believeEmpty );
        serviceUrl_ = serviceUrl;
        siaVersion_ = siaVersion;
        format_ = format;
        tfact_ = tfact;
        coding_ = coding;
    }

    public StarTable performSearch( double ra, double dec, double sr )
            throws IOException {
        StarTable table =
            siaVersion_.executeQuery( serviceUrl_, ra, dec, sr * 2,
                                      format_, tfact_, coding_ );
        return getConsistentTable( table );
    }

    public int getRaIndex( StarTable result ) {
        return siaVersion_.usesUcd1()
             ? getUcd1RaIndex( result )
             : getObscoreColumnIndex( result, "s_ra", "pos.eq.ra" );
    }

    public int getDecIndex( StarTable result ) {
        return siaVersion_.usesUcd1()
             ? getUcd1DecIndex( result )
             : getObscoreColumnIndex( result, "s_dec", "pos.eq.dec" );
    }

    public void close() {
    }

    /**
     * Returns the index of a table column matching a given
     * column name and/or UCD.
     * This is intended for colums following the ObsCore specification,
     * where column names and UCDs are rigidly specified.
     *
     * @param  table  table
     * @param  cname  target column name
     * @param  ucd1p  target UCD1+ value
     * @return  index of column matching specification, or -1 if not found
     */
    private static int getObscoreColumnIndex( StarTable table,
                                              String cname, String ucd1p ) {
        int nc = table.getColumnCount();

        /* Try name; this ought to get it. */
        for ( int ic = 0; ic < nc; ic++ ) {
            if ( cname.equals( table.getColumnInfo( ic ).getName() ) ) {
                return ic;
            }
        }

        /* If name fails, UCD may do.  ObsCore uses UCD1+. */
        for ( int ic = 0; ic < nc; ic++ ) {
            if ( ucd1p.equals( table.getColumnInfo( ic ).getUCD() ) ) {
                return ic;
            }
        }

        /* No luck. */
        return -1;
    }
}
