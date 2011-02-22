package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.ListModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.vo.TapTableLoadDialog;

/**
 * TapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 */
public class TopcatTapTableLoadDialog extends TapTableLoadDialog {
    private final RegistryDialogAdjuster adjuster_;

    public TopcatTapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "tap", false );
    }

    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        adjuster_.adjustComponent();
        return comp;
    }

    public boolean acceptResourceIdList( String[] ivoids, String msg ) {
        return adjuster_.acceptResourceIdLists()
            && super.acceptResourceIdList( ivoids, msg );
    }

    protected StarTable getUploadTable( String upLabel ) {

        /* Get the list of known TopcatModels, since these are the ones
         * that may be referred to as an upload table. */
        ListModel tcListModel =
            ControlWindow.getInstance().getTablesListModel();
        TopcatModel[] tcModels = new TopcatModel[ tcListModel.getSize() ];
        for ( int i = 0; i < tcModels.length; i++ ) {
            tcModels[ i ] = (TopcatModel) tcListModel.getElementAt( i );
        }

        /* Check for a match against the label of a known table. 
         * If found and syntactically legal, return the appropriate table.
         * If found and illegal, reject with a helpful message. */
        for ( int i = 0; i < tcModels.length; i++ ) {
            TopcatModel tcModel = tcModels[ i ];
            String tlabel = tcModel.getLabel();
            if ( upLabel.equalsIgnoreCase( tlabel ) ||
                 upLabel.equalsIgnoreCase( '"' + tlabel + '"' ) ) {
                if ( tlabel.matches( "[A-Za-z][A-Za-z0-9_]*" ) ) {
                    return tcModel.getApparentStarTable();
                }
                else {
                    String msg = "Illegal upload table name \"" + upLabel + "\""
                               + "\nMust be alphanumeric or of form T<n>;"
                               + " try T" + tcModel.getID();
                    throw new IllegalArgumentException( msg );
                }
            }
        }

        /* Check for a match of the form "T<n>", where <n> is table ID of
         * a currently loaded table. */
        for ( int i = 0; i < tcModels.length; i++ ) {
            TopcatModel tcModel = tcModels[ i ];
            if ( upLabel.equalsIgnoreCase( "T" + tcModel.getID() ) ) {
                return tcModel.getApparentStarTable();
            }
        }

        /* Otherwise, reject with a helpful error message. */
        StringBuffer sbuf = new StringBuffer()
            .append( "No upload table available under the name " )
            .append( upLabel )
            .append( ".\n" );
        if ( tcModels.length == 0 ) {
            sbuf.append( "No tables are currently loaded" );
        }
        else {
            TopcatModel tcModel = tcModels[ 0 ];
            sbuf.append( "Use either T<n> or <alphanumeric_name>" );
        }
        throw new IllegalArgumentException( sbuf.toString() );
    }
}
