package uk.ac.starlink.ttools.votlint;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract handler superclass for elements with STREAM children
 * (BINARY or FITS).
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Apr 2005
 */
public abstract class StreamingHandler extends ElementHandler {

    private FieldHandler[] fields_;
    private TableHandler table_;

    public void startElement() {

        /* Identify the DATA and TABLE ancestor elements of this one. */
        DataHandler data = null;
        if ( getAncestry().getParent() instanceof DataHandler ) {
            data = (DataHandler) getAncestry().getParent();
            if ( data.getAncestry().getParent() instanceof TableHandler ) {
                table_ = (TableHandler) data.getAncestry().getParent();
            }
            else {
                error( new VotLintCode( "DDS" ), "DATA not child of TABLE" );
            }
        }
        else {
            error( new VotLintCode( "DDS" ), this + " not child of DATA" );
        }

        /* Store the fields. */
        if ( data != null ) {
            fields_ = new FieldHandler[ data.getColumnCount() ];
            for ( int icol = 0; icol < fields_.length; icol++ ) {
                fields_[ icol ] = data.getField( icol );
            }
        }
        else {
            error( new VotLintCode( "DDS" ), this + " outside DATA" );
        }
    }

    /**
     * Returns the fields used for this stream.
     *
     * @return  FieldHandler objects for the columns held in this stream
     */
    public FieldHandler[] getFields() {
        return fields_;
    }

    /**
     * Invoked to log that a row has been counted (used for keeping track
     * of how many have been seen).
     */
    protected void foundRow() {
        if ( table_ != null ) {
            table_.foundRow();
        }
    }

    /**
     * Consumes a decoded input stream containing the data of a table,
     * encoded according to the rules specified by this element.
     *
     * @param  in  input stream to read
     */
    public abstract void feed( InputStream in ) throws IOException;

}
