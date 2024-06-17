package uk.ac.starlink.topcat.interop;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.topcat.func.BasicImageDisplay;

/**
 * ImageActivity implementation using PLASTIC for the external communications.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2008
 */
public class PlasticImageActivity implements ImageActivity {

    private final TopcatPlasticListener plasticServer_;
    private final JComboBox<String> formatSelector_;
    private final ViewerComboBoxModel viewerModel_;
    private final ComboBoxModel<Object> fitsAppModel_;

    /**
     * Constructor.
     *
     * @param   plasticServer   plastic connection manager
     */
    public PlasticImageActivity( TopcatPlasticListener plasticServer ) {
        plasticServer_ = plasticServer;
        formatSelector_ = new JComboBox<String>( KNOWN_FORMATS );
        formatSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                setFormat( String.valueOf( formatSelector_
                                          .getSelectedItem() ) );
            }
        } );
        fitsAppModel_ = plasticServer
                       .createPlasticComboBoxModel( MessageId.FITS_LOADIMAGE );
        viewerModel_ = new ViewerComboBoxModel();
        String format = FORMAT_FITS;
        formatSelector_.setSelectedItem( format );
        setFormat( format );
    }

    public ComboBoxModel<?> getTargetSelector() {
        return viewerModel_;
    }

    public JComboBox<String> getFormatSelector() {
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
        viewerModel_.setFits( FORMAT_FITS.equals( format ) );
    }

    /**
     * ComboBoxModel which displays available viewers.
     * Elements are all instances of {@link ImageViewer}.
     */
    private class ViewerComboBoxModel extends AbstractListModel<ImageViewer>
                                      implements ComboBoxModel<ImageViewer> {
        private ImageViewer[] baseViewers_;
        private boolean isFits_;
        private Object selectedItem_;

        private final ImageViewer basicViewer_ = new ImageViewer() {
            public boolean viewImage( String label, String location ) {
                BasicImageDisplay.displayBasicImage( label, location );
                return true;
            }
            public String toString() {
                return "Basic Viewer (internal)";
            }
        };

        /**
         * Constructor.
         */
        ViewerComboBoxModel() {
            setFits( false );
        }

        /**
         * Configures model according to whether the format type is FITS or not.
         *
         * @param  isFits  is it FITS?
         */
        void setFits( boolean isFits ) {
            isFits_ = isFits;
            List<ImageViewer> vList = new ArrayList<ImageViewer>();
            vList.add( basicViewer_ );
            baseViewers_ = vList.toArray( new ImageViewer[ 0 ] );
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
            return baseViewers_.length
                 + ( isFits_ ? fitsAppModel_.getSize() : 0 );
        }

        public ImageViewer getElementAt( int index ) {
            if ( index < baseViewers_.length ) {
                return baseViewers_[ index ];
            }
            else if ( isFits_ ) {
                Object item =
                    fitsAppModel_.getElementAt( index - baseViewers_.length );
                if ( item instanceof ApplicationItem ) {
                    ApplicationItem app = (ApplicationItem) item;
                    return new PlasticViewer( app.getId(), app + " (PLASTIC)" );
                }
                else {
                    return new PlasticViewer( null, "All Listeners (PLASTIC)" );
                }
            }
            else {
                return null;
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
     * ImageViewer implementation based on a Plastic recipient.
     */
    private class PlasticViewer implements ImageViewer {
        private final URI recipient_;
        private final String name_;
        private final String id_;

        /**
         * Constructor.
         *
         * @param   recipient  recipient application URI, or null for broadcast
         * @param   name   user-visible label
         */
        PlasticViewer( URI recipient, String name ) {
            recipient_ = recipient;
            name_ = name;
            id_ = recipient_ == null ? ""
                                     : recipient_.toString();
        }

        public boolean viewImage( String label, String location ) {
            try {
                List<String> argList =
                    Arrays.asList( new String[] { location, label, } );
                URI msgId = MessageId.FITS_LOADIMAGE;
                plasticServer_.register();
                PlasticHubListener hub = plasticServer_.getHub();
                URI plasticId = plasticServer_.getRegisteredId();
                Map<?,?> responses = ( recipient_ == null )
                       ? hub.request( plasticId, msgId, argList )
                       : hub.requestToSubset( plasticId, msgId, argList,
                                              Collections
                                             .singletonList( recipient_ ) );
                for ( Object response : responses.values() ) {
                    if ( response instanceof Boolean &&
                         ((Boolean) response).booleanValue() ) {
                        return true;
                    }
                }
                return false;
            }
            catch ( IOException e ) {
                return false;
            }
        }

        public boolean equals( Object other ) {
            return other instanceof PlasticViewer
                && ((PlasticViewer) other).id_.equals( this.id_ );
        }

        public int hashCode() {
            return id_.hashCode();
        }

        public String toString() {
            return name_;
        }
    }
}
