//package GOV.nasa.gsfc.util.gui;

package jsky.util.gui;

//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference

//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//=== End File Prolog=======================================================

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * A fairly generic class that provides a print preview capability.
 * Code adapted from the online book "Swing" by Matt ? and Pavel ?
 * I forget the online url
 *
 * @version 2000.01.07
 * @author S. Grosvenor
 */
public class PrintPreview extends JFrame {

    protected int fPageWidth;
    protected int fPageHeight;
    protected Printable fTarget;
    protected JComboBox fComboBoxScale;
    protected PreviewContainer fPanelPreview;
    protected ActionListener fPrintListener;

    public PrintPreview(Printable target) {
        this(null, target, "Print Preview");
    }

    public PrintPreview(ActionListener printListener, Printable target, String title) {
        super(title);
        setSize(370, 510);
        fTarget = target;

        JToolBar toolbar = new JToolBar();
        JButton buttonPrint = new JButton("Print");
        buttonPrint.setMnemonic('p');
        buttonPrint.setToolTipText("Print the preview contents");
        if (printListener == null) {
            fPrintListener = new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        // Use default printer, no dialog
                        PrinterJob prnJob = PrinterJob.getPrinterJob();
                        prnJob.setPrintable(fTarget);
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        prnJob.print();
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        dispose();
                    }
                    catch (PrinterException ex) {
                        ex.printStackTrace();
                        System.err.println("Printing error: " + ex.toString());
                    }
                }
            };
        }
        else {
            fPrintListener = printListener;
        }
        buttonPrint.addActionListener(fPrintListener);
        toolbar.add(buttonPrint);

        JButton buttonClose = new JButton("Close");
        buttonClose.setMnemonic('c');
        buttonClose.setToolTipText("Close Preview");
        ActionListener lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        buttonClose.addActionListener(lst);
        toolbar.add(buttonClose);

        String[] scales = {"10 %", "25 %", "50 %", "100 %"};
        fComboBoxScale = new JComboBox(scales);
        fComboBoxScale.setToolTipText("Zoom");

        int scale = 50;
        fComboBoxScale.setSelectedItem("50 %");

        lst = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Thread runner = new Thread() {

                    public void run() {
                        String str = fComboBoxScale.getSelectedItem().toString();
                        if (str.endsWith("%")) str = str.substring(0, str.length() - 1);
                        str = str.trim();
                        int sc = 0;

                        try {
                            sc = Integer.parseInt(str);
                        }
                        catch (NumberFormatException ex) {
                            return;
                        }

                        int w = (int) (fPageWidth * sc / 100);
                        int h = (int) (fPageHeight * sc / 100);

                        Component[] comps = fPanelPreview.getComponents();
                        for (int k = 0; k < comps.length; k++) {
                            if (!(comps[k] instanceof PagePreview)) continue;
                            PagePreview pp = (PagePreview) comps[k];
                            pp.setScaledSize(w, h);
                        }
                        fPanelPreview.doLayout();
                        fPanelPreview.getParent().getParent().validate();
                    }
                };
                runner.start();
            }

        };

        fComboBoxScale.addActionListener(lst);

        Dimension dim = new Dimension(80, 35);

        buttonPrint.setMaximumSize(dim);
        buttonClose.setMaximumSize(dim);
        fComboBoxScale.setMaximumSize(dim);

        fComboBoxScale.setEditable(true);
        toolbar.addSeparator();
        toolbar.add(fComboBoxScale);
        getContentPane().add(toolbar, BorderLayout.NORTH);

        fPanelPreview = new PreviewContainer();

        PrinterJob prnJob = PrinterJob.getPrinterJob();
        PageFormat pageFormat = prnJob.defaultPage();
        if (pageFormat.getHeight() == 0 || pageFormat.getWidth() == 0) {
            System.err.println("Unable to determine default page size");
            return;
        }

        fPageWidth = (int) (pageFormat.getWidth());
        fPageHeight = (int) (pageFormat.getHeight());

        int pageIndex = 0;
        int w = (int) (fPageWidth * scale / 100);
        int h = (int) (fPageHeight * scale / 100);

        try {
            while (true) {
                BufferedImage img = new BufferedImage(fPageWidth, fPageHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, fPageWidth, fPageHeight);
                if (fTarget.print(g, pageFormat, pageIndex) != Printable.PAGE_EXISTS)
                    break;
                PagePreview pp = new PagePreview(w, h, img);
                fPanelPreview.add(pp);
                pageIndex++;
            }
        }
        catch (PrinterException e) {
            DialogUtil.error(e);
        }

        JScrollPane ps = new JScrollPane(fPanelPreview);
        getContentPane().add(ps, BorderLayout.CENTER);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    class PreviewContainer extends JPanel {

        protected int H_GAP = 16;
        protected int V_GAP = 10;

        public Dimension getPreferredSize() {
            int n = getComponentCount();
            if (n == 0) return new Dimension(H_GAP, V_GAP);

            Component comp = getComponent(0);
            Dimension dc = comp.getPreferredSize();
            int w = dc.width;
            int h = dc.height;

            Dimension dp = getParent().getSize();
            int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
            int nRow = n / nCol;
            if (nRow * nCol < n) nRow++;

            int ww = nCol * (w + H_GAP) + H_GAP;
            int hh = nRow * (h + V_GAP) + V_GAP;
            Insets ins = getInsets();

            return new Dimension(ww + ins.left + ins.right,
                    hh + ins.top + ins.bottom);
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public void doLayout() {
            Insets ins = getInsets();
            int x = ins.left + H_GAP;
            int y = ins.top + V_GAP;

            int n = getComponentCount();
            if (n == 0) return;

            Component comp = getComponent(0);
            Dimension dc = comp.getPreferredSize();
            int w = dc.width;
            int h = dc.height;

            Dimension dp = getParent().getSize();
            int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
            int nRow = n / nCol;

            if (nRow * nCol < n) nRow++;

            int index = 0;
            for (int k = 0; k < nRow; k++) {
                for (int m = 0; m < nCol; m++) {
                    if (index >= n)
                        return;
                    comp = getComponent(index++);
                    comp.setBounds(x, y, w, h);
                    x += w + H_GAP;
                }
                y += h + V_GAP;
                x = ins.left + H_GAP;
            }
        }
    }

    class PagePreview extends JPanel {

        protected int m_w;
        protected int m_h;
        protected Image m_source;
        protected Image m_img;

        public PagePreview(int w, int h, Image source) {
            m_w = w;
            m_h = h;
            m_source = source;
            m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
            m_img.flush();
            setBackground(Color.white);
            setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
        }

        public void setScaledSize(int w, int h) {
            m_w = w;
            m_h = h;
            m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
            repaint();
        }

        public Dimension getPreferredSize() {
            Insets ins = getInsets();
            return new Dimension(m_w + ins.left + ins.right,
                    m_h + ins.top + ins.bottom);
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public void paint(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.drawImage(m_img, 0, 0, this);
            paintBorder(g);
        }
    }
}

