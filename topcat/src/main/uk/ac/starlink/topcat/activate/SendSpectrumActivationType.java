package uk.ac.starlink.topcat.activate;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.ListModel;
import org.astrogrid.samp.Message;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.interop.SampCommunicator;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * ActivationType for displaying a spectrum in an external viewer.
 *
 * @author   Mark Taylor
 * @since    30 Jan 2018
 */
public class SendSpectrumActivationType implements ActivationType {

    public String getName() {
        return "Send Spectrum";
    }

    public String getDescription() {
        return "Send the content of a file or URL column as a Spectrum"
             + " to an external application using SAMP";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new SpectrumColumnConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return tinfo.tableHasFlag( ColFlag.SPECTRUM )
             ? Suitability.SUGGESTED
             : tinfo.getUrlSuitability();
    }

    /**
     * Configurator implementation for URLs pointing to (SSA) spectra.
     */
    private static class SpectrumColumnConfigurator extends
            UrlColumnConfigurator {
        final TopcatModel tcModel_;
        final SampSender specSender_;
        final ListModel<?> clientListModel_;

        private static final String SPECTRUM_MTYPE =
            "spectrum.load.ssa-generic";

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model
         * @param  communicator   handles external communication
         */
        SpectrumColumnConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Spectrum",
                   new ColFlag[] { ColFlag.SPECTRUM, ColFlag.URL, } );
            tcModel_ = tinfo.getTopcatModel();
            ActionForwarder forwarder = getActionForwarder();
            specSender_ = new SampSender( SPECTRUM_MTYPE );
            clientListModel_ = specSender_.getClientListModel();
            clientListModel_.addListDataListener( forwarder );
            specSender_.getConnector().addConnectionListener( forwarder );

            /* Set up a selector for the spectrum viewer. */
            JComboBox<?> viewerSelector =
                new JComboBox<Object>( specSender_.getClientSelectionModel() );
            viewerSelector.addActionListener( forwarder );
            getQueryPanel()
           .add( new LineBox( "Spectrum Viewer",
                              new ShrinkWrapper( viewerSelector ) ) );
        }

        protected Activator createActivator( final ColumnData cdata ) {
            if ( clientListModel_.getSize() > 0 ) {
                return new Activator() {
                    public boolean invokeOnEdt() {
                        return false;
                    }
                    public Outcome activateRow( long lrow,
                                                ActivationMeta actMeta ) {
                        return activateSpectrumRow( cdata, lrow );
                    }
                };
            }
            else {
                return null;
            }
        }

        protected String getConfigMessage( ColumnData cdata ) {
            return specSender_.getUnavailableText();
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

        /**
         * Performs the activation action by sending the spectrum.
         *
         * @param   cdata  column data holding spectrum location
         * @param   lrow   row index
         * @return   activation result
         */
        private Outcome activateSpectrumRow( ColumnData cdata, long lrow ) {

            /* Read spectrum location. */
            Object locval;
            try {
                locval = cdata.readValue( lrow );
            }
            catch ( IOException e ) {
                return Outcome.failure( e );
            }
            if ( ! ( locval instanceof String ) ) {
                return Outcome
                      .failure( locval == null ? "No location"
                                               : "Bad location: " + locval );
            }
            String loc = (String) locval;

            /* Turn the location into a URL. */
            final URL url;
            File file = new File( loc );
            if ( file.exists() ) {
                url = URLUtils.makeFileURL( file );
            }
            else {
                try {
                    url = URLUtils.newURL( loc );
                }
                catch ( MalformedURLException e ) {
                    return Outcome.failure( "Bad URL/no such file: " + loc );
                }
            }

            /* Read the rest of the row and use it to assemble
             * the spectrum metadata. */
            StarTable table = tcModel_.getDataModel();
            Object[] row;
            try {
                row = table.getRow( lrow );
            }
            catch ( IOException e ) {
                return Outcome.failure( e );
            }
            Map<String,Object> specMeta =
                new LinkedHashMap<String,Object>();
            int ncol = table.getColumnCount();
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object value = row[ icol ];
                if ( value != null ) {
                    ColumnInfo info = table.getColumnInfo( icol );
                    String ucd = info.getUCD();
                    String utype = info.getUtype();
                    if ( ucd != null ) {
                        specMeta.put( ucd, value );
                    }
                    if ( utype != null ) {
                        specMeta.put( utype, value );
                    }
                }
            }

            /* Send the message. */
            Message message = new Message( SPECTRUM_MTYPE );
            message.addParam( "url", url.toString() );
            message.addParam( "meta", SampCommunicator
                                     .sanitizeMap( specMeta ) );
            return specSender_.activateMessage( message );
        }
    }
}
