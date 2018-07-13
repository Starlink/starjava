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
 * ActivationType for displaying a FITS image in an external viewer.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2018
 */
public class SendImageActivationType implements ActivationType {

    public String getName() {
        return "Send FITS Image";
    }

    public String getDescription() {
        return "Send the content of a file or URL column as a FITS image"
             + " to an external application using SAMP";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new ImageColumnConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasFlag( ColFlag.IMAGE )
             ? Suitability.SUGGESTED
             : tinfo.getUrlSuitability();
    }

    /**
     * Configurator implementation for URLs pointing to FITS images.
     */
    private static class ImageColumnConfigurator extends UrlColumnConfigurator {
        final TopcatModel tcModel_;
        final SampSender imageSender_;
        final ListModel clientListModel_;

        private static final String IMAGE_MTYPE = "image.load.fits";

        /**
         * Constructor.
         *
         * @param  tinfo   table information
         */
        ImageColumnConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Image",
                   new ColFlag[] { ColFlag.IMAGE, ColFlag.URL, } );
            tcModel_ = tinfo.getTopcatModel();
            imageSender_ = new SampSender( IMAGE_MTYPE );
            ActionForwarder forwarder = getActionForwarder();
            clientListModel_ = imageSender_.getClientListModel();
            clientListModel_.addListDataListener( forwarder );
            imageSender_.getConnector().addConnectionListener( forwarder );
            JComboBox viewerSelector =
                 new JComboBox( imageSender_.getClientSelectionModel() );
            viewerSelector.addActionListener( forwarder );
            getQueryPanel().add( new LineBox( "Image Viewer",
                                              viewerSelector ) );
        }

        protected Activator createActivator( final ColumnData cdata ) {
            if ( clientListModel_.getSize() > 0 ) {
                return new UrlColumnActivator( cdata, false ) {
                    protected Outcome activateUrl( URL url, long lrow ) {
                        Message message = new Message( IMAGE_MTYPE );
                        message.addParam( "url", url.toString() );
                        return imageSender_.activateMessage( message );
                    }
                };
            }
            else {
                return null;
            }
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return imageSender_.getUnavailableText();
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
