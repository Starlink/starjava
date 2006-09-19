package uk.ac.starlink.ttools.task;

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

    private StarTable[] tables_;
    private String[] locs_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public InputTablesParameter( String name ) {
        super( name );
        setUsage( "<table> ..." );

        setDescription( new String[] {
            "Locations of the input tables.",
            "Either specify the parameter multiple times, or supply the",
            "input tables as a space-separated list within a single use.",
            "Each table location may be a filename or URL, and may point",
            "to data compressed in one of the supported compression formats",
            "(Unix compress, gzip or bzip2).",
        } );

        getStreamParameter().setDescription( new String[] {
            getStreamParameter().getDescription(),
            "The same streaming flag applies to all the tables specified by",
            "<code>" + getName() + "</code>.",
        } );

        getFormatParameter().setDescription( new String[] {
            getFormatParameter().getDescription(),
            "The same format parameter applies to all the tables specified by",
            "<code>" + getName() + "</code>.",
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
    public StarTable[] tablesValue( Environment env ) throws TaskException {
        checkGotValue( env );
        if ( tables_ == null ) {
            String locset = stringValue( env );
            String[] locs;
            if ( locset.indexOf( getValueSeparator() ) >= 0 ) {
                locs = locset.split( "\\" + getValueSeparator() );
            }
            else {
                locs = locset.trim().split( " +" );
            }
            int nloc = locs.length;
            StarTable[] tables = new StarTable[ nloc ];
            for ( int i = 0; i < nloc; i++ ) {
                tables[ i ] = makeTable( env, locs[ i ] );
            }
            locs_ = locs;
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
        tablesValue( env );
        return locs_;
    }

    /**
     * Sets the value of this parameter from an array of tables.
     *
     * @param  tables  input table array
     */
    public void setValueFromTables( StarTable[] tables ) {
        tables_ = tables;
        locs_ = new String[ tables.length ];
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < tables.length; i++ ) {
            locs_[ i ] = tables[ i ].getName();
            if ( i > 0 ) {
                sbuf.append( ' ' );
            }
            sbuf.append( locs_[ i ] );
        }
        setStringValue( sbuf.toString() );
        setGotValue( true );
    }
}
