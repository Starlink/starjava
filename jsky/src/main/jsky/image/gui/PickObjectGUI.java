
package jsky.image.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.ResourceBundle;

/**
 * Title:        GUI File
 * Description:  Generated with JBuilder
 * Copyright:    Copyright (c) 2000 Aura Inc.
 * Company:      Gemini 8M Telescopes Project
 * @author Allan Brighton
 * @version 1.0
 */

public class PickObjectGUI extends JPanel {

    static ResourceBundle res = ResourceBundle.getBundle("jsky.image.gui.i18n.jb");
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JPanel topPanel = new JPanel();
    JPanel infoPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JButton pickButton = new JButton();
    JButton addButton = new JButton();
    JButton closeButton = new JButton();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JPanel imagePanel = new JPanel();
    JButton zoomInButton = new JButton();
    JButton zoomOutButton = new JButton();
    JLabel magLabel = new JLabel();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    JLabel imageXLabel = new JLabel();
    JLabel imageYLabel = new JLabel();
    JTextField imageXField = new JTextField();
    JTextField imageYField = new JTextField();
    JLabel raLabel = new JLabel();
    JLabel decLabel = new JLabel();
    JLabel equinoxLabel = new JLabel();
    JLabel peakLabel = new JLabel();
    JLabel backgroundLabel = new JLabel();
    JLabel fwhmLabel = new JLabel();
    JLabel angleLabel = new JLabel();
    JLabel pixelsLabel = new JLabel();
    JTextField raField = new JTextField();
    JTextField decField = new JTextField();
    JTextField equinoxField = new JTextField();
    JTextField peakField = new JTextField();
    JTextField backgroundField = new JTextField();
    JTextField fwhmField = new JTextField();
    JTextField angleField = new JTextField();
    JTextField pixelsField = new JTextField();
    TitledBorder titledBorder1;
    TitledBorder titledBorder2;
    TitledBorder titledBorder3;
    JPanel optionPanel = new JPanel();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    TitledBorder titledBorder4;
    JCheckBox autoAddCheckBox = new JCheckBox();

