package uk.ac.starlink.table;

/**
 * TableScheme that yields a table with a single column giving loop
 * variable values.
 *
 * @author   Mark Taylor
 * @since    20 Jul 2020
 */
public class LoopTableScheme implements TableScheme, Documented {

    public LoopTableScheme() {
    }

    public String getSchemeName() {
        return "loop";
    }

    public String getSchemeUsage() {
        return "<count>|<start>,<end>[,<step>]";
    }

    public String getExampleSpecification() {
        return "6";
    }

    public String getXmlDescription() {
        String prefix = ":" + getSchemeName() + ":";
        return new StringBuffer()
            .append( "<p>Generates a table whose single column\n" )
            .append( "increments over a given range.\n" )
            .append( "</p>\n" )
            .append( "<p>The specification may either be a single value N\n" )
            .append( "giving the number of rows,\n" )
            .append( "which yields values in the range 0..N-1,\n" )
            .append( "or two or three comma-separated values\n" )
            .append( "giving the <i>start</i>, <i>end</i>\n" )
            .append( "and optionally <i>step</i>\n" )
            .append( "corresponding to the conventional specification\n" )
            .append( "of a loop variable.\n")
            .append( "</p>\n" )
            .append( "<p>The supplied numeric parameters are interpreted as\n" )
            .append( "floating point values, but the output column type\n" )
            .append( "will be 32- or 64-bit integer\n" )
            .append( "or 64-bit floating point,\n" )
            .append( "depending on the values that it has to take.\n" )
            .append( "</p>" )
            .append( "<p>Examples:\n" )
            .append( "<ul>\n" )
            .append( "<li><code>" + prefix + "5</code>:\n" )
            .append( "a 5-row table whose integer column has values\n" )
            .append( "0, 1, 2, 3, 4\n" )
            .append( "</li>\n" )
            .append( "<li><code>" + prefix + "10,20</code>:\n" )
            .append( "a 10-row table whose integer column has values\n" )
            .append( "10, 11, ... 19\n" )
            .append( "</li>\n" )
            .append( "<li><code>" + prefix + "1,2,0.25</code>:\n" )
            .append( "a 10-row table whose floating point column has values\n" )
            .append( "1.00, 1.25, 1.50, 1.75\n" )
            .append( "</li>\n" )
            .append( "<li><code>" + prefix + "1e10</code>:\n" )
            .append( "a ten billion row table, with 64-bit integer values\n" )
            .append( "</li>\n" )
            .append( "</ul>\n" )
            .append( "</p>\n" )
            .toString();
    }

    public StarTable createTable( String spec ) throws TableFormatException {
        String[] args = spec.split( ",", -1 );
        String colName = "i";
        double start = 0;
        double end = 16;
        double step = 1;
        Boolean isInteger = null;
        int narg = args.length;
        if ( narg < 1 || narg > 3 ) {
            throw new TableFormatException( "Wrong number of args");
        }
        try {
            if ( narg == 1 ) {
                end = Double.parseDouble( args[ 0 ].replaceAll( "_", "" ) );
            }
            else if ( narg >= 2 ) {
                start = Double.parseDouble( args[ 0 ].replaceAll( "_", "" ) );
                end = Double.parseDouble( args[ 1 ].replaceAll( "_", "" ) );
            }
            if ( narg > 2 ) {
                step = Double.parseDouble( args[ 2 ].replaceAll( "_", "" ) );
            }
        }
        catch ( NumberFormatException e ) {
            throw new TableFormatException( "Bad number", e );
        }
        return new LoopStarTable( colName, start, end, step, isInteger );
    }
}
