package uk.ac.starlink.vo;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Panel for displaying metadata under headings.
 * It is designed to be contained in a scrollpane with vertical scrolling.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2015
 */
public class MetaPanel extends JPanel implements Scrollable {

    /**
     * Constructor.
     */
    public MetaPanel() {
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    }

    /**
     * Adds a field for displaying a single-line item.
     *
     * @param   heading   item heading text
     * @return  component whose content can be set
     */
    public JTextComponent addLineField( String heading ) {
        JTextField field = new JTextField();
        field.setEditable( false );
        field.setOpaque( false );
        field.setBorder( BorderFactory.createEmptyBorder() );
        addHeadedComponent( heading, field );
        return field;
    }

    /**
     * Adds a field for displaying a text item with potentially multiple lines.
     *
     * @param   heading   item heading text
     * @return  component whose content can be set
     */
    public JTextComponent addMultiLineField( String heading ) {
        JTextArea field = new JTextArea();
        field.setLineWrap( true );
        field.setWrapStyleWord( true );
        field.setEditable( false );
        field.setOpaque( false );
        addHeadedComponent( heading, field );
        return field;
    }

    /**
     * Adds a field for displaying a text item formatted as HTML text.
     *
     * @param   heading   item heading text
     * @return  component whose content can be set
     */
    public JTextComponent addHtmlField( String heading ) {
        JEditorPane field = new JEditorPane() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension( size.width, Math.max( size.height, 15 ) );
            }
        };
        field.setEditorKit( new HTMLEditorKit() );
        field.putClientProperty( JEditorPane.HONOR_DISPLAY_PROPERTIES, true );
        field.setEditable( false );
        field.setOpaque( false );
        addHeadedComponent( heading, field );
        return field;
    }

    /**
     * Adds a field intended to contain a clickable URL.
     * If a non-null UrlHandler is supplied, its {@link UrlHandler#clickUrl}
     * method is invoked when the user clicks on this field.
     *
     * @param  heading  item heading text
     * @param  urlHandler  handler used when the field is clicked on;
     *                     may be null
     */
    public JTextComponent addUrlField( String heading,
                                       final UrlHandler urlHandler ) {
        final JTextComponent field = addLineField( heading );
        if ( urlHandler != null ) {
            field.setForeground( new Color( 0x0000ee ) );
            try {
                field.setCursor( Cursor.getPredefinedCursor( Cursor
                                                            .HAND_CURSOR ) );
            }
            catch ( Exception e ) {
                //
            }
            field.addMouseListener( new MouseAdapter() {
                public void mouseClicked( MouseEvent evt ) {
                    String txt = field.getText();
                    if ( evt.getButton() == evt.BUTTON1 && txt != null ) {
                        try {
                            urlHandler.clickUrl( new URL( txt ) );
                        }
                        catch ( MalformedURLException e ) {
                            // no action
                        }
                    }
                }
            } );
        }
        return field;
    }

    /**
     * Sets the content of a field.
     * As well as the obvious, it makes sure that the caret stays at
     * the start.  If it doesn't do that, you end up seeing the end
     * rather than the start of long strings.
     *
     * @param  field  field
     * @param  text   new content
     */
    public void setFieldText( JTextComponent field, String text ) {
        field.setText( text );
        field.setCaretPosition( 0 );
    }

    public Dimension getPreferredScrollableViewportSize() {
        return super.getPreferredSize();
    }

    public int getScrollableUnitIncrement( Rectangle visibleRect,
                                           int orientation, int direction ) {
        return getFontMetrics( getFont() ).getHeight();
    }

    public int getScrollableBlockIncrement( Rectangle visibleRect,
                                            int orientation, int direction ) {
        return getScrollableUnitIncrement( visibleRect, orientation,
                                           direction );
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    /**
     * Adds an item to this component with a heading.
     *
     * @param  heading   item heading text
     * @param  comp   item content component
     */
    private void addHeadedComponent( String heading, JComponent comp ) {
        JComponent headLine = Box.createHorizontalBox();
        headLine.add( new JLabel( heading + ":" ) );
        headLine.add( Box.createHorizontalGlue() );
        add( headLine );
        comp.setBorder( BorderFactory.createEmptyBorder( 0, 20, 0, 0 ) );
        add( comp );
    }
}
