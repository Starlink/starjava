/*
 * $Id: SSTrainingWriter.java,v 1.1 2001/08/27 22:16:41 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import diva.util.ModelWriter;

/**
 * SSTrainingWriter (Single Stroke Training Writer) takes a
 * SSTrainingModel and writes it out to an outputstream.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.1 $
 */
public class SSTrainingWriter implements ModelWriter {
    /**
     * Write the training model to the output stream.
     * The caller is responsible for closing the
     * stream.
     */
    public void writeModel(Object m, OutputStream out) throws IOException{
        Writer writer =
            new BufferedWriter(new OutputStreamWriter(out));
        writeModel(m, writer);
        writer.flush();
        writer.close();
    }
    /**
     * Write the training model to the character-output stream.
     * The caller is responsible for closing the
     * stream.
     */
    public void writeModel(Object m, Writer writer) throws IOException{
        SSTrainingModel model = (SSTrainingModel)m;        
        writeHeader(writer);
        writer.write("<"+SSTrainingParser.MODEL_TAG+">\n");
        for(Iterator i = model.types(); i.hasNext();){
            String type = (String)i.next();
            writer.write("<" + SSTrainingParser.TYPE_TAG + " " + SSTrainingParser.NAME_TAG + "=\"");
            writer.write(type);
            writer.write("\">\n");
            if(model.positiveExampleCount(type)>0){
                for(Iterator pi = model.positiveExamples(type); pi.hasNext();){
                    TimedStroke stroke = (TimedStroke)pi.next();
                    writeExample(stroke, SSTrainingModel.POSITIVE, writer);
                }
            }
            if(model.negativeExampleCount(type)>0){
                for(Iterator ni = model.negativeExamples(type); ni.hasNext();){
                    TimedStroke stroke = (TimedStroke)ni.next();
                    writeExample(stroke, SSTrainingModel.NEGATIVE, writer);
                }
            }
            writer.write("</" + SSTrainingParser.TYPE_TAG + ">\n");
        }
        System.out.println("HERE");
        writer.write("</" + SSTrainingParser.MODEL_TAG + ">\n");
    }

    /**
     * Write the stroke information (x, y, timestamp) and its
     * label (indicating either positive or negative example) to the
     * character-output stream.
     */
    private void writeExample(TimedStroke stroke, boolean label, Writer writer)
            throws IOException {
        String lbl = (label) ? "+" : "-";
        writer.write("<" + SSTrainingParser.EXAMPLE_TAG + " " + SSTrainingParser.LABEL_TAG + "=\"");
        writer.write(lbl);
        writer.write("\" " + SSTrainingParser.POINTS_TAG + "=\"");
        if(stroke == null){
            System.out.println("NULL stroke!");
        }
        writeStroke(stroke, writer);
        writer.write("\"/>\n");
    }

    /**
     * Write out the sequence of points in the stroke.  This includes
     * the x, y, and timestamp information of a point.
     */
    public static void writeStroke(TimedStroke s, Writer writer)
            throws IOException {
        int len = s.getVertexCount();
        for(int k=0; k< len; k++){
            double x = s.getX(k);
            String xs = String.valueOf(x);
            writer.write(xs);
            writer.write(" ");
            double y = s.getY(k);
            String ys = String.valueOf(y);
            writer.write(ys);
            writer.write(" ");
            long t = s.getTimestamp(k);
            String ts = String.valueOf(t);
            writer.write(ts);
            if(k != (len-1)){
                writer.write(" ");
            }
        }
    }

    /**
     * Write header information to the character-output stream.
     */
    private void writeHeader(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" standalone=\"no\"?>\n");
        writer.write("<!DOCTYPE " + SSTrainingParser.MODEL_TAG + " PUBLIC \""
                + SSTrainingParser.PUBLIC_ID + "\"\n\t\""
                + SSTrainingParser.DTD_URL + "\">\n\n");
    }
}

