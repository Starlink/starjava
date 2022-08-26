package uk.ac.starlink.topcat;

/**
 * RowSubset implementation that includes a maximum of one row index.
 * The {@link #getMaskId} return value depends on the state, not the identity.
 *
 * @author   Mark Taylor
 * @since    26 Aug 2022
 */
public class SingleRowSubset extends RowSubset {

    private long lrow_;

    /**
     * Constructor.
     *
     * @param  name  subset name
     */
    public SingleRowSubset( String name ) {
        super( name );
        lrow_ = -1;
    }

    /**
     * Sets the included row index.
     *
     * @param   lrow  index of single included row, or -1 for no rows
     */
    public void setRowIndex( long lrow ) {
        lrow_ = lrow;
    }
 
    /**
     * Returns the included row index.
     *
     * @return  index of single included row, or -1 for no rows
     */
    public long getRowIndex() {
        return lrow_;
    }

    public boolean isIncluded( long lrow ) {
        return lrow == lrow_ && lrow >= 0;
    }

    /**
     * Returns a value dependent on the selected row.
     */
    @Override
    public String getMaskId() {
        StringBuffer sbuf = new StringBuffer()
                           .append( getClass().getName() )
                           .append( '=' );
        if ( lrow_ >= 0 ) {
            sbuf.append( lrow_ );
        }
        return sbuf.toString();
    }
}
