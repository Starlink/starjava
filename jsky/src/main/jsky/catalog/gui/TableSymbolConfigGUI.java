/**
 * Title:        User interface layout for plot symbol configuration <p>
 * Description:  JBuilder generated source file <p>
 * Company:      Gemini<p>
 * @author Allan Brighton
 * @version 1.0
 */

package jsky.catalog.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import java.awt.event.*;
import java.util.ResourceBundle;

public class TableSymbolConfigGUI extends JPanel {

    static ResourceBundle res = ResourceBundle.getBundle("jsky.catalog.gui.i18n.jb");
    JPanel jPanel1 = new JPanel();
    JScrollPane jScrollPane1 = new JScrollPane();
    JTable symbolTable = new JTable();
    TitledBorder titledBorder1;
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel2 = new JPanel();
    Border border1;
    JPanel jPanel3 = new JPanel();
    JButton addButton = new JButton();
    JButton removeButton = new JButton();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    Border border2;
    TitledBorder titledBorder2;
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JLabel jLabel3 = new JLabel();
    JLabel jLabel4 = new JLabel();
    JComboBox symbolComboBox = new JComboBox();
    JLabel jLabel5 = new JLabel();
    JLabel jLabel6 = new JLabel();
    JLabel jLabel7 = new JLabel();
    JLabel jLabel8 = new JLabel();
    JComboBox colorComboBox = new JComboBox();
    JTextField ratioTextField = new JTextField();
    JTextField labelTextField = new JTextField();
    JTextField sizeTextField = new JTextField();
    JTextField angleTextField = new JTextField();
    JTextField conditionTextField = new JTextField();
    JComboBox unitsComboBox = new JComboBox();
    JLabel jLabel9 = new JLabel();
    JScrollPane jScrollPane2 = new JScrollPane();
    JScrollPane jScrollPane3 = new JScrollPane();
    JList useList = new JList();
    JList ignoreList = new JList();
    BasicArrowButton leftArrowButton = new BasicArrowButton(SwingConstants.WEST);
    BasicArrowButton rightArrowButton = new BasicArrowButton(SwingConstants.EAST);
    JLabel jLabel10 = new JLabel();
    GridBagLayout gridBagLayout1 = new GridBagLayout();

    public TableSymbolConfigGUI() {
        try {
            jbInit();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), res.getString("Plot_Symbols"));
        border1 = new TitledBorder(new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142)), res.getString("Plot_Symbols"));
        border2 = new EtchedBorder(EtchedBorder.RAISED, Color.white, new Color(142, 142, 142));
        titledBorder2 = new TitledBorder(border2, res.getString("Edit"));
        this.setLayout(gridBagLayout1);
        jScrollPane1.setPreferredSize(new Dimension(453, 150));
        jPanel1.setBorder(border1);
        jPanel1.setMinimumSize(new Dimension(34, 80));
        jPanel1.setPreferredSize(new Dimension(463, 80));
        jPanel1.setLayout(borderLayout1);
        borderLayout1.setHgap(5);
        borderLayout1.setVgap(5);
        jPanel2.setBorder(titledBorder2);
        jPanel2.setLayout(gridBagLayout2);
        addButton.setToolTipText(res.getString("Add_a_new_plot_symbol"));
        addButton.setText(res.getString("Add"));
        addButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        removeButton.setToolTipText(res.getString("Remove_the_selected"));
        removeButton.setText(res.getString("Remove"));
        removeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeButton_actionPerformed(e);
            }
        });
        jLabel1.setToolTipText(res.getString("Symbol_to_use_to_plot"));
        jLabel1.setText(res.getString("Symbol"));
        jLabel2.setToolTipText(res.getString("Ratio_of_width_to"));
        jLabel2.setText(res.getString("Ratio"));
        jLabel3.setToolTipText(res.getString("Label_for_symbol"));
        jLabel3.setText(res.getString("Label"));
        jLabel4.setToolTipText(res.getString("Size_of_symbol"));
        jLabel4.setText(res.getString("Size"));
        jLabel5.setToolTipText(res.getString("Color_in_which_to"));
        jLabel5.setText(res.getString("Color"));
        jLabel6.setToolTipText(res.getString("Angle_of_rotation_for"));
        jLabel6.setText(res.getString("Angle"));
        jLabel7.setToolTipText(res.getString("Condition_for"));
        jLabel7.setText(res.getString("Condition"));
        jLabel8.setToolTipText(res.getString("Units_in_which_to"));
        jLabel8.setText(res.getString("Units"));
        this.setMinimumSize(new Dimension(400, 360));
        this.setPreferredSize(new Dimension(427, 389));
        jLabel9.setToolTipText(res.getString("Columns_used_as"));
        jLabel9.setText(res.getString("Columns"));
        jLabel10.setToolTipText("");
        jLabel10.setText(res.getString("Used"));
        leftArrowButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                leftArrowButton_actionPerformed(e);
            }
        });
        rightArrowButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                rightArrowButton_actionPerformed(e);
            }
        });
        leftArrowButton.setToolTipText(res.getString("Use_the_column"));
        rightArrowButton.setToolTipText(res.getString("Don_t_use_the_column"));
        symbolTable.setToolTipText(res.getString("Plot_symbol"));
        useList.setToolTipText(res.getString("Column_headings_that"));
        ignoreList.setToolTipText(res.getString("List_of_columns_not"));
        symbolComboBox.setToolTipText(res.getString("Select_the_shape_for"));
        colorComboBox.setToolTipText(res.getString("Select_the_color_of"));
        ratioTextField.setToolTipText(res.getString("Ratio_of_width_to"));
        angleTextField.setToolTipText(res.getString("Angle_of_rotation_for"));
        labelTextField.setToolTipText(res.getString("Label_for_symbol"));
        conditionTextField.setToolTipText(res.getString("Condition_for"));
        sizeTextField.setToolTipText(res.getString("Size_of_symbol"));
        unitsComboBox.setToolTipText(res.getString("Units_in_which_to"));
        this.add(jPanel1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(11, 11, 0, 11), 0, 0));
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        this.add(jPanel2, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(11, 11, 0, 11), 0, 0));
        jPanel2.add(jLabel4, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel9, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jScrollPane2, new GridBagConstraints(2, 0, 1, 3, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jScrollPane2.getViewport().add(useList, null);
        jPanel2.add(jScrollPane3, new GridBagConstraints(4, 0, 1, 3, 1.0, 1.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(symbolComboBox, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(colorComboBox, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(sizeTextField, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel3, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(labelTextField, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel2, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(ratioTextField, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel1, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel8, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(unitsComboBox, new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel7, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(conditionTextField, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel6, new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(angleTextField, new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(jLabel5, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel2.add(leftArrowButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 20, 2, 20), 0, 0));
        jPanel2.add(rightArrowButton, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 20, 2, 20), 0, 0));
        jPanel2.add(jLabel10, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                , GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 10, 5), 0, 0));
        this.add(jPanel3, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0
                , GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(11, 11, 11, 11), 0, 0));
        jPanel3.add(addButton, null);
        jPanel3.add(removeButton, null);
        jScrollPane3.getViewport().add(ignoreList, null);
        jScrollPane1.getViewport().add(symbolTable, null);
    }

    void leftArrowButton_actionPerformed(ActionEvent e) {

    }

    void rightArrowButton_actionPerformed(ActionEvent e) {

    }

    void addButton_actionPerformed(ActionEvent e) {

    }

    void removeButton_actionPerformed(ActionEvent e) {

    }
}
