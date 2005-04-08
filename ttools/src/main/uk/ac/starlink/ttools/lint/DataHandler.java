package uk.ac.starlink.ttools.lint;

/**
 * Element handler for DATA elements.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class DataHandler extends ElementHandler {

    private ValueParser[] parsers_;

    public void startElement() {

        /* Acquire the list of parsers which the parent TABLE element has
         * accumulated and store them for the convenience of child 
         * elements which actually need to do the encoding. */
        TableHandler table =
            (TableHandler) getAncestry().getAncestor( TableHandler.class );
        if ( table != null ) {
            parsers_ = (ValueParser[])
                       table.getParsers().toArray( new ValueParser[ 0 ] );
            if ( parsers_.length == 0 ) {
                error( "There are no columns in this table!" );
            }
        }
        else {
            parsers_ = new ValueParser[ 0 ];
            error( getName() + " outside TABLE" );
        }
    }

    /**
     * Returns the parser for a given column.
     *
     * @param  icol  column index
     * @return   parser for column <tt>icol</tt>, or null if we don't know
     *           how to parse it
     */
    public ValueParser getParser( int icol ) {
        return icol < parsers_.length ? parsers_[ icol ] : null;
    }

    /**
     * Returns the number of columns in this DATA's table.
     *
     * @return   column count
     */
    public int getColumnCount() {
        return parsers_.length;
    }
}
