package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableModel;
import uk.ac.starlink.vo.datalink.LinkColMap;
import uk.ac.starlink.vo.datalink.LinksDoc;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;

/**
 * A window which displays the TopcatModel's table as a DataLink
 * {links}-response table.  It's possible (in principle)
 * to invoke actions for each row.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2018
 */
public class DatalinkWindow extends AuxWindow {

    /**
     * Constructor.
     *
     * @param  tcModel  model containing the data for the table concerned
     * @param  parent   component used for window positioning
     */
    public DatalinkWindow( final TopcatModel tcModel, Component parent ) {
        super( tcModel, "DataLink View", parent );
        DatalinkPanel dlPanel = new DatalinkPanel( false, false ) {
            @Override
            protected void configureJTable( JTable jtable, LinksDoc linksDoc ) {
                jtable.setModel( tcModel.getViewModel() );
                jtable.setColumnModel( tcModel.getColumnModel() );
            }
        };
        dlPanel.setLinksDoc( new ViewLinksDoc( tcModel ) );
        getMainArea().add( dlPanel );
        addHelp( "DatalinkWindow" );
    }

    /**
     * Indicates whether there is any point posting this window for a
     * given table.
     *
     * @param  tcModel  topcat model
     * @return  true iff table appears to have any links
     */
    public static boolean isUseful( TopcatModel tcModel ) {
        return LinksDoc.isLinksResponse( tcModel.getDataModel(), 2 );
    }

    /**
     * LinksDoc implementation for a TopcatModel.
     * All of its characteristics are generated dynamically from the
     * TopcatModel's view model, whose behaviour may change
     * over the lifetime of this object.
     */
    private static class ViewLinksDoc extends LinksDoc {
        private final ViewerTableModel viewModel_;

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model
         */
        ViewLinksDoc( TopcatModel tcModel ) {
            viewModel_ = tcModel.getViewModel();
        }
        public StarTable getResultTable() {
            return viewModel_.getSnapshot();
        }
        public ServiceDescriptor[] getServiceDescriptors() {
            return LinksDoc.getServiceDescriptors( getResultTable() );
        }
        public LinkColMap getColumnMap() {
            return LinkColMap.getMap( getResultTable() );
        }
    }
}
