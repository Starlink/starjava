package uk.ac.starlink.ttools.build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.join.MatchEngineParameter;

public class MatcherUsage {

    private final String matcherName_;
    private final MatchEngineParameter matcherParam_;
    private final MatchEngine engine_;

    public MatcherUsage( String matcherName ) throws UsageException {
        matcherName_ = matcherName;
        matcherParam_ = new MatchEngineParameter( "matcher" );
        engine_ = matcherParam_.createEngine( matcherName );
    }

    public String getRawUsage() throws Exception {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( matcherParam_.getName() )
            .append( "=" )
            .append( matcherName_ );
        String pad = sbuf.toString().replaceAll( ".", " " );
        String vu = matcherParam_.getValuesUsage( engine_, sbuf.length() );
        String pu = matcherParam_
             .getConfigUsage( engine_,
                              matcherParam_.getMatchParametersParameter(),
                              engine_.getMatchParameters() );
        String tu = matcherParam_
             .getConfigUsage( engine_,
                              matcherParam_.getTuningParametersParameter(),
                              engine_.getTuningParameters() );
        sbuf.append( vu );
        if ( pu != null && pu.trim().length() > 0 ) {
            sbuf.append( '\n' )
                .append( pad )
                .append( pu );
        }
        if ( tu != null && tu.trim().length() > 0 ) {
            sbuf.append( '\n' )
                .append( pad )
                .append( tu );
        }
        return sbuf.toString();
    }

    public String getXMLUsage() throws Exception {
        return new StringBuffer()
            .append( "<blockcode>" )
            .append( "<![CDATA[" )
            .append( getRawUsage() )
            .append( "]]>" )
            .append( "</blockcode>\n" )
            .append( "<blockquote>\n" )
            .append( "<dl>\n" )
            .append( getXmlEntry( matcherParam_
                                 .createMatchTupleParameter( "*" ),
                                  engine_.getTupleInfos() ) )
            .append( getXmlEntry( matcherParam_.getMatchParametersParameter(),
                                  getValueInfos( engine_
                                                .getMatchParameters() ) ) )
            .append( getXmlEntry( matcherParam_.getTuningParametersParameter(),
                                  getValueInfos( engine_
                                                .getTuningParameters() ) ) )
            .append( "</dl>\n" )
            .append( "</blockquote>\n" )
            .toString();
    }

    private String getXmlEntry( Parameter param, ValueInfo[] infos ) {
        if ( infos.length == 0 ) {
            return "";
        }
        String pname = param.getName();
        StringBuffer sbuf = new StringBuffer()
            .append( "<dt>" )
            .append( "<code>" )
            .append( pname )
            .append( "</code>" )
            .append( ':' )
            .append( "</dt>\n" )
            .append( "<dd>\n" )
            .append( "<p>\n" )
            .append( "<ul>\n" );
        for ( int i = 0; i < infos.length; i++ ) {
            ValueInfo info = infos[ i ];
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( "<![CDATA[" )
                .append( MatchEngineParameter.getInfoUsage( info ) )
                .append( "]]>" )
                .append( "</code>: " )
                .append( "<![CDATA[" )
                .append( info.getDescription() )
                .append( "]]>" )
                .append( "</li>" )
                .append( '\n' );
        }
        sbuf.append( "</ul>\n" )
            .append( "</p>\n" )
            .append( "</dd>\n" );
        return sbuf.toString();
    }

    private ValueInfo[] getValueInfos( DescribedValue[] dvals ) {
        ValueInfo[] infos = new ValueInfo[ dvals.length ];
        for ( int i = 0; i < dvals.length; i++ ) {
            infos[ i ] = dvals[ i ].getInfo();
        }
        return infos;
    }

    public static void main( String[] args ) throws Exception {
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
