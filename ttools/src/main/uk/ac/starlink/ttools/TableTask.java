package uk.ac.starlink.ttools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.util.Loader;

/**
 * Generic superclass for table processing utilities.
 *
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public abstract class TableTask {

    private boolean noAction_;
    private boolean debug_;
    private boolean verbose_;
    private StarTableFactory treader_;

    public TableTask() {

        /* Ensure that the properties are loaded; this may contain JDBC
         * configuration which should be got before DriverManager might be
         * initialised. */
        Loader.loadProperties();
    }

    /**
     * Returns the name by which this task would like to be known.
     */
    public abstract String getCommandName();

    /**
     * Runs this task.
     *
     * @param   args  command line arguments
     * @return   true iff the task has executed successfully
     */
    public boolean run( String[] args ) {
        List argList = new ArrayList( Arrays.asList( args ) );
        if ( setArgs( argList ) && argList.isEmpty() ) {
            try {
                if ( ! isNoAction() ) {
                    execute();
                }
                return true;
            }
            catch ( IOException e ) {
                if ( debug_ ) {
                    e.printStackTrace( System.err );
                }
                else {
                    System.err.println( e.getMessage() );
                }
                return false;
            }
        }
        else {
            System.err.println( getUsage() );
            return false;
        }
    }

    /**
     * Consume a list of arguments.
     * Any arguments which this task knows about should be noted and
     * removed from the list.  Any others should be ignored,
     * and left in the list.
     * The return value should be true if everything looks OK,
     * false if there is some syntax error in the arguments.
     *
     * @param   argList  an array of strings obtained from the command line
     * @return  true  iff the arguments are unobjectionable
     */
    public boolean setArgs( List argList ) {

        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                if ( arg.equals( "-disk" ) ) {
                    it.remove();
                    StoragePolicy.setDefaultPolicy( StoragePolicy.PREFER_DISK );
                    getTableFactory().setStoragePolicy( StoragePolicy
                                                       .PREFER_DISK );
                }
                else if ( arg.equals( "-debug" ) ) {
                    it.remove();
                    debug_ = true;
                }
                else if ( arg.equals( "-v" ) || arg.equals( "-verbose" ) ) {
                    it.remove();
                    verbose_ = true;
                }
                else if ( arg.equals( "-h" ) || arg.equals( "-help" ) ) {
                    it.remove();
                    System.out.println( getHelp() );
                    noAction_ = true;
                }
            }
        }

        /* Adjust logging level if necessary. */
        if ( verbose_ ) {
            Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.INFO );
        }
        else {
            Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );
        }

        return true;
    }

    public boolean isVerbose() {
        return verbose_;
    }

    public boolean isNoAction() {
        return noAction_;
    }

    /**
     * Performs the work of this task;
     */
    public abstract void execute() throws IOException;

    /**
     * Returns a table factory.
     *
     * @return   factory
     */
    public StarTableFactory getTableFactory() {
        if ( treader_ == null ) {
            treader_ = new StarTableFactory( false );
        }
        return treader_;
    }

    /**
     * Returns a help message.  May be more verbose than usage message.
     *
     * @return   help string
     */
    public String getHelp() {
        return getUsage();
    }

    /**
     * Returns a usage message.
     * This is composed of both {@link #getGenericOptions} and 
     * {@link #getSpecificOptions}.
     *
     * @return   usage string
     */
    public String getUsage() {
        StringBuffer line = new StringBuffer( "Usage: " + getCommandName() );
        String pad = line.toString().replaceAll( ".", " " );
        List uwords = new ArrayList();
        uwords.addAll( Arrays.asList( getGenericOptions() ) );
        uwords.addAll( Arrays.asList( getSpecificOptions() ) );
        StringBuffer ubuf = new StringBuffer();
        for ( Iterator it = uwords.iterator(); it.hasNext(); ) {
            String word = (String) it.next();
            if ( line.length() + word.length() > 75 ) {
                ubuf.append( line )
                    .append( "\n" );
                line = new StringBuffer( pad );
            }
            line.append( " " )
                .append( word );
        }
        ubuf.append( line );
        return "\n" + ubuf.toString() + "\n";
    }

    /**
     * Returns a list of generic options understood by this class.
     *
     * @return  generic options (one string per word)
     */
    public String[] getGenericOptions() {
        return new String[] {
            "[-disk]",
            "[-debug]",
            "[-h[elp]]", 
            "[-v[erbose]]",
        };
    }

    /**
     * Returns a list of options specfic to this TableTask subclass.
     *
     * @return  specific options (one string per word)
     */
    public abstract String[] getSpecificOptions();

}
