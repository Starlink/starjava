package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import uk.ac.starlink.splat.util.Utilities;

/**
 * Controller for saving, restoring and deleting plot configuration
 * data stored in XML files. The configurations are stored in a
 * permanent file (PlotConfigs.xml) which has each configuration
 * characterised by a description (created by the user) and a date
 * that the configuration was created (or maybe last updated).
 * <p>
 * An instance of this class should be associated with a
 * PlotConfigFrame object that acts as a view for the restored
 * configuration and a model for the current configuration.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see #PlotConfig, #PlotConfigFrame
 */
public class PlotConfigSaveFrame extends JFrame
{
    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBar = new JPanel();

    /**
     * Container for view of currently saved states.
     */
    protected JPanel statusView = new JPanel();

    /**
     * Visible configuration object. Mediates to the actual stores.
     */
    protected PlotConfigFrame config = null;

    /**
     * Name of the file containing the XML descriptions.
     */
    protected String storageFile = "PlotConfigs.xml";

    /**
     * Create an instance.
     */
    public PlotConfigSaveFrame( PlotConfigFrame config )
    {
        this.config = config;
        contentPane = (JPanel) getContentPane();
        initUI();
        initFrame();
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Save or Restore Plot Configurations" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( statusView, BorderLayout.CENTER );
        contentPane.add( actionBar, BorderLayout.SOUTH );
        setSize( new Dimension( 400, 650 ) );
        setVisible( true );
    }

    /**
     * Initialise the user interface. This is the action bar and the
     * status view.
     */
    protected void initUI()
    {
        //  Action bar uses a BoxLayout.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
    }
}

