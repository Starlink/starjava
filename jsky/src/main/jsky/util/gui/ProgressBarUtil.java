/*
 * ESO Archive
 *
 * $Id: ProgressBarUtil.java,v 1.6 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  2000/01/23  Created
 */

package jsky.util.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import jsky.util.SwingWorker;


/**
 * Adds animation methods to a JProgressBar, to be used, for example,
 * when downloading a URL where the Content-length is unknown.
 */
public class ProgressBarUtil extends JProgressBar {

    /** The model for the progress bar */
    protected DefaultBoundedRangeModel model;

    /** Background thread util */
    protected SwingWorker worker;

    /** Default model range size */
    protected static final int DEFAULT_SIZE = 32;


    /** Constructor */
    public ProgressBarUtil() {
        setStringPainted(false);
    }


    /** Do something to look busy. */
    public void startAnimation() {
        if (worker != null)
            return;
        setStringPainted(false);
        model = new DefaultBoundedRangeModel(0, 20, 0, DEFAULT_SIZE);
        setModel(model);
        worker = new SwingWorker() {

            int value = model.getValue();

            public Object construct() {
                while (true) {
                    try {
                        Thread.sleep(100);
                        if (value != model.getValue())
                            return null; // return if the value was set explicitly
                        model.setValue(value = (model.getValue() + 1) % DEFAULT_SIZE);
                    }
                    catch (Exception e) {
                        return null;
                    }
                }
            }
        };
        worker.start();
    }


    /** Stop looking busy. */
    public void stopAnimation() {
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        if (model != null) {
            model.setValue(0);
        }
    }


    /** Test main. */
    public static void main(String[] args) {
        JFrame frame = new JFrame();

        JPanel top = new JPanel();
        final ProgressBarUtil progressBarUtil = new ProgressBarUtil();
        top.add(progressBarUtil);
        frame.getContentPane().add(top, BorderLayout.NORTH);

        JPanel bot = new JPanel();
        JButton busyButton = new JButton("Busy");
        JButton stopButton = new JButton("Stop");
        bot.add(busyButton);
        bot.add(stopButton);
        frame.getContentPane().add(bot, BorderLayout.SOUTH);

        busyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                progressBarUtil.startAnimation();
            }
        });

        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                progressBarUtil.stopAnimation();
            }
        });

        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new BasicWindowMonitor());
    }
}

