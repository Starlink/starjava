package uk.ac.starlink.topcat.activate;

import java.io.IOException;
import javax.swing.JComboBox;
import javax.swing.ListModel;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;

/**
 * ActivationType implementation that sends sky coordinates to other
 * applications using SAMP.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2018
 */
public class SendSkyPosActivationType implements ActivationType {

    public String getName() {
        return "Send Sky Coordinates";
    }

    public String getDescription() {
        return "Send sky coordinates using SAMP";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.getSkySuitability();
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new SendConfigurator( tinfo );
    }

    /**
     * Configurator implementation for use with this activation type.
     */
    private static class SendConfigurator extends SkyPosConfigurator {

        private final SampSender skySender_;
        private final ListModel clientListModel_;

        private static final String SKY_MTYPE = "coord.pointAt.sky";
     
        /**
         * Constructor.
         *
         * @param  tinfo  table information
         */
        SendConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo );
            skySender_ = new SampSender( SKY_MTYPE );
            clientListModel_ = skySender_.getClientListModel();
            JComboBox appSelector =
                new JComboBox( skySender_.getClientSelectionModel() );
            getStack().addLine( "Target Application", appSelector );
            ActionForwarder forwarder = getActionForwarder();
            clientListModel_.addListDataListener( forwarder );
            skySender_.getConnector().addConnectionListener( forwarder );
            appSelector.addActionListener( forwarder );
        }

        public Activator createActivator( ColumnData raData,
                                          ColumnData decData ) {
            if ( clientListModel_.getSize() > 0 ) {
                return new SkyPosActivator( raData, decData, false ) {
                    protected Outcome useSkyPos( double raDeg, double decDeg ) {
                        Message msg = new Message( SKY_MTYPE );
                        msg.addParam( "ra", SampUtils.encodeFloat( raDeg ) );
                        msg.addParam( "dec", SampUtils.encodeFloat( decDeg ) );
                        return skySender_.activateMessage( msg );
                    }
                };
            }
            else {
                return null;
            }
        }

        public String getSkyConfigMessage() {
            return skySender_.getUnavailableText();
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public ConfigState getState() {
            return getSkyPosState();
        }

        public void setState( ConfigState state ) {
            setSkyPosState( state );
        }
    }
}