    public PickObjectGUI() {
        try {
            jbInit();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), res.getString("Area_of_image_to_be"));
        titledBorder2 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), res.getString("Image_Statistics"));
        titledBorder3 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), res.getString("Area_of_image_to_be"));
        titledBorder4 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), res.getString("Options"));
        this.setLayout(gridBagLayout1);
        pickButton.setToolTipText(res.getString("Select_object_in"));
        pickButton.setText(res.getString("Pick"));
        pickButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                pickObject(e);
            }
        });
        addButton.setToolTipText(res.getString("Add_a_row_to_the"));
        addButton.setText(res.getString("Add"));
        addButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                add(e);
            }
        });

        closeButton.setToolTipText(res.getString("Close_this_window"));
        closeButton.setText(res.getString("Close"));
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                close(e);
            }
        });
        infoPanel.setBorder(titledBorder2);
        infoPanel.setLayout(gridBagLayout3);
        topPanel.setBorder(titledBorder3);
        topPanel.setLayout(gridBagLayout2);
        zoomInButton.setToolTipText(res.getString("Zoom_in"));
        zoomInButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomIn(e);
            }
        });
        zoomOutButton.setToolTipText(res.getString("Zoom_out"));
        zoomOutButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomOut(e);
            }
        });
        magLabel.setBorder(BorderFactory.createEtchedBorder());
        magLabel.setHorizontalAlignment(SwingConstants.CENTER);
        magLabel.setText("10x");
        imageXLabel.setToolTipText(res.getString("Center_X_image"));
        imageXLabel.setText(res.getString("Image_X"));
        imageYLabel.setToolTipText(res.getString("Center_Y_image"));
        imageYLabel.setText(res.getString("Image_Y"));
        raLabel.setToolTipText(res.getString("World_coordinates_RA"));
        raLabel.setText(res.getString("RA"));
        decLabel.setToolTipText(res.getString("World_coordinates_Dec"));
        decLabel.setText(res.getString("Dec"));
        equinoxLabel.setToolTipText(res.getString("The_equinox_of_RA_and"));
        equinoxLabel.setText(res.getString("Equinox"));
        peakLabel.setToolTipText(res.getString("Peak_value_of_object"));
        peakLabel.setText(res.getString("Peak_value_above_bg"));
        backgroundLabel.setToolTipText(res.getString("Mean_background_level"));
        backgroundLabel.setText(res.getString("Background_level"));
        fwhmLabel.setToolTipText(res.getString("Full_width_half"));
        fwhmLabel.setText(res.getString("FWHM_X_Y"));
        angleLabel.setToolTipText(res.getString("Angle_of_major_axis"));
        angleLabel.setText(res.getString("Angle_of_X_axis"));
        pixelsLabel.setToolTipText(res.getString("Number_of_pixels"));
        pixelsLabel.setText(res.getString("Pixels_in_X_Y"));
        imageXField.setBorder(BorderFactory.createEtchedBorder());
        imageXField.setToolTipText(res.getString("Center_X_image"));
        imageXField.setEditable(false);
        imageYField.setBorder(BorderFactory.createEtchedBorder());
        imageYField.setToolTipText(res.getString("Center_Y_image"));
        imageYField.setEditable(false);
        raField.setBorder(BorderFactory.createEtchedBorder());
        raField.setToolTipText(res.getString("World_coordinates_RA"));
        raField.setEditable(false);
        decField.setBorder(BorderFactory.createEtchedBorder());
        decField.setToolTipText(res.getString("World_coordinates_Dec"));
        decField.setEditable(false);
        equinoxField.setBorder(BorderFactory.createEtchedBorder());
        equinoxField.setToolTipText(res.getString("The_equinox_of_RA_and"));
        equinoxField.setEditable(false);
        peakField.setBorder(BorderFactory.createEtchedBorder());
        peakField.setToolTipText(res.getString("Peak_value_of_object"));
        peakField.setEditable(false);
        backgroundField.setBorder(BorderFactory.createEtchedBorder());
        backgroundField.setToolTipText(res.getString("Mean_background_level"));
        backgroundField.setEditable(false);
        fwhmField.setBorder(BorderFactory.createEtchedBorder());
        fwhmField.setToolTipText(res.getString("Full_width_half"));
        fwhmField.setEditable(false);
        angleField.setBorder(BorderFactory.createEtchedBorder());
        angleField.setToolTipText(res.getString("Angle_of_major_axis"));
        angleField.setEditable(false);
        pixelsField.setBorder(BorderFactory.createEtchedBorder());
        pixelsField.setToolTipText(res.getString("Number_of_pixels"));
        pixelsField.setEditable(false);
        this.setPreferredSize(new Dimension(275, 700));
        optionPanel.setLayout(gridBagLayout4);
        optionPanel.setBorder(titledBorder4);
        autoAddCheckBox.setText(res.getString("Multi_Pick_Auto_Add"));
        autoAddCheckBox.setSelected(true);
        autoAddCheckBox.setToolTipText(res.getString("Pick_multiple_objects"));
        imagePanel.setMinimumSize(new Dimension(200, 200));
        imagePanel.setPreferredSize(new Dimension(200, 200));
        this.add(topPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(12, 12, 0, 12), 0, 0));
        topPanel.add(imagePanel, new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        topPanel.add(zoomInButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        topPanel.add(zoomOutButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        topPanel.add(magLabel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(infoPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(12, 12, 0, 12), 0, 0));
        infoPanel.add(imageXLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(imageYLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(imageXField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(imageYField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(raLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(decLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(equinoxLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(peakLabel, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(backgroundLabel, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(fwhmLabel, new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(angleLabel, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 0, 0), 0, 0));
        infoPanel.add(pixelsLabel, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 11, 6, 6), 0, 0));
        infoPanel.add(raField, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(decField, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(equinoxField, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(peakField, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(backgroundField, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(fwhmField, new GridBagConstraints(1, 8, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(angleField, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 0, 11), 0, 0));
        infoPanel.add(pixelsField, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 11, 6, 11), 0, 0));
        this.add(buttonPanel, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 11, 6, 11), 0, 0));
        buttonPanel.add(pickButton, null);
        buttonPanel.add(addButton, null);
        buttonPanel.add(closeButton, null);
        this.add(optionPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(12, 12, 0, 12), 0, 0));
        optionPanel.add(autoAddCheckBox, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    }

    void pickObject(ActionEvent e) {

    }

    void close(ActionEvent e) {

    }

    void add(ActionEvent e) {

    }

    void zoomIn(ActionEvent e) {

    }

    void zoomOut(ActionEvent e) {

    }
}
