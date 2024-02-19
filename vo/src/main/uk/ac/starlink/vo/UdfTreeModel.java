package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * Model for tree which displays TAP User Defined Function definitions.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2024
 * @see  <a href="https://www.ivoa.net/documents/TAPRegExt/20120827/REC-TAPRegExt-1.0.html#langs">TAPRegExt 1.0 sec 2.3</a>
 */
public class UdfTreeModel implements TreeModel {

    private final List<TreeModelListener> listeners_;
    private final NodeRenderer rootRenderer_;
    private final NodeRenderer nameRenderer_;
    private final NodeRenderer signatureRenderer_;
    private final NodeRenderer argRenderer_;
    private final NodeRenderer descriptionRenderer_;
    private Node root_;

    private static final Pattern FUNC_REGEX =
        Pattern.compile( " *([A-Za-z0-9_-]+) *\\((.*)\\) *" );
    private static final Pattern FORM_REGEX =
        Pattern.compile( " *([A-Za-z0-9_-]+) *\\((.*)\\) *-> *(.*?) *" );

    /**
     * Constructor.
     * The scroll pane containing the tree which this model will be
     * displayed has to be supplied, since some rendering gymnastics
     * are required when the scroll pane viewport is resized.
     *
     * @param  treeScroller   scroll pane containing tree
     */
    public UdfTreeModel( JScrollPane treeScroller ) {
        listeners_ = new ArrayList<TreeModelListener>();

        /* Arrange to re-render some of the cells when the viewport
         * width changes.  This is necessary to reflow wrapped text. */
        JViewport viewport = treeScroller.getViewport();
        viewport.addChangeListener( new ChangeListener() {
            int width_;
            public void stateChanged( ChangeEvent evt ) {
                int width = viewport.getExtentSize().width;
                if ( width != width_ ) {
                    updateTextNodes( new TreePath( root_ ) );
                    width_ = width;
                }
            }
        } );

        /* Set up renderers for different types of tree node. */
        Font labelFont =
            UIManager.getFont( "Label.font" ).deriveFont( Font.BOLD );
        Color sigColor = new Color( 0x2020d0 );
        Font sigFont =
            new Font( Font.MONOSPACED, Font.PLAIN, labelFont.getSize() );
        Border descripBorder =
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder( Color.LIGHT_GRAY, 1 ),
                BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) );
        rootRenderer_ = new NodeRenderer() {
            public boolean isWidthSensitive() {
                return false;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {
                JLabel label = dfltRendering.get();
                label.setText( node.getText() );
                label.setFont( labelFont );
                return label;
            }
        };
        Icon emptyIcon = new Icon() {
            public int getIconHeight() {
                return 1;
            }
            public int getIconWidth() {
                return 22;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
            }
        };
        argRenderer_ = new NodeRenderer() {
            public boolean isWidthSensitive() {
                return false;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {
                JLabel label = dfltRendering.get();
                label.setIcon( emptyIcon );
                return label;
            }
        };
        nameRenderer_ = new NodeRenderer() {
            public boolean isWidthSensitive() {
                return true;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {
                JLabel label = dfltRendering.get();
                Icon icon = ResourceIcon.NODE_FUNCTION;
                String text = node.getText();
                label.setIcon( icon );
                label.setFont( labelFont );
                label.setText( text );
                int extraWidth = 50 + ( icon == null ? 0 : icon.getIconWidth());
                int wrapWidth = viewport.getWidth() - extraWidth;
                int labelWidth = viewport.getGraphics()
                                .getFontMetrics( labelFont )
                                .stringWidth( text );
                if ( labelWidth > wrapWidth ) {
                    Matcher matcher = FUNC_REGEX.matcher( node.getText() );
                    if ( matcher.matches() ) {
                        int narg = matcher.group( 2 ).replaceAll( "[^,]", "" )
                                  .length() + 1;
                        String abbrev = matcher.group( 1 )
                                      + "(..." + narg + " args...)";
                        label.setText( abbrev );
                    }
                }
                return label;
            }
        };
        signatureRenderer_ = new NodeRenderer() {
            public boolean isWidthSensitive() {
                return true;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {

                /* Form is an xs:token, so whitespace is collapsed. */
                String text = node.getText().trim().replaceAll( "\\s+", " " );
                return createWrappedTextComponent( viewport, text,
                                                   ResourceIcon.NODE_SIGNATURE,
                                                   sigFont, sigColor,
                                                   (Border) null );
            }
        };
        descriptionRenderer_ = new NodeRenderer() {
            public boolean isWidthSensitive() {
                return true;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {

                /* Description is an xs:string.  It's not clear what's the
                 * best way to present this, but I've chosen to wrap text
                 * in what looks like paragraphs, but preserve paragraph
                 * breaks marked by multiple newline characters.
                 * Leading spaces are elided since it's common for text in
                 * XML content to have non-significant indentation.
                 * But this will in some cases lose intended formatting. */
                String text = node.getText().trim()
                             .replaceAll( "\\t", " " )
                             .replaceAll( " *\\n *", "\n" )
                             .replaceAll( " +", " " )
                             .replaceAll( "([^\\n])\\n([^\\n])", "$1 $2" );
                return createWrappedTextComponent( viewport, text,
                                                   ResourceIcon.NODE_DOC,
                                                   (Font) null, (Color) null,
                                                   descripBorder );
            }
        };

        /* Set the initial, empty, content of the model. */
        setUdfs( new TapLanguageFeature[ 0 ] );
    }

    /**
     * Set the content of this tree by providing TAP language features
     * which are assumed to represent UDFs.
     *
     * @param  features  features representing UDFs
     */
    public void setUdfs( TapLanguageFeature[] features ) {
        root_ = createArrayNode( "User-Defined Functions", rootRenderer_,
                                 features == null
                               ? new Node[ 0 ]
                               : Arrays.stream( features )
                                       .map( f -> createUdfNode( f ) )
                                       .toArray( n -> new Node[ n ] ) );
        TreeModelEvent evt = new TreeModelEvent( this, new Object[] { root_ } );
        for ( TreeModelListener l : listeners_ ) {
            l.treeStructureChanged( evt );
        }
    }

    public Object getRoot() {
        return root_;
    }

    public int getChildCount( Object parent ) {
        return ((Node) parent).getChildCount();
    }

    public Object getChild( Object parent, int index ) {
        return ((Node) parent).getChild( index );
    }

    public boolean isLeaf( Object node ) {
        return ((Node) node).isLeaf();
    }

    public int getIndexOfChild( Object parent, Object child ) {
        return parent == null || child == null
             ? -1
             : ((Node) parent).getChildIndex( (Node) child );
    }

    public void addTreeModelListener( TreeModelListener l ) {
        listeners_.add( l );
    }

    public void removeTreeModelListener( TreeModelListener l ) {
        listeners_.remove( l );
    }

    public void valueForPathChanged( TreePath path, Object newValue ) {
        assert false : "Tree is not editable from GUI";
    }

    /**
     * Recursive method called if the viewport may have changed width
     * to make sure that nodes with wrapping text are wrapped at
     * the right width.
     * 
     * @param   path  this node and all its descendants will be refreshed
     */
    private void updateTextNodes( TreePath path ) {
        Node node = (Node) path.getLastPathComponent();

        /* If this node has wrapped text, signal to listeners that it has
         * undergone a change.  This will trigger re-rendering appropriate
         * for the current viewport width. */
        if ( node.getRenderer().isWidthSensitive() ) {
            TreeModelEvent evt = new TreeModelEvent( this, path );
            for ( TreeModelListener l : listeners_ ) {
                l.treeNodesChanged( evt );
            }
        }

        /* Recurse. */
        int nchild = node.getChildCount();
        for ( int ic = 0; ic < nchild; ic++ ) {
            updateTextNodes( path.pathByAddingChild( node.getChild( ic ) ) );
        }
    }

    /**
     * Creates a tree node for a given feature.
     *
     * @param  feature   features assumed to represent a UDF
     * @return  tree node
     */
    private Node createUdfNode( TapLanguageFeature feature ) {
        String form = feature.getForm();
        Signature sig = createSignature( form );
        String label = sig == null ? null : sig.toCompactString();
        if ( label == null || label.trim().length() == 0 ) {
            label = form;
        }
        Arg[] args = sig.getArgs();
        int narg = args.length;
        String[] sigItems = new String[ narg + 1 ];
        for ( int i = 0; i < narg; i++ ) {
            Arg arg = args[ i ];
            sigItems[ i ] = Integer.toString( i + 1 ) + ": "
                          + arg.getArgName() + " " + arg.getArgType();
        }
        sigItems[ narg ] = "return: " + sig.getReturnType();
        Node[] argNodes = Arrays.stream( sigItems )
                         .map( txt -> createLeafNode( txt, argRenderer_ ) )
                         .toArray( n -> new Node[ n ] );
        String description = feature.getDescription();
        return createArrayNode( label, nameRenderer_, new Node[] {
            createArrayNode( form, signatureRenderer_, argNodes ),
            createLeafNode( description, descriptionRenderer_ ),
        } );
    }

    /**
     * Returns a renderer that can be used for the JTree displaying
     * this model.
     */
    public static TreeCellRenderer createRenderer() {
        return new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent( JTree tree,
                                                           Object value,
                                                           boolean isSel,
                                                           boolean isExp,
                                                           boolean isLeaf,
                                                           int irow,
                                                           boolean hasFocus ) {
                Node node = (Node) value;
                Supplier<JLabel> dfltRendering = () ->
                    (JLabel) super.getTreeCellRendererComponent( tree,
                                                                 node.getText(),
                                                                 isSel, isExp,
                                                                 isLeaf, irow,
                                                                 hasFocus );
                return node.getRenderer().renderNode( node, dfltRendering );
            }
        };
    }

    /**
     * Creates a component that can be used to render a node whose
     * content is text that may need to be wrapped to the width of the
     * containing viewport.
     *
     * @param  viewport  viewport containing the text
     * @param  text     text to display
     * @param  icon     icon to display
     * @param  font     text font
     * @param  color    text foreground colour
     * @param  border   border for component
     * @return  component for display in a tree
     */
    private static JComponent
            createWrappedTextComponent( JViewport viewport,
                                        String text, Icon icon,
                                        Font font, Color color,
                                        Border border ) {

        /* Set up a text area with the requested content and line wrapping. */
        JTextArea textArea = new JTextArea();
        textArea.setLineWrap( true );
        textArea.setWrapStyleWord( true );
        textArea.setEditable( false );
        textArea.setText( text );
        if ( font != null ) {
            textArea.setFont( font );
        }
        if ( color != null ) {
            textArea.setForeground( color );
        }

        /* Work out what width corresponds to the current available width
         * for the text component.  The constant here is obviously a bit
         * of a hack, it represents the offset from the left margin of
         * the tree at which these components are expected to appear. */
        int extraWidth = 50 + ( icon == null ? 0 : icon.getIconWidth() );
        int wrapWidth = viewport.getWidth() - extraWidth;

        /* Wrap the lines so they will fit in the required width.
         * This is done using bad magic from
         * https://stackoverflow.com/questions/4083322/.
         * It seems to work. */
        textArea.setSize( wrapWidth, 1 );
        textArea.setSize( textArea.getPreferredSize() );

        /* Add an icon etc. */
        JPanel panel = new JPanel( new BorderLayout() );
        if ( icon != null ) {
            JComponent iconBox = Box.createVerticalBox();
            JLabel iconLabel = new JLabel( icon );
            iconBox.add( iconLabel );
            iconBox.add( Box.createVerticalGlue() );
            iconBox.setOpaque( false );
            iconLabel.setOpaque( false );
            panel.add( iconBox, BorderLayout.WEST );
        }
        panel.add( textArea, BorderLayout.CENTER );
        panel.setOpaque( false );
        if ( border != null ) {
            textArea.setBorder( border );
        }

        /* Return the configured component. */
        return panel;
    }

    /**
     * Makes plain text safe for interpolation into HTML source.
     *
     * @param  txt  raw text
     * @return  escaped text
     */
    private static String escapeHtml( String txt ) {
        return txt.replace( "&", "&amp;" )
                  .replace( "<", "&lt;" )
                  .replace( ">", "&gt;" );
    }

    /**
     * Returns a tree node that does not have children.
     *
     * @param  text  text representation of node
     * @param  renderer  renderer for node
     * @return  new node
     */
    private static Node createLeafNode( String text, NodeRenderer renderer ) {
        return new Node() {
            public int getChildIndex( Node child ) {
                return -1;
            }
            public boolean isLeaf() {
                return true;
            }
            public int getChildCount() {
                return 0;
            }
            public Node getChild( int index ) {
                return null;
            }
            public String getText() {
                return text;
            }
            public NodeRenderer getRenderer() {
                return renderer;
            }
        };
    }

    /**
     * Returns a tree node that has children.
     *
     * @param   text  text representation of node
     * @param   renderer   renderer for node
     * @param   children   child nodes
     * @return   new node
     */
    private static Node createArrayNode( String text, NodeRenderer renderer,
                                         Node[] children ) {
        return new Node() {
            public int getChildIndex( Node child ) {
                return Arrays.asList( children ).indexOf( child );
            }
            public boolean isLeaf() {
                return children == null || children.length == 0;
            }
            public int getChildCount() {
                return children.length;
            }
            public Node getChild( int index ) {
                return children[ index ];
            }
            public String getText() {
                return text;
            }
            public NodeRenderer getRenderer() {
                return renderer;
            }
        };
    }

    /**
     * Parses a TAPRegExt language feature form value as a UDF signature.
     *
     * @param  form   form element from TAPRegExt language feature
     * @return   parsed signature, or null if it really doesn't look like one
     */
    static Signature createSignature( String form ) {
        Matcher matcher = FORM_REGEX.matcher( form );
        if ( matcher.matches() ) {
            String name = matcher.group( 1 );
            String[] argTxts = matcher.group( 2 ).split( "\\s*,\\s*", -1 );
            String type = matcher.group( 3 );
            Arg[] args = Arrays.stream( argTxts )
                        .map( txt -> createArg( txt ) )
                        .toArray( n -> new Arg[ n ] );
            String compactString =
                  name != null && name.trim().length() > 0
                ? new StringBuffer()
                 .append( name )
                 .append( "(" )
                 .append( Arrays.stream( args )
                                .map( Arg::getArgName )
                                .collect( Collectors.joining( ", " ) ) )
                 .append( ")" )
                 .toString()
                : form;
            return new Signature() {
                public String getName() {
                    return name;
                }
                public Arg[] getArgs() {
                    return args;
                }
                public String getReturnType() {
                    return type;
                }
                public String toCompactString() {
                    return compactString;
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Parses the argument part of a TAPRegExt UDF form.
     *
     * @param  txt  text representing argument in signature
     * @return  parsed argument
     */
    private static Arg createArg( String txt ) {
        String[] words = txt.trim().split( "\\s+", 2 );
        String name = words.length > 0 ? words[ 0 ] : null;
        String type = words.length > 1 ? words[ 1 ].replaceAll( "\\s+", " " )
                                       : null;
        return new Arg() {
            public String getArgName() {
                return name;
            }
            public String getArgType() {
                return type;
            }
        };
    }

    /**
     * Represents an entry in this tree model.
     */
    private static interface Node {

        /**
         * Gives a textual representation for this node.
         *
         * @return  text of node, may be short or long
         */
        String getText();

        /**
         * Returns a renderer for this node.
         *
         * @return  renderer
         */
        NodeRenderer getRenderer();

        /**
         * Indicates whether this node is a leaf of the tree.
         *
         * @return  false if this node type cannot have children
         */
        boolean isLeaf();

        /**
         * Returns the number of children owned by this node.
         *
         * @return  child count
         */
        int getChildCount();

        /**
         * Returns a child of this node.
         *
         * @param  index  index
         * @return  child at given index
         */
        Node getChild( int index );

        /**
         * Returns the index of a child from this parent.
         *
         * @param  child  child
         * @return   index of child, or -1 if it's not a child
         */
        int getChildIndex( Node child );
    }

    /**
     * Performs rendering for a node in this tree.
     */
    private static interface NodeRenderer {

        /**
         * Returns true if this node has text that may need to be reformatted
         * according to the width of the containing component.
         *
         * @return  true  if wrapping may be required
         */
        boolean isWidthSensitive();

        /**
         * Renders a node for display in the tree.
         *
         * @param   node  node to render
         * @return   dfltRendering  supplier for default rendering of node
         */
        Component renderNode( Node node, Supplier<JLabel> dfltRendering );
    }

    /**
     * Represents a UDF signature as represented by a TAPRegExt language
     * feature form component.
     */
    static interface Signature {

        /**
         * Returns the function name.
         *
         * @return  name
         */
        String getName();

        /**
         * Returns the declared arguments of the function.
         *
         * @return  arguments
         */
        Arg[] getArgs();

        /**
         * Returns the declared return type of the function.
         *
         * @return  return type
         */
        String getReturnType();

        /**
         * Returns a somewhat abbreviated representation of this signature.
         *
         * @return  short string
         */
        String toCompactString();
    }

    /**
     * Represents an argument in a UDF signature.
     */
    static interface Arg {

        /**
         * Returns the argument name.
         *
         * @return  name
         */
        String getArgName();

        /**
         * Returns the argument type.
         *
         * @return  type
         */
        String getArgType();
    }
}
