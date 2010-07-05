package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.LineEnvironment;
import uk.ac.starlink.task.MultiParameter;
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

    private final BooleanParameter multiParam_;
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

        multiParam_ = new BooleanParameter( "multi" );
        multiParam_.setPrompt( "Load all tables from each input file" );
        multiParam_.setDescription( new String[] {
            "<p>Determines whether all tables, or just the first one,",
            "from input table files will be used.",
            "If set <code>false</code>, then just the first table from each",
            "file named by <code>" + getName() + "</code>",
            "will be used.",
            "If <code>true</code>, then all tables present in those",
            "input files will be used.",
            "This only has an effect for file formats which are capable",
            "of containing more than one table, which effectively means",
            "FITS and VOTable and their variants.",
            "</p>",
        } );
        multiParam_.setDefault( "false" );
    }

    /**
     * Returns the parameter which determines whether just the first or all
     * tables in a multi-table container file will be used.
     *
     * @return  multi-table parameter
     */
    public BooleanParameter getMultiParameter() {
        return multiParam_;
    }

    public char getValueSeparator() {
        return '\u00a0';  // non-breaking space
    }

    /**
     * Returns the array of tables specified by this parameter.
     * The <code>toString</code> method of the returned elements
     * can be used to refer to them in user-directed messages.
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
            final TableProducer[] tprods;

            /* If we may have multiple tables per location, we have to get
             * them all up front, otherwise we can't return them as an array,
             * since we don't know how many there will be.  Probably with
             * more cautious design of the interfaces, this could have been
             * avoided. */
            if ( multiParam_.booleanValue( env ) ) {
                List<TableProducer> tprodList = new ArrayList<TableProducer>();
                String[] locs = stringsValue( env );
                for ( int il = 0; il < locs.length; il++ ) {
                    String loc = locs[ il ];
                    StarTable[] tables = makeTables( env, loc );
                    int ntab = tables.length;
                    for ( int itab = 0; itab < ntab; itab++ ) {
                        final StarTable table = tables[ itab ];
                        final String loci = ntab == 1
                                          ? loc
                                          : ( loc + "#" + ( itab + 1 ) );
                        tprodList.add( new TableProducer() {
                            public StarTable getTable() {
                                return table;
                            }
                            public String toString() {
                                return loci;
                            }
                        } );
                    }
                }
                tprods = tprodList.toArray( new TableProducer[ 0 ] );
            }

            /* If we have exactly one table per location, we can defer
             * table construction until later (during the execution phase).
             * This is generally a better idea, since it means parameter
             * errors can get picked up before potentially expensive
             * processing has started. */
            else {
                String[] locs = stringsValue( env );
                int nloc = locs.length;
                tprods = new TableProducer[ nloc ];
                for ( int i = 0; i < nloc; i++ ) {
                    final String loc = locs[ i ];
                    tprods[ i ] = new TableProducer() {
                        public StarTable getTable() throws TaskException {
                            return makeTable( env, loc );
                        }
                        public String toString() {
                            return loc;
                        }
                    };
                }
            }

            /* Store the tables. */
            tables_ = tprods;
        }
        return tables_;
    }

    /**
     * Returns an array of table locations representing the input tables
     * specified by this parameter.
     * Note the number of elements does not necessarily match the number
     * of tables, if the multi-table parameter is true.
     *
     * @param   env  execution environment
     * @return   input table locations
     */
    private String[] stringsValue( Environment env ) throws TaskException {
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
     * Sets the value of this parameter from an array of TableProducers.
     * The <code>toString</code> method of each element should be suitable
     * for use in user-directed messges.
     *
     * @param  tables  input table array
     */
    public void setValueFromTables( TableProducer[] tables ) {
        tables_ = tables;
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < tables.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ' ' );
            }
            sbuf.append( tables[ i ].toString() );
        }
        setStringValue( sbuf.toString() );
        setGotValue( true );
    }
}
