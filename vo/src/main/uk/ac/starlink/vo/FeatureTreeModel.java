package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
 * Model for tree which displays ADQL language features.
 * This includes standard and user-defined functions,
 * as well as other mandatory and declared language features.
 *
 * @author   Mark Taylor
 * @since    19 Feb 2024
 * @see  <a href="https://www.ivoa.net/documents/TAPRegExt/20120827/REC-TAPRegExt-1.0.html#langs">TAPRegExt 1.0 sec 2.3</a>
 */
public class FeatureTreeModel implements TreeModel {

    private final List<TreeModelListener> listeners_;
    private final NodeRenderer rootRenderer_;
    private final NodeRenderer categoryRenderer_;
    private final NodeRenderer functionRenderer_;
    private final NodeRenderer featureRenderer_;
    private final NodeRenderer signatureRenderer_;
    private final NodeRenderer plainRenderer_;
    private final NodeRenderer descriptionRenderer_;
    private TapCapability tcap_;
    private AdqlVersion adqlVersion_;
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
    public FeatureTreeModel( JScrollPane treeScroller ) {
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
        Font labelFont = UIManager.getFont( "Label.font" );
        Color sigColor = new Color( 0x2020d0 );
        Color descripColor = new Color( 0x707070 );
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
                label.setText( node.text_ );
                return label;
            }
        };
        categoryRenderer_ = new NodeRenderer() {
            public boolean isWidthSensitive() {
                return false;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {
                JLabel label = dfltRendering.get();
                label.setText( node.text_
                             + " (" + node.children_.size() + ")" );
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
        plainRenderer_ = new NodeRenderer() {
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
        functionRenderer_ = new NodeRenderer() {
            private final Icon icon_ = ResourceIcon.NODE_FUNCTION;
            public boolean isWidthSensitive() {
                return true;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {
                JLabel label = dfltRendering.get();
                String text = node.text_;
                label.setIcon( icon_ );
                label.setText( text );
                int wrapWidth = viewport.getWidth()
                              - getIndentX( node ) - icon_.getIconWidth();
                int labelWidth = viewport.getGraphics()
                                .getFontMetrics( label.getFont() )
                                .stringWidth( text );
                if ( labelWidth > wrapWidth ) {
                    Matcher matcher = FUNC_REGEX.matcher( node.text_ );
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
        featureRenderer_ = new NodeRenderer() {
            private final Icon icon_ = ResourceIcon.NODE_FEATURE;
            public boolean isWidthSensitive() {
                return false;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {
                JLabel label = dfltRendering.get();
                label.setIcon( icon_ );
                return label;
            }
        };
        signatureRenderer_ = new NodeRenderer() {
            private final Icon icon_ = ResourceIcon.NODE_SIGNATURE;
            public boolean isWidthSensitive() {
                return true;
            }
            public Component renderNode( Node node,
                                         Supplier<JLabel> dfltRendering ) {

                /* Form is an xs:token, so whitespace is collapsed. */
                String text = node.text_.trim().replaceAll( "\\s+", " " );
                return createWrappedTextComponent( viewport, node, text, icon_,
                                                   sigFont, sigColor,
                                                   (Border) null );
            }
        };
        descriptionRenderer_ = new NodeRenderer() {
            private final Icon icon_ = ResourceIcon.NODE_DOC;
            private final Font font_ = UIManager.getFont( "TextArea.font" );
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
                String text = node.text_.trim()
                             .replaceAll( "\\t", " " )
                             .replaceAll( " *\\n *", "\n" )
                             .replaceAll( " +", " " )
                             .replaceAll( "([^\\n])\\n([^\\n])", "$1 $2" );
                return createWrappedTextComponent( viewport, node, text, icon_,
                                                   font_, descripColor,
                                                   descripBorder );
            }
        };

        /* Set the initial, empty, content of the model. */
        adqlVersion_ = AdqlVersion.V20;
        tcap_ = null;
        updateContent();
    }

    /**
     * Set the content of this tree according to a supplied TapCapability.
     * 
     * @param  tcap  capability
     */
    public void setCapability( TapCapability tcap ) {
        tcap_ = tcap;
        updateContent();
    }

    /**
     * Sets the ADQL version for which this tree should display information.
     *
     * @param  adqlVersion  version
     */
    public void setAdqlVersion( AdqlVersion adqlVersion ) {
        if ( ! Objects.equals( adqlVersion, adqlVersion_ ) ) {
            adqlVersion_ = adqlVersion;
            updateContent();
        }
    }

    public Object getRoot() {
        return root_;
    }

    public int getChildCount( Object parent ) {
        return ((Node) parent).children_.size();
    }

    public Object getChild( Object parent, int index ) {
        return ((Node) parent).children_.get( index ) ;
    }

    public boolean isLeaf( Object node ) {
        return ((Node) node).children_.isEmpty();
    }

    public int getIndexOfChild( Object parent, Object child ) {
        return parent == null || child == null
             ? -1
             : ((Node) parent).children_.indexOf( child );
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
     * Populates the tree according to the current configuration.
     * Should be called if the content (TAP capabilities or ADQL version)
     * may have changed.
     */
    private void updateContent() {

        /* Build function subtree. */
        Node mathNode = createFunctionsNode( "ADQL Maths",
                                             AdqlFeature.getMathsFunctions() );
        Node trigNode = createFunctionsNode( "ADQL Trig",
                                             AdqlFeature.getTrigFunctions() );
        Node geomNode = createFunctionsNode(
            "ADQL " + adqlVersion_.getNumber() + " Geometry",
            AdqlFeature.getGeomFunctions( adqlVersion_, tcap_ ) );
        Node optfuncNode = AdqlVersion.V20.equals( adqlVersion_ )
                     ? null
                     : createFunctionsNode(
                          "ADQL " + adqlVersion_.getNumber() + " Optional",
                          AdqlFeature.getOptionalFunctions( tcap_ ) );
        Node udfNode = new Node( "Service-specific UDFs", categoryRenderer_ );
        TapLanguageFeature[] udfFeatures =
            Arrays.stream( tcap_ == null ? new TapLanguage[ 0 ]
                                         : tcap_.getLanguages() )
           .flatMap( lang -> lang.getFeaturesMap().entrySet().stream() )
           .filter( entry -> AdqlFeature.UDF_FILTER.test( entry.getKey() ) )
           .flatMap( entry -> Arrays.stream( entry.getValue() ) )
           .toArray( n -> new TapLanguageFeature[ n ] );
        for ( TapLanguageFeature feat : udfFeatures ) {
            udfNode.addChild( createUdfNode( feat ) );
        }
        Node functionNode = new Node( "Functions", rootRenderer_ );
        functionNode.addChild( udfNode );
        functionNode.addChild( geomNode );
        if ( optfuncNode != null ) {
            functionNode.addChild( optfuncNode );
        }
        functionNode.addChild( mathNode );
        functionNode.addChild( trigNode );

        /* Build feature subtree. */
        Node optFeatureNode = AdqlVersion.V20.equals( adqlVersion_ )
            ? null
            : createFeaturesNode( "ADQL " + adqlVersion_.getNumber() +
                                  " Optional",
                                  AdqlFeature.getOptionalFeatures( tcap_ ) );
        Node customFeatureNode =
            new Node( "Service-specific", categoryRenderer_ );
        Node[] customFeatureNodes =
            Arrays.stream( tcap_ == null ? new TapLanguage[ 0 ]
                                         : tcap_.getLanguages() )
           .flatMap( lang -> lang.getFeaturesMap().entrySet().stream() )
           .filter( entry -> AdqlFeature.NONSTD_FILTER.test( entry.getKey() ) )
           .flatMap( e -> Arrays.stream( e.getValue() )
                         .map( f -> createCustomFeatureNode( e.getKey(), f ) ) )
           .toArray( n -> new Node[ n ] );

        for ( Node node : customFeatureNodes ) {
            customFeatureNode.addChild( node );
        }
        Node featureNode = new Node( "Features", rootRenderer_ );
        featureNode.addChild( customFeatureNode );
        if ( optFeatureNode != null ) {
            featureNode.addChild( optFeatureNode );
        }

        /* Update root node content. */
        root_ = new Node( "Language Variant", rootRenderer_ );
        root_.addChild( functionNode );
        root_.addChild( featureNode );
        TreeModelEvent evt = new TreeModelEvent( this, new Object[] { root_ } );
        for ( TreeModelListener l : listeners_ ) {
            l.treeStructureChanged( evt );
        }
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
        if ( node.renderer_.isWidthSensitive() ) {
            TreeModelEvent evt = new TreeModelEvent( this, path );
            for ( TreeModelListener l : listeners_ ) {
                l.treeNodesChanged( evt );
            }
        }

        /* Recurse. */
        for ( Node child : node.children_ ) {
            updateTextNodes( path.pathByAddingChild( child ) );
        }
    }

    /**
     * Creates a tree node representing a UDF.
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
        Node signatureNode = new Node( form, signatureRenderer_ );
        if ( sig != null ) {
            int iarg = 0;
            for ( Arg arg : sig.getArgs() ) {
                String argTxt = Integer.toString( ++iarg ) + ": "
                              + arg.getArgName() + " " + arg.getArgType();
                signatureNode.addChild( new Node( argTxt, plainRenderer_ ) );
            }
            signatureNode.addChild( new Node( "return: " + sig.getReturnType(),
                                              plainRenderer_ ) );
        }
        Node udfNode = new Node( label, functionRenderer_ );
        udfNode.addChild( signatureNode );
        udfNode.addChild( new Node( feature.getDescription(),
                                    descriptionRenderer_ ) );
        return udfNode;
    }

    /**
     * Creates a node representing a non-standard feature.
     *
     * @param   ivoid  feature type
     * @param   feature   feature object
     * @return  new node
     */
    private Node createCustomFeatureNode( Ivoid ivoid,
                                          TapLanguageFeature feature ) {
        Node node = new Node( feature.getForm(), featureRenderer_ );
        if ( ivoid != null ) {
            node.addChild( new Node( ivoid.toString(), plainRenderer_ ) );
        }
        node.addChild( new Node( feature.getDescription(),
                                 descriptionRenderer_ ) );
        return node;
    }

    /**
     * Creates a node to display a group of more or less standard
     * ADQL functions.
     *
     * @param   name    node name
     * @param  funcs   child functions in the node
     */
    private Node createFunctionsNode( String name,
                                      AdqlFeature.Function[] funcs ) {
        Node funcsNode = new Node( name, categoryRenderer_ );
        for ( AdqlFeature.Function func : funcs ) {
            String fname = func.getName();
            AdqlFeature.Arg[] args = func.getArgs();
            int narg = args.length;
            StringBuffer lbuf = new StringBuffer( fname ).append( '(' );
            StringBuffer sbuf = new StringBuffer( fname ).append( '(' );
            for ( int iarg = 0; iarg < narg; iarg++ ) {
                AdqlFeature.Arg arg = args[ iarg ];
                if ( iarg > 0 ) {
                    lbuf.append( ", " );
                    sbuf.append( ", " );
                }
                lbuf.append( arg.getName() );
                sbuf.append( arg.getName() );
                if ( arg.getType() != null ) {
                    sbuf.append( ' ' )
                        .append( arg.getType() );
                }
            }
            lbuf.append( ')' );
            sbuf.append( ") -> " )
                .append( func.getReturnType() );
            String label = lbuf.toString();
            String sig = sbuf.toString();
            Node funcNode = new Node( label, functionRenderer_ );
            funcNode.addChild( new Node( sig, signatureRenderer_ ) );
            funcNode.addChild( new Node( func.getDescription(),
                                         descriptionRenderer_ ) );
            funcsNode.addChild( funcNode );
        }
        return funcsNode;
    }

    /**
     * Creates a node containing a number of non-function
     * language features.
     *
     * @param  name  node name
     * @param  features   features to form children
     */
    private Node createFeaturesNode( String name, AdqlFeature[] features ) {
        Node featsNode = new Node( name, categoryRenderer_ );
        for ( AdqlFeature feat : features ) {
            Node featNode = new Node( feat.getName(), featureRenderer_ );
            featNode.addChild( new Node( feat.getDescription(),
                                         descriptionRenderer_ ) );
            featsNode.addChild( featNode );
        }
        return featsNode;
    }

    /**
     * Returns a renderer that can be used for the JTree displaying
     * this model.
     *
     * @return   renderer
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
                                                                 node.text_,
                                                                 isSel, isExp,
                                                                 isLeaf, irow,
                                                                 hasFocus );
                return node.renderer_.renderNode( node, dfltRendering );
            }
        };
    }

    /**
     * Creates a component that can be used to render a node whose
     * content is text that may need to be wrapped to the width of the
     * containing viewport.
     *
     * @param  viewport  viewport containing the text
     * @param  node     node to be rendered
     * @param  text     text to display
     * @param  icon     icon to display
     * @param  font     text font
     * @param  color    text foreground colour
     * @param  border   border for component
     * @return  component for display in a tree
     */
    private static JComponent
            createWrappedTextComponent( JViewport viewport, Node node,
                                        String text, Icon icon,
                                        Font font, Color color,
                                        Border border ) {

        /* Set up a suitable text display component. */
        JLabel textLabel = new JLabel();
        if ( font != null ) {
            textLabel.setFont( font );
        }
        if ( color != null ) {
            textLabel.setForeground( color );
        }

        /* Wrap the lines so they will fit in the required width.
         * HTML rendering is a semi-documented feature of (some?) Swing
         * components.  This approach was adopted from a suggestion at
         * https://stackoverflow.com/questions/4083322/.  That page also
         * suggests another way round this (used in an earlier version)
         * which seems to work as well but looks even hairier. */
        int wrapWidth = viewport.getWidth() - getIndentX( node );
        if ( icon != null ) {
            wrapWidth -= icon.getIconWidth();
        }
        textLabel.setText(
            new StringBuffer()
           .append( "<html><body width='" )
           .append( wrapWidth )
           .append( "'><p>" )
           .append( escapeHtml( text ) )
           .append( "</p></body></html>" )
           .toString()
        );

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
        panel.add( textLabel, BorderLayout.CENTER );
        panel.setOpaque( false );
        if ( border != null ) {
            textLabel.setBorder( border );
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
     * Returns the horizontal position in the JTree window at which
     * the text part of a node will start.
     *
     * @param   node  node to display
     * @return  horizontal indent of node text, in pixels
     */
    private static int getIndentX( Node node ) {

        /* This number is a bit of a guess.  It works OK for me on linux. */
        int indentPerLevel = 24;
        int indent = 0;
        while ( ( node = node.parent_ ) != null ) {
            indent += indentPerLevel;
        }
        return indent;
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
    private static class Node {

        final String text_;
        final NodeRenderer renderer_;
        final List<Node> children_;
        Node parent_;
        private final List<Node> mutableChildren_;  // don't use it

        /**
         * Constructor.
         *
         * @param  text  node text
         * @param  renderer  node renderer
         */
        public Node( String text, NodeRenderer renderer ) {
            text_ = text;
            renderer_ = renderer;
            mutableChildren_ = new ArrayList<Node>();
            children_ = Collections.unmodifiableList( mutableChildren_ );
        }

        /**
         * Adds a child to this node.
         *
         * @param  child  new child
         */
        public void addChild( Node child ) {
            child.parent_ = this;
            mutableChildren_.add( child );
        }
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
