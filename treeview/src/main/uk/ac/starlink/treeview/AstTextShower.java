package uk.ac.starlink.treeview;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import uk.ac.starlink.ast.*;

/**
 * Provides a JComponent showing giving the textual representation of
 * an AST object.
 */
public class AstTextShower extends JPanel {

    /**
     * Creates an AstTextShower.
     *
     * @param  astob  the AstObject to display
     */
    public AstTextShower( AstObject astob ) {

        /* Set layout for this component. */
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

        /* Make containers to put controls into. */
        Box commbox = new Box( BoxLayout.X_AXIS );
        Box fullbox = new Box( BoxLayout.X_AXIS );
        Box textbox = new Box( BoxLayout.X_AXIS );

        /* Construct the text area view and model. */
        final JTextArea ta = new JTextArea();
        final Document doc = ta.getDocument();
        ta.setEditable( false );
        textbox.add( ta );
        textbox.add( Box.createGlue() );

        /* Set up a channel for writing AstObjects into the text area. */
        final Channel chan = new Channel() {
            public void sink( String line ) {
                ta.append( line + "\n" );
            }
        };

        /* Set up a check box for whether comments are shown. */
        final JCheckBox commbutt = new JCheckBox();
        commbutt.setSelected( chan.getComment() );
        commbox.add( new JLabel( "Display comments: " ) );
        commbox.add( commbutt );
        commbox.add( Box.createGlue() );

        /* Set up radio buttons for level of detail. */
        ButtonGroup bgrp = new ButtonGroup();
        JRadioButton bm = new JRadioButton( "Minimum" );
        JRadioButton b0 = new JRadioButton( "Normal" );
        JRadioButton bp = new JRadioButton( "Maximum" );
        bm.setActionCommand( "-1" );
        b0.setActionCommand( "0" );
        bp.setActionCommand( "1" );
        bgrp.add( bm );
        bgrp.add( b0 );
        bgrp.add( bp );
        fullbox.add( new JLabel( "Level of detail: " ) );
        fullbox.add( bm );
        fullbox.add( b0 );
        fullbox.add( bp );
        fullbox.add( Box.createGlue() );

        /* Construct a listener to write the object down the channel
         * whenever one of the buttons is pushed. */
        final AstObject astobject = astob;
        ActionListener listen = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( evt.getSource() == commbutt ) {
                    chan.setComment( commbutt.isSelected() );
                }
                else {
                    chan.setFull( Integer.parseInt( evt.getActionCommand() ) );
                }
                Rectangle vis = getVisibleRect();
                try {
                    doc.remove( 0, doc.getLength() );
                }
                catch ( BadLocationException e ) {
                }
                try {
                   chan.write( astobject );
                }
                catch ( IOException e ) {
                }
                ta.moveCaretPosition( 0 );
            }
        };

        commbutt.addActionListener( listen );
        bm.addActionListener( listen );
        b0.addActionListener( listen );
        bp.addActionListener( listen );

        /* Put the constituent components into this container. */
        add( commbox );
        add( fullbox );
        add( textbox );

        /* Click one of the buttons to trigger the initial display. */
        b0.doClick();
    }
}
