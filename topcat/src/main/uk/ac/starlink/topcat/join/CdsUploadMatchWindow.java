package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import uk.ac.starlink.topcat.AuxWindow;

/**
 * Window for doing upload sky matches using the CDS X-Match service.
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public class CdsUploadMatchWindow extends AuxWindow {

    /**
     * Constructor.
     *
     * @param  parent  parent window
     */
    @SuppressWarnings("this-escape")
    public CdsUploadMatchWindow( Component parent ) {
        super( "CDS Upload X-Match", parent );
        JComponent main = getMainArea();
        UploadMatchPanel matchPanel =
            new UploadMatchPanel( placeProgressBar() );
        main.add( matchPanel, BorderLayout.CENTER );
        JComponent controls = getControlPanel();
        controls.add( new JButton( matchPanel.getStartAction() ) );
        controls.add( new JButton( matchPanel.getStopAction() ) );

        getToolBar().add( matchPanel.getCoverageModel().createToolbarButton() );
        getToolBar().addSeparator();
        JMenu searchMenu = new JMenu( "Search" );
        searchMenu.add( matchPanel.getCoverageModel().createMenuItem() );
        getJMenuBar().add( searchMenu );

        addHelp( "CdsUploadMatchWindow" );
    }
}
