package uk.ac.starlink.topcat;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.topcat.func.BasicImageDisplay;
import uk.ac.starlink.topcat.func.Sog;

/**
 * ComboBoxModel containing available {@link ImageViewer} objects.
 * This will contain some in-JVM options and some PLASTIC-based ones.
 *
 * @author   Mark Taylor
 * @since    21 Dec 2007
 */
public class ImageViewerComboBoxModel implements ComboBoxModel {

    private final TopcatPlasticListener plasticServer_;
    private final ComboBoxModel plasticViewerModel_;
    private final ImageViewer[] staticViewers_;
    private ImageViewer selected_;

    /**
     * Constructor.
     *
     * @param  plasticServer  provider of PLASTIC services
     */
    public ImageViewerComboBoxModel( TopcatPlasticListener plasticServer ) {
        plasticServer_ = plasticServer;

        /* Prepare list of PLASTIC viewers. */
        plasticViewerModel_ = plasticServer
            .createPlasticComboBoxModel( MessageId.FITS_LOADIMAGE );

        /* Prepare in-JVM viewer list. */
        List svList = new ArrayList();
        svList.add( new ImageViewer() {
            public boolean viewImage( String label, String location ) {
                BasicImageDisplay.displayBasicImage( label, location );
                return true;
            }
            public String toString() {
                return "Basic Viewer";
            }
        } );
        if ( TopcatUtils.canSog() ) {
            svList.add( new ImageViewer() {
                public boolean viewImage( String label, String location ) {
                    Sog.sog( label, location );
                    return true;
                }
                public String toString() {
                    return "SoG";
                }
            } );
        }
        staticViewers_ = (ImageViewer[]) svList.toArray( new ImageViewer[ 0 ] );
        setSelectedItem( getElementAt( 0 ) );
    }

    public void addListDataListener( ListDataListener l ) {
        plasticViewerModel_.addListDataListener( l );
    }

    public void removeListDataListener( ListDataListener l ) {
        plasticViewerModel_.removeListDataListener( l );
    }

    public int getSize() {
        return staticViewers_.length + plasticViewerModel_.getSize();
    }

    public Object getElementAt( int index ) {
        if ( index < staticViewers_.length ) {
            return staticViewers_[ index ];
        }
        else {
            index -= staticViewers_.length;
            final Object app = plasticViewerModel_.getElementAt( index );
            URI recipient = app instanceof ApplicationItem
                          ? ((ApplicationItem) app).getId()
                          : null;
            return new PlasticViewer( recipient ) {
                public String toString() {
                    return app.toString() + " (PLASTIC)";
                }
            };
        }
    }

    public void setSelectedItem( Object item ) {
        selected_ = ( item instanceof ImageViewer ) ? (ImageViewer) item
                                                    : null;
    }

    public Object getSelectedItem() {
        return selected_;
    }

    /**
     * Returns the selected item cast to an ImageViewer.
     *
     * @return   {@link #getSelectedItem}
     */
    public ImageViewer getSelectedViewer() {
        return selected_;
    }

    /**
     * ImageViewer implementation based on a Plastic recipient.
     */
    private class PlasticViewer implements ImageViewer {
        private final URI recipient_;
        private final String id_;

        /**
         * Constructor.
         *
         * @param   recipient  recipient application URI, or null for broadcast
         */
        PlasticViewer( URI recipient ) {
            recipient_ = recipient;
            id_ = recipient_ == null ? ""
                                     : recipient_.toString();
        }

        public boolean viewImage( String label, String location ) {
            try {
                List argList =
                    Arrays.asList( new Object[] { location, label, } );
                URI msgId = MessageId.FITS_LOADIMAGE;
                plasticServer_.register();
                PlasticHubListener hub = plasticServer_.getHub();
                URI plasticId = plasticServer_.getRegisteredId();
                Map responses = ( recipient_ == null )
                    ? hub.request( plasticId, msgId, argList )
                    : hub.requestToSubset( plasticId, msgId, argList,
                                           Collections
                                          .singletonList( recipient_ ) );
                for ( Iterator it = responses.values().iterator();
                      it.hasNext(); ) {
                    Object response = it.next();
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
    }
}
