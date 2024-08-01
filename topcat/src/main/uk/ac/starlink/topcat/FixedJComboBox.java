package uk.ac.starlink.topcat;

import java.util.Objects;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListDataEvent;

/**
 * JComboBox drop-in replacement that fixes an action firing misfeature
 * in the standard implementation.
 *
 * <p>The problem is to do with firing ActionEvents when the
 * ComboBoxModel is changed.
 * According to the {@link javax.swing.JComboBox#addActionListener}
 * documentation, ActionListeners should only be informed when a
 * selection is made, that is when the selection changes.
 * In general that's true, an ActionEvent is only fired when the
 * model selection changes, but at least in Oracle Java 8/OpenJDK 8
 * such an event can be fired if the model content (list data) changes
 * but the current selection is null both before and after that change
 * (if the selection is an particular non-null value before and after,
 * no ActionEvent is fired, as expected).
 * Since there are a lot of listeners on these selectors in topcat,
 * and certain user actions can cause a large number of changes to
 * the model without changing the selection, this can cause a lot of
 * unnecessary events to be fired, which can in some circumstances
 * result in genuine responsiveness issues.
 *
 * <p>This class overrides the {@link #contentsChanged} method,
 * and enough other methods to make that work, so that no event is fired
 * in the event of a null-&gt;null selection change.
 *
 * @author   Mark Taylor
 * @since    6 Oct 2023
 */
public class FixedJComboBox<E> extends JComboBox<E> {

    private Object oldSelection_;

    /**
     * Creates a JComboBox with a default data model.
     */
    @SuppressWarnings("this-escape")
    public FixedJComboBox() {
        super();
        oldSelection_ = getModel().getSelectedItem();
    }

    /**
     * Creates a JComboBox that takes its items from an existing ComboBoxModel.
     *
     * @param  model  model
     */
    @SuppressWarnings("this-escape")
    public FixedJComboBox( ComboBoxModel<E> model ) {
        super( model );
        oldSelection_ = getModel().getSelectedItem();
    }

    /**
     * Creates a JComboBox that contains the elements in the specified array.
     *
     * @param  items  array populating default combo box model
     */
    @SuppressWarnings("this-escape")
    public FixedJComboBox( E[] items ) {
        super( items );
        oldSelection_ = getModel().getSelectedItem();
    }

    /**
     * Overriding this method is questionable, since the OpenJDK javadoc says:
     * "This method is public as an implementation side effect.
     * do not call or override.".  So there could be problems.
     * But if the method exists at all to be overridden, this
     * implementation shouldn't change its behaviour in a bad way,
     * and if it doesn't exist it shouldn't have any effect,
     * so I don't *expect* runtime problems with either OpenJDK or other JDKs.
     *
     * <p>However if an attempt is made to compile this class against a JDK
     * in which JComboBox lacks the <code>contentsChanged</code> method,
     * there will be trouble.
     * It could be averted by removing the <code>@Override</code> annotation,
     * and invoking the superclass method using reflection.
     */
    @Override
    public void contentsChanged( ListDataEvent evt ) {
        Object oldSelection = oldSelection_;
        Object newSelection = getModel().getSelectedItem();

        /* This is the material change from the superclass implementation.
         * Here is the OpenJDK 8 implementation (GPL Classpath exception):
         *
         *   public void contentsChanged(ListDataEvent e) {
         *     Object oldSelection = selectedItemReminder;
         *     Object newSelection = dataModel.getSelectedItem();
         *     if (oldSelection == null || !oldSelection.equals(newSelection)) {
         *         selectedItemChanged();
         *         if (!selectingItem) {
         *             fireActionEvent();
         *         }
         *     }
         *   }
         *
         * An action is triggered if both old and new selections are null.
         * I speculate it was done like this just from laziness because
         * testing equality in the presence of nulls is fiddly.
         *
         * In the replacement code we make sure that in the case of
         * either object- or null- pre/post equality, no events are fired.
         */
        if ( ! Objects.equals( oldSelection, newSelection ) ) {
            super.contentsChanged( evt );
        }
    }

    // The remaining methods are overridden just to make sure that the
    // oldSelection_ field is kept up to date.
    // This code is written with reference to the OpenJDK 8 implementation,
    // which uses the protected field selectedItemReminder for that purpose.
    // We could just use selectedItemReminder here directly, but it is
    // documented as "implementation specific" with a warning not to use it.

    @Override
    public void setModel( ComboBoxModel<E> model ) {
        super.setModel( model );
        oldSelection_ = model.getSelectedItem();
    }

    @Override
    public void removeAllItems() {
        super.removeAllItems();
        oldSelection_ = null;
    }

    @Override
    protected void selectedItemChanged() {
        super.selectedItemChanged();
        oldSelection_ = getModel().getSelectedItem();
    }
}
