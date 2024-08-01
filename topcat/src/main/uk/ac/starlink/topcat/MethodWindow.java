package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import uk.ac.starlink.ttools.build.Heading;
import uk.ac.starlink.ttools.gui.MethodBrowser;

/**
 * A panel displaying the static public methods of a set of classes.
 * This is currently arranged as a tree.  If javadocs are available
 * (under names as described by the {@link uk.ac.starlink.ttools.gui.DocNames}
 * class) these are displayed alongside.
 * 
 * <p>Currently, the items in the tree can be an instance of one of the
 * following classes:
 * <ul>
 * <li>{@link java.lang.Class}
 * <li>{@link java.lang.reflect.Method}
 * <li>{@link java.lang.reflect.Field}
 * <li>{@link uk.ac.starlink.ttools.build.Heading}
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 */
public class MethodWindow extends AuxWindow {

    private static MethodWindow window;

    private final MethodBrowser browser_;
    private final DefaultMutableTreeNode activNode_;
    static final String SYNTAX_HELP_ID = "jel";

    /**
     * Construct a new method browser.
     *
     * @param  parent  a parent component which may be used for positioning
     *         purposes
     */
    @SuppressWarnings("this-escape")
    public MethodWindow( Component parent ) {
        super( "Available Functions", parent );
        browser_ = new MethodBrowser();
        getMainArea().add( browser_ );
        final DefaultMutableTreeNode root = browser_.getRoot();

        /* Put class information into the tree. */
        for ( Class<?> clazz : TopcatJELUtils.getStaticClasses() ) {
            browser_.addStaticClass( clazz, root );
        }
        activNode_ = new DefaultMutableTreeNode( Heading.ACTIVATION, true );
        root.add( activNode_ );
        for ( Class<?> clazz : TopcatJELUtils.getActivationStaticClasses() ) {
            browser_.addStaticClass( clazz, activNode_ );
        }

        /* Action for adding a new class. */
        final Component subpar = this;
        final String[] message = new String[] {
            "Enter the fully-qualified path of a",
            "class on the class path containing",
            "public static methods to import into",
            "the expressions namespace",
        };
        Action addAction = new BasicAction( "Add Class", ResourceIcon.ADD,
                                            "Add a class containing " +
                                            "more methods" ) {
            public void actionPerformed( ActionEvent evt ) {
                String cname = null;
                while ( true ) {
                    Object c = JOptionPane
                              .showInputDialog( subpar, message,
                                                "Add New Class",
                                                JOptionPane.QUESTION_MESSAGE,
                                                null, null, cname );
                    cname = c == null ? null : c.toString();
                    if ( cname == null ) {
                        return;
                    }
                    else {
                        Class<?> clazz = TopcatJELUtils.classForName( cname );
                        if ( clazz != null ) {
                            TopcatJELUtils.getStaticClasses().add( clazz );
                            browser_.addStaticClass( clazz, root );
                            return;
                        }
                        else {
                            JOptionPane
                           .showMessageDialog( subpar, "The class " + cname + 
                                               " is not on the class path",
                                               "Class not found",
                                               JOptionPane.ERROR_MESSAGE );
                        }
                    }
                }
            }
        };
        addAction.setEnabled( TopcatUtils.canJel() );

        /* Action to display expression syntax. */
        Action syntaxAction = new HelpAction( SYNTAX_HELP_ID, this );
        syntaxAction.putValue( Action.NAME, "Syntax Help" );
        syntaxAction.putValue( Action.SHORT_DESCRIPTION,
                               "Display manual section on expression syntax "
                             + "in help browser" );
        syntaxAction.putValue( Action.SMALL_ICON, ResourceIcon.SYNTAX );

        /* Open up the top level. */
        TreePath rootPath = new TreePath( root );
        browser_.getTree().expandPath( rootPath );

        /* Add actions to toolbar. */
        getToolBar().add( addAction );
        getToolBar().add( syntaxAction );
        getToolBar().addSeparator();

        /* Add actions to menu bar. */
        JMenu funcMenu = new JMenu( "Functions" );
        funcMenu.add( addAction );
        funcMenu.add( syntaxAction );
        getJMenuBar().add( funcMenu );

        /* Add standard help actions. */
        addHelp( "MethodWindow" );
    }

    /**
     * Makes sure that the activation node is either expanded or collapsed.
     *
     * @param  show  true to expand the activation classes, 
     *               false to collapse them
     */
    private void showActivation( boolean show ) {
        TreePath activPath =
            new TreePath( new Object[] { browser_.getRoot(), activNode_ } );
        if ( show ) {
            browser_.getTree().expandPath( activPath );
        }
        else {
            browser_.getTree().collapsePath( activPath );
        }
    }

    /**
     * Returns an action which corresponds to displaying a MethodWindow.
     *
     * @param  parent  a component which may be used as a parent
     *                 for positioning purposes
     * @param  activation  true iff you want to see the activation actions
     *                     as well as the normal functions
     */
    public static Action getWindowAction( final Component parent,
                                          final boolean activation ) {
        return new BasicAction( "Available Functions",
                                ResourceIcon.FUNCTION,
                                "Display information about " +
                                "available algebraic functions" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( window == null ) {
                    window = new MethodWindow( parent );
                }
                window.showActivation( activation );
                window.makeVisible();
            }
        };
    }

    /**
     * Returns the MethodBrowser used by this window.
     *
     * @return  method browser
     */
    public MethodBrowser getBrowser() {
        return browser_;
    }
}
