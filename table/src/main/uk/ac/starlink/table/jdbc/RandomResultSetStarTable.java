package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RandomStarTable;

/**
 * StarTable implementation based on a random-access {@link java.sql.ResultSet}.
 * Such results sets have a <code>type</code> of 
 * <code>ResultSet.TYPE_SCROLL_*</code> and are generally produced from
 * by using a Statement which has been constructed using corresponding type.
 * This implementation is thread-safe.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2007
 */
public class RandomResultSetStarTable extends RandomStarTable {

    private final StarResultSet srset_;

    /**
     * Constructor.
     *
     * @param  rset  result set containing data
     * @throws  IllegalArgumentException  if <code>rset</code> is not 
     *          random access
     */
    public RandomResultSetStarTable( ResultSet rset ) throws SQLException {
        this( new StarResultSet( rset ) );
    }

    /**
     * Constructor.
     *
     * @param  srset  result set containing data
     * @throws  IllegalArgumentException  if <code>srset</code> is not 
     *          random access
     */
    public RandomResultSetStarTable( StarResultSet srset ) {
        if ( ! srset.isRandom() ) {
            throw new IllegalArgumentException( "ResultSet does not provide " +
                                                "random access (wrong type)" );
        }
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
        return srset_.getRowCount();
    }

    public List getColumnAuxDataInfos() {
        return srset_.getColumnAuxDataInfos();
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        synchronized ( srset_ ) {
            srset_.setRowIndex( lrow );
            return srset_.getCell( icol );
        }
    }

    public Object[] getRow( long lrow ) throws IOException {
        synchronized ( srset_ ) {
            srset_.setRowIndex( lrow );
            return srset_.getRow();
        }
    }

}
