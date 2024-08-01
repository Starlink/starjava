package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.ttools.plot.Style;

/**
 * Graphical component which provides a GUI for editing the characteristics
 * of a {@link Style} object.  This is an abstract superclass; a specialised
 * implementation will be required for each <code>Style</code> implementation.
 * Since Style objects are usually immutable, this doesn't (necessarily)
 * edit a single style object; instead you configure it with an existing
 * style using the {@link #setState} method and later use the 
 * {@link #getStyle} method and others to obtain the new style which is
 * a result of the editing.
 *
 * @author   Mark Taylor
 * @since    10 Jan 2005
 */
public abstract class StyleEditor extends JPanel
                                  implements ActionListener, ChangeListener {

    private final JLabel legendLabel_;
    private final JTextField labelField_;
    private final JCheckBox labelHider_;
    private final ActionForwarder actionForwarder_;
    private boolean initialised_;
    private SetId setId_;
    private Style initialStyle_;
    private String initialLabel_;
    private boolean initialHideLegend_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public StyleEditor() {
        super();
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        actionForwarder_ = new ActionForwarder();

        /* Legend box. */
        legendLabel_ = new JLabel();
        labelField_ = new JTextField();
        labelField_.addActionListener( this );
        labelHider_ = new JCheckBox( "Hide Legend" );
        labelHider_.addActionListener( this );
        JComponent legendBox = Box.createHorizontalBox();
        legendBox.add( new JLabel( "Icon: " ) );
        legendBox.add( legendLabel_ );
        legendBox.add( Box.createHorizontalStrut( 10 ) );
        legendBox.add( new JLabel( "Label: " ) );
        legendBox.add( labelField_ );
        legendBox.add( Box.createHorizontalStrut( 10 ) );
        legendBox.add( labelHider_ );
        legendBox.add( Box.createHorizontalGlue() );
        legendBox.setBorder( AuxWindow.makeTitledBorder( "Legend" ) );
        add( legendBox );
    }

    public void setVisible( boolean visible ) {
        if ( visible && ! initialised_ ) {
            init();
            initialised_ = true;
        }
        super.setVisible( visible );
    }

    /**
     * Performs initialisation after construction but before the first
     * display of this component.
     */
    protected void init() {
        setState( null, "", false );
    }

    /**
     * Sets the state of this component ready for editing.
     *
     * @param   style  style 
     * @param   label  textual label to use in legends annotating the
     *          style being edited
     * @param   hideLegend  whether this style is to be excluded from
     *          plot legends
     */
    public void setState( Style style, String label, boolean hideLegend ) {
        initialStyle_ = style;
        initialLabel_ = label;
        initialHideLegend_ = hideLegend;
        labelField_.setText( label );
        labelHider_.setSelected( hideLegend );
        setStyle( style );
        refreshState();
    }

    /**
     * Sets the style.  Implementations should configure their visual
     * state so that it matches the characteristics of the given style.
     *
     * @param  style current style
     */
    public abstract void setStyle( Style style );

    /**
     * Returns a style object derived from the current state of this
     * component.
     *
     * @return  current (edited) style
     */
    public abstract Style getStyle();

    /**
     * Returns the help ID associated with this editor.
     *
     * @return  ID within TOPCAT HelpSet
     */
    public abstract String getHelpID();

    /**
     * Returns the label currently entered in this component.
     *
     * @return   label
     */
    public String getLabel() {
        return labelField_.getText();
    }

    /**
     * Returns whether the Hide Legend check box is currently selected.
     *
     * @return   true iff legend will be hidden for this style
     */
    public boolean getHideLegend() {
        return labelHider_.isSelected();
    }

    /**
     * Sets the set identifier for the style which this editor is currently
     * editing.
     *
     * @param   id   set identifier
     */ 
    public void setSetId( SetId id ) {
        setId_ = id;
        refreshState();
    }

    /**
     * Returns the set identifier for the style which this editor is currently
     * editing.
     *
     * @return  set identifier
     */
    public SetId getSetId() {
        return setId_;
    }

    /**
     * Undoes any changes done since {@link #setState} was called.
     */
    public void cancelChanges() {
        setState( initialStyle_, initialLabel_, initialHideLegend_ );
    }

    /**
     * Adds an action listener.  It will be notified every time something
     * the state described by this component changes.
     *
     * @param  listener   listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes an action listener which was previously added.
     *
     * @param   listener  listener to remove
     * @see  #addActionListener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }

    /**
     * Invoked every time the style described by the current state of this
     * component changes.
     */
    public void actionPerformed( ActionEvent evt ) {
        refreshState();
        actionForwarder_.actionPerformed( evt );
    }

    /**
     * Invoked every time the style described by the current state of this
     * component changes.
     */
    public void stateChanged( ChangeEvent evt ) {
        refreshState();
        actionForwarder_.stateChanged( evt );
    }

    /**
     * Ensures that all the visual components of this editor match its
     * internal state.
     */
    protected void refreshState() {
        legendLabel_.setIcon( getLegendIcon() );
        labelField_.setEnabled( ! labelHider_.isSelected() );
        repaint();
    }

    /**
     * Returns the icon to be used for the display legend of this editor.
     *
     * @return  legend icon
     */
    public Icon getLegendIcon() {
        return getStyle().getLegendIcon();
    }
}
