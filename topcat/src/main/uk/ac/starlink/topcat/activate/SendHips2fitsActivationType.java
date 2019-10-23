package uk.ac.starlink.topcat.activate;

import java.net.URL;
import java.util.function.Predicate;
import javax.swing.ListModel;
import javax.swing.JComboBox;
import org.astrogrid.samp.Message;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.ttools.func.URLs;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Activation type that sends a SAMP message referencing a FITS image
 * derived from the CDS Hips2fits service.
 *
 * @author   Mark Taylor
 * @since    23 Oct 2019
 */
public class SendHips2fitsActivationType implements ActivationType {

    private static final String IMAGE_MTYPE = "image.load.fits";

    public SendHips2fitsActivationType() {
    }

    public String getName() {
        return "Send HiPS cutout";
    }

    public String getDescription() {
        return "Sends via SAMP a FITS file cutout from a chosen HiPS survey."
             + " This uses the Hips2Fits service provided by CDS.";
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.getSkySuitability();
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new SendConfigurator( tinfo );
    }

    /**
     * Configurator for use with this class.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    private static class SendConfigurator extends Hips2fitsConfigurator {

        final SampSender imageSender_;
        final ListModel clientListModel_;

        /**
         * Constructor.
         *
         * @param   tinfo  topcat model information
         */
        SendConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, new Predicate<HipsSurvey>() {
                public boolean test( HipsSurvey hips ) {
                    return hips.hasFits();
                }
            } );
            imageSender_ = new SampSender( IMAGE_MTYPE );
            ActionForwarder forwarder = getActionForwarder();
            clientListModel_ = imageSender_.getClientListModel();
            clientListModel_.addListDataListener( forwarder );
            imageSender_.getConnector().addConnectionListener( forwarder );
            JComboBox viewerSelector =
                 new JComboBox( imageSender_.getClientSelectionModel() );
            viewerSelector.addActionListener( forwarder );
            getStack().addLine( "Image Viewer",
                                new ShrinkWrapper( viewerSelector ) );
        }

        @Override
        public Activator createActivator( ColumnData raData,
                                          ColumnData decData ) {
            return clientListModel_.getSize() > 0
                 ? super.createActivator( raData, decData )
                 : null;
        }

        @Override
        public String getSkyConfigMessage() {
            return clientListModel_.getSize() > 0
                 ? super.getSkyConfigMessage()
                 : imageSender_.getUnavailableText();
        }

        protected Outcome useHips( String hipsId, double raDeg, double decDeg,
                                   double fovDeg, int npix ) {
            if ( clientListModel_.getSize() > 0 ) {
                String url = URLs.hips2fitsUrl( hipsId, "fits", raDeg, decDeg,
                                                fovDeg, npix );
                Message message = new Message( IMAGE_MTYPE );
                message.addParam( "url", url.toString() );
                return imageSender_.activateMessage( message );
            }
            else {
                return Outcome.failure( "No SAMP clients" );
            }
        }
    }
}
