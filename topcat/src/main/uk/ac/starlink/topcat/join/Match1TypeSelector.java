package uk.ac.starlink.topcat.join;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import uk.ac.starlink.table.join.Match1Type;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.ttools.join.Match1TypeParameter;

/**
 * Component which allows the user to select a 
 * {@link uk.ac.starlink.table.join.Match1Type} from a list of options.
 *
 * @author   Mark Taylor
 * @since    16 Nov 2007
 */
public class Match1TypeSelector extends JPanel {

    private final ActionForwarder forwarder_;
    private TypeOption option_;

    private static final TypeOption IDENTIFY =
        new FixedTypeOption( "Mark Groups of Rows",
                             Match1Type.createIdentifyType(),
                             Match1TypeParameter.IDENTIFY );
    private static final TypeOption ELIMINATE_0 =
        new FixedTypeOption( "Eliminate All Grouped Rows",
                             Match1Type.createEliminateMatchesType( 0 ),
                             Match1TypeParameter.ELIMINATE_0 );
    private static final TypeOption ELIMINATE_1 =
        new FixedTypeOption( "Eliminate All But First of Each Group",
                             Match1Type.createEliminateMatchesType( 1 ),
                             Match1TypeParameter.ELIMINATE_1 );
    private static final TypeOption WIDE =
        new WideTypeOption( "New Table With Groups of Size " );

    /** Options offered by this class. */
    private static final TypeOption[] OPTIONS = new TypeOption[] {
        IDENTIFY, ELIMINATE_0, ELIMINATE_1, WIDE,
    };

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public Match1TypeSelector() {
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        forwarder_ = new ActionForwarder();

        /* Lay out one button for each option. */
        ButtonGroup buttGrp = new ButtonGroup();
        for ( int i = 0; i < OPTIONS.length; i++ ) {
            final TypeOption opt = OPTIONS[ i ];
            Action buttAct = new AbstractAction() {
                public void actionPerformed( ActionEvent evt ) {
                    option_ = opt;
                    forwarder_.actionPerformed( evt );
                }
            };
            JRadioButton butt = new JRadioButton( buttAct );
            buttGrp.add( butt );
            Box line = Box.createHorizontalBox();
            line.add( butt );
            line.add( new JLabel( " " + opt.getDescription() ) );
            JSpinner extra = opt.getExtra();
            if ( extra != null ) {
                extra.addChangeListener( forwarder_ );
                line.add( extra );
            }
            line.add( Box.createHorizontalGlue() );
            add( line );
            if ( i == 0 ) {
                butt.doClick();
            }
        }
    }

    /**
     * Returns the match option which the user has selected.
     * Will not be null.
     *
     * @return  internal match type
     */
    public Match1Type getType1() {
        return option_.getType1();
    }

    /**
     * Returns a textual description of the match option selected.
     *
     * @return  internal match type description, not null
     */
    public String getType1Description() {
        return option_.getDescription();
    }

    /**
     * Returns the word used in stilts commands to represent the
     * currently selected option.
     *
     * @return  match type word
     */
    public String getType1Word() {
        return option_.getType1Word();
    }

    /**
     * Adds a listener to be notified when the selected type may have changed.
     *
     * @param  l  listener to add
     */
    public void addActionListener( ActionListener l ) {
        forwarder_.addActionListener( l );
    }

    /**
     * Removes a type change listener.
     *
     * @param  l  listener to remove
     */
    public void removeActionListener( ActionListener l ) {
        forwarder_.removeActionListener( l );
    }

    /**
     * Abstract helper class defining one of the match type options.
     */
    private static abstract class TypeOption {

        private final String description_;

        /**
         * Constructor.
         *
         * @param  description  option description, used as button annotation
         */
        TypeOption( String description ) {
            description_ = description;
        }

        /**
         * Returns the button option description, suitable for use as
         * button annotation.
         *
         * @return  description
         */
        public String getDescription() {
            return description_;
        }

        /**
         * Returns any additional components to display on the line containing
         * the button selecting this option.
         *
         * @return  array, possibly empty, of additional UI components
         */
        abstract JSpinner getExtra();

        /**
         * Returns the internal match type selected by this option.
         *
         * @return  match type
         */
        public abstract Match1Type getType1();

        /**
         * Returns the word used in stilts commands to represent this option.
         *
         * @return  type word
         */
        public abstract String getType1Word();
    }

    /**
     * TypeOption implementation for a fixed match type.
     */
    private static class FixedTypeOption extends TypeOption {
        private final Match1Type type1_;
        private final String word_;

        /**
         * Constructor.
         *
         * @param   option description
         * @param   match type
         * @param   word  corresponding string value for
         *                stilts Match1TypeParameter
         */
        FixedTypeOption( String description, Match1Type type1, String word ) {
            super( description );
            type1_ = type1;
            word_ = word;
        }

        public Match1Type getType1() {
            return type1_;
        }

        public String getType1Word() {
            return word_;
        }

        JSpinner getExtra() {
            return null;
        }
    }

    /**
     * Type option implementation which generates a wide table containing
     * one internal N-way matches per row.
     */
    private static class WideTypeOption extends TypeOption {
        private final JSpinner widthSelector_;

        /**
         * Constructor.
         *
         * @param   description  description
         */
        WideTypeOption( String description ) {
            super( description );
            SpinnerNumberModel widthModel = new SpinnerNumberModel();
            widthModel.setValue( Integer.valueOf( 2 ) );
            widthModel.setMinimum( Integer.valueOf( 2 ) );
            widthSelector_ = new JSpinner( widthModel );
        }

        public Match1Type getType1() {
            int wideness = ((Number) widthSelector_.getValue()).intValue();
            return Match1Type.createWideType( wideness );
        }

        public String getType1Word() {
            int wideness = ((Number) widthSelector_.getValue()).intValue();
            return Match1TypeParameter.WIDE_PREFIX + wideness;
        }

        JSpinner getExtra() {
            return widthSelector_;
        }
    }
}
