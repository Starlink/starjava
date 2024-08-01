package uk.ac.starlink.vo;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;

/**
 * Button used for selecting between And and Or semantics for combining
 * search terms.
 *
 * <p>Note that the {@link #isAnd} method is used to test its state,
 * not <code>isSelected</code>.  Listeners for this state should use
 * {@link #addAndListener} not <code>addActionListener</code>.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2015
 */
public class AndButton extends JButton {

    private final Action act_;
    private final List<ActionListener> andListeners_;
    private boolean and_;
    private Dimension prefSize_ = new Dimension( 0, 0 );

    /**
     * Constructor.
     *
     * @param  isAnd  initial state
     */
    @SuppressWarnings("this-escape")
    public AndButton( boolean isAnd ) {
        act_ = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                and_ = ! and_;
                updateAndState();
                for ( ActionListener l : andListeners_ ) {
                    l.actionPerformed( evt );
                }
            }
        };
        act_.putValue( Action.SHORT_DESCRIPTION,
                       "Toggles whether search terms are combined "
                     + "using AND or OR " );
        and_ = isAnd;
        andListeners_ = new ArrayList<ActionListener>();
        updateAndState();
        setAction( act_ );
    }

    /**
     * Returns state.
     *
     * @return   true for AND, false for OR
     */
    public boolean isAnd() {
        return and_;
    }

    /**
     * Sets state.
     *
     * @param  isAnd   true for AND, false for OR
     */
    public void setAnd( boolean isAnd ) {
        and_ = isAnd;
        updateAndState();
    }

    /**
     * Add a listener for changes to the toggle state of this button.
     *
     * @param  l  listener to add
     */
    public void addAndListener( ActionListener l ) {
        andListeners_.add( l );
    }

    /**
     * Remove a listener added by addAndListener.
     *
     * @param  l  listener to remove
     */
    public void removeAndListener( ActionListener l ) {
        andListeners_.remove( l );
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension psize = super.getPreferredSize();
        if ( psize == null ) {
            return null;
        }
        prefSize_ = new Dimension( Math.max( prefSize_.width, psize.width ),
                                   Math.max( prefSize_.height, psize.height ) );
        return prefSize_;
    }

    /**
     * Resets the appearance of this button according to its current state.
     */
    private void updateAndState() {
        act_.putValue( Action.NAME, and_ ? "And" : "Or " );
    }
}
