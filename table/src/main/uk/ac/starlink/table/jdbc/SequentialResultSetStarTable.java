package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.UnrepeatableSequenceException;

/**
 * StarTable implementation based on a {@link java.sql.ResultSet}.
 * It can read through the data once, and no more.
 *
 * <p><b>Beware</b>: it therefore breaks the general contract of 
 * {@link uk.ac.starlink.table.StarTable}, in that calls of
 * {@link #getRowSequence} after the first one will throw a
 * {@link uk.ac.starlink.table.UnrepeatableSequenceException}.
 * Only use this class if you know that the table needs to be read once only.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2007
 */
public class SequentialResultSetStarTable extends AbstractStarTable {

    private final StarResultSet srset_;
    private boolean seqCreated_;

    /**
     * Constructor.
     *
     * @param  rset  result set containing data - should be positioned at start
     */
    public SequentialResultSetStarTable( ResultSet rset ) throws SQLException {
        this( new StarResultSet( rset ) );
    }

    /**
     * Constructs from a StarResultSet.
     *
     * @param srset  result set containing data - should be positioned at start
     */
    public SequentialResultSetStarTable( StarResultSet srset ) {
        srset_ = srset;
    }

    /**
     * Returns the result set on which this table is built.
     *
     * @return  result set
     */
    public ResultSet getResultSet() {
        return srset_.getResultSet();
    }

    public int getColumnCount() {
        return srset_.getColumnInfos().length;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return srset_.getColumnInfos()[ icol ];
    }

    public long getRowCount() {
        return -1L;
    }

    public List getColumnAuxDataInfos() {
        return srset_.getColumnAuxDataInfos();
    }

    /**
     * The first time it is called, returns an iterator over the rows of the
     * result set.  
     * Subsequent calls will throw an {@link UnrepeatableSequenceException}.
     *
     * @throws   UnrepeatableSequenceException  if called more than once
     */
    public synchronized RowSequence getRowSequence()
            throws UnrepeatableSequenceException, IOException {
        if ( seqCreated_ ) {
            throw new UnrepeatableSequenceException();
        }
        else {
            RowSequence rseq = srset_.createRowSequence();
            seqCreated_ = true;
            return rseq;
        }
    }
}
