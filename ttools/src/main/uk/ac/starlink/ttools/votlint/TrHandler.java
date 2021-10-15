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

        /* Check we've had the right number of cells in this row. */
        DataHandler data = family.getAncestor( DataHandler.class );
        if ( data != null ) {
            int ncol = data.getColumnCount();
            int nchild = family.getChildCount();
            if ( ncol != nchild ) {
                warning( new VotLintCode( "TR9" ),
                         "Wrong number of TDs in row" +
                         " (expecting " + ncol + " found " + nchild + ")" );
            }
        }
        else {
            error( new VotLintCode( "TRS" ), getName() + " outside DATA" );
        }

        /* Inform the table that another row has gone by. */
        TableHandler table = family.getAncestor( TableHandler.class );
        if ( table != null ) {
            table.foundRow();
        }
        else {
            error( new VotLintCode( "TRS" ), getName() + " outside TABLE" );
        }
    }
}
