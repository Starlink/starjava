package uk.ac.starlink.ttools.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Manages export of a list of lines to the JSON format used for
 * serialization of Jupyter notebooks (.ipynb files).
 *
 * <p>Currently only source code cells containing python are supported.
 * The format was reverse-engineered by looking at an ipynb file
 * saved by the Jupyter installation I happen to have on my machine.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2020
 */
public class JupyterCell {

    private static final Map<?,?> EMPTY_MAP = new HashMap<Object,Object>();
    private static final List<?> EMPTY_LIST = new ArrayList<Object>();
    private final List<String> lines_;

    /**
     * Constructs a Jupyter code cell based on a given list of source code
     * lines.
     *
     * @param  lines  lines of code, no trailing newlines required 
     */
    public JupyterCell( List<String> lines ) {
        lines_ = lines;
    }

    /**
     * Convenience constructor for an array of lines.
     *
     * @param  lines  lines of code, no trailing newlines required 
     */
    public JupyterCell( String[] lines ) {
        this( Arrays.asList( lines ) );
    }

    /**
     * Turns this cell into a JSON object, suitable for export.
     *
     * @return  JSON representation of this cell
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put( "cell_type", "code" );
        json.put( "execution_count", JSONObject.NULL );
        json.put( "metadata", EMPTY_MAP );
        json.put( "outputs", EMPTY_LIST );
        json.put( "source",
                  lines_.stream().map( s -> s + "\n" )
                        .collect( Collectors.toList() ) );
        return json;
    }

    /**
     * Turns a list of cells into a JSON representation of a notebook,
     * suitable for export to an ipynb file.
     *
     * @param   cells  list of cells
     * @return   JSON representation of notebook
     */
    public static JSONObject toNotebook( List<JupyterCell> cells ) {
        JSONObject cmode = new JSONObject();
        cmode.put( "name", "ipython" );
        cmode.put( "version", 3 );

        JSONObject langinfo = new JSONObject();
        langinfo.put( "codemirror_mode", cmode );
        langinfo.put( "file_extension", ".py" );
        langinfo.put( "mimetype", "text/x-python" );
        langinfo.put( "name", "python" );
        langinfo.put( "nbconvert_exporter", "python" );
        langinfo.put( "pygments_lexer", "ipython3" );
        langinfo.put( "version", "3.6.7" );

        JSONObject kernelspec = new JSONObject();
        kernelspec.put( "display_name", "Python 3" );
        kernelspec.put( "language", "python" );
        kernelspec.put( "name", "python3" );

        JSONObject meta = new JSONObject();
        meta.put( "kernelspec", kernelspec );
        meta.put( "language_info", langinfo );
 
        JSONObject json = new JSONObject();
        json.put( "cells",
                  cells.stream()
                       .map( JupyterCell::toJson )
                       .collect( Collectors.toList() ) );
        json.put( "metadata", meta );
        json.put( "nbformat", 4 );
        json.put( "nbformat_minor", 2 );
        return json;
    }
}
