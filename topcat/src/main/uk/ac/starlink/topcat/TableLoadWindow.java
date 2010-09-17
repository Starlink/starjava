package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JProgressBar;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.load.TableLoadClient;
import uk.ac.starlink.table.load.TableLoadDialog2;
import uk.ac.starlink.table.load.TableLoadPanel;

/**
 * Window to contain a single TableLoadDialog.
 * It wraps the dialogue component in a TableLoadPanel, and then in an
 * AuxWindow with appropriate menus and toolbar etc.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2010
 */
public class TableLoadWindow extends AuxWindow {

    private final ToggleButtonModel stayOpenModel_;

    /**
     * Constructor.
     *
     * @param  parent  parent component
     * @param  tld   load dialogue
     * @param  tfact   representative table factory
     */
    public TableLoadWindow( Component parent, TableLoadDialog2 tld,
                            StarTableFactory tfact ) {
        super( tld.getName(), parent );

        /* Wrap the dialogue in a panel which supplies OK and Cancel buttons. */
        final TableLoadPanel tlp = new TableLoadPanel( tld, tfact ) {
            protected TableLoadClient getLoadClient() {
                return TableLoadWindow.this.createTableLoadClient();
            }
        };

        /* Add a model and button to control whether the window stays open
         * after a completed load. */
        stayOpenModel_ =
            new ToggleButtonModel( "Stay Open", ResourceIcon.DO_WHAT,
                                   "Keep window open even after " +
                                   "successful load" );
        getToolBar().add( stayOpenModel_.createToolbarButton() );
        getToolBar().addSeparator();

        /* Add a progress bar. */
        JProgressBar progBar = placeProgressBar();
        progBar.setString( "" );
        progBar.setStringPainted( true );
        tlp.setProgressBar( progBar );

        /* Arrange to cancel the load if the window is closed. */
        addWindowListener( new WindowAdapter() {
            public void windowClosed( WindowEvent evt ) {
                Action cancelAct = tlp.getCancelAction();
                if ( tlp.isLoading() ) {
                    tlp.getCancelAction()
                       .actionPerformed( new ActionEvent( evt.getSource(),
                                                          evt.getID(),
                                                          "Close" ) );
                }
            }
        } );

        /* Place components. */
        getMainArea().add( tlp );
        JMenu[] menus = tld.getMenus();
        if ( menus != null ) {
            for ( int im = 0; im < menus.length; im++ ) {
                getJMenuBar().add( menus[ im ] );
            }
        }
        Action[] toolActs = tld.getToolbarActions();
        if ( toolActs != null && toolActs.length > 0 ) {
            for ( int ia = 0; ia < toolActs.length; ia++ ) {
                getToolBar().add( new JButton( toolActs[ ia ] ) );
            }
            getToolBar().addSeparator();
        }
        addHelp( tld.getClass().getName().replaceFirst( ".*\\.", "" ) );
    }

    protected TableLoadClient createTableLoadClient() {
        return new TopcatLoadClient( TableLoadWindow.this,
                                     ControlWindow.getInstance() ) {
            public void endSequence( boolean cancelled ) {
                super.endSequence( cancelled );
                if ( ! cancelled && getLoadCount() > 0 &&
                     ! stayOpenModel_.isSelected() ) {
                    TableLoadWindow.this.dispose();
                }
            }
        };
    }
}
