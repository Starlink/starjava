package uk.ac.starlink.treeview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.PrintWriter;
import java.io.Writer;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Allows display of the details of a node.  This class provides
 * a text viewing window which will do some rather simple markup
 * on some textual elements.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class DetailViewer {

    private final JTabbedPane tabbed;
    protected StyledTextArea over;

    /**
     * Constructs a blank DetailViewer, with just a heading name.
     *
     * @param  overName  the name used for the main (overview) panel
     */
    public DetailViewer( String overName ) {

        /* Construct a tabbed pane.  We have to jump through a few hoops
         * to make sure that scroll bars are always painted in the right
         * place. */
        tabbed = new JTabbedPane() {
            private ComponentListener listener;
            public void addNotify() {
                super.addNotify();
                listener = new ComponentAdapter() {
                    public void componentResized( ComponentEvent evt ) {
                        tabbed.setPreferredSize( getParent().getSize() );
                        tabbed.revalidate();
                    }
                };
                listener.componentResized( null );
                getParent().addComponentListener( listener );
            }
            public void removeNotify() {
                super.removeNotify();
                getParent().removeComponentListener( listener );
            }
        };

        /* Get the main overview pane and add it as the first item. */
        over = new StyledTextArea();
        addPane( overName, over );
    }

    /**
     * Constructs a DetailViewer which contains basic information
     * (name, node type etc) for a given DataNode.
     *
     * @param  node  the DataNode to which this viewer relates
     */
    public DetailViewer( DataNode node ) {
        this( "Overview" );

        /* Add the items which apply to all nodes. */
        over.addIcon( node.getIcon() );
        over.addSpace();
        over.addTitle( node.getLabel() ); 
        String name = node.getName();
        if ( name.trim() != "" ) {
            over.addKeyedItem( "Name", node.getName() );
        }
        over.addKeyedItem( "Node type", node.getNodeType() );
        String path = TreeviewUtil.getNodePath( node );
        if ( path != null ) {
            over.addKeyedItem( "Path", path );
        }
    }

    public JComponent getComponent() {
        return tabbed;
    }

    public void addPane( String title, Component comp ) {
        tabbed.addTab( title, comp );
    }

    /**
     * Add a new pane which will draw itself at a fixed size and should
     * be contained within scrollbars if necessary.
     */
    public void addPane( String title, final ComponentMaker maker ) {
        final Container box = new Box( BoxLayout.X_AXIS );
        addPane( title, box );
        tabbed.addChangeListener( new ChangeListener() {
            private boolean done = false;
            synchronized public void stateChanged( ChangeEvent evt ) {
                if ( tabbed.getSelectedComponent() == box && ! done ) {
                    done = true;
                    tabbed.removeChangeListener( this );
                    JComponent comp;
                    try {
                        comp = new JScrollPane( maker.getComponent() );
                    }
                    catch ( Exception e ) {
                        StyledTextArea sta = new StyledTextArea();
                        sta.addTitle( "Error" );
                        sta.setWrap( false );
                        sta.addKeyedItem( "Exception class",
                                          e.getClass().getName() );
                        sta.addKeyedItem( "Message", e.getMessage() );
                        sta.addSubHead( "Stack trace" );
                        PrintWriter pw = new PrintWriter( sta.lineAppender() );
                        e.printStackTrace( pw );
                        comp = sta;
                    }
                    box.add( comp );
                }
            }
        } );
    }

    /**
     * Add a new pane which will draw itself at a size appropriate to the
     * size of its container.  
     * The <tt>JComponent</tt> returned by <tt>maker</tt>
     * should generally have a <tt>paintComponent</tt> method
     * which senses its actual size and draws itself accordingly,
     * something like this:
     * <pre>
     *     protected void paintComponent( Graphics g ) {
     *         super.paintComponent( g );
     *         doScaledPainting( getSize() );
     *     }
     * </pre>
     * or, perhaps for efficiency, more like this:
     * <pre>
     *     private Dimension lastSize;
     *     protected void paintComponent( Graphics g ) {
     *         super.paintComponent( g );
     *         Dimension size = getSize();
     *         if ( ! size.equals( lastSize ) ) {
     *             setPreferredSize( size );
     *             reconfigureComponentToSize( size );
     *         }
     *         doPainting();
     *     }
     * </pre>
     */
    public void addScalingPane( String title, final ComponentMaker maker ) {
        final Container box = new Box( BoxLayout.X_AXIS );
        addPane( title, box );
        tabbed.addChangeListener( new ChangeListener() {
            private boolean done = false;
            synchronized public void stateChanged( ChangeEvent evt ) {
                if ( tabbed.getSelectedComponent() == box && ! done ) {
                    done = true;
                    tabbed.removeChangeListener( this );
                    JComponent comp;
                    try {
                        comp = maker.getComponent();
                    }
                    catch ( Exception e ) {
                        StyledTextArea sta = new StyledTextArea();
                        sta.addTitle( "Error" );
                        sta.setWrap( false );
                        sta.addKeyedItem( "Exception class",
                                          e.getClass().getName() );
                        sta.addKeyedItem( "Message", e.getMessage() );
                        sta.addSubHead( "Stack trace" );
                        PrintWriter pw = new PrintWriter( sta.lineAppender() );
                        e.printStackTrace( pw );
                        comp = new JScrollPane( sta );
                    }
                    box.add( comp );
                }
            }
        } );
    }

    /**
     * Selects the numbered pane for display as if the user had selected
     * the tab; number 0 is the initial
     * overview pane, and the others are in the order they were added.
     *
     * @param  index  the index of the pane to select
     */
    public void setSelectedIndex( int index ) {
        tabbed.setSelectedIndex( index );
    }

    /**
     * Adds an array of Actions to be invoked to the viewer.  These will
     * typically be added as a set of JButtons under a heading at the
     * current place in the output; therefore this method should normally
     * only be called once, with all the actions which are to be added,
     * rather than once for each action.
     * <p>
     * As a convenience, null elements of the array will be ignored, and 
     * if there are no non-null elements the effect will be the same
     * as not making the call at all.
     *
     * @param  acts   an array of actions which may be invoked.  Null actions
     *                are ignored
     */
    public void addActions( Action[] acts ) {
        int nact = 0;
        for ( int i = 0; i < acts.length; i++ ) {
            if ( acts[ i ] != null ) {
                nact++;
            }
        }
        if ( acts != null && nact > 0 ) {
            over.addSeparator();
            over.addTitle( "External programs" );
            for ( int i = 0; i < acts.length; i++ ) {
                if ( acts[ i ] != null ) {
                    Action act = acts[ i ];
                    over.addAction( act );
                    if ( i < acts.length - 1 ) {
                        over.addSpace();
                    }
                }
            }
        }
    }

    public void addTitle( String title ) {
        over.addTitle( title );
    }

    public void addSubHead( String text ) {
        over.addSubHead( text );
    }

    public void addKeyedItem( String name, String value ) {
        over.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, Object value ) {
        addKeyedItem( name, value == null ? "null" : value.toString() );
    }

    public void addKeyedItem( String name, double value ) {
        over.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, float value ) {
        over.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, long value ) {
        over.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, int value ) {
        over.addKeyedItem( name, value );
    }

    public void addKeyedItem( String name, boolean value ) {
        over.addKeyedItem( name, value );
    }

    public void logError( Throwable th ) {
        over.logError( th );
    }

    public void addSeparator() {
        over.addSeparator();
    }

    public void addText( String text ) {
        over.addText( text );
    }

    public void addIcon( Icon icon ) {
        over.addIcon( icon );
    }

    public void addSpace() {
        over.addSpace();
    }

    /** 
     * Returns a Writer object into which text for display in the detail
     * viewer can be written.
     *
     * @return            a Writer object via which text can be inserted into 
     *                    the detail viewer
     */
    public Writer lineAppender() {
        return over.limitedLineAppender( 0 );
    }

    /**
     * Returns a Writer object into which a limited number of lines of text
     * for display in the detail viewer window can be written.  
     *
     * @param   maxLines  the maximum number of lines which can be written
     *                    before the Writer will throw a
     *                    DetailViewer.MaxLinesWrittenException
     * @return            a Writer object via which text can be inserted into 
     *                    the detail viewer
     */
    public Writer limitedLineAppender( int maxLines ) {
        return over.limitedLineAppender( maxLines );
    }
}
