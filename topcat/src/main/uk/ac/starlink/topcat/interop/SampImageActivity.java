package uk.ac.starlink.topcat.interop;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListModel;
import org.astrogrid.samp.Client;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.SubscribedClientListModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.topcat.func.BasicImageDisplay;
import uk.ac.starlink.topcat.func.Sog;
import uk.ac.starlink.util.URLUtils;

/**
 * ImageActivity implementation using SAMP for the external communications.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2008
 */
public class SampImageActivity implements ImageActivity {

    private final GuiHubConnector connector_;
    private final SubscribedClientListModel clientModel_;
    private final JComboBox formatSelector_;
    private final ViewerComboBoxModel viewerModel_;
    private static final Map mfactMap_ = createMessageFactoryMap();

    /**
     * Constructor.
     *
     * @param  connector  SAMP connector
     */
    public SampImageActivity( GuiHubConnector connector ) {
        connector_ = connector;
        formatSelector_ = new JComboBox( KNOWN_FORMATS );
        formatSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                setFormat( String.valueOf( formatSelector_
                                          .getSelectedItem() ) );
            }
        } );
        clientModel_ =
            new SubscribedClientListModel( connector, new String[ 0 ] );
        viewerModel_ = new ViewerComboBoxModel();
        String format = FORMAT_FITS;
        formatSelector_.setSelectedItem( format );
        setFormat( format );
    }

    public ComboBoxModel getTargetSelector() {
        return viewerModel_;
    }

    public JComboBox getFormatSelector() {
        return formatSelector_;
    }

    public void displayImage( String location, String label ) {
        Object item = viewerModel_.getSelectedItem();
        if ( item instanceof ImageViewer ) {
            ((ImageViewer) item).viewImage( label, location );
        }
        else {
            assert false;
        }
    }

    /**
     * Reconfigures components to use a given image format.
     * 
     * @param  format  one of the FORMAT_* members of ImageActivity
     */
    private void setFormat( String format ) {
        MessageFactory mfact = (MessageFactory) mfactMap_.get( format );
        clientModel_.setMTypes( mfact == null
                              ? new String[ 0 ]
                              : new String[] { mfact.mtype_ } );
        viewerModel_.setFormat( format );
    }

    /**
     * Sets up a map from image format name to MessageFactory.
     *
     * @return   new  String->MessageFactory map
     */
    private static Map createMessageFactoryMap() {
        Map map = new HashMap();
        map.put( FORMAT_FITS, new MessageFactory( "image.load.fits" ) {
            public Map createMessage( String location, String label ) {
                URL url = URLUtils.makeURL( location );
                return url == null ? null
                                   : new Message( "image.load.fits" )
                                    .addParam( "url", url.toString() );
            }
        } );
        map.put( FORMAT_JPEG, new MessageFactory( "image.load.jpeg" ) {
            public Map createMessage( String location, String label ) {
                URL url = URLUtils.makeURL( location );
                return url == null ? null
                                   : new Message( "image.load.jpeg" )
                                    .addParam( "url", url.toString() );
            }
        } );
        return map;
    }

    /**
     * ComboBoxModel which displays available viewers.
     * Elements are all instances of {@link ImageViewer}.
     */
    private class ViewerComboBoxModel extends AbstractListModel
                                      implements ComboBoxModel {
        private ImageViewer[] baseViewers_;
        private Object selectedItem_;

        /**
         * Constructor.
         */
        ViewerComboBoxModel() {
            baseViewers_ = new ImageViewer[ 0 ];
        }

        /**
         * Configures for use with a given image format.
         *
         * @param  format  image format name
         */
        public void setFormat( String format ) {
            List viewerList = new ArrayList();
            if ( FORMAT_FITS.equals( format ) ||
                 FORMAT_JPEG.equals( format ) ||
                 FORMAT_GIF.equals( format ) ||
                 FORMAT_PNG.equals( format ) ) {
                viewerList.add( new DefaultImageViewer( "Basic viewer"
                                                      + " (internal)" ) {
                    public boolean viewImage( String label, String location ) {
                        BasicImageDisplay.displayBasicImage( label, location );
                        return true;
                    }
                } );
                if ( TopcatUtils.canSog() ) {
                    viewerList.add( new DefaultImageViewer( "SoG (internal)" ) {
                        public boolean viewImage( String label, String loc ) {
                            Sog.sog( label, loc );
                            return true;
                        }
                    } );
                }
            }
            final MessageFactory mfact =
                (MessageFactory) mfactMap_.get( format );
            if ( mfact != null ) {
                viewerList.add( new DefaultImageViewer( "All Clients (SAMP)" ) {
                    public boolean viewImage( String label, String location ) {
                        try {
                            HubConnection connection =
                                connector_.getConnection();
                            if ( connection != null ) {
                                Map msg =
                                    mfact.createMessage( location, label );
                                if ( msg != null ) {
                                    connection.notifyAll( msg );
                                    return true;
                                }
                                else {
                                    return false;
                                }
                            }
                            else {
                                return false;
                            }
                        }
                        catch ( IOException e ) {
                            return false;
                        }
                    }
                } );
            }

            baseViewers_ =
                (ImageViewer[]) viewerList.toArray( new ImageViewer[ 0 ] );
            int nv = getSize();
            boolean selectionLegal = false;
            for ( int iv = 0; iv < nv; iv++ ) {
                selectionLegal = selectionLegal
                              || getElementAt( iv ).equals( selectedItem_ );
            }
            if ( ! selectionLegal ) {
                selectedItem_ = nv > 0 ? getElementAt( 0 ) : null;
            }
            fireContentsChanged( this, -1, -1 );
        }

        public int getSize() {
            return baseViewers_.length + clientModel_.getSize();
        }

        public Object getElementAt( int index ) {
            int nb = baseViewers_.length;
            if ( index < nb ) {
                return baseViewers_[ index ];
            }
            else {
                Client client =
                    (Client) clientModel_.getElementAt( index - nb );
                MessageFactory mfact = 
                    (MessageFactory) mfactMap_.get( formatSelector_
                                                   .getSelectedItem() );
                return new ClientImageViewer( client, mfact );
            }
        }

        public void setSelectedItem( Object item ) {
            selectedItem_ = item;
        }

        public Object getSelectedItem() {
            return selectedItem_;
        }
    }

    /**
     * Convenience ImageViewer implementation which handles equality based
     * on a supplied name.
     */
    private static abstract class DefaultImageViewer implements ImageViewer {
        private final String label_;

        /**
         * Constructor.
         *
         * @param   label   object name, also used for equality
         */
        DefaultImageViewer( String label ) {
            label_ = label;
        }

        public boolean equals( Object other ) {
            return other instanceof DefaultImageViewer
                && ((DefaultImageViewer) other).label_.equals( this.label_ );
        }

        public int hashCode() {
            return label_.hashCode();
        }

        public String toString() {
            return label_;
        }
    }

    /**
     * ImageViewer implementation which sends a message to a SAMP client.
     */
    private class ClientImageViewer implements ImageViewer {
        private final Client client_;
        private final MessageFactory mfact_;

        /**
         * Constructor.
         *
         * @param  client  intended recipient
         * @param  mfact   defines message type
         */
        ClientImageViewer( Client client, MessageFactory mfact ) {
            client_ = client;
            mfact_ = mfact;
        }

        public boolean viewImage( String label, String location ) {
            try {
                HubConnection connection = connector_.getConnection();
                if ( connection != null ) {
                    Map msg = mfact_.createMessage( location, label );
                    if ( msg != null ) {
                        connection.notify( client_.getId(), msg );
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
            }
            catch ( IOException e ) {
                return false;
            }
        }

        public int hashCode() {
            return client_.getId().hashCode();
        }

        public boolean equals( Object other ) {
            return other instanceof ClientImageViewer
                && ((ClientImageViewer) other).client_.getId()
                                              .equals( client_.getId() );
        }

        public String toString() {
            return client_.toString();
        }
    }

    /**
     * Encapsulates information about a type of SAMP message for passing
     * images to other applications.
     */
    private static abstract class MessageFactory {
        final String mtype_;

        /**
         * Constructor.
         *
         * @param  mtype  MType
         */
        MessageFactory( String mtype ) {
            mtype_ = mtype;
        }

        /**
         * Constructs a message which can be sent via SAMP.
         *
         * @param  location   filename or URL locating image file
         * @param  target application label
         */
        abstract Map createMessage( String location, String label );
    }
}
