/*
 * $Id: MSTrainingWriter.java,v 1.2 2001/10/03 18:42:17 hwawen Exp $
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
 * MSTrainingWriter (Multi-Stroke Training Writer) takes a
 * MSTrainingModel and writes it out to an outputstream.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 */
public class MSTrainingWriter implements ModelWriter {
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
        MSTrainingModel model = (MSTrainingModel)m;        
        writeHeader(writer);
        writer.write("<"+MSTrainingParser.MODEL_TAG+">\n");
        for(Iterator i = model.types(); i.hasNext();){
            String type = (String)i.next();
            writer.write("<" + MSTrainingParser.TYPE_TAG + " " + MSTrainingParser.NAME_TAG + "=\"");
            writer.write(type);
            writer.write("\">\n");
            if(model.positiveExampleCount(type)>0){
                for(Iterator pi = model.positiveExamples(type); pi.hasNext();){
                    TimedStroke strokes[] = (TimedStroke [])pi.next();
                    writeExample(strokes, MSTrainingModel.POSITIVE, writer);
                }
            }
            if(model.negativeExampleCount(type)>0){
                for(Iterator ni = model.negativeExamples(type); ni.hasNext();){
                    TimedStroke strokes[] = (TimedStroke [])ni.next();
                    writeExample(strokes, MSTrainingModel.NEGATIVE, writer);
                }
            }
            writer.write("</" + MSTrainingParser.TYPE_TAG + ">\n");
        }
        writer.write("</" + MSTrainingParser.MODEL_TAG + ">\n");
    }

    /**
     * Write the stroke information (x, y, timestamp) and its
     * label (indicating either positive or negative example) to the
     * character-output stream.
     */
    private void writeExample(TimedStroke strokes[], boolean label, Writer writer)
            throws IOException {
        String lbl = (label) ? "+" : "-";
        int numStrokes = strokes.length;
        writer.write("<" + MSTrainingParser.EXAMPLE_TAG + " " + MSTrainingParser.LABEL_TAG + "=\"");
        writer.write(lbl);
        writer.write("\" numStrokes=\"" + numStrokes +"\">\n");

        for(int i=0; i<numStrokes; i++){
            writer.write("\t<" + MSTrainingParser.STROKE_TAG+" " + MSTrainingParser.POINTS_TAG + "=\"");
            SSTrainingWriter.writeStroke(strokes[i], writer);
            writer.write("\"/>\n");
        }
        writer.write("</" + MSTrainingParser.EXAMPLE_TAG + ">\n");
    }

    /**
     * Write header information to the character-output stream.
     */
    private void writeHeader(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" standalone=\"no\"?>\n");
        writer.write("<!DOCTYPE " + MSTrainingParser.MODEL_TAG + " PUBLIC \""
                + MSTrainingParser.PUBLIC_ID + "\"\n\t\""
                + MSTrainingParser.DTD_URL + "\">\n\n");
    }
}

