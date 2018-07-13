package uk.ac.starlink.topcat.activate;

import java.net.URL;
import javax.swing.JComboBox;
import javax.swing.ListModel;
import org.astrogrid.samp.Message;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * ActivationType for sending a VOTable to an external application.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2018
 */
public class SendTableActivationType implements ActivationType {

    public String getName() {
        return "Send VOTable";
    }

    public String getDescription() {
        return "Send the content of a file or URL column as a VOTable"
             + " to an external application using SAMP";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new SendTableColumnConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return Suitability.AVAILABLE;
    }

    /**
     * Configurator implementation for URLs pointing to VOTables.
     */
    private static class SendTableColumnConfigurator
            extends UrlColumnConfigurator {
        final TopcatModel tcModel_;
        final SampSender votableSender_;
        final ListModel clientListModel_;

        private static final String VOTABLE_MTYPE = "table.load.votable";

        /**
         * Constructor.
         *
         * @param  tinfo  table information
         */
        SendTableColumnConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "VOTable", new ColFlag[] {
                       ColFlag.VOTABLE, ColFlag.DATALINK, ColFlag.URL,
            } );
            tcModel_ = tinfo.getTopcatModel();
            votableSender_ = new SampSender( VOTABLE_MTYPE );
            ActionForwarder forwarder = getActionForwarder();
            clientListModel_ = votableSender_.getClientListModel();
            clientListModel_.addListDataListener( forwarder );
            votableSender_.getConnector().addConnectionListener( forwarder );
            JComboBox viewerSelector =
                new JComboBox( votableSender_.getClientSelectionModel() );
            viewerSelector.addActionListener( forwarder );
            getQueryPanel().add( new LineBox( "Table Viewer",
                                              viewerSelector ) );
        }

        protected Activator createActivator( final ColumnData cdata ) {
            if ( clientListModel_.getSize() > 0 ) {
                return new UrlColumnActivator( cdata, false ) {
                    protected Outcome activateUrl( URL url, long lrow ) {
                        Message message = new Message( VOTABLE_MTYPE );
                        message.addParam( "url", url.toString() );
                        return votableSender_.activateMessage( message );
                    }
                };
            }
            else {
                return null;
            }
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return votableSender_.getUnavailableText();
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public ConfigState getState() {
            return getUrlState();
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
        }
    }
}
