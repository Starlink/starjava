package uk.ac.starlink.ttools.task;

/**
 * Task to copy multiple homogeneously acquired tables to an output 
 * container file.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2010
 */
public class MultiCopy extends TableMultiCopy {
    public MultiCopy() {
        super( "Writes multiple tables to a single container file",
               new HomogeneousTablesInput( true ) );
    }
}
