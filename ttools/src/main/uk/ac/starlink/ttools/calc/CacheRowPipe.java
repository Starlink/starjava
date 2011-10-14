package uk.ac.starlink.ttools.calc;

import java.io.IOException;
import uk.ac.starlink.table.OnceRowPipe;
import uk.ac.starlink.table.RowPipe;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;

/**
 * RowPipe implementation which caches the data to provide a random-access
 * StarTable.
 *
 * <p>The current implementation simply delegates to a
 * {@link uk.ac.starlink.table.OnceRowPipe} and caches the result before
 * returning it from <code>waitForStarTable</code>.  This is crude,
 * and means that the output rows don't start to come until all the input
 * rows have been written to the pipe (so there's not much point using a pipe).
 * A future implementation should get smarter with threads to improve this.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2011
 */
public class CacheRowPipe implements RowPipe {

    private final RowPipe basePipe_;

    /**
     * Constructor.
     */
    public CacheRowPipe() {
        basePipe_ = new OnceRowPipe();
    }

    public void acceptMetadata( StarTable meta ) throws TableFormatException {
        basePipe_.acceptMetadata( meta );
    }

    public void acceptRow( Object[] row ) throws IOException {
        basePipe_.acceptRow( row );
    }

    public void endRows() throws IOException {
        basePipe_.endRows();
    }

    public void setError( IOException err ) {
        basePipe_.setError( err );
    }

    /**
     * Returns a multiply-readable random access table.
     *
     * @return   random access, multiply readable table with the same
     *           content as the rows accepted by this pipe
     */
    public StarTable waitForStarTable() throws IOException {
        return Tables.randomTable( basePipe_.waitForStarTable() );
    }
}
