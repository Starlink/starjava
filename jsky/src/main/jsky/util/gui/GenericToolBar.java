/*
 * ESO Archive
 *
 * $Id: GenericToolBar.java,v 1.17 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/31  Created
 */

package jsky.util.gui;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import jsky.util.I18N;
import jsky.util.Resources;


/**
 * Implements a generic toolbar that can be used for a number
 * of file/URL browser type applications. The class using this toolbar
 * must implement the GenericToolBarTarget interface.
 */
public class GenericToolBar extends JToolBar {

    // Used to access internationalized strings (see i18n/gui*.proprties)
    private static final I18N _I18N = I18N.getInstance(GenericToolBar.class);

    /** Target panel */
    protected GenericToolBarTarget target;

    /** Handle for the "Open" button */
    protected JButton openButton;

    /** Handle for the "Back" button */
    protected JButton backButton;

    /** Handle for the "Forward" button */
    protected JButton forwardButton;

    /** If true, display button icons */
    protected boolean showPictures = true;

    /** If true, display button labels */
    protected boolean showText = true;

    /**
     * Create the toolbar for the given Generic target.
     *
     * @param target the target object owning the toolbar
     * @param addItems if true, add the toolbar items, otherwise they should
     *                 be added by calling addToolBarItems() in a derived class.
     * @param orientation the orientation desired
     */
    public GenericToolBar(GenericToolBarTarget target, boolean addItems, int orientation) {
        super(orientation);
        putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        setFloatable(false);

        if (target != null) {
            target.getBackAction().setEnabled(false);
            target.getForwAction().setEnabled(false);
            this.target = target;
        }

        if (addItems) {
            addToolBarItems();
        }
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), getBorder()));
    }

    /**
     * Create the toolbar for the given Generic target.
     *
     * @param target the target object owning the toolbar
     * @param addItems if true, add the toolbar items, otherwise they should
     *                 be added by calling addToolBarItems() in a derived class.
     */
    public GenericToolBar(GenericToolBarTarget target, boolean addItems) {
        this(target, addItems, HORIZONTAL);
    }

    /**
     * Create the toolbar for the given Generic target
     */
    public GenericToolBar(GenericToolBarTarget target) {
        this(target, true);
    }

    /**
     * Create the toolbar with no target (derived class must add all items)
     */
    public GenericToolBar() {
        this(null, true);
    }


    /**
     * Add the items to the tool bar
     */
    protected void addToolBarItems() {
        add(makeOpenButton());
        addSeparator();
        add(makeBackButton());
        add(makeForwardButton());
    }


    /** Set the common attributes for toolbar buttons */
    protected AbstractButton setupButton(AbstractButton button) {
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        return button;
    }


    /**
     * Make the Open button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Open button
     */
    protected JButton makeOpenButton() {
        if (openButton == null) {
            openButton = makeButton(_I18N.getString("fileOpenTip"), target.getOpenAction(), false);
        }

        updateButton(openButton, _I18N.getString("open"), Resources.getIcon("Open24.gif"));
        target.getOpenAction().setEnabled(true);
        return openButton;
    }


    /**
     * Make the Back button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Back button
     */
    protected JButton makeBackButton() {
        if (backButton == null) {
            backButton = makeButton(_I18N.getString("fileBackTip"), target.getBackAction(), false);
        }

        updateButton(backButton, _I18N.getString("back"), Resources.getIcon("Back24.gif"));

        return backButton;
    }

    /**
     * Make the Forward button, if it does not yet exists. Otherwise update the display
     * using the current options for displaying text or icons.
     *
     * @return the Forward button
     */
    protected JButton makeForwardButton() {
        if (forwardButton == null) {
            forwardButton = makeButton(_I18N.getString("fileForwardTip"), target.getForwAction(), false);
        }

        updateButton(forwardButton, _I18N.getString("forward"), Resources.getIcon("Forward24.gif"));

        return forwardButton;
    }


    /**
     * Make and return a toolbar button.
     *
     * @param toolTip the tool tip text
     * @param action the action for the button
     * @param isMenuButton true if the button will have a menu
     */
    protected JButton makeButton(String toolTip, Action action, boolean isMenuButton) {
        JButton button = new JButton();
        //if (!isMenuButton)
        //    button.setFont(button.getFont().deriveFont(Font.PLAIN));
        button.setToolTipText(toolTip);
        if (action != null) {
            action.addPropertyChangeListener(new ButtonPropertyChangeListener(button));
            button.setEnabled(action.isEnabled());
            button.addActionListener(action);
        }
        setupButton(button);

        return button;
    }


    /**
     * Make and return a toolbar toggle button.
     *
     * @param toolTip the tool tip text
     * @param action the action for the button
     * @param isMenuButton true if the button will have a menu
     */
    protected JToggleButton makeToggleButton(String toolTip, Action action) {
        JToggleButton button = new JToggleButton();
        button.setToolTipText(toolTip);
        if (action != null) {
            action.addPropertyChangeListener(new ButtonPropertyChangeListener(button));
            button.setEnabled(action.isEnabled());
            button.addActionListener(action);
        }
        setupButton(button);

        return button;
    }


    /**
     * Make and return a menu button with the given tool tip and menu.
     *
     * @param toolTip the tool tip text for the button
     * @param menu the menu for the button
     */
    protected JButton makeMenuButton(String toolTip, final JPopupMenu menu) {
        JButton button = makeButton(toolTip, null, true);
        button.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                //if (e.isPopupTrigger()) {
                menu.show(e.getComponent(), e.getX(), e.getY());
                //}
            }

            public void mouseReleased(MouseEvent e) {
                //if (e.isPopupTrigger()) {
                menu.show(e.getComponent(), e.getX(), e.getY());
                //}
            }
        });
        return button;
    }

    /**
     * Update the given button to display text, and/or icons, depending on the
     * current settigs.
     *
     * @param button the button
     * @param text the button text
     * @param icon the URL string for the button's icon
     */
    protected void updateButton(AbstractButton button, String text, Icon icon) {
        if (showText) {
            button.setText(text);
        }
        else {
            button.setText(null);
        }

        button.setAlignmentX(CENTER_ALIGNMENT);

        if (showPictures) {
            button.setIcon(icon);
        }
        else {
            button.setIcon(null);
        }
    }

    /** Set to true to show toolbar buttons with icons */
    public void setShowPictures(boolean b) {
        showPictures = b;
        update();
    }

    /** Set to true to show toolbar buttons with labels */
    public void setShowText(boolean b) {
        showText = b;
        update();
    }

    /**
     * Update the toolbar display using the current text/pictures options.
     */
    public void update() {
        makeOpenButton();
        makeBackButton();
        makeForwardButton();
    }

    public JButton getOpenButton() {
        return openButton;
    }

    public JButton getBackButton() {
        return backButton;
    }

    public JButton getForwardButton() {
        return forwardButton;
    }

    /**
     * Local class to enable or disable toolbar buttons when the related actions are
     * enabled or disabled.
     */
    protected class ButtonPropertyChangeListener implements PropertyChangeListener {

        AbstractButton button;

        public ButtonPropertyChangeListener(AbstractButton button) {
            this.button = button;
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals("enabled"))
                button.setEnabled(((Boolean) e.getNewValue()).booleanValue());
        }
    }
}
