package uk.ac.starlink.topcat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * A panel displaying the static public methods of a set of classes.
 * This is currently arranged as a tree.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MethodWindow extends AuxWindow {

    private static MethodWindow window;

    private final JTree tree = new JTree( new DefaultMutableTreeNode() );
    private final DefaultMutableTreeNode calcNode;
    private final DefaultMutableTreeNode activNode;

    /**
     * Construct a new method browser.
     *
     * @param  parent  a parent component which may be used for positioning
     *         purposes
     */
    public MethodWindow( Component parent ) {
        super( "Available Functions", parent );
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode)
                                            model.getRoot();
        calcNode = new DefaultMutableTreeNode( "General Functions", true );
        activNode = new DefaultMutableTreeNode( "Activation Functions", true );
        root.add( calcNode );
        root.add( activNode );

        /* Put class information into the tree. */
        for ( Iterator it = JELUtils.getGeneralStaticClasses().iterator();
              it.hasNext(); ) {
            addStaticClass( (Class) it.next(), calcNode );
        }
        for ( Iterator it = JELUtils.getActivationStaticClasses().iterator();
              it.hasNext(); ) {
            addStaticClass( (Class) it.next(), activNode );
        }

        /* Arrange for suitable rendering. */
        tree.setRootVisible( false );
        tree.setShowsRootHandles( true );
        tree.setCellRenderer( new DefaultTreeCellRenderer() {
            Font basicFont;
            Font strongFont;
            public Component getTreeCellRendererComponent( JTree tree, 
                                                           Object value,
                                                           boolean selected, 
                                                           boolean expanded,
                                                           boolean leaf,
                                                           int irow, 
                                                           boolean hasFocus ) {
                Component comp =
                    super.getTreeCellRendererComponent( tree, value, selected,
                                                        expanded, leaf, irow,
                                                        hasFocus );
                if ( comp instanceof JLabel ) {
                    JLabel label = (JLabel) comp;
                    String text = textFor( value );
                    if ( text != null ) {
                        if ( basicFont == null ) {
                            basicFont = label.getFont();
                            strongFont = basicFont.deriveFont( Font.BOLD );
                        }
                        boolean topLevel = value == calcNode 
                                        || value == activNode;
                        label.setFont( topLevel ? strongFont : basicFont );
                        label.setText( text );
                    }
                }
                return comp;
            }
        } );

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
                    if ( c == null ) {
                        return;
                    }
                    else {
                        cname = c.toString();
                        try {
                            Class clazz = this.getClass().forName( cname );
                            JELUtils.getGeneralStaticClasses().add( clazz );
                            addStaticClass( clazz, root );
                            return;
                        }
                        catch ( ClassNotFoundException e ) {
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

        /* Open up the top level. */
        TreePath rootPath = new TreePath( root );
        tree.expandPath( rootPath );
        tree.expandPath( rootPath.pathByAddingChild( calcNode ) );

        /* Add the tree to this panel. */
        JScrollPane scroller = new JScrollPane( tree );
        scroller.setPreferredSize( new Dimension( 500, 250 ) );
        getMainArea().add( scroller );

        /* Tools. */
        getToolBar().add( addAction );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "MethodWindow" );

        /* Make the component visible. */
        pack();
        setVisible( true );
    }

    /**
     * Adds a new class to the tree containing available static methods.
     *
     * @param  clazz  class to add
     * @param  parent  tree node to append it to
     */
    public void addStaticClass( Class clazz, DefaultMutableTreeNode parent ) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode clazzNode = new DefaultMutableTreeNode( clazz );
        model.insertNodeInto( clazzNode, parent, parent.getChildCount() );
                              
        parent.add( clazzNode );
        List methods =
            new ArrayList( Arrays.asList( clazz.getDeclaredMethods() ) );
        for ( Iterator it = methods.iterator(); it.hasNext(); ) {
            Method meth = (Method) it.next();
            int mods = meth.getModifiers();
            if ( ! Modifier.isStatic( mods ) ||
                 ! Modifier.isPublic( mods ) ) {
                it.remove();
            }
        }
        Collections.sort( methods, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ((Method) o1).getName()
                      .compareTo( ((Method) o2).getName() );
            }
        } );
        for ( Iterator it = methods.iterator(); it.hasNext(); ) {
            model.insertNodeInto( new DefaultMutableTreeNode( it.next() ),
                                  clazzNode, clazzNode.getChildCount() );
        }
    }

    /**
     * Makes sure that the activation node is either expanded or collapsed.
     *
     * @param  show  true to expand the activation classes, 
     *               false to collapse them
     */
    private void showActivation( boolean show ) {
        TreePath activPath =
            new TreePath( new Object[] { tree.getModel().getRoot(),
                                         activNode } );
        if ( show ) {
            tree.expandPath( activPath );
        }
        else {
            tree.collapsePath( activPath );
        }
    }

    /**
     * Returns an action which corresponds to displaying a MethodWindow.
     *
     * @param  parent  a component which may be used as a parent
     *                 for positioning purposes
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
                else {
                    window.showActivation( activation );
                    window.makeVisible();
                }
            }
        };
    }

    /**
     * Returns the string to get used for representing a node in the tree.
     *
     * @param   node  tree node
     */
    public String textFor( Object node ) {
        if ( node instanceof DefaultMutableTreeNode ) {
            Object userObj = ((DefaultMutableTreeNode) node).getUserObject();
            if ( userObj instanceof Class ) {
                return ((Class) userObj).getName();
            }
            else if ( userObj instanceof Method ) {
                Method method = (Method) userObj;
                StringBuffer sbuf = new StringBuffer()
                    .append( method.getReturnType().getName() )
                    .append( ' ' )
                    .append( method.getName() )
                    .append( "( " );
                Class[] params = method.getParameterTypes();
                for ( int i = 0; i < params.length; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( params[ i ].getName() );
                }
                sbuf.append( " )" );
                return sbuf.toString();
            }
            else if ( userObj != null ) {
                return userObj.toString();
            }
        }
        return null;
    }
}
