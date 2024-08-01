package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenu;
import uk.ac.starlink.hapi.HapiTableLoadDialog;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadClient;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.vo.TapTableLoadDialog;

/**
 * Window to contain a single TableLoadDialog.
 * It wraps the dialogue component in a TableLoadPanel, and then in an
 * AuxWindow with appropriate menus and toolbar etc.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2010
 */
public class TableLoadDialogWindow extends AuxWindow {

    private final TableLoadDialog tld_;
    private final LoadWindow loadWin_;
    private final ToggleButtonModel stayOpenModel_;

    /**
     * Constructor.
     *
     * @param  parent   parent component
     * @param  tld     table load dialogue
     * @param  loadWin  load window
     * @param  tfact  sample table factory
     */
    @SuppressWarnings("this-escape")
    public TableLoadDialogWindow( Component parent, TableLoadDialog tld,
                                  LoadWindow loadWin, StarTableFactory tfact ) {
        super( tld.getName(), parent );
        tld_ = tld;
        loadWin_ = loadWin;

        /* Add a model and button to control whether the window stays open
         * after a completed load. */
        stayOpenModel_ =
            new ToggleButtonModel( "Stay Open", ResourceIcon.KEEP_OPEN,
                                   "Keep window open even after " +
                                   "successful load" );
        stayOpenModel_.setSelected( isPinnedDflt( tld ) );
        getToolBar().add( stayOpenModel_.createToolbarButton() );
        getToolBar().addSeparator();
        JMenu windowMenu = getWindowMenu();
        windowMenu.insert( stayOpenModel_.createMenuItem(),
                           windowMenu.getItemCount() - 2 );

        /* Action to initiate loading. */
        Action okAct = new BasicAction( "OK", null, "Load Selected Table" ) {
            public void actionPerformed( ActionEvent evt ) {
                performLoad();
            }
        };
        getControlPanel().add( Box.createHorizontalGlue() );
        getControlPanel().add( new JButton( okAct ) );

        /* Configure main panel for dialogue and install into window. */
        tld.configure( tfact, okAct );
        getMainArea().add( tld.getQueryComponent() );

        /* Add dialogue-specific menus and toolbar buttons. */
        JMenu[] menus = tld.getMenus();
        if ( menus != null ) {
            for ( int im = 0; im < menus.length; im++ ) {
                getJMenuBar().add( menus[ im ] );
            }
        }
        Action[] toolActs = tld.getToolbarActions();
        if ( toolActs != null && toolActs.length > 0 ) {
            for ( int ia = 0; ia < toolActs.length; ia++ ) {
                getToolBar().add( toolActs[ ia ] );
            }
            getToolBar().addSeparator();
        }

        /* Add standard help actions. */
        addHelp( tld.getClass().getName().replaceFirst( ".*\\.", "" )
                                         .replaceFirst( "^Topcat", "" ) );
    }

    /**
     * Invoked when the OK button is hit.
     */
    private void performLoad() {

        /* Acquire a TableLoader by interrogating the state of the GUI. */
        TableLoader loader;
        try { 
            loader = tld_.createTableLoader();
        }
        catch ( RuntimeException e ) {
            ErrorDialog.showError( this, "Can't Load Table", e );
            return;
        }
        if ( loader == null ) {
            return;
        }

        /* Prepare a LoadClient which will close this window if appropriate
         * when done. */
        ControlWindow controlWin = ControlWindow.getInstance();
        TableLoadClient loadClient = new TopcatLoadClient( this, controlWin ) {
            public void endSequence( boolean cancelled ) {
                super.endSequence( cancelled );
                if ( ! cancelled && getLoadCount() > 0 &&
                     ! stayOpenModel_.isSelected() ) {
                    TableLoadDialogWindow.this.dispose();
                }
            }
        };

        /* Perform asynchronous table loading. */
        controlWin.runLoading( loader, loadClient, tld_.getIcon() );
    }

    /**
     * Determines whether the window associated with a given load dialogue
     * should be pinned open by default.
     *
     * @param  tld  load dialogue
     * @return   if false, window will disappear when a load is complete;
     *           if true, it will stay posted
     */
    private static boolean isPinnedDflt( TableLoadDialog tld ) {
        return tld instanceof TapTableLoadDialog
            || tld instanceof HapiTableLoadDialog;
    }
}
