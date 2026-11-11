package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JTextArea;

/**
 * Window for defining up a mutually exclusive group of subsets
 * based on the values of a given table expression.
 *
 * @author   Fergus Baker
 * @since    06 Mar 2026
 */
public class CellViewWindow extends AuxWindow {
    String cellString_;
    JTextArea textArea_;

    /**
     * Constructor.
     *
     * Initialises the Cell View window without any display text.
     *
     * @param title The title for this window.
     * @param parent The parent component.
     */
    @SuppressWarnings("this-escape")
    public CellViewWindow( String title, Component parent ) {
        super(title, parent);
        textArea_ = new JTextArea( 5, 25 );
        textArea_.setEditable( false );
        textArea_.setLineWrap( true );
        textArea_.setWrapStyleWord( true );

        /* These cause the this-escape warning, but are perfectly safe in this
         * context. */
        setPreferredSize( new Dimension( 300, 200 ) );
        getContentPane().add( textArea_ );
        addHelp( null );
    }

    /**
     * Used to set the text to display in the Cell View window.
     *
     * @param text Text to display in this component.
     */
    public void setText( String text ) {
        textArea_.selectAll();
        textArea_.replaceSelection( text );
    }
}
