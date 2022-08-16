package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;
import uk.ac.starlink.vo.ResolverException;
import uk.ac.starlink.vo.ResolverInfo;

/**
 * Filter which adds RA, Dec coordinate columns by performing name resolution
 * using an external service.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2010
 */
public class ResolverFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public ResolverFilter() {
        super( "addresolve",
               "<col-id-objname> <col-name-ra> <col-name-dec>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Performs name resolution on the string-valued column",
            "<code>&lt;col-id-objname&gt;</code> and appends two new columns",
            "<code>&lt;col-name-ra&gt;</code> and",
            "<code>&lt;col-name-dec&gt;</code>",
            "containing the resolved Right Ascension and Declination",
            "in degrees.",
            "</p>",
            explainSyntax( new String[] { "col-id-objname" } ),
            "<p>UCDs are added to the new columns in a way which tries to",
            "be consistent with any UCDs already existing in the table.",
            "</p>",
            "<p>Since this filter works by interrogating a remote service,",
            "it will obviously be slow.",
            "The current implementation is experimental;",
            "it may be replaced in a future release",
            "by some way of doing the same thing (perhaps a new STILTS task)",
            "which is able to work more efficiently by dispatching multiple",
            "concurrent requests.",
            "</p>",
            "<p>This is currently implemented using the Simbad service",
            "operated by",
            "<webref url='http://cdsweb.u-strasbg.fr/'>CDS</webref>.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String objId = null;
        String raName = null;
        String decName = null;
        while ( argIt.hasNext() &&
                ( objId == null || raName == null || decName == null ) ) {
            String arg = argIt.next();
            if ( objId == null ) {
                argIt.remove();
                objId = arg;
            }
            else if ( raName == null ) {
                argIt.remove();
                raName = arg;
            }
            else if ( decName == null ) {
                argIt.remove();
                decName = arg;
            }
        }
        if ( objId != null && raName != null && decName != null ) {
            final Resolver resolver = new SesameResolver();
            final String objId0 = objId;
            final String raName0 = raName;
            final String decName0 = decName;
            return new ProcessingStep() {
                public StarTable wrap( StarTable base ) throws IOException {
                    int iNameCol = new ColumnIdentifier( base )
                                  .getColumnIndex( objId0 );
                    ColumnSupplement inNameSup =
                        new PermutedColumnSupplement( base,
                                                      new int[] { iNameCol } );
                    ColumnSupplement outCoordsSup =
                        new ResolverSupplement( inNameSup, resolver,
                                                raName0, decName0,
                                                Tables.getColumnInfos( base ),
                                                100000 );
                    StarTable out = new AddColumnsTable( base, outCoordsSup );
                    int ncol = out.getColumnCount();
                    AddColumnFilter.checkDuplicatedName( out, ncol - 2 );
                    AddColumnFilter.checkDuplicatedName( out, ncol - 1 );
                    return out;
                }
            };
        }
        else {
            throw new ArgException( "Bad " + getName() + " specification" );
        }
    }

    /**
     * ColumnSupplement which provides RA, Dec columns by performing
     * name resolution.
     */
    private static class ResolverSupplement extends CalculatorColumnSupplement {

        private final Resolver resolver_;
        private final Map<String,Pair> cache_;

        /**
         * Constructor.
         *
         * @param  nameSup  column supplement with a single column,
         *                  the name to be resolved
         * @param  resolver  resolver implementation to use
         * @param  raName  name of new RA column
         * @param  decName  name of new Declination column
         * @param  baseColInfos  column metadata for the table this
         *                       will be added to
         * @param  cacheSize  max size of resolver cache;
         *                    if &lt;=0 cache size is unlimited
         */
        ResolverSupplement( ColumnSupplement nameSup, Resolver resolver,
                            String raName, String decName,
                            ColumnInfo[] baseColInfos, final int cacheSize )
                throws IOException {
            super( nameSup, createColumnInfos( raName, decName, resolver,
                                               baseColInfos ) );
            resolver_ = resolver;
            Map<String,Pair> cache =
                cacheSize >= 0 ? new LinkedHashMap<String,Pair>() {
                                     protected boolean removeEldestEntry(
                                             Map.Entry<String,Pair> eldest ) {
                                         return size() > cacheSize;
                                     }
                                 }
                               : new HashMap<String,Pair>();
            cache_ = Collections.synchronizedMap( cache );
        }

