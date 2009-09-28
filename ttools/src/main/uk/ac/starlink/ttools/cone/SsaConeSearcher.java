package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.vo.DalQuery;

/**
 * ConeSearcher implementation using a VO
 * <a href="http://www.ivoa.net/Documents/latest/SSA.html"
 *    >Simple Spectral Access</a> service.
 * The matches will be available spectra rather than crossmatched objects,
 * but the mechanics are much the same.
 *
 * @author   Mark Taylor
 * @since    24 Sep 2009
 */
public class SsaConeSearcher extends DalConeSearcher implements ConeSearcher {

    private final String serviceUrl_; 
    private final String specFormat_;
    private final StarTableFactory tfact_;
    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    private static final Pattern RA_NAME_REGEX =
        Pattern.compile( "RA_?J?(2000)?", Pattern.CASE_INSENSITIVE );
    private static final Pattern DEC_NAME_REGEX =
        Pattern.compile( "DEC?L?_?J?(2000)?", Pattern.CASE_INSENSITIVE );

    /**
     * Constructor.
     */
    public SsaConeSearcher( String serviceUrl, String specFormat,
                            boolean believeEmpty, StarTableFactory tfact ) {
        super( "SSA", "1.04", believeEmpty );
        serviceUrl_ = serviceUrl;
        specFormat_ = specFormat;
        tfact_ = tfact;
    }

    public StarTable performSearch( double ra, double dec, double sr )
            throws IOException {
        DalQuery query = new DalQuery( serviceUrl_, "SSA", ra, dec, sr * 2 );
        if ( specFormat_ != null && specFormat_.trim().length() > 0 ) {
            query.addArgument( "FORMAT", specFormat_ );
        }
        StarTable table = query.execute( tfact_ );
        table = getConsistentTable( table );
        return table;
    }

    public int getRaIndex( StarTable result ) {
        // Could work harder here (and for getDecIndex); the correct thing
        // to do for SSA 1.04 would be to look for the column with 
        // utype ssa:Char.SpatialAxis.Coverage.Location.Value, and interpret
        // this in conjunction with other STC-like columns to make sense
        // of it as an ICRS position.  Two problems here: 1 - STC; 2 - the
        // spatialaxis.coverage column looks like a 2-element vector (at 
        // least in some SSA results), so it can't have a column index.
        // Would need to redefine ConeSearcher interface so it gets
        // something more flexible than a column interface, or in the
        // performSearch method rejig the table so that it contained some
        // new columns with ICRS RA & Dec.  Since I suspect SSA from
        // ttools is going to be rather niche functionality, I don't think
        // it's worth the effort just now.
        return guessPosColumn( result, "ra", RA_NAME_REGEX );
    }

    public int getDecIndex( StarTable result ) {
        return guessPosColumn( result, "dec", DEC_NAME_REGEX );
    }

    public void close() {
    }

    /**
     * Looks at table columns and makes a guess about which, if any,
     * gives RA/Dec values in degrees.
     *
     * @param  table  table to examine
     * @param  ucdAtom  UCD atom describing value "ra" or "dec"
     * @param  nameRegex  regular expression for matching a column name
     *                    giving the quantity
     * @return  index of column containing requested quantity, or -1
     *          if one can't be found
     */
    private static int guessPosColumn( StarTable table, String ucdAtom,
                                       Pattern nameRegex ) {
        // See also SkyMatch2Mapping.guessDegreesExpression

        /* Prepare possible matching UCD1 and UCD1+ strings. */
        String atom = ucdAtom.toLowerCase();
        final String ucd1Part = "pos_eq_" + atom;
        final String ucd1Full = ucd1Part + "_main";
        final String ucd1pPart = "pos.eq." + atom;
        final String ucd1pFull = ucd1pPart + ";meta.main";

        /* Examine each column, assigning a score to columns that look like
         * they might be what we're after.  The best score is retained. */
        int bestIndex = -1;
        int score = 0;
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
                String ucd = info.getUCD();
                String name = info.getName();
                if ( ucd != null && ucd.length() > 0 ) {
                    ucd = ucd.trim().toLowerCase();

                    /* Full UCD1+ match. */
                    if ( score < 20 && ucd.equals( ucd1pFull ) ) {
                        bestIndex = icol;
                        score = 20;
                    }

                    /* Full UCD1 match. */
                    if ( score < 18 && ucd.equals( ucd1Full ) ) {
                        bestIndex = icol;
                        score = 18;
                    }

                    /* Partial UCD1+ match. */
                    if ( score < 10 && ucd.equals( ucd1pPart ) ) {
                        bestIndex = icol;
                        score = 10;
                    }

                    /* Partial UCD1 match. */
                    if ( score < 8 && ucd.equals( ucd1Part ) ) {
                        bestIndex = icol;
                        score = 8;
                    }
                }

                /* Column name match. */
                if ( name != null && name.length() > 0 ) {
                    if ( score < 5 &&
                         nameRegex.matcher( name.trim() ).matches() ) {
                        bestIndex = icol;
                        score = 5;
                    }
                }
            }
        }

        /* Check degrees.  Anything else is pretty unlikely (we know it's
         * numeric), but should warn in case. */
        final boolean isDeg;
        if ( bestIndex >= 0 ) {
            ColumnInfo info = table.getColumnInfo( bestIndex );
            logger_.info( "Identified column " + info + " as "
                        + ucdAtom.toUpperCase() );
            String units = info.getUnitString();
            if ( units == null || units.trim().length() == 0 ) {
                logger_.info( "No units listed for column " + info.getName()
                            + " - assume degrees" );
                isDeg = true;
            }
            else if ( units.toLowerCase().startsWith( "rad" ) ) {
                logger_.warning( "Uh-oh - column is in radians.  Forget it." );
                isDeg = false;
            }
            else if ( units.toLowerCase().startsWith( "deg" ) ) {
                isDeg = true;
            }
            else {
                logger_.info( "Units for column " + info.getName()
                            + " listed as " + units + " - assume degrees" );
                isDeg = true;
            }
        }
        else {
            isDeg = false;
        }

        /* Return result. */
        if ( isDeg && bestIndex >= 0 ) {
            return bestIndex;
        }
        else {
            logger_.warning( "Can't identify " + ucdAtom.toUpperCase()
                           + " column in degrees" );
            return -1;
        }
    }
}
