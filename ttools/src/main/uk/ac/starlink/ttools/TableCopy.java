package uk.ac.starlink.ttools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableBuilder;

/**
 * Top level class for TCOPY tool.   This is an alias for TablePipe
 * functionality, reverse-engineered to do the same job as the old
 * uk.ac.starlink.table.TableCopy.
 *
 * @author   Mark Taylor (Starlink)
 * @since    27 Apr 2005
 */
public class TableCopy {

    private String[] pipeArgs_;
    private boolean verbose_;
    private boolean wantHelp_;

    private TableCopy( String[] args ) throws ArgException {

        /* Process flags. */
        List argList = new ArrayList( Arrays.asList( args ) );
        String ifmt = null;
        String ofmt = null;
        String in = null;
        String out = null;
        List flags = new ArrayList();
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.equals( "-disk" ) ||
                 arg.equals( "-debug" ) ) {
                it.remove();
                flags.add( arg );
            }
            else if ( arg.equals( "-v" ) || arg.equals( "-verbose" ) ) {
                it.remove();
                verbose_ = true;
                flags.add( arg );
            }
            else if ( arg.equals( "-h" ) || arg.equals( "-help" ) ) {
                wantHelp_ = true;
                return;
            }
            else if ( arg.equals( "-ifmt" ) ) {
                it.remove();
                if ( it.hasNext() ) {
                    ifmt = (String) it.next();
                    it.remove();
                }
                else {
                    throw new ArgException( "Missing input format",
                                            "-ifmt <in-format>" );
                }
            }
            else if ( arg.equals( "-ofmt" ) ) {
                it.remove();
                if ( it.hasNext() ) {
                    ofmt = (String) it.next();
                    it.remove();
                }
                else {
                    throw new ArgException( "Missing output format",
                                            "-ofmt <out-format>" );
                }
            }
            else if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                throw new ArgException( "Unknown flag " + arg );
            }
            else if ( in == null ) {
                it.remove();
                in = arg;
            }
            else if ( out == null ) {
                it.remove();
                out = arg;
            }
            else {
                throw new ArgException( "Extra argument(s) " + arg + " ..." );
            }
        }

        /* Construct TablePipe argument list. */
        List tpArgs = new ArrayList();
        tpArgs.addAll( flags );
        if ( ifmt != null ) {
            tpArgs.add( "-ifmt" );
            tpArgs.add( ifmt );
        }
        tpArgs.add( "-write" );
        if ( ofmt != null ) {
            tpArgs.add( "-ofmt" );
            tpArgs.add( ofmt );
        }
        if ( out != null ) {
            tpArgs.add( "-o" );
            tpArgs.add( out );
        }
        if ( in != null ) {
            tpArgs.add( in );
        }
        pipeArgs_ = (String[]) tpArgs.toArray( new String[ 0 ] );
    }

    private boolean isVerbose() {
        return verbose_;
    }

    private boolean wantsHelp() {
        return wantHelp_;
    }

    private String[] getPipeArgs() {
        return pipeArgs_;
    }

    public static void main( String[] args ) {

        /* Set up usage message. */
        String usage = "Usage: " + getCommandName() + " ";
        char[] padc = new char[ usage.length() ];
        Arrays.fill( padc, ' ' );
        String pad = "\n" + new String( padc );
        usage += "[-disk] [-debug] [-h[elp]] [-v[erbose]]"
               + pad + "[-ifmt <in-format>] [-ofmt <out-format>]"
               + pad + "[<in-table> [<out-table>]]";

        /* Translate the commands to TablePipe-friendly ones. */
        try {
            TableCopy tc = new TableCopy( args );
            if ( tc.wantsHelp() ) {
                System.out.println( usage );
                System.out.println( extraHelp() );
                return;
            }
            String[] pipeArgs = tc.getPipeArgs();

            /* Echo command if necessary. */
            if ( tc.isVerbose() ) {
                System.err.print( "tpipe" );
                for ( int i = 0; i < pipeArgs.length; i++ ) {
                    System.err.print( " " + pipeArgs[ i ] );
                }
                System.err.println();
                System.err.flush();
            }

            /* Execute TablePipe. */
            TablePipe.main( pipeArgs );
        }
        catch ( ArgException e ) {
            boolean debug = false;
            if ( debug ) {
                e.printStackTrace( System.err );
            }
            else {
                String msg = e.getMessage();
                String ufrag = e.getUsageFragment();
                String us = ufrag == null ? usage : "Usage: " + ufrag;
                System.err.println();
                System.err.println( e.getMessage() );
                System.err.println( "\n" + us + "\n" );
            }
            System.exit( 1 );
        }
    }

    /**
     * Returns a string giving additional help text suitable for a full
     * usage message.
     *
     * @return  multi-line partial usage message
     */
    private static String extraHelp() {
        StringBuffer help = new StringBuffer();       
        StarTableFactory tfact = new StarTableFactory();
        StarTableOutput toutput = new StarTableOutput();

        help.append( "\n   Auto-detected in-formats:\n" );
        for ( Iterator it = tfact.getDefaultBuilders().iterator();
              it.hasNext(); ) {
            help.append( "      " )
                .append( ((TableBuilder) it.next())
                        .getFormatName().toLowerCase() )
                .append( '\n' );
        }

        help.append( "\n   Known in-formats:\n" );
        for ( Iterator it = tfact.getKnownFormats().iterator();
              it.hasNext(); ) {
            help.append( "      " )
                .append( ((String) it.next()).toLowerCase() )
                .append( '\n' );
        }

        help.append( "\n   Known out-formats:\n" );
        for ( Iterator it = toutput.getKnownFormats().iterator();
              it.hasNext(); ) {
            help.append( "      " )
                .append( ((String) it.next()).toLowerCase() )
                .append( '\n' );
        }

        return help.toString();
    }

    /**
     * Returns the short command name for this application.
     *
     * @return   "tcopy"
     */
    public static String getCommandName() {
        return "tcopy";
    }
}
