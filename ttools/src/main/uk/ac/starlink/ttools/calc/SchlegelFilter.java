package uk.ac.starlink.ttools.calc;

import java.util.Iterator;
import uk.ac.starlink.ttools.filter.ArgException;
import uk.ac.starlink.ttools.filter.ProcessingStep;

/**
 * Filter that applies the SchlegelCalculator.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2011
 */
public class SchlegelFilter
        extends ColumnCalculatorFilter<SchlegelCalculator.Spec> {

    /**
     * Constructor.
     */
    public SchlegelFilter() {
        super( "addschlegel", createUsage(), new SchlegelCalculator() );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Adds columns giving Schlegel dust values",
            "at a given sky position (J2000).",
            "Some or all of E(B-V) Reddening, 100 Micro Emission,",
            "and Dust Temperature can be added.",
            "By default the value averaged over an area (5 degrees?)",
            "is returned, but other statistics can be requested as well",
            "or instead.",
            "</p>",
            "<p>An example invocation would be:",
            "<pre>" + getName() + " -results emission -stats mean,std</pre>",
            "giving the mean and standard deviation for just the",
            "100 micron emission quantity.",
            "</p>",
            "<p>This uses the service described at",
            "<webref url='http://irsa.ipac.caltech.edu/applications/DUST/'/>.",
            "</p>",
            explainSyntax( new String[] { "ra-expr", "dec-expr", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String[] tupleExprs = new String[ 2 ];
        SchlegelCalculator.ResultType[] rtypes =
            SchlegelCalculator.ResultType.values();
        SchlegelCalculator.Statistic[] stats = {
            SchlegelCalculator.DEFAULT_STAT };
        String raExpr = null;
        String decExpr = null;
        while ( argIt.hasNext() && ( raExpr == null || decExpr == null ) ) {
            String arg = (String) argIt.next();
            if ( arg.equals( "-results" ) && argIt.hasNext() ) {
                argIt.remove();
                String resultTxt = (String) argIt.next();
                argIt.remove();
                rtypes = decodeResultTypes( resultTxt );
            }
            else if ( arg.equals( "-stats" ) && argIt.hasNext() ) {
                argIt.remove();
                String statTxt = (String) argIt.next();
                argIt.remove();
                stats = decodeStats( statTxt );
            }
            else if ( raExpr == null ) {
                argIt.remove();
                raExpr = arg;
            }
            else if ( decExpr == null ) {
                argIt.remove();
                decExpr = arg;
            }
        }
        if ( raExpr == null || decExpr == null ) {
            throw new ArgException( "No ra/dec specified" );
        }
        return createCalcStep( new String[] { raExpr, decExpr },
                               new SchlegelCalculator.Spec( rtypes, stats ) );
    }

    /**
     * Turns a (comma-separated) string into an array of ResultTypes.
     *
     * @param  txt  input string
     * @return   result types
     */
    private static SchlegelCalculator.ResultType[]
            decodeResultTypes( String txt )
            throws ArgException {
        String[] words = splitEnumList( txt );
        int nword = words.length;
        SchlegelCalculator.ResultType[] rtypes =
            new SchlegelCalculator.ResultType[ nword ];
        for ( int iw = 0; iw < nword; iw++ ) {
            String word = words[ iw ];
            try {
                rtypes[ iw ] =
                    SchlegelCalculator.ResultType.valueOf( word.toUpperCase() );
            }
            catch ( IllegalArgumentException e ) {
                throw new ArgException( "No such result type \""
                                      + word + "\"" );
            }
        }
        return rtypes;
    }

    /**
     * Turns a (comma-separated) string into an array of Statistics.
     *
     * @param   txt  input string
     * @return  statistic objects
     */
    private static SchlegelCalculator.Statistic[] decodeStats( String txt )
            throws ArgException {
        String[] words = splitEnumList( txt );
        int nword = words.length;
        SchlegelCalculator.Statistic[] stats =
            new SchlegelCalculator.Statistic[ nword ];
        for ( int iw = 0; iw < nword; iw++ ) {
            String word = words[ iw ];
            try {
                stats[ iw ] =
                    SchlegelCalculator.Statistic.valueOf( word.toUpperCase() );
            }
            catch ( IllegalArgumentException e ) {
                throw new ArgException( "No such statistic \"" + word + "\"" );
            }
        }
        return stats;
    }

    /**
     * Splits a comma-separated string.
     *
     * @param  txt  input string
     * @return  word array
     */
    private static String[] splitEnumList( String txt ) {
        return txt.split( " *, *" );
    }

    /**
     * Returns the usage string for this filter.
     *
     * @return  usage string
     */
    private static String createUsage() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "[-results (" );
        SchlegelCalculator.ResultType[] resultTypes = 
            SchlegelCalculator.ResultType.values();
        for ( int ir = 0; ir < resultTypes.length; ir++ ) {
            if ( ir > 0 ) {
                sbuf.append( "|" );
            }
            sbuf.append( resultTypes[ ir ].toString().toLowerCase() );
        }
        sbuf.append( ")[,...]]" );
        sbuf.append( "\n" );
        sbuf.append( "[-stats (" );
        SchlegelCalculator.Statistic[] stats =
            SchlegelCalculator.Statistic.values();
        for ( int is = 0; is < stats.length; is++ ) {
            if ( is > 0 ) {
                sbuf.append( "|" );
            }
            sbuf.append( stats[ is ].toString().toLowerCase() );
        }
        sbuf.append( ")[,...]]" );
        sbuf.append( "\n" );
        sbuf.append( "<ra-expr> <dec-expr>" );
        return sbuf.toString();
    }
}
