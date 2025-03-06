package uk.ac.starlink.ttools.scheme;

import uk.ac.starlink.table.Documented;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableScheme;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.DocUtils;

/**
 * TableScheme that can produce tables representing a sequence of 2d or 3d 
 * points in space corresponding to iterated sequence that follow
 * (strange) attractors.
 * This can be useful for generating large tables with non-trivial content
 * columns (X,Y) or (X,Y,Z), especially for plotting demos.
 *
 * <p>Currently three attractor types are supported, as documented in
 * the usage string.  An example specification would be something like
 * <pre>
 *    attractor:1e7,clifford,-1.25,1.66,-1.79,-0.25
 * </pre>
 * where the first number is row count, the name is one of the supported
 * attractor types, and the numbers are parameters to that attractor.
 * The number of parameters depends on the type, but if no numbers are
 * supplied, then an example set is used.  Note if you supply parameters
 * at random you have a good chance of getting a boring (non-strange)
 * attractor.
 *
 * @author   Mark Taylor
 * @since    20 Jul 2020
 */
public class AttractorScheme implements TableScheme, Documented {

    /** Supported attractor families. */
    private final AttractorFamily[] FAMILIES = new AttractorFamily[] {
        AttractorFamily.CLIFFORD,
        AttractorFamily.RAMPE,
        AttractorFamily.HENON,
    };

    public String getSchemeName() {
        return "attractor";
    }

    public String getSchemeUsage() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<nrow>[,(" );
        for ( int i = 0; i < FAMILIES.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( "|" );
            }
            AttractorFamily family = FAMILIES[ i ];
            sbuf.append( family.getName() )
                .append( '[' );
            for ( int j = 0; j < family.getParamCount(); j++ ) {
                sbuf.append( ',' )
                    .append( (char) ( 'a' + j ) );
            }
            sbuf.append( ']' );
        }
        sbuf.append( ")]" );
        return sbuf.toString();
    }

    public String getExampleSpecification() {
        return "6," + AttractorFamily.RAMPE.getName();
    }

    public String getXmlDescription() {
        String prefix = ":" + getSchemeName() + ":";
        return DocUtils.join( new String[] {
            "<p>Generates tables listing points sampled from",
            "one of a specified family of strange attractors.",
            "These can provide tables with (X,Y) or (X,Y,Z) columns",
            "and arbitrarily many rows.",
            "They can be used, for instance, to make (beautiful)",
            "example large-scale scatter plots in 2-d or 3-d space.",
            "</p>",
            "<p>The specification syntax is of the form",
            "<code>" + prefix
                     + "&lt;nrow&gt;,&lt;family-name&gt;[,&lt;args&gt;]"
                     + "</code>",
            "where",
            "<code>&lt;nrow&gt;</code> is the number of rows required,",
            "<code>&lt;family-name&gt;</code> is the name of one of",
            "the supported families of attractors, and",
            "<code>&lt;args&gt;</code> is an optional comma-separated list",
            "of numeric arguments specifying the family-specific parameters",
            "of the required attractor.",
            "If the <code>&lt;args&gt;</code> part is omitted,",
            "an example attractor from the family is used.",
            "Note that picking <code>&lt;args&gt;</code> values at random",
            "will often result in rather boring (non-strange) attractors.",
            "</p>",
            "<p>The following families are currently supported:",
            createFamilyDocs( FAMILIES ),
            "</p>",
        } );
    }

    /**
     * Returns an XML string with documentation for each of a given list
     * of attractor families.
     *
     * @param  families   items to document
     * @return  text of DL element 
     */
    private String createFamilyDocs( AttractorFamily[] families ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<dl>\n" );
        int jex = 0;
        String[] nrows = new String[] {
            "9999", "1_000_000", "65536", "1e7", "400", "4e6",
            "10e6", "4", "5.5e5",
        };
        for ( AttractorFamily fam : families ) {
            String name = fam.getName();
            sbuf.append( DocUtils.join( new String[] {
                "<dt>" + name + "</dt>",
                "<dd><p>",
                "<a href='" + fam.getDocUrl() + "'>" + name + "</a>",
                "attractors are " + fam.getDimCount() + "-dimensional",
                "and have " + fam.getParamCount() + " parameters,",
                "with suggested values",
                "in the range +/-" + fam.getMaxAbsParam() + ".",
                "</p>",
            } ) );
            sbuf.append( "<p>The iteration is defined by the equations:\n" )
                .append( "<pre>" );
            for ( String eq : fam.getEquations() ) {
                sbuf.append( "    <![CDATA[" )
                    .append( eq )
                    .append( "]]>\n" );
            }
            sbuf.append( "</pre>\n" );
            sbuf.append( "</p>\n" );
            sbuf.append( "<p>Examples:<ul>\n" );
            int iex = 0;
            for ( double[] params : fam.getExamples() ) {
                double[] p = iex++ == 0 ? null : params;
                String nrow = nrows[ jex++ % nrows.length ];
                sbuf.append( "<li><code>" + formatExample( fam, nrow, p )
                                          + "</code></li>\n" );
            }
            sbuf.append( "</ul></p></dd>\n" );
        }
        sbuf.append( "</dl>\n" );
        return sbuf.toString();
    }

    /**
     * Returns a plain text representation of the scheme specification
     * for a given attractor-scheme example.
     *
     * @param   fam  attractor family
     * @param   nrow   required row count
     * @param   params  attractor parameter array, or null for default
     * @return   scheme specification string
     */
    private String formatExample( AttractorFamily fam, String nrow,
                                  double[] params ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( ":" )
            .append( getSchemeName() )
            .append( ":" )
            .append( nrow )
            .append( "," )
            .append( fam.getName() );
        if ( params != null ) {
            for ( double p : params ) {
                sbuf.append( "," )
                    .append( p );
            }
        }
        return sbuf.toString();
    }

    public StarTable createTable( String argtxt ) throws TableFormatException {
        String[] args = argtxt.split( ",", -1 );
        int narg = args.length;
        String name = AttractorFamily.RAMPE.getName();
        double[] params = null;
        long nrow = 1_000_000;
        if ( narg < 1 ) {
            throw new TableFormatException( "Too few arguments" );
        }
        try {
            if ( narg > 0 ) {
                nrow = Tables.parseCount( args[ 0 ] );
            }
            if ( narg > 1 ) {
                name = args[ 1 ];
            }
            if ( narg > 2 ) {
                params = new double[ narg - 2 ];
                for ( int i = 0; i < narg - 2; i++ ) {
                    params[ i ] = Double.parseDouble( args[ i + 2 ] );
                }
            }
        }
        catch ( NumberFormatException e ) {
            throw new TableFormatException( "Not numeric", e );
        }
        AttractorFamily family = null;
        for ( AttractorFamily fam : FAMILIES ) {
            if ( fam.getName().equalsIgnoreCase( name ) ) {
                family = fam;
            }
        }
        if ( family == null ) {
            throw new TableFormatException( "Unknown attractor family "
                                          + name );
        }
        int npc = family.getParamCount();
        if ( params != null && params.length != npc ) {
            throw new TableFormatException( "Wrong number of attractor params; "
                                          + name + " requires " + npc );
        }
        if ( params == null || params.length == 0 ) {
            params = family.getExamples()[ 0 ];
        }
        double[] seed = null;
        AttractorFamily.Attractor att = family.createAttractor( params, seed );
        return new AttractorStarTable( att, nrow );
    }
}
