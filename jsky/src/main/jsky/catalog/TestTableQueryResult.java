/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: TestTableQueryResult.java,v 1.3 2002/08/04 21:48:50 brighton Exp $
 */

package jsky.catalog;

/**
 * Used for testing. This class provides dummy catalog data to display
 * in a table.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 *
 */
public class TestTableQueryResult extends MemoryCatalog {

    public TestTableQueryResult() {
        super();

        Object[] columnNames = new Object[]{
            "Id", "RA", "DEC", "PosErr", "Mag"
        };

        setFields(new FieldDescAdapter[]{
            new FieldDescAdapter("Id"),
            new FieldDescAdapter("RA"),
            new FieldDescAdapter("DEC"),
            new FieldDescAdapter("PosErr"),
            new FieldDescAdapter("Mag")
        });

        Object[][] data = new Object[][]{
            {"GSC0285600314", "03:19:44.44", "+41:30:58.21", new Double(0.2), new Double(13.98)},
            {"GSC0285601162", "03:19:51.15", "+41:31:25.97", new Double(0.2), new Double(12.93)},
            {"GSC0285601446", "03:19:55.54", "+41:31:23.74", new Double(0.2), new Double(13.72)},
            {"GSC0285601402", "03:19:52.48", "+41:33:0.04", new Double(0.2), new Double(14.67)},
            {"GSC0285601758", "03:19:48.56", "+41:28:9.44", new Double(0.2), new Double(13.78)},
            {"GSC0285600856", "03:19:37.23", "+41:29:9.78", new Double(1.2), new Double(13.94)},
            {"GSC0285601330", "03:19:51.48", "+41:27:24.41", new Double(0.2), new Double(11.31)},
            {"GSC0285600582", "03:19:54.12", "+41:33:48.49", new Double(0.2), new Double(10.75)},
            {"GSC0285601492", "03:19:43.79", "+41:27:25.56", new Double(0.2), new Double(14.56)},
            {"GSC0286902884", "03:20:0.87", "+41:33:14.00", new Double(0.2), new Double(13.07)},
            {"GSC0285600662", "03:19:51.46", "+41:34:25.36", new Double(0.2), new Double(11.67)},
            {"GSC0285601540", "03:19:34.87", "+41:33:42.80", new Double(0.2), new Double(13.62)},
            {"GSC0285600832", "03:19:36.32", "+41:34:21.40", new Double(0.2), new Double(13.69)},
            {"GSC0285600098", "03:19:32.99", "+41:34:15.42", new Double(0.4), new Double(13.40)},
            {"GSC0286902678", "03:20:4.95", "+41:34:21.76", new Double(0.2), new Double(13.08)},
            {"GSC0285601066", "03:19:34.23", "+41:34:50.23", new Double(0.2), new Double(12.10)},
            {"GSC0285601092", "03:19:47.80", "+41:35:46.82", new Double(0.2), new Double(12.93)},
            {"GSC0285600288", "03:19:21.10", "+41:31:19.74", new Double(0.2), new Double(13.13)},
            {"GSC0285601350", "03:19:21.30", "+41:29:27.53", new Double(1.2), new Double(10.51)},
            {"GSC0285601426", "03:19:40.49", "+41:35:44.59", new Double(0.2), new Double(12.26)},
            {"GSC0285601660", "03:19:19.80", "+41:32:2.54", new Double(0.2), new Double(13.65)},
            {"GSC0285600050", "03:19:28.14", "+41:26:29.00", new Double(0.2), new Double(13.49)},
            {"GSC0285600676", "03:19:17.86", "+41:30:25.06", new Double(0.2), new Double(13.87)},
            {"GSC0286902672", "03:20:17.31", "+41:32:34.26", new Double(0.2), new Double(12.47)},
            {"GSC0285600980", "03:19:36.51", "+41:25:2.75", new Double(0.2), new Double(11.09)},
            {"GSC0285600418", "03:19:24.11", "+41:34:44.58", new Double(0.2), new Double(11.69)},
            {"GSC0285600616", "03:19:37.64", "+41:36:26.75", new Double(0.2), new Double(12.20)},
            {"GSC0285601598", "03:19:15.86", "+41:31:58.58", new Double(0.2), new Double(13.45)},
            {"GSC0286902860", "03:20:19.22", "+41:33:4.93", new Double(0.2), new Double(12.98)},
            {"GSC0285601584", "03:19:55.61", "+41:24:30.49", new Double(0.2), new Double(14.35)},
            {"GSC0285601412", "03:19:25.94", "+41:35:25.62", new Double(0.2), new Double(13.91)},
            {"GSC0286902669", "03:20:22.14", "+41:32:23.46", new Double(0.2), new Double(11.55)},
            {"GSC0285601044", "03:19:45.74", "+41:24:6.01", new Double(0.2), new Double(13.97)},
            {"GSC0286902594", "03:20:15.39", "+41:34:55.92", new Double(0.2), new Double(11.76)},
            {"GSC0285601824", "03:19:22.38", "+41:25:46.16", new Double(0.2), new Double(13.07)}
        };

        setDataVector(data, columnNames);
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex >= 3)
            return Double.class;
        return String.class;
    }
}


