package uk.ac.starlink.treeview;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;

/**
 * Allows display of the details of a node.  This class provides
 * a text viewing window which will do some rather simple markup
 * on some textual elements.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class DetailViewer {

    private StyledDocument doc;
    private final JTabbedPane tabbed;
    private JTextPane over;

    public DetailViewer() {

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

        /* The following magic gets a JTextPane which will not wrap lines. */
        over = new JTextPane() {
            public boolean getScrollableTracksViewportWidth() {
                return ( getSize().width < getParent().getSize().width );
            }
            public void setSize( Dimension d ) {
                if ( d.width < getParent().getSize().width ) {
                    d.width = getParent().getSize().width;
                }
                super.setSize( d );
            }
        };
 
        doc = new DefaultStyledDocument();
        over.setDocument( doc );

        over.setEditable( false );
        Style defStyle = StyleContext.getDefaultStyleContext()
                                     .getStyle( StyleContext.DEFAULT_STYLE );

        /* Set up some styles. */
        Style normalStyle = over.addStyle( "normal", defStyle );
        StyleConstants.setFontFamily( normalStyle, "Bitstream Charter" );

        Style titleStyle = over.addStyle( "title", normalStyle );
        StyleConstants.setUnderline( titleStyle, true );
        StyleConstants.setFontSize( titleStyle, 
                                    StyleConstants
                                   .getFontSize( titleStyle ) + 2 );

        Style screedStyle = over.addStyle( "screed", normalStyle );
        StyleConstants.setFontFamily( screedStyle, "Monospaced" );

        Style nameStyle = over.addStyle( "itemname", normalStyle );
        StyleConstants.setBold( nameStyle, true );

        Style valueStyle = over.addStyle( "itemvalue", normalStyle );

        Style headStyle = over.addStyle( "heading", normalStyle );
        StyleConstants.setUnderline( headStyle, true );

        Style errorBodyStyle = over.addStyle( "errorbody", normalStyle );
        StyleConstants.setItalic( errorBodyStyle, true );

        Style errorHeadStyle = over.addStyle( "errorhead", errorBodyStyle );
        StyleConstants.setBold( errorHeadStyle, true );

        /* Add the overview to the tabbed pane. */
        addPane( "Overview", over );
    }

    public DetailViewer( DataNode node ) {
        this();
        Style iconStyle = over.addStyle( "icon", over.getStyle( "title" ) );
        StyleConstants.setIcon( iconStyle, node.getIcon() );
        append( "icon", " " );
        addTitle( node.getLabel() ); 
        String name = node.getName();
        if ( name.trim() != "" ) {
           addKeyedItem( "Name", node.getName() );
        }
        addKeyedItem( "Node type", node.getNodeType() );
    }

    public JComponent getComponent() {
        return tabbed;
    }

    public void addPane( String title, Component comp ) {
        tabbed.addTab( title, comp );
    }

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
                        comp = new JTextArea( e.toString() );
                        e.printStackTrace();
                    }
                    box.add( comp );
                }
            }
        } );
    }

    public void addPane( String title, final ComponentMaker2 maker ) {
        final Container ybox = new Box( BoxLayout.Y_AXIS );
        addPane( title, ybox );
        tabbed.addChangeListener( new ChangeListener() {
            private boolean done = false;
            synchronized public void stateChanged( ChangeEvent evt ) {
                if ( tabbed.getSelectedComponent() == ybox && ! done ) {
                    done = true;
                    tabbed.removeChangeListener( this );
                    try {
                        JComponent[] components = maker.getComponents();
                        ybox.add( components[ 0 ] );
                        ybox.add( new JScrollPane( components[ 1 ] ) );
                        ybox.add( Box.createGlue() );
                    }
                    catch ( Exception e ) {
                        JComponent comp = new JTextArea( e.toString() );
                        e.printStackTrace();
                    }
                }
            }
        } );
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
            addSeparator();
            addTitle( "External programs" );
            Style buttstyle = over.addStyle( "button", 
                                             over.getStyle( "title" ) );
            for ( int i = 0; i < acts.length; i++ ) {
                if ( acts[ i ] != null ) {
                    Action act = acts[ i ];
                    JButton butt = new JButton( act );
                    StyleConstants.setComponent( buttstyle, butt );
                    append( "button", " " );
                    if ( i < acts.length - 1 ) {
                        append( "default", "   " );
                    }
                }
            }
        }
    }

    public void addTitle( String title ) {
        append( "title", title + "\n" );
        addSeparator();
    }

    public void addSubHead( String text ) {
        addSeparator();
        append( "heading", text + "\n" );
    }

    public void addKeyedItem( String name, String value ) {
        append( "itemname", name + ":  " );
        append( "itemvalue", value + "\n" );
    }

    public void logError( Throwable th ) {
        append( "normal", "\n" );
        append( "errorhead", "Error:  " );
        append( "errorbody", th.getMessage() );
        append( "normal", "\n" );
        th.printStackTrace();
    }

    public void addSeparator() {
        append( "normal", "\n" );
    }

    public void addText( String text ) {
        append( "screed", text + "\n" );
    }

    /** 
     * Returns a Writer object into which text for display in the detail
     * viewer can be written.
     *
     * @return            a Writer object via which text can be inserted into 
     *                    the detail viewer
     */
    public Writer lineAppender() {
        return limitedLineAppender( 0 );
    }

    /**
     * Returns a Writer object into which a limited number of lines of text
     * for display in the detail viewer window can be written.  
     * If more than maxLines lines are 
     * written, a {@link DetailViewer.MaxLinesWrittenException} will be
     * thrown by the <code>write</code> method of the returned Writer
     * object responsible for exceeding the limit.
     *
     * @param   maxLines  the maximum number of lines which can be written
     *                    before the Writer will throw a
     *                    DetailViewer.MaxLinesWrittenException
     * @return            a Writer object via which text can be inserted into 
     *                    the detail viewer
     */
    public Writer limitedLineAppender( final int maxLines ) {
        return new Writer() {
            private int nline = 0;
            public void write( char[] cbuf, int off, int len ) 
                        throws MaxLinesWrittenException {
                String buf = new String( cbuf, off, len );
                boolean truncate = false;
                int pos = 0;
                if ( maxLines > 0 ) {
                    while ( ( pos = buf.indexOf( '\n', pos ) + 1 ) > 0 ) {
                        nline++;
                        if ( nline >= maxLines && pos < buf.length() ) {
                            buf = buf.substring( 0, pos );
                            truncate = true;
                        }
                    }
                }
                append( "screed", buf );
                if ( truncate ) {
                    throw new MaxLinesWrittenException();
                }
            }
            public void flush() {}
            public void close() {}
        };
    }

    /* Exception thrown by limitedLineAppender. */
    public static class MaxLinesWrittenException extends IOException {
        MaxLinesWrittenException() {}
    }

    public JTextArea addTextArea() {
        JTextArea ta = new JTextArea();
        ta.setLineWrap( false );
        JScrollPane sp = new JScrollPane( ta );
        Style componentStyle = over.addStyle( "component", 
                                              over.getStyle( "normal" ) );
        StyleConstants.setComponent( componentStyle, sp );
        append( "normal", "\n" );
        append( "component", " " );
        append( "normal", "\n" );
        over.setCaretPosition( 0 );
        return ta;
    }

    private void append( String sname, String text ) {
        try{
            doc.insertString( doc.getLength(), text, over.getStyle( sname ) );
        }
        catch ( BadLocationException e ) {
            throw new IllegalStateException( e.toString() );
        }
    }
}
