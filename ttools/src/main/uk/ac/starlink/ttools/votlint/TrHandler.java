package uk.ac.starlink.ttools.votlint;

/**
 * Element handler for TR element.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Apr 2005
 */
public class TrHandler extends ElementHandler {

    public void endElement() {
        Ancestry family = getAncestry();

        /* Checke we've had the right number of cells in this row. */
        DataHandler data =
            (DataHandler) family.getAncestor( DataHandler.class );
        if ( data != null ) {
            int ncol = data.getColumnCount();
            int nchild = family.getChildCount();
            if ( ncol != nchild ) {
                warning( "Wrong number of TDs in row" +
                         " (expecting " + ncol + " found " + nchild + ")" );
            }
        }
        else {
            error( getName() + " outside DATA" );
        }

        /* Inform the table that another row has gone by. */
        TableHandler table =
            (TableHandler) family.getAncestor( TableHandler.class );
        if ( table != null ) {
            table.foundRow();
        }
        else {
            error( getName() + " outside TABLE" );
        }
    }
}