        protected Object[] calculate( Object[] inValues ) {
            Object item = inValues[ 0 ];
            if ( item instanceof String ) {
                String objName = (String) item;
                if ( objName.trim().length() > 0 ) {
                    if ( ! cache_.containsKey( objName ) ) {
                        final Pair p;
                        double[] radec = resolver_.resolve( objName );
                        if ( radec == null || Double.isNaN( radec[ 0 ] )
                                           || Double.isNaN( radec[ 1 ] ) ) {
                            p = null;
                        }
                        else {
                            p = new Pair( radec[ 0 ], radec[ 1 ] );
                        }
                        cache_.put( objName, p );
                    }
                    Pair p = cache_.get( objName );
                    if ( p != null ) {
                        return new Object[] { new Double( p.ra_ ),
                                              new Double( p.dec_ ) };
                    }
                }
            }
            return new Object[ 2 ];
        }

        /**
         * Returns column metadata items appropriate for RA and Dec columns.
         */
        private static ColumnInfo[]
                       createColumnInfos( String raName, String decName,
                                          Resolver resolver,
                                          ColumnInfo[] baseColInfos ) {

            /* Set up basic metadata. */
            String serviceName = resolver.getServiceName();
            ColumnInfo raInfo =
                new ColumnInfo( raName, Double.class,
                                "Resolved Right Ascension as determined by "
                              + serviceName );
            ColumnInfo decInfo =
                new ColumnInfo( decName, Double.class,
                                "Resolved Declination as determined by "
                              + serviceName );

            /* Units. */
            raInfo.setUnitString( "deg" );
            decInfo.setUnitString( "deg" );

            /* Work out what UCDs would be appropriate by examining the
             * ones currently in the base table.  There are two main jobs:
             * see if we want UCD1 or UCD1+, and see whether we already 
             * have RA/DEC UCDs. */
            boolean hasRa = false;
            boolean hasDec = false;
            int like1 = 0;
            int like1plus = 0;
            Pattern raRegex = Pattern.compile( "^pos.eq.ra.*",
                                               Pattern.CASE_INSENSITIVE );
            Pattern decRegex = Pattern.compile( "^pos.eq.dec.*",
                                                Pattern.CASE_INSENSITIVE );
            for ( int icol = 0; icol < baseColInfos.length; icol++ ) {
                String ucd = baseColInfos[ icol ].getUCD();
                if ( ucd != null ) {
                    hasRa = hasRa || raRegex.matcher( ucd ).matches();
                    hasDec = hasDec || decRegex.matcher( ucd ).matches();
                    int leng = ucd.length();
                    for ( int i = 0; i < leng; i++ ) {
                        char c = ucd.charAt( i );
                        if ( c == '_' ) {
                            like1++;
                        }
                        else if ( c == '.' ) {
                            like1plus++;
                        }
                    }
                }
            }
            boolean is1plus = like1plus >= like1;
            final String raUcd;
            final String decUcd;
            if ( hasRa || hasDec ) {
                raUcd = is1plus ? "pos.eq.ra" : "POS_EQ_RA";
                decUcd = is1plus ? "pos.eq.dec" : "POS_EQ_DEC";
            }
            else {
                raUcd = is1plus ? "pos.eq.ra;meta.main" : "POS_EQ_RA_MAIN";
                decUcd = is1plus ? "pos.eq.dec;meta.main" : "POS_EQ_DEC_MAIN";
            }
            raInfo.setUCD( raUcd );
            decInfo.setUCD( decUcd );

            /* Return the result. */
            return new ColumnInfo[] { raInfo, decInfo };
        }

        /**
         * Utility class to aggregate an RA,Dec coordinate pair.
         */
        private static class Pair {
            final double ra_;
            final double dec_;
            Pair( double ra, double dec ) {
                ra_ = ra;
                dec_ = dec;
            }
        }
    }

    /**
     * Interface for a generic name resolution service.
     */
    private static abstract class Resolver {
        private final String serviceName_;

        /**
         * Constructor.
         *
         * @param  serviceName  short human-readable service name
         */
        protected Resolver( String serviceName ) {
            serviceName_ = serviceName;
        }

        /**
         * Returns the service name.
         *
         * @return  short human-readable service name
         */
        public String getServiceName() {
            return serviceName_;
        }

        /**
         * Performs name resolution.
         *
         * @param   name  object name
         * @return  2-element RA,Dec position in degrees,
         *          or null if resolution fails
         */
        public abstract double[] resolve( String name );
    }

    /**
     * Resolver implementation using CDS Sesame service.
     */
    private static class SesameResolver extends Resolver {
        SesameResolver() {
            super( "Sesame" );
        }
        public double[] resolve( String objName ) {
            try {
                ResolverInfo info = ResolverInfo.resolve( objName );
                return new double[] { info.getRaDegrees(),
                                      info.getDecDegrees() };
            }
            catch ( ResolverException e ) {
                return null;
            }
        }
    }
}
