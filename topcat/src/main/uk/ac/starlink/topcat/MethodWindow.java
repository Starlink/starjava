package uk.ac.starlink.topcat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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

    private static Action windowAction;
    private static MethodWindow window;

    private final JTree tree = new JTree( new DefaultMutableTreeNode() );
    private List staticClasses = JELUtils.getStaticClasses();

    /**
     * Construct a new method browser.
     *
     * @param  parent  a parent component which may be used for positioning
     *         purposes
     */
    public MethodWindow( Component parent ) {
        super( "Available Methods", parent );

        /* Put class information into the tree. */
        for ( Iterator it = staticClasses.iterator(); it.hasNext(); ) {
            Class clazz = (Class) it.next();
            if ( ! clazz.equals( JELUtils.class ) ) {
                addStaticClass( clazz );
            }
        }

        /* Arrange for suitable rendering. */
        tree.setRootVisible( false );
        tree.setShowsRootHandles( true );
        tree.setCellRenderer( new DefaultTreeCellRenderer() {
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
                    String text = textFor( value );
                    if ( text != null ) {
                        ((JLabel) comp).setText( text );
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
                String cname = JOptionPane
                              .showInputDialog( subpar, message, 
                                                "Enter new class",
                                                JOptionPane.QUESTION_MESSAGE );
                if ( cname != null ) {
                    try {
                        Class clazz = this.getClass().forName( cname );
                        staticClasses.add( clazz );
                        addStaticClass( clazz );
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
        };

        /* Open up the top level. */
        tree.expandPath( new TreePath( tree.getModel().getRoot() ) );

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
     */
    public void addStaticClass( Class clazz ) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        DefaultMutableTreeNode clazzNode = new DefaultMutableTreeNode( clazz );
        model.insertNodeInto( clazzNode, root, root.getChildCount() );
                              
        ((DefaultMutableTreeNode) model.getRoot()).add( clazzNode );
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
     * Returns an action which corresponds to displaying a MethodWindow.
     *
     * @param  parent  a component which may be used as a parent
     *                 for positioning purposes
     */
    public static Action getWindowAction( final Component parent ) {
        if ( windowAction == null ) {
            windowAction = new BasicAction( "Available Functions",
                                            ResourceIcon.FUNCTION,
                                            "Display information about " +
                                            "available algebraic functions" ) {
                public void actionPerformed( ActionEvent evt ) {
                    if ( window == null ) {
                        window = new MethodWindow( parent );
                    }
                    else {
                        window.makeVisible();
                    }
                }
            };
        }
        return windowAction;
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
        }
        return null;
    }
}
