package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.ttools.ArgException;

/**
 * Processing mode which writes out a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public class CopyMode extends ProcessingMode {

    private String outLoc_;
    private String outFmt_;
    private StarTableOutput toutput_;

    public String getName() {
        return( "write" );
    }

    public void setArgs( List argList ) throws ArgException {
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                if ( arg.equals( "-ofmt" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        outFmt_ = (String) it.next();
                        it.remove();
                        if ( outFmt_ != null && outFmt_.trim().length() > 0 ) {
                            try {
                                getTableOutput().getHandler( outFmt_ );
                            }
                            catch ( TableFormatException e ) {
                                String ufrag = "-" + getName() + " " +
                                               getModeUsage() + "\n" +
                                               getHelp();
                                String msg = e.getMessage();
                                if ( msg == null ) {
                                    msg = "No handler for output format " +
                                          outFmt_;
                                }
                                throw new ArgException( msg, ufrag );
                            }
                        }
                    }
                    else {
                        throw new ArgException( "Missing output format" );
                    }
                }
                else if ( arg.equals( "-o" ) || arg.equals( "-out" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        outLoc_ = (String) it.next();
                        it.remove();
                    }
                    else {
                        throw new ArgException( "Missing output file" );
                    }
                }
            }
        }
    }

    public String getModeUsage() {
        return "[-ofmt <out-format>] [-o <out-table>]";
    }

    public String getHelp() {
        StringBuffer buf = new StringBuffer();
        buf.append( "\n   Known out-formats:\n" );
        for ( Iterator it = getTableOutput().getKnownFormats().iterator();
              it.hasNext(); ) {
            buf.append( "      " )
               .append( ((String) it.next()).toLowerCase() )
               .append( '\n' );
        }
        return buf.toString();
    }

    /**
     * Writes the input table to an output table.
     * 
     * @param  table input table
     */
    public void process( StarTable table ) throws IOException {
        StarTableOutput toutput = getTableOutput();
        if ( outLoc_ == null ) {
            if ( outFmt_ == null ) {
                outFmt_ = "text";
            }
            toutput.writeStarTable( table, System.out,
                                    toutput.getHandler( outFmt_ ) );
        }
        else {
            toutput.writeStarTable( table, outLoc_, outFmt_ );
        }
    }

    /**
     * Returns a table output marshaller.
     *
     * @return  table outputter
     */
    private StarTableOutput getTableOutput() {
        if ( toutput_ == null ) {
            toutput_ = new StarTableOutput();
        }
        return toutput_;
    }
}
