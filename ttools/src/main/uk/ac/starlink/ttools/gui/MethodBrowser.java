package uk.ac.starlink.ttools.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.ttools.build.HideDoc;
import uk.ac.starlink.ttools.build.Heading;

/**
 * Component displaying the static public members of a set of classes.
 * This is currently arranged as a tree.  If javadocs are available
 * (under names as described by {@link uk.ac.starlink.ttools.gui.DocNames})
 * these are displayed alongside.
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
public class MethodBrowser extends JPanel {

    private final JTree tree_;
    private final JEditorPane docPane_;
    private final Map<Object,String> objLabels_;
    private static final Pattern TITLE_PATTERN =
        Pattern.compile( ".*<title>([^<&].*?)</title>.*",
                         Pattern.CASE_INSENSITIVE );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.gui" );

    /**
     * Constructor.
     */
    public MethodBrowser() {
        super( new BorderLayout() );
        tree_ = new JTree( new DefaultMutableTreeNode() );
        objLabels_ = new HashMap<Object,String>();

        /* Arrange for suitable rendering. */
        tree_.setRootVisible( false );
        tree_.setShowsRootHandles( true );
        tree_.setCellRenderer( new DefaultTreeCellRenderer() {
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

        /* Open up the top level. */
        DefaultMutableTreeNode root = getRoot();
        TreePath rootPath = new TreePath( root );
        tree_.expandPath( rootPath );

        /* Listen to the tree. */
        tree_.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                nodeSelected( evt.getNewLeadSelectionPath() );
            }
        } );
        tree_.getSelectionModel()
             .setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

        /* Put the tree in a scrolling panel. */
        JScrollPane treeScroller = new JScrollPane( tree_ );
        treeScroller.setPreferredSize( new Dimension( 350, 450 ) );

        /* Set up a documentation viewer. */
        docPane_ = new JEditorPane() {
            boolean fontSet;
            public void paint( Graphics g ) {
                if ( ! fontSet ) {
                    fontSet = true;
                    setFont( Font.decode( "SansSerif" ) );
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                                     RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
                super.paint( g2 );
            }
        };
        docPane_.putClientProperty( JEditorPane.HONOR_DISPLAY_PROPERTIES,
                                    Boolean.TRUE );
        docPane_.setEditable( false );
        HTMLEditorKit ekit = new HTMLEditorKit();
        StyleSheet stylesheet = new StyleSheet();
        stylesheet.addStyleSheet( ekit.getStyleSheet() );
        stylesheet.addRule( "p {margin-top: 10}" );
        stylesheet.addRule( "code {color: #6F3F0F}" );  // brown
        ekit.setStyleSheet( stylesheet );
        docPane_.setEditorKit( ekit );
        docPane_.setText( getInstructions() );
        JScrollPane docScroller = new JScrollPane( docPane_ );
        docScroller.setPreferredSize( new Dimension( 500, 450 ) );

        /* Set up the main panel with a split pane. */
        JSplitPane splitter =
            new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, false,
                            treeScroller, docScroller );
        add( splitter, BorderLayout.CENTER );
    }

    /**
     * Returns the tree which displays the classes.
     *
     * @return tree
     */
    public JTree getTree() {
        return tree_;
    }

    /**
     * Returns the tree model which contains the classes for display.
     *
     * @return  tree model
     */
    public DefaultTreeModel getTreeModel() {
        return (DefaultTreeModel) tree_.getModel();
    }

    /**
     * Returns the root node of the tree.
     *
     * @return   tree root
     */
    public DefaultMutableTreeNode getRoot() {
        return (DefaultMutableTreeNode) getTreeModel().getRoot();
    }

    /**
     * Adds the static members of a set of classes to the tree root for display.
     *
     * @param   clazzes  classes for display
     */
    public void addStaticClasses( Class<?>[] clazzes ) {
        DefaultMutableTreeNode root = getRoot();
        for ( int i = 0; i < clazzes.length; i++ ) {
            addStaticClass( clazzes[ i ], root );
        }
        tree_.expandPath( new TreePath( root ) );
    }

    /**
     * Adds a new class to the tree containing available static methods.
     *
     * @param  clazz  class to add
     * @param  parent  tree node to append it to
     */
    public void addStaticClass( Class<?> clazz,
                                DefaultMutableTreeNode parent ) {

        /* Add a node based on the class itself. */
        DefaultMutableTreeNode clazzNode = new DefaultMutableTreeNode( clazz );
        getTreeModel().insertNodeInto( clazzNode, parent,
                                       parent.getChildCount() );
        parent.add( clazzNode );

        /* Add nodes based on its public static members. */
        addPublicStaticMembers( clazzNode, clazz.getDeclaredMethods() );
        addPublicStaticMembers( clazzNode, clazz.getDeclaredFields() );
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
        List<Member> mems = new ArrayList<Member>( Arrays.asList( members ) );
        for ( Iterator<Member> it = mems.iterator(); it.hasNext(); ) {
            Member mem = it.next();
            int mods = mem.getModifiers();
            if ( ! Modifier.isStatic( mods ) ||
                 ! Modifier.isPublic( mods ) ||
                 ( mem instanceof AccessibleObject &&
                   ((AccessibleObject) mem)
                       .getAnnotation( HideDoc.class ) != null ) ) {
                it.remove();
            }
        }
        Collections.sort( mems, new Comparator<Member>() {
            public int compare( Member m1, Member m2 ) {
                return m1.getName().compareTo( m2.getName() );
            }
        } );
        DefaultTreeModel model = getTreeModel();
        for ( Member mem : mems ) {
            model.insertNodeInto( new DefaultMutableTreeNode( mem ),
                                  parent, parent.getChildCount() );
        }
    }

    /**
     * Invoked when a new tree node is selected.
     */
    private void nodeSelected( TreePath path ) {

        /* Find out what object has just been selected, and attempt to
         * configure the documentation panel to display something useful
         * about it. */
        if ( path != null ) {
            Object leaf = path.getLastPathComponent();
            if ( leaf instanceof DefaultMutableTreeNode ) {
                Object item = ((DefaultMutableTreeNode) leaf).getUserObject();
                URL docUrl = DocNames.docURL( item );
                if ( docUrl != null ) {
                    try {
                        docPane_.setPage( docUrl );
                    }
                    catch ( IOException e ) {
                        logger_.warning( "Trouble loading documentation at "
                                       + docUrl );
                    }
                }
                else {
                    logger_.warning( "No documentation for " + leaf );
                    docPane_.setText( null );
                }
            }
        }
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
        else if ( ! objLabels_.containsKey( userObj ) ) {
            String text = null;
            if ( userObj instanceof Heading ) {
                text = ((Heading) userObj).getUserString();
            }
            if ( text == null ) {
                text = readTitleFromResource( DocNames.docURL( userObj ) );
            }
            if ( text == null ) {
                if ( userObj instanceof Class ) {
                    text = ((Class<?>) userObj).getName();
                }
                else if ( userObj instanceof Method ) {
                    Method method = (Method) userObj;
                    StringBuffer sbuf = new StringBuffer()
                        .append( method.getReturnType().getName() )
                        .append( ' ' )
                        .append( method.getName() )
                        .append( "( " );
                    Class<?>[] params = method.getParameterTypes();
                    for ( int i = 0; i < params.length; i++ ) {
                        if ( i > 0 ) {
                            sbuf.append( ", " );
                        }
                        sbuf.append( params[ i ].getName() );
                    }
                    sbuf.append( " )" );
                    return sbuf.toString();
                }
                else if ( userObj instanceof Field ) {
                    Field field = (Field) userObj;
                    return field.getName();
                }
                else if ( userObj != null ) {
                    text = userObj.toString();
                }
            }
            objLabels_.put( userObj, text );
        }
        return objLabels_.get( userObj );
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

    /**
     * Returns text for instructions to display in the text panel
     * when no function documentation is visible.
     *
     * @return  HTML instruction text
     */
    private static String getInstructions() {
        return new StringBuffer()
            .append( "<h1>Function Browser</h1>" )
            .append( "<p>Open tree nodes on the left by double-clicking\n" )
            .append( "to Select categories of functions.\n" )
            .append( "Clicking on the name of a function or constant\n" )
            .append( "will show details of its usage and semantics.\n" )
            .append( "</p>" )
            .toString();
    }
}
