package uk.ac.starlink.topcat.activate;

import java.awt.BorderLayout;
import java.io.IOException;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import org.astrogrid.samp.Message;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnDataComboBox;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * ActivationType for sending an ASCII MOC to an external application.
 *
 * @author   Mark Taylor
 * @since    7 Oct 2026
 */
public class SendMocActivationType implements ActivationType {

    public static final String MOC_MTYPE = "coverage.load.moc.ascii";
    private static final String MOC_KEY = "moc";

    /**
     * Constructor.
     */ 
    public SendMocActivationType() {
    }

    public String getName() {
        return "Send ASCII MOC";
    }

    public String getDescription() {
        return "Send a MOC string describing sky coverage "
             + "to an external application using SAMP";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasFlag( ColFlag.MOC )
             ? Suitability.SUGGESTED
             : Suitability.PRESENT;
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new SendMocConfigurator( tinfo );
    }

    /**
     * Configurator implementation for values containing ASCII MOCs.
     */
    private static class SendMocConfigurator
            extends AbstractActivatorConfigurator {

        private final ColumnDataComboBox colSelector_;
        private final SampSender mocSender_;

        /**
         * Constructor.
         *
         * @param  tinfo  table information
         */
        SendMocConfigurator( TopcatModelInfo tinfo ) {
            super( new JPanel( new BorderLayout() ) );
            ActionForwarder forwarder = getActionForwarder();

            /* Construct target client selection component. */
            mocSender_ = new SampSender( MOC_MTYPE );
            mocSender_.getConnector().addConnectionListener( forwarder );
            mocSender_.getClientListModel().addListDataListener( forwarder );
            JComboBox<?> clientSelector =
                new JComboBox<Object>( mocSender_.getClientSelectionModel() );
            clientSelector.addActionListener( forwarder );

            /* Construct MOC column selection component. */
            TopcatModel tcModel = tinfo.getTopcatModel();
            ColumnDataComboBoxModel colModel =
                new ColumnDataComboBoxModel( tcModel, String.class, true );
            colSelector_ = new ColumnDataComboBox();
            colSelector_.setModel( colModel );
            colSelector_.addActionListener( forwarder );
            UrlColumnConfigurator
               .configureDefaultSelection( colModel, tinfo,
                                           new ColFlag[] { ColFlag.MOC } );

            /* Place components. */
            LabelledComponentStack stack = new LabelledComponentStack();
            stack.addLine( "ASCII MOC", colSelector_ );
            stack.addLine( "Target Application", clientSelector );
            getPanel().add( stack, BorderLayout.NORTH );
        }

        public Activator getActivator() {
            Object item = colSelector_.getSelectedItem();
            if ( item instanceof ColumnData &&
                 mocSender_.getClientListModel().getSize() > 0 ) {
                ColumnData cdata = (ColumnData) item;
                return new Activator() {
                    public Outcome activateRow( long lrow,
                                                ActivationMeta meta ) {
                        Object value;
                        try {
                            value = cdata.readValue( lrow );
                        }
                        catch ( IOException e ) {
                            return Outcome.failure( e );
                        }
                        if ( value instanceof String ) {
                            String asciiMoc = (String) value;
                            if ( asciiMoc.trim().length() > 0 ) {
                                Message message = new Message( MOC_MTYPE );
                                message.addParam( "moc", asciiMoc );
                                return mocSender_.activateMessage( message );
                            }
                        }
                        return Outcome.failure( "No MOC text" );
                    }
                    public boolean invokeOnEdt() {
                        return false;
                    }
                };
            }
            else {
                return null;
            }
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public String getConfigMessage() {
            Object item = colSelector_.getSelectedItem();
            return item instanceof ColumnData
                 ? mocSender_.getUnavailableText()
                 : "No ASCII MOC specified";
        }

        public void setState( ConfigState state ) {
            state.restoreSelection( MOC_KEY, colSelector_ );
        }

        public ConfigState getState() {
            ConfigState state = new ConfigState();
            state.saveSelection( MOC_KEY, colSelector_ );
            return state;
        }
    }
}
