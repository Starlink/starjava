package uk.ac.starlink.datanode.viewers;

import java.awt.Component;
import java.awt.Dimension;
import java.io.Writer;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class StyledTextArea extends JTextPane {

    private StyledDocument doc;
    private boolean wrap;

    public StyledTextArea() {
        super();

        doc = new DefaultStyledDocument();
        setDocument( doc );

        setEditable( false );
        Style defStyle = StyleContext.getDefaultStyleContext()
                                     .getStyle( StyleContext.DEFAULT_STYLE );

        /* Set up some styles. */
        Style normalStyle = addStyle( "normal", defStyle );
        StyleConstants.setFontFamily( normalStyle, "Bitstream Charter" );

        Style titleStyle = addStyle( "title", normalStyle );
        StyleConstants.setUnderline( titleStyle, true );
        StyleConstants.setFontSize( titleStyle,
                                    StyleConstants
                                   .getFontSize( titleStyle ) + 2 );

        Style screedStyle = addStyle( "screed", normalStyle );
        StyleConstants.setFontFamily( screedStyle, "Monospaced" );

        Style nameStyle = addStyle( "itemname", normalStyle );
        StyleConstants.setBold( nameStyle, true );

        Style valueStyle = addStyle( "itemvalue", normalStyle );

        Style headStyle = addStyle( "heading", normalStyle );
        StyleConstants.setUnderline( headStyle, true );

        Style errorBodyStyle = addStyle( "errorbody", normalStyle );
        StyleConstants.setItalic( errorBodyStyle, true );

        Style errorHeadStyle = addStyle( "errorhead", errorBodyStyle );
        StyleConstants.setBold( errorHeadStyle, true );

        Style iconStyle = addStyle( "icon", getStyle( "title" ) );
        Style buttStyle = addStyle( "component", getStyle( "title" ) );
    }

    private void append( String sname, String text ) {
        try{
            doc.insertString( doc.getLength(), text, getStyle( sname ) );
        }
        catch ( BadLocationException e ) {
            throw new IllegalStateException( e.toString() );
        }
    }

    /**
     * Determines whether text will be wrapped.
     *
     * @param  wrap  true iff you want text to be wrapped.
     */
    public void setWrap( boolean wrap ) {
        this.wrap = wrap;
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

        /* Use non-breaking spaces to stop the line being broken in 
         * bad places. */
        append( "itemname", name.replace( ' ', '\u00a0' ) + ":\u00a0\u00a0" );
        append( "itemvalue", value + "\n" );
    }

    public void addKeyedItem( String name, Object value ) {
        addKeyedItem( name, value.toString() );
    }

    public void addKeyedItem( String name, int value ) {
        addKeyedItem( name, Integer.toString( value ) );
    }

    public void addKeyedItem( String name, long value ) {
        addKeyedItem( name, Long.toString( value ) );
    }

    public void addKeyedItem( String name, double value ) {
        addKeyedItem( name, Double.toString( value ) );
    }

    public void addKeyedItem( String name, float value ) {
        addKeyedItem( name, Float.toString( value ) );
    }

    public void addKeyedItem( String name, boolean value ) {
        addKeyedItem( name, value ? "yes" : "no" );
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

    public void addIcon( Icon icon ) {
        Style iconStyle = doc.getStyle( "icon" );
        StyleConstants.setIcon( iconStyle, icon );
        append( "icon", " " );
    }

    public void addAction( Action act ) {
        addComponent( new JButton( act ) );
    }

    public void addComponent( Component comp ) {
        Style compStyle = doc.getStyle( "component" );
        StyleConstants.setComponent( compStyle, comp );
        append( "component", " " );
    }

    public void addSpace() {
        append( "default", "  " );
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
            private boolean truncate = false;
            public void write( char[] cbuf, int off, int len ) {
                if ( truncate ) {
                    return;
                }
                String buf = new String( cbuf, off, len );
                int pos = 0;
                if ( maxLines > 0 ) {
                    while ( ( pos = buf.indexOf( '\n', pos ) + 1 ) > 0 ) {
                        nline++;
                        if ( nline >= maxLines && pos < buf.length() ) {
                            buf = buf.substring( 0, pos );
                            truncate = true;
                            break;
                        }
                    }
                }
                StyledTextArea.this.append( "screed", buf );
                if ( truncate ) {
                    StyledTextArea.this.append( "screed", "   ...." );
                }
            }
            public void flush() {}
            public void close() {}
        };
    }


    /**
     * Ensures that lines will not be wrapped.
     */
    public boolean getScrollableTracksViewportWidth() {
        return wrap || ( getSize().width < getParent().getSize().width );
    }

    /**
     * Ensures that lines will not be wrapped.
     */
    public void setSize( Dimension d ) {
        int pwidth = getParent().getSize().width;
        if ( d.width < pwidth || wrap && d.width > pwidth ) {
            d.width = pwidth;
        }
        super.setSize( d );
    }

}
