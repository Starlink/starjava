package uk.ac.starlink.topcat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.topcat.doc.DocNames;
import uk.ac.starlink.topcat.doc.Heading;

/**
 * A panel displaying the static public methods of a set of classes.
 * This is currently arranged as a tree.  If javadocs are available
 * (under names as described by the {@link uk.ac.starlink.topcat.doc.DocNames}
 * class) these are displayed alongside.
 * 
 * <p>Currently, the items in the tree can be an instance of one of the
 * following classes:
 * <ul>
 * <li>{@link java.lang.Class}
 * <li>{@link java.lang.reflect.Method}
 * <li>{@link java.lang.reflect.Field}
 * <li>{@link uk.ac.starlink.topcat.doc.Heading}
 * </ul>
 *
 * @author   Mark Taylor (Starlink)
 */
public class MethodWindow extends AuxWindow implements TreeSelectionListener {

    private static MethodWindow window;

    private final JTree tree = new JTree( new DefaultMutableTreeNode() );
    private final JEditorPane docPane;
    private final DefaultMutableTreeNode activNode;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.topcat" );
    private static Map objLabels = new HashMap();
    private static Pattern TITLE_PATTERN = 
        Pattern.compile( ".*<title>([^<&].*?)</title>.*", 
                         Pattern.CASE_INSENSITIVE );

    /**
     * Construct a new method browser.
     *
     * @param  parent  a parent component which may be used for positioning
     *         purposes
     */
    public MethodWindow( Component parent ) {
        super( "Available Functions", parent );
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode)
                                            getTreeModel().getRoot();

