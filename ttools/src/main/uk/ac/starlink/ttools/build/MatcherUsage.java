package uk.ac.starlink.ttools.build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.task.MatchEngineParameter;

public class MatcherUsage {

    private final String matcherName_;

    public MatcherUsage( String matcherName ) {
        matcherName_ = matcherName;
    }

    public String getRawUsage() throws Exception {
        MatchEngineParameter matcherParam = 
            new MatchEngineParameter( "matcher" );
        MatchEngine engine = matcherParam.createEngine( matcherName_ );
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( matcherParam.getName() )
            .append( "=" )
            .append( matcherName_ );
        String pad = sbuf.toString().replaceAll( ".", " " );
        String vu = matcherParam.getValuesUsage( engine );
        String pu = matcherParam.getParamsUsage( engine );
        sbuf.append( vu );
        if ( pu != null && pu.trim().length() > 0 ) {
            sbuf.append( '\n' )
                .append( pad )
                .append( pu );
        }
        return sbuf.toString();
    }

    public String getXMLUsage() throws Exception {
        return "<verbatim><![CDATA[" + getRawUsage() + "]]></verbatim>";
    }

    public static void main( String[] args ) throws Exception {
        if ( args.length == 1 ) {
            System.out.println( new MatcherUsage( args[ 0 ] ).getRawUsage() );
        }
        else {
            File dir = new File( "." );
            for ( int i = 0; i < args.length; i++ ) {
                String matcherName = args[ i ];
                MatcherUsage usage = new MatcherUsage( matcherName );
                String name = matcherName.replaceAll( "\\+", "." );
                String fname = "matcher-" + name + "-usage.xml";
                File file = new File( dir, fname );
                System.out.println( "Writing " + fname );
                OutputStream out = new FileOutputStream( fname );
                out = new BufferedOutputStream( out );
                new PrintStream( out )
               .print( new MatcherUsage( matcherName ).getXMLUsage() );
                out.close();
            }
        }
    }
}
