package uk.ac.starlink.datanode.viewers;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.FitsChan;

/**
 * Provides a JComponent showing giving the FITS representation of
 * an AST object.
 */
public class AstFITSShower extends JPanel {

    /**
     * Creates an AstTextShower.
     *
     * @param  astob  the AstObject to display
     */
    public AstFITSShower( AstObject astob ) {

        /* Set layout for this component. */
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

        /* Make containers to put controls into. */
        Box encobox = new Box( BoxLayout.Y_AXIS );
        Box topbox = new Box( BoxLayout.X_AXIS );
        Box textbox = new Box( BoxLayout.X_AXIS );

        /* Construct the text area view and model. */
        final JTextArea ta = new JTextArea();
        final Document doc = ta.getDocument();
        ta.setEditable( false );
        ta.setFont( new Font( "Monospaced", ta.getFont().getStyle(), 
                                            ta.getFont().getSize() ) );
        textbox.add( ta );
        textbox.add( Box.createGlue() );

        /* Set up a channel for writing AstObjects into the text area. */
        final FitsChan fchan = new FitsChan();
        fchan.write( astob );

        /* Set up controls. */
        List buttons = new ArrayList();

        /* Set up radio buttons for FITS encoding. */
        ButtonGroup egrp = new ButtonGroup();
        encobox.add( new JLabel( "FITS encoding: " ) );
        String[] encodings = new String[] { 
            "DSS", "FITS-WCS", "FITS-PC", "FITS-IRAF", "FITS-AIPS", "NATIVE" };
        AbstractButton initialState = null;
        for ( int i = 0; i < encodings.length; i++ ) {
            JRadioButton encobutt = new JRadioButton( encodings[ i ] );
            encobutt.setActionCommand( encodings[ i ] );
            egrp.add( encobutt );
            encobox.add( encobutt );
            buttons.add( encobutt );
            if ( i == 0 || fchan.getEncoding().equals( encodings[ i ] ) ) {
                initialState = encobutt;
            }
        }
        topbox.add( encobox );
        topbox.add( Box.createGlue() );

        /* Construct a listener to write the object down the channel
         * whenever one of the buttons is pushed. */
        final AstObject astobject = astob;
        ActionListener listen = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                String cmd = evt.getActionCommand();
                fchan.setEncoding( cmd );
                try {
                    doc.remove( 0, doc.getLength() );
                }
                catch ( BadLocationException e ) {
                    e.printStackTrace();
                }
                while ( fchan.getNcard() > 0 ) {
                    fchan.setCard( fchan.getNcard() );
                    fchan.delFits();
                }
                fchan.write( astobject );
                fchan.setCard( 1 );
                String line;
                while ( ( line = fchan.findFits( "%f", true ) ) != null ) {
                    ta.append( line + '\n' );
                }
                ta.moveCaretPosition( 0 );
            }
        };

        /* Ensure that all the controls will invoke the action listener. */
        for ( Iterator it = buttons.iterator(); it.hasNext(); ) {
            ( (AbstractButton) it.next() ).addActionListener( listen );
        }

        /* Put the constituent components into this container. */
        add( topbox );
        add( textbox );

        /* Click one of the buttons to trigger the initial display. */
        initialState.doClick();
    }
}
