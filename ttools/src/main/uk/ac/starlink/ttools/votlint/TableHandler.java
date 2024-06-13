package uk.ac.starlink.ttools.votlint;

import java.util.ArrayList;
import java.util.List;

/**
 * ElementHandler for TABLE elements.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class TableHandler extends ElementHandler {

    private long nrowsSpecified_ = -1L;
    private long nrowsSeen_ = 0L;
    private List<FieldHandler> fields_ = new ArrayList<FieldHandler>();

    public void endElement() {

        /* If we had an nrows attribute, make sure it agrees with the
         * number of rows that were present. */
        if ( nrowsSpecified_ >= 0 ) {
            if ( nrowsSeen_ != nrowsSpecified_ ) {
                error( new VotLintCode( "NRM" ),
                       "Row count (" + nrowsSeen_ + ") not equal to " +
                       "nrows attribute (" + nrowsSpecified_ +")" );
            }
        }
    }

    /**
     * Called to register that a row in this table has been seen.
     * Used to keep track of how many rows there are.
     */
    public void foundRow() {
        nrowsSeen_++;
    }

    /**
     * Returns the index of the row currently being read.
     *
     * @return  zero-based row index
     */
    public long getCurrentRowIndex() {
        return nrowsSeen_;
    }

    /**
     * Called by a child FIELD element to indicate that a column has been
     * seen, and to describe how to check the contents of that column.
     * If no working parser can be made, this method should be called
     * with <tt>parser=null</tt>.
     *
     * @param   field  parser for the newly added field
     */
    public void registerField( FieldHandler field ) {
        fields_.add( field );
    }

    /**
     * Returns a list of the fields currently found.  There will be
     * one for each FIELD child so far encountered.
     *
     * @return  parser list
     */
    public List<FieldHandler> getFields() {
        return fields_;
    }

    /**
     * Attribute checker which checks a TABLE's nrows attribute.
     */
    public static class NrowsChecker implements AttributeChecker {

        public void check( String value, ElementHandler handler ) {

            /* Check and store the nrows value.  It will be examined at the
             * end of this TABLE element for consistency with the number
             * of rows actually encountered. */
            TableHandler thandler = (TableHandler) handler;
            try {
                long nr = Long.parseLong( value );
                if ( nr < 0 ) {
                    handler.error( new VotLintCode( "NRM" ),
                                   "Negative value for nrows: " + value );
                }
                else {
                    thandler.nrowsSpecified_ = nr;
                }
            }
            catch ( IllegalArgumentException e ) {
                handler.error( new VotLintCode( "NFT" ),
                               "Bad number format for nrows: " + value );
            }
        }
    }
}
