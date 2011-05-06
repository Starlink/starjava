package uk.ac.starlink.topcat.interop;

import java.awt.Component;
import java.io.IOException;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.SubsetWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.plot.DensityWindow;
import uk.ac.starlink.vo.RegistryPanel;

/**
 * Abstract interface for inter-application messaging requirements of TOPCAT.
 * This can be implemented by SAMP or PLASTIC (or others).
 *
 * @author   Mark Taylor
 * @since    4 Sep 2008
 */
public interface TopcatCommunicator {

    /**
     * Returns the name of the protocol over which this object is implemented.
     *
     * @return   protocol name
     */
    String getProtocolName();

    /**
     * Must be called before any of the actions provided by this object
     * are used.  May initiate communication with the messaging system etc.
     *
     * @return  true iff there is a current connection
     */
    boolean setActive();

    /**
     * Returns a list of actions suitable for insertion in a general purpose
     * menu associated with interoperability functionality
     * (register/unregister etc).
     * 
     * @return   action list
     */
    Action[] getInteropActions();

    /**
     * Returns an object that can send send the currently selected
     * table from TOPCAT to other applications.
     *
     * @return  table transmitter
     */
    Transmitter getTableTransmitter();

    /**
     * Returns an object that can send the density map currently displayed
     * in the density plot window to other applications as a FITS image.
     *
     * @param  densityWindow  density plot window
     * @return  new image transmitter
     */
    Transmitter createImageTransmitter( DensityWindow densityWindow );

    /**
     * Returns an object that can send the RowSubset currently selected in
     * a given subset window to other applications as a row selection on
     * a commonly-known table.
     *
     * @param  tcModel   table
     * @param  subsetWindow   subset window
     * @return  new subset transmitter
     */
    Transmitter createSubsetTransmitter( TopcatModel tcModel,
                                         SubsetWindow subsetWindow );

    /**
     * Returns an object that can send the currently displayed resources
     * from a registry panel.
     *
     * @param   regPanel  registry panel component
     * @param   resourceType  resource subtype - must match the appropriate
     *          voresource.loadlist.* MType subtype
     * @return  new resource list transmitter
     */
    Transmitter createResourceListTransmitter( RegistryPanel regPanel,
                                               String resourceType );

    /**
     * Returns an object which can be used to send messages drawing attention
     * to particular sky positions.
     *
     * @return   new activity object
     */
    SkyPointActivity createSkyPointActivity();

    /**
     * Returns an object which can be used to send messages highlighting
     * single table rows.
     *
     * @return   new activity object
     */
    RowActivity createRowActivity();

    /**
     * Returns an object which can be used to send messages selecting 
     * table row subsets.
     *
     * @return   new activity object
     */
    SubsetActivity createSubsetActivity();

    /**
     * Returns an object which can be used to display images.
     * Note this may include options apart from interop-type ones
     * (display in local viewers).
     *
     * @return  new activity object
     */
    ImageActivity createImageActivity();

    /**
     * Returns an object which can be used to display spectra.
     *
     * @return  new activity object
     */
    SpectrumActivity createSpectrumActivity();

    /**
     * Attempts to start a messaging hub suitable for use with this object.
     *
     * @param  external  true to run hub in external JVM,
     *                   false to run it in the current one
     */
    void startHub( boolean external ) throws IOException;

    /**
     * According to the policy of this communicator, this method may
     * start a hub if none is already running.
     */
    void maybeStartHub() throws IOException;

    /**
     * Optionally returns a panel which can be displayed in the control
     * window to show communications status.
     *
     * @return   status component, or null if unimplemented
     */
    JComponent createInfoPanel();

    /**
     * Constructs an action which will display a control window for this
     * communicator.
     *
     * @param   parent  parent component
     * @return   communicator control window, or null if none is available
     */
    Action createWindowAction( Component parent );

    /**
     * Indicates (without attempting a connection) whether a hub connection is
     * currently in force.
     *
     * @return   whether hub is connected
     */
    boolean isConnected();

    /**
     * Adds a listener which will be notified any time that connection status
     * may have changed.
     *
     * @param   listener   listener to add
     */
    void addConnectionListener( ChangeListener listener );
}
