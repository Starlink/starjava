package uk.ac.starlink.util.gui;

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
import javax.swing.Timer;

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
    private final Timer timer_;
    private volatile IOException error_;
    private volatile boolean isStarted_;
    private volatile boolean isComplete_;
    private volatile T data_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util.gui" );

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
        timer_  = new Timer( 20, new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                for ( ActionListener listener : listenerList_ ) {
                    listener.actionPerformed( evt );
                }
            }
        } );
        timer_.stop();
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
     * Resets the state of this downloader, as if no download attempt
     * had been made.
     */
    public void clearData() {
        isStarted_ = false;
        isComplete_ = false;
        data_ = null;
        error_ = null;
        informListeners();
    }

    /**
     * Sets the state of this downloader as if it had just downloaded the
     * given data item.  This can be necessary to restore its state,
     * since it affects the monitor component.
     *
     * @param  value  value that would have been downloaded
     */
    public void setData( T value ) {
        isStarted_ = true;
        isComplete_ = true;
        data_ = value;
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
    public T waitForData() {
        if ( isComplete() ) {
            return data_;
        }
        else {
            isStarted_ = true;
            try {
                timer_.start();
                data_ = readData();
            }
            finally {
                timer_.stop();
                isComplete_ = true;
                informListeners();
            }
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
        final int period = 1000;
        Icon icon = new Icon() {
            public int getIconWidth() {
                return size;
            }
            public int getIconHeight() {
                return size;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                boolean isComplete = isComplete();
                boolean isStarted = isStarted_;
                Graphics2D g2 = (Graphics2D) g.create();
                g = null;
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON );
                final Color fillColor;
                if ( isComplete ) { 
                    fillColor = getData() == null ? Color.RED : Color.GREEN;
                }
                else if ( isStarted ) {
                    fillColor = Color.ORANGE;
                }
                else {
                    fillColor = null;
                }
                if ( fillColor != null ) {
                    g2.setColor( fillColor );
                    g2.fillOval( x + 2, y + 2, size - 4, size - 4 );
                }
                g2.setColor( Color.DARK_GRAY );
                g2.drawOval( x + 1, y + 1, size - 2, size - 2 );
                g2.setColor( Color.GRAY );
                g2.drawOval( x + 2, y + 2, size - 4, size - 4 );
                if ( isStarted && ! isComplete ) {
                    double phase =
                        ( System.currentTimeMillis() % period ) * 1.0 / period;
                    int r =
                        (int) ( ( Math.abs( phase - 0.5 ) ) * ( size - 2 ) );
                    g2.setColor( Color.GRAY );
                    g2.drawOval( x + size / 2 - r, y + size / 2 - r,
                                 2 * r, 2 * r );
                }
            }
            private Color getStatusColor() {
                if ( isComplete() ) {
                    return getData() == null ? Color.RED : Color.GREEN;
                }
                else if ( isStarted_ ) {
                    return Color.ORANGE;
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
