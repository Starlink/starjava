package uk.ac.starlink.ttools.task;

/**
 * Task to add multiple tables, perhaps with different formats and
 * preprocessing, to an output container file.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2010
 */
public class MultiCopyN extends TableMultiCopy {
    public MultiCopyN() {
        super( "Writes multiple processed tables to single container file",
               new VariableTablesInput( true ) );
    }
}
