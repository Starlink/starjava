package uk.ac.starlink.ttools.plot2.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import uk.ac.starlink.ttools.plot2.ReportMap;

/**
 * SpecifierPanel subclass that puts a number of alternative
 * SpecifierPanels alongside each other and lets the user interact
 * with any one of them to perform a selection.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2017
 */
public class MultiSpecifierPanel<T> extends SpecifierPanel<T> {

    private final List<Specifier<T>> specifiers_;

    /**
     * Constructor.
     *
     * @param  isXFill  true if the graphical component should expand to fill
     *                  the available horizontal space
     * @param  dflt   default value
     * @param  specifiers   list of alternative specifier instances
     */
    @SuppressWarnings("this-escape")
    public MultiSpecifierPanel( boolean isXFill, T dflt,
                                List<Specifier<T>> specifiers ) {
        super( isXFill );
        specifiers_ = specifiers;
        setSpecifiedValue( dflt );
    }

    protected JComponent createComponent() {
        final JComponent box = new Box( BoxLayout.X_AXIS ) {
            @Override
            public void setEnabled( final boolean enabled ) {
                super.setEnabled( enabled );
                for ( Specifier<T> s : specifiers_ ) {
                    s.getComponent().setEnabled( enabled );
                }
            }
        };
        final ActionListener forwarder = getActionForwarder();
        boolean start = true;
        for ( Specifier<T> s : specifiers_ ) {
            if ( ! start ) {
                box.add( Box.createHorizontalStrut( 10 ) );
            }
            start = false;
            box.add( s.getComponent() );
            final Specifier<T> s0 = s;
            s0.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    T value = s0.getSpecifiedValue();
                    for ( Specifier<T> s1 : specifiers_ ) {
                        if ( s1 != s0 ) {
                            s1.setSpecifiedValue( value );
                        }
                    }
                    forwarder.actionPerformed( evt );
                }
            } );
        }
        return box;
    }

    public void setSpecifiedValue( T value ) {
        for ( Specifier<T> s : specifiers_ ) {
            s.setSpecifiedValue( value );
        }
    }

    public T getSpecifiedValue() {
        return specifiers_.get( 0 ).getSpecifiedValue();
    }

    public void submitReport( ReportMap report ) {
        for ( Specifier<T> s : specifiers_ ) {
            s.submitReport( report );
        }
    }
}
