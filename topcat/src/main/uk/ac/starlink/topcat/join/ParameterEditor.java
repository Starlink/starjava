package uk.ac.starlink.topcat.join;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Graphical component which can edit in place a DescribedValue.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class ParameterEditor extends JTextField 
                             implements ActionListener, FocusListener {

    private final DescribedValue dval;

    /**
     * Constructs a new editor.
     *
     * @param  dval  the DescribedValue that this object will edit
     */
    public ParameterEditor( DescribedValue dval ) {
        super( 16 );
        this.dval = dval;
        setText( dval.getValueAsString( 16 ) );
        String descrip = dval.getInfo().getDescription();
        if ( descrip != null && descrip.trim().length() > 0 ) {
            setToolTipText( descrip );
        }
        addActionListener( this );
        addFocusListener( this );
    }

    /**
     * Effects the change to the described value.
     * This method is invoked when the GUI undergoes any change that
     * might signal the user has made a change that should be attended to.
     *
     * @param  text  new textual representation of the value
     */
    private void setValue( String text ) {
        try {
            dval.setValue( dval.getInfo().unformatString( text ) );
        }
        catch ( RuntimeException e ) {
            setText( dval.getValueAsString( 16 ) );
            ErrorDialog.showError( e, "Illegal value \"" + text + "\"" +
                                      " for parameter " + dval.getInfo(),
                                   this );
        }
    }

    public void actionPerformed( ActionEvent evt ) {
        setValue( getText() );
    }

    public void focusGained( FocusEvent evt ) {
        setValue( getText() );
    }

    public void focusLost( FocusEvent evt ) {
        setValue( getText() );
    }
}
