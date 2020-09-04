package uk.ac.starlink.ttools.build;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.Documented;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableScheme;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.Stilts;

/**
 * Writes a section of XML text documenting the details of TableSchemes
 * available within the STILTS application.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2020
 */
public class SchemeDoc {

    private final boolean requiresDoc_;

    /**
     * Constructor.
     *
     * @param  requiresDoc  if true, an error is generated for TableSchemes
     *                      that do not implement the interface
     *                      uk.ac.starlink.table.Documented
     */
    public SchemeDoc( boolean requiresDoc ) {
        requiresDoc_ = requiresDoc;
    }

    /**
     * Returns a string containing an XML subsubsect element decribing
     * a given TableScheme.
     *
     * @param   scheme  table scheme, preferably implementing Documented
     * @return   XML text
     */
    public String getXmlDoc( TableScheme scheme ) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        String name = scheme.getSchemeName();
        sbuf.append( "<subsubsect id='scheme-" )
            .append( name )
            .append( "'>\n" )
            .append( "<subhead><title><code>" )
            .append( name )
            .append( "</code></title></subhead>\n" )
            .append( "<p>Usage: <code><![CDATA[:" )
            .append( name )
            .append( ":" )
            .append( scheme.getSchemeUsage() )
            .append( "]]></code>\n" )
            .append( "</p>\n" );
        if ( scheme instanceof Documented ) {
            sbuf.append( DocUtils.getXmlDescription( (Documented) scheme ) );
        }
        else if ( requiresDoc_ ) {
            throw new IllegalArgumentException( "Scheme does not implement "
                                              + "Documented" );
        }
        String exSpec = scheme.getExampleSpecification();
        if ( exSpec != null ) {
            sbuf.append( "<p>Example:\n" )
                .append( "<verbatim><![CDATA[\n" )
                .append( ":" )
                .append( scheme.getSchemeName() )
                .append( ":" )
                .append( exSpec )
                .append( "\n" )
                .append( Tables.tableToString( scheme.createTable( exSpec ),
                                               null ) )
                .append( "\n]]></verbatim>\n" )
                .append( "</p>\n" );
        }
        sbuf.append( "</subsubsect>\n" );
        return sbuf.toString();
    }

    /**
     * Writes a sequence of XML subsubsect elements to standard output,
     * documenting the TableSchemes available by default from STILTS.
     */
    public static void main( String[] args ) throws IOException {
        String usage = "\n   Usage: "
                     + SchemeDoc.class.getName() 
                     + " [-[no]stil]"
                     + " [-[no]stilts]"
                     + "\n";
        boolean hasStil = true;
        boolean hasStilts = false;
        for ( String arg : args ) {
            if ( "-stil".equals( arg ) ) {
                hasStil = true;
            }
            else if ( "-nostil".equals( arg ) ) {
                hasStil = false;
            }
            else if ( "-stilts".equals( arg ) ) {
                hasStilts = true;
            }
            else if ( "-nostilts".equals( arg ) ) {
                hasStilts = false;
            }
            else {
                System.err.println( usage );
                System.exit( 1 );
            }
        }
        List<TableScheme> schemes = new ArrayList<>();
        if ( hasStilts ) {
            schemes.addAll( Arrays.asList( Stilts.getStandardSchemes() ) );
        }
        if ( hasStil ) {
            schemes.addAll( new StarTableFactory().getSchemes().values() );
        }
        PrintStream out = System.out;
        SchemeDoc sdoc = new SchemeDoc( true );
        for ( TableScheme scheme : schemes ) {
            out.println( sdoc.getXmlDoc( scheme ) );
        }
    }
}
