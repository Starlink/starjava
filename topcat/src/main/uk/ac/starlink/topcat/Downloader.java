package uk.ac.starlink.topcat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Manages downloading of data that only needs to be got once.
 *
 * @author   Mark Taylor
 * @since    13 Jun 2014
 */
public abstract class Downloader<T> {

    private final Class<T> clazz_;
    private final String dataDescription_;
    private final List<ActionListener> listenerList_;
    private final Runnable changeRunnable_;
    private volatile IOException error_;
    private volatile boolean isStarted_;
    private volatile boolean isComplete_;
    private volatile T data_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     *
     * @param   clazz  type of data downloaded
     * @param   dataDescription  short description of downloaded data,
     *                           may be used in logging messages
     */
    public Downloader( Class<T> clazz, String dataDescription ) {
        clazz_ = clazz;
        dataDescription_ = dataDescription;
        listenerList_ = new ArrayList<ActionListener>();
        changeRunnable_ = new Runnable() {
            final ActionEvent evt = new ActionEvent( this, 0, "Changed" );
            public void run() {
                for ( ActionListener listener : listenerList_ ) {
                    listener.actionPerformed( evt );
                }
            }
        };
    }

    /**
     * Performs the actual download.  Implementations are encouraged
     * to log query and details of success if applicable at the INFO level,
     * but an error will be logged by the Downloader.
     *
     * @return  downloaded data
     */
    public abstract T attemptReadData() throws IOException;

    /**
     * Indicates whether the data has been downloaded.
     * If this method returns true, then {@link #getData} will return the
     * result.
     *
     * @return  true iff download has completed, successfully or otherwise
     */
    public boolean isComplete() {
        return isComplete_;
    }

    /**
     * Immediately returns the downloaded data, or null if it has not been
     * downloaded, or if a download has failed.
     *
     * @return  data
     */
    public T getData() {
        return data_;
    }

    /**
     * Resets the state of this downloader, as if the no download attempt
     * had been made.
     */
    public synchronized void clearData() {
        isStarted_ = false;
        isComplete_ = false;
        data_ = null;
        error_ = null;
        informListeners();
    }

    /**
     * Downloads the data if necessary, and returns its content.
     * If a download attempt has already been completed, this will return
     * immediately, otherwise it will block.
     * If the download failed, null will be returned.
     *
     * @return   data or null on failure
     */
    public synchronized T waitForData() {
        if ( isComplete() ) {
            return data_;
        }
        else {
            isStarted_ = true;
            data_ = readData();
            isComplete_ = true;
            informListeners();
            return data_;
        }
    }

    /**
     * Returns a little component that monitors status of this downloader.
     * Currently, it is blank before the download has happened,
     * then turns to green on success or red on failure.
     */
    public JComponent createMonitorComponent() {
        final int size = 10;
        Icon icon = new Icon() {
            public int getIconWidth() {
                return size;
            }
            public int getIconHeight() {
                return size;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Graphics2D g2 = (Graphics2D) g.create();
                g = null;
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON );
                Color fillColor = getStatusColor();
                if ( fillColor != null ) {
                    g2.setColor( fillColor );
                    g2.fillOval( x + 1, y + 1, size - 2, size - 2 );
                }
                g2.setColor( Color.DARK_GRAY );
                g2.drawOval( x + 1, y + 1, size - 2, size - 2 );
                g2.setColor( Color.GRAY );
                g2.drawOval( x + 2, y + 2, size - 4, size - 4 );
            }
            private Color getStatusColor() {
                if ( isComplete() ) {
                    return getData() == null ? Color.RED : Color.GREEN;
                }
                else {
                    return null;
                }
            }
        };
        final JLabel label = new JLabel( icon );
        addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                label.setToolTipText( getStatus() );
                label.repaint();
            }
        } );
        return label;
    }

    /**
     * Adds a listener that will be notified if the data acquisition status
     * changes.
     *
     * @param  listener   listener
     */
    public void addActionListener( ActionListener listener ) {
        listenerList_.add( listener );
    }

    /**
     * Removes a previously added listener.
     *
     * @param  listener   listener
     */
    public void removeActionListener( ActionListener listener ) {
        listenerList_.remove( listener );
    }

    /**
     * Informs listeners if the status has changed.
     */
    private void informListeners() {
        SwingUtilities.invokeLater( changeRunnable_ );
    }

    /**
     * Reads the data and logs any error.
     *
     * @return  data, or null on error
     */
    private T readData() {
        try {
            return attemptReadData();
        }
        catch ( IOException e ) {
            error_ = e;
            logger_.log( Level.WARNING, "Failed to read " + dataDescription_,
                         e );
            return null;
        }
    }

    /**
     * Returns a short status string, suitable for use in a tooltip.
     *
     * @return  status of this downloader
     */
    private String getStatus() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( dataDescription_ )
            .append( " download: " );
        if ( isComplete_ ) {
            if ( getData() == null ) {
                sbuf.append( "failed" );
                if ( error_ != null ) {
                    sbuf.append( " (" )
                        .append( error_ )
                        .append( ")" );
                }
            }
            else {
                sbuf.append( "success" );
            }
        }
        else if ( isStarted_ ) {
            sbuf.append( "running" );
        }
        else {
            sbuf.append( "inactive" );
        }
        return sbuf.toString();
    }
}
