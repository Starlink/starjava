package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * Panel for displaying metadata under headings.
 *
 * @author   Mark Taylor
 * @since    16 Feb 2015
 */
public class MetaPanel extends JPanel {

    private final JComponent vbox_;

    /**
     * Constructor.
     */
    public MetaPanel() {
        super( new BorderLayout() );
        vbox_ = Box.createVerticalBox();
        add( vbox_, BorderLayout.NORTH );
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
        JEditorPane field = new JEditorPane();
        field.setEditable( false );
        field.setOpaque( false );
        addHeadedComponent( heading, field );
        return field;
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
        vbox_.add( headLine );
        JComponent cLine = Box.createHorizontalBox();
        cLine.add( Box.createHorizontalStrut( 20 ) );
        cLine.add( comp );
        vbox_.add( cLine );
    }
}