        /* Put class information into the tree. */
        for ( Iterator it = TopcatJELUtils.getStaticClasses().iterator();
              it.hasNext(); ) {
            addStaticClass( (Class) it.next(), root );
        }
        activNode = new DefaultMutableTreeNode( Heading.ACTIVATION, true );
        root.add( activNode );
        for ( Iterator it = TopcatJELUtils.getActivationStaticClasses()
                                          .iterator(); it.hasNext(); ) {
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
                if ( comp instanceof JLabel &&
                     value instanceof DefaultMutableTreeNode ) {
                    JLabel label = (JLabel) comp;
                    Object userObj = ((DefaultMutableTreeNode) value)
                                    .getUserObject();
                    String text = textFor( userObj );
                    if ( text != null ) {
                        if ( basicFont == null ) {
                            basicFont = label.getFont();
                            strongFont = basicFont.deriveFont( Font.BOLD );
                        }
                        label.setFont( userObj instanceof Heading ? strongFont 
                                                                  : basicFont );
                        label.setText( text );
                    }
                    Icon icon = iconFor( userObj );
                    if ( icon != null ) {
                        label.setIcon( icon );
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
                    if ( cname == null ) {
                        return;
                    }
                    else {
                        Class clazz = TopcatJELUtils.classForName( cname );
                        if ( clazz != null ) {
                            TopcatJELUtils.getStaticClasses().add( clazz );
                            addStaticClass( clazz, root );
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

        /* Open up the top level. */
        TreePath rootPath = new TreePath( root );
        tree.expandPath( rootPath );

        /* Listen to the tree. */
        tree.addTreeSelectionListener( this );
        tree.getSelectionModel()
            .setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

        /* Put the tree in a scrolling panel. */
        JScrollPane treeScroller = new JScrollPane( tree );
        treeScroller.setPreferredSize( new Dimension( 350, 450 ) );

        /* Set up a documentation viewer. */
        docPane = new JEditorPane();
        docPane.setEditable( false );
        JScrollPane docScroller = new JScrollPane( docPane );
        docScroller.setPreferredSize( new Dimension( 500, 450 ) );

        /* Set up the main panel with a split pane. */
        JSplitPane splitter = 
            new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, false,
                            treeScroller, docScroller );
        getMainArea().add( splitter );

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
                              
        /* Add a node based on the class itself. */
        DefaultMutableTreeNode clazzNode = new DefaultMutableTreeNode( clazz );
        getTreeModel().insertNodeInto( clazzNode, parent, 
                                       parent.getChildCount() );
        parent.add( clazzNode );

        /* Add nodes based on its public static members. */
        addPublicStaticMembers( clazzNode, clazz.getDeclaredFields() );
        addPublicStaticMembers( clazzNode, clazz.getDeclaredMethods() );
    }

    /**
     * Adds an array of class members to a node in the tree.
     * Only the static public members will be added.
     *
     * @param  parent    node under which to add members
     * @param  members   member items to add
     */
    private void addPublicStaticMembers( DefaultMutableTreeNode parent,
                                         Member[] members ) {
        List mems = new ArrayList( Arrays.asList( members ) );
        for ( Iterator it = mems.iterator(); it.hasNext(); ) {
            Member mem = (Member) it.next();
            int mods = mem.getModifiers();
            if ( ! Modifier.isStatic( mods ) ||
                 ! Modifier.isPublic( mods ) ) {
                it.remove();
            }
        }
        Collections.sort( mems, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ((Member) o1).getName()
                      .compareTo( ((Member) o2).getName() );
            }
        } );
        DefaultTreeModel model = getTreeModel();
        for ( Iterator it = mems.iterator(); it.hasNext(); ) {
            
            model.insertNodeInto( new DefaultMutableTreeNode( it.next() ),
                                  parent, parent.getChildCount() );
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
     * Implements the TreeSelectionListener interface.  This method will
     * be called when a new node is selected.
     */
    public void valueChanged( TreeSelectionEvent evt ) {

        /* Find out what object has just been selected, and attempt to 
         * configure the documentation panel to display something useful
         * about it. */
        TreePath path = evt.getNewLeadSelectionPath();
        URL docUrl = null;
        if ( path != null ) {
            Object leaf = path.getLastPathComponent();
            if ( leaf instanceof DefaultMutableTreeNode ) {
                Object item = ((DefaultMutableTreeNode) leaf).getUserObject();
                docUrl = DocNames.docURL( item );
                if ( docUrl != null ) {
                    try {
                        docPane.setPage( docUrl );
                    }
                    catch ( IOException e ) {
                        logger.info( "Trouble loading documentation at "
                                   + docUrl );
                    }
                }
                else {
                    logger.info( "No documentation for " + leaf );
                }
            }
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
     * @param   userObj  user object at node
     * @return  suitable text
     */
    public String textFor( Object userObj ) {
        if ( userObj == null ) {
            return null;
        }
        else if ( ! objLabels.containsKey( userObj ) ) {
            String text = null;
            if ( userObj instanceof Heading ) {
                text = ((Heading) userObj).getUserString();
            }
            if ( text == null ) {
                text = readTitleFromResource( DocNames.docURL( userObj ) );
            }
            if ( text == null ) {
                if ( userObj instanceof Class ) {
                    text = ((Class) userObj).getName();
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
                    text = userObj.toString();
                }
            }
            objLabels.put( userObj, text );
        }
        return (String) objLabels.get( userObj );
    }

    /**
     * Returns the icon to get used for representing a node in the tree.
     *
     * @param  userObj  user object at node
     * @return  suitable icon
     */
    public Icon iconFor( Object userObj ) {
        if ( userObj instanceof Method ) { 
            return ResourceIcon.FUNCTION_NODE;
        }
        else if ( userObj instanceof Field ) {
            return ResourceIcon.CONSTANT_NODE;
        }
        else if ( userObj instanceof Class ) {
            return ResourceIcon.LIBRARY_NODE;
        }
        else if ( userObj instanceof Heading ) {
            return ResourceIcon.FOLDER_NODE;
        }
        else if ( userObj == null ) {
            return null;
        }
        else {
            assert false;
            return null;
        }
    }

    /**
     * Returns the model used by this window's tree.  
     * This method is package-visible rather than private because it is
     * used by unit tests.
     *
     * @return  model containing things that are documented by this window
     */
    DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) tree.getModel();
    }

    /**
     * Returns the contents of the TITLE element of the HTML file at the 
     * given URL.  If no title can be found, null is returned.
     * This doesn't do a full parse, it makes some assumptions about
     * the format of the resource (title element all on one line).
     *
     * @param  url  URL
     * @return   title string read from URL
     */
    private String readTitleFromResource( URL url ) {
        if ( url == null ) {
            return null;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader( 
                     new InputStreamReader( url.openStream() ) );
            for ( String line; ( line = in.readLine() ) != null; ) {
                Matcher matcher = TITLE_PATTERN.matcher( line );
                if ( matcher.matches() ) {
                    return matcher.group( 1 );
                }
            }
            return null;
        }
        catch ( IOException e ) {
            return null;
        }
        finally {
            if ( in != null ) {
                try {
                    in.close();
                }
                catch ( IOException e ) {
                    // never mind
                }
            }
        }
    }
}
