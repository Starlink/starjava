package uk.ac.starlink.ttools.task;

import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for specifying an indeterminate number of input tables.
 * All use the same format specifier and streaming flag.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2006
 */
public class InputTablesParameter extends AbstractInputTableParameter
                                  implements MultiParameter {

    private TableProducer[] tables_;
    private String[] locs_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public InputTablesParameter( String name ) {
        super( name );
        setUsage( "<table> ..." );

        String indir = String.valueOf( LineEnvironment.INDIRECTION_CHAR );
        setDescription( new String[] {
            "<p>Locations of the input tables.",
            "Either specify the parameter multiple times, or supply the",
            "input tables as a space-separated list within a single use.",
            "Each table location may be a filename or URL, and may point",
            "to data compressed in one of the supported compression formats",
            "(Unix compress, gzip or bzip2).",
            "</p>",
            "<p>A list of input table locations may be given in an external",
            "file by using the indirction character '" + indir + "'.",
            "Thus \"<code>" + getName() + "=" + indir + "filename</code>\"",
            "causes the file <code>filename</code> to be read for a list",
            "of input table locations.  The locations in the file should",
            "each be on a separate line.",
            "</p>",
        } );

        getStreamParameter().setDescription( new String[] {
            getStreamParameter().getDescription(),
            "<p>The same streaming flag applies to all the tables specified by",
            "<code>" + getName() + "</code>.",
            "</p>",
        } );

        getFormatParameter().setDescription( new String[] {
            getFormatParameter().getDescription(),
            "<p>The same format parameter applies to all the tables",
            "specified by <code>" + getName() + "</code>.",
            "</p>",
        } );
    }

    public char getValueSeparator() {
        return '\u00a0';  // non-breaking space
    }

    /**
     * Returns the array of tables specified by this parameter.
     *
     * @param  env  execution environment
     * @return   array of input tables
     */
    public TableProducer[] tablesValue( final Environment env )
            throws TaskException {
        checkGotValue( env );
        if ( tables_ == null ) {

            /* Acquire the values of the associated parameters from the
             * environment.  Although we don't use them yet, this is 
             * necessary so that the environment knows that we are going
             * to use them, and doesn't count them as orphaned. */
            getFormatParameter().stringValue( env );
            getStreamParameter().stringValue( env );

            /* Get an array of TableProducers representing the value of
             * this parameter. */
            String[] locs = stringsValue( env );
            int nloc = locs.length;
            TableProducer[] tables = new TableProducer[ nloc ];
            for ( int i = 0; i < nloc; i++ ) {
                final String loc = locs[ i ];
                tables[ i ] = new TableProducer() {
                    public StarTable getTable() throws TaskException {
                        return makeTable( env, loc );
                    }
                };
            }

            /* Store it. */
            tables_ = tables;
        }
        return tables_;
    }

    /**
     * Returns an array of table locations representing the input tables
     * specified by this parameter.
     *
     * @param   env  execution environment
     * @return   input table locations
     */
    public String[] stringsValue( Environment env ) throws TaskException {
        checkGotValue( env );
        if ( locs_ == null ) {
            String locset = stringValue( env );
            String[] locs;
            if ( locset.indexOf( getValueSeparator() ) >= 0 ) {
                locs = locset.split( "\\" + getValueSeparator() );
            }
            else {
                locs = locset.trim().split( " +" );
            }
            locs_ = locs;
        }
        return locs_;
    }

    /**
     * Sets the value of this parameter from an array of tables.
     *
     * @param  tables  input table array
     */
    public void setValueFromTables( TableProducer[] tables ) {
        tables_ = tables;
        locs_ = new String[ tables.length ];
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < tables.length; i++ ) {
            locs_[ i ] = "table_" + ( i + 1 );
            if ( i > 0 ) {
                sbuf.append( ' ' );
            }
            sbuf.append( locs_[ i ] );
        }
        setStringValue( sbuf.toString() );
        setGotValue( true );
    }
}
