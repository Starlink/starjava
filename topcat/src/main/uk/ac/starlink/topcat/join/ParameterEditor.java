package uk.ac.starlink.topcat.join;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Graphical component which can edit in place a DescribedValue.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class ParameterEditor extends JComponent {

    private final DescribedValue dval_;
    private final JTextField field_;
    private final JComboBox<ValueCodec> conversionChooser_;
    private final ValueCodec codec0_;
    private final List<ChangeListener> listenerList_;

    /**
     * Constructs a new editor.
     *
     * @param  dval  the DescribedValue that this object will edit
     */
    @SuppressWarnings("this-escape")
    public ParameterEditor( DescribedValue dval ) {
        dval_ = dval;
        listenerList_ = new ArrayList<ChangeListener>();
        ValueInfo info = dval.getInfo();
        String descrip = info.getDescription();
        String units = info.getUnitString();

        /* Set up the text field for entering values. */
        field_ = new JTextField( 16 );
        if ( descrip != null && descrip.trim().length() > 0 ) {
            field_.setToolTipText( descrip );
        }
        ActionListener updateListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateValue();
            }
        };
        field_.addActionListener( updateListener );
        field_.addFocusListener( new FocusListener() {
            public void focusGained( FocusEvent evt ) {
                updateValue();
            }
            public void focusLost( FocusEvent evt ) {
                updateValue();
            }
        } );

        /* Set up a chooser for value conversion. */
        ValueCodec[] codecs = ValueCodec.getCodecs( info );
        if ( codecs.length > 1 ) {
            codec0_ = null;
            conversionChooser_ = new JComboBox<ValueCodec>( codecs );
            conversionChooser_.setSelectedIndex( 0 );
            conversionChooser_.addActionListener( updateListener );
            conversionChooser_.setToolTipText( "Units for " + info.getName() );
        }
        else {
            conversionChooser_ = null;
            codec0_ = codecs[ 0 ];
        }

        /* Lay out components. */
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( field_ );
        if ( conversionChooser_ != null ) {
            add( Box.createHorizontalStrut( 5 ) );
            add( conversionChooser_ );
        }
        else if ( units != null && units.trim().length() > 0 ) {
            add( Box.createHorizontalStrut( 5 ) );
            add( new JLabel( " (" + units.trim() + ")" ) );
        }

        /* Set the displayed value to the parameter's initial value. */
        initValue();
    }

    /**
     * Sets the displayed value to that supplied by the underlying
     * DescribedValue.
     */
    public final void initValue() {
        field_.setText( getCodec().formatValue( dval_.getValue(), 16 ) );
    }

    /**
     * Returns the currently active value codec - this is what converts
     * between the representation of the value in the text field and the
     * actual value in the parameter.
     *
     * @return  current codec
     */
    private ValueCodec getCodec() {
        if ( codec0_ != null ) {
            return codec0_;
        }
        else {
            return conversionChooser_
                  .getItemAt( conversionChooser_.getSelectedIndex() );
        }
    }

    /**
     * Effects the change to the described value.
     * This method is invoked when the GUI undergoes any change that
     * might signal the user has made a change that should be attended to.
     */
    private void updateValue() {
        Object oldVal = dval_.getValue();
        ValueCodec codec = getCodec();
        String text = field_.getText();
        try {
            dval_.setValue( codec.unformatString( text ) );
        }
        catch ( RuntimeException e ) {
            field_.setText( codec.formatValue( dval_.getValue(), 16 ) );
            String msg = "Illegal value \"" + text + "\" for parameter " +
                         dval_.getInfo();
            ErrorDialog.showError( this, "Value Error", e, msg );
        }

        /* Alert listeners. */
        if ( ! isSame( oldVal, dval_.getValue() ) ) {
            ChangeEvent evt = new ChangeEvent( this );
            for ( ChangeListener l : listenerList_ ) {
                l.stateChanged( evt );
            }
        }
    }

    /**
     * Adds a listener which will be informed if this editor's value is changed.
     *
     * @param  listener  new listener
     */
    public void addChangeListener( ChangeListener listener ) {
        listenerList_.add( listener );
    }

    /**
     * Removes a listener previously added by <code>addChangeListener</code>.
     *
     * @param  listener  listener to remove
     */
    public void removeChangeListener( ChangeListener listener ) {
        listenerList_.remove( listener );
    }

    /**
     * Equality function which uses <code>equals</code> but copes with nulls.
     *
     * @param  o1  object 1
     * @param  o2  object 2
     * @return   true iff <code>o1</code> and <code>o2</code> are the same
     */
    private static boolean isSame( Object o1, Object o2 ) {
        if ( o1 == null ) {
            return o2 == null;
        }
        else {
            return o1.equals( o2 );
        }
    }
}
