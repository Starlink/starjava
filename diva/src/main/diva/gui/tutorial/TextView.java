/*
 * $Id: TextView.java,v 1.2 2001/07/22 22:01:34 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.tutorial;

import diva.resource.RelativeBundle;
import diva.resource.DefaultBundle;

import diva.gui.*;
import diva.gui.toolbox.ListDataModel;
import diva.gui.toolbox.FocusMouseListener;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Image;
import java.awt.datatransfer.Clipboard;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * A simple MDI text editor view.  FIXME
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public class TextView extends AbstractView {
    private JEditorPane _editorPane;
    private JScrollPane _scrollPane;
    public TextView(TextDocument doc) {
        super(doc);
    }
    public TextDocument getTextDocument() {
        return (TextDocument)getDocument();
    }
    public JComponent getComponent() {
        if(_scrollPane == null) {
            TextDocument td = (TextDocument)getDocument();
            _editorPane = new JEditorPane();
            _editorPane.setText(td.getText());
            // Get notified every time text is changed in the component to update
            // our text document.  The "Document" here is a
            // javax.swing.text.Document.  Don't get confused!
            _editorPane.getDocument().addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        getTextDocument().setText(_editorPane.getText());
                    }
                    public void insertUpdate(DocumentEvent e) {
                        getTextDocument().setText(_editorPane.getText());
                    }
                    public void removeUpdate(DocumentEvent e) {
                        getTextDocument().setText(_editorPane.getText());
                    }
                });
            _scrollPane = new JScrollPane(_editorPane);
        }
        return _scrollPane;
    }
    public String getTitle() {
        return getDocument().getTitle();
    }
    public String getShortTitle() {
        return getTitle();
    }
}

