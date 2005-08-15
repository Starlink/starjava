package uk.ac.starlink.ttools.task;

/**
 * Task which copies a single table from input to output.
 *
 * @author   Mark Taylor 
 * @since    15 Aug 2005
 */
public class TableCopy extends MapperTask {
    public TableCopy() {
        super( new CopyMapper(), false );
    }
}
