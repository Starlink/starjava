/*
 * Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TabbedPanel.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;


/**
 * A panel containing a JTabbedPane, some dialog buttons, and methods
 * to access them.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class TabbedPanel extends JPanel implements ActionListener {

    private Component _parent;
    private JTabbedPane _tabbedPane;
    private JButton _okButton;
    private JButton _applyButton;
    private JButton _cancelButton;

    public TabbedPanel(Component parent) {
        super();
        _parent = parent;
        _tabbedPane = new JTabbedPane();
        setLayout(new BorderLayout());
        add(_tabbedPane, BorderLayout.CENTER);
        add(makeButtonPanel(), BorderLayout.SOUTH);
    }

    /** Make and return the button panel */
    protected JPanel makeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        _okButton = new JButton("OK");
        _okButton.addActionListener(this);
        _okButton.setToolTipText("Apply changes and close this window");
        buttonPanel.add(_okButton);

        _applyButton = new JButton("Apply");
        _applyButton.setToolTipText("Apply changes");
        buttonPanel.add(_applyButton);

        _cancelButton = new JButton("Cancel");
        _cancelButton.addActionListener(this);
        _cancelButton.setToolTipText("Cancel changes and close this window");
        buttonPanel.add(_cancelButton);

        return buttonPanel;
    }

    public void actionPerformed(ActionEvent e) {
        _parent.setVisible(false);
    }

    public JTabbedPane getTabbedPane() {
        return _tabbedPane;
    }

    public JButton getOKButton() {
        return _okButton;
    }

    public JButton getApplyButton() {
        return _applyButton;
    }

    public JButton getCancelButton() {
        return _cancelButton;
    }
}
