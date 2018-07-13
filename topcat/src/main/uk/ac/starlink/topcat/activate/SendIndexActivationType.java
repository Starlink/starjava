package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.util.Map;
import javax.swing.ListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.interop.TopcatSampControl;

/**
 * ActivationType implementation for sending row index via SAMP.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2018
 */
public class SendIndexActivationType implements ActivationType {

    private final boolean isSelectTarget_;

    /**
     * Constructor.
     *
     * @param   isSelectTarget   if true, the user can configure the target
     *                           client, if false the message is broadcast
     *                           to all clients
     */
    public SendIndexActivationType( boolean isSelectTarget ) {
        isSelectTarget_ = isSelectTarget;
    }

    public String getName() {
        return isSelectTarget_ ? "Send row index"
                               : "Broadcast row index";
    }

    public String getDescription() {
        return "Send Row Highlight message to "
             + ( isSelectTarget_ ? "selected client"
                                 : "all listening clients" )
             + " using SAMP";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return isSelectTarget_ ? Suitability.PRESENT
                               : Suitability.SUGGESTED;
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        final SampSender rowSender = new SampSender( "table.highlight.row" );
        final TopcatModel tcModel = tinfo.getTopcatModel();
        final SendIndexActivator activator =
            new SendIndexActivator( tcModel, rowSender );
        final LabelledComponentStack stack = new LabelledComponentStack();
        JPanel panel = new JPanel( new BorderLayout() );
        panel.add( stack, BorderLayout.NORTH );
        return new AbstractActivatorConfigurator( panel ) {
            /** Constructor. */ {
                ActionForwarder forwarder = getActionForwarder();
                if ( isSelectTarget_ ) {
                    JComboBox clientSelector =
                        new JComboBox( rowSender.getClientSelectionModel() );
                    stack.addLine( "Target Client", clientSelector );
                    clientSelector.addActionListener( forwarder );
                    rowSender.getConnector().addConnectionListener( forwarder );
                }
                rowSender.getClientListModel().addListDataListener( forwarder );
                rowSender.getSampControl().getIdentifiableTableListModel()
                         .addListDataListener( forwarder );
            }
            public Activator getActivator() {
                return activator.hasClients() && activator.hasPublicIdentifier()
                     ? activator
                     : null;
            }
            public String getConfigMessage() {
                String unText = rowSender.getUnavailableText();
                if ( unText != null ) {
                    return unText;
                }
                else if ( ! activator.hasPublicIdentifier() ) {
                    return "Table not known to SAMP clients";
                }
                else {
                    return null;
                }
            }
            public Safety getSafety() {
                return Safety.SAFE;
            }
            public ConfigState getState() {
                return new ConfigState();
            }
            public void setState( ConfigState state ) {
            }
        };
    }

    /**
     * Activator implementation for this type.
     */
    private static class SendIndexActivator implements Activator {
        private final TopcatModel tcModel_;
        private final SampSender rowSender_;
        private final TopcatSampControl sampControl_;

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model
         * @param  rowPointer  knows how to send row highlight messages
         */
        SendIndexActivator( TopcatModel tcModel, SampSender rowSender ) {
            tcModel_ = tcModel;
            rowSender_ = rowSender;
            sampControl_ = rowSender.getSampControl();
        }

        public boolean invokeOnEdt() {
            return false;
        }

        public Outcome activateRow( long lrow, ActivationMeta meta ) {
            if ( meta == null || ! meta.isInhibitSend() ) {
                Map message = sampControl_.createRowMessage( tcModel_, lrow );
                if ( message != null ) {
                    return rowSender_.activateMessage( message );
                }
                else {
                    return Outcome.failure( "Can't identify table" );
                }
            }
            else {
                return Outcome.success( "(no send to avoid ping pong)" );
            }
        }

        /**
         * Indicates whether any clients are present to receive index
         * highlight messages.
         *
         * @return  true iff highlight-capable clients are subscribed
         */
        public boolean hasClients() {
            return rowSender_.getClientListModel().getSize() > 0;
        }

        /**
         * Indicates whether it's possible to construct a row highlight
         * message for this activator's table.
         *
         * @return  true iff the table has an ID or URL
         */
        public boolean hasPublicIdentifier() {
            ListModel tableIdListModel =
                sampControl_.getIdentifiableTableListModel();
            int n = tableIdListModel.getSize();
            for ( int i = 0; i < n; i++ ) {
                if ( tableIdListModel.getElementAt( i ) == tcModel_ ) {
                    return true;
                }
            }
            return false;
        }
    }
}
