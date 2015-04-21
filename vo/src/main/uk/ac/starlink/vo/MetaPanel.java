package uk.ac.starlink.vo;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.text.JTextComponent;

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
