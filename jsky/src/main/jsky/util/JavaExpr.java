/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: JavaExpr.java,v 1.3 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.util;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.DVResolver;
import gnu.jel.Evaluator;
import gnu.jel.Library;

import java.util.Hashtable;
import java.util.Vector;
import java.lang.Math;


/**
 * This class is a simple wrapper for the JEL package and provides support
 * for evaluating dynamic, numerical expressions in Java.
 *
 * @author Allan Brighton
 */
public class JavaExpr implements DVResolver {

    // the compiled expression
    private CompiledExpression _expr;

    // Used to access dynamic variables
    private Object[] _context = new Object[1];

    // Vector of VarValue: Maps variable names to values
    private Vector _vars = new Vector();

    // Cached number of variables (since it is often only 1)
    private int _numVars = 0;


    // Export the static methods of java.lang.Math
    private static final Class[] _stLib = new Class[]{
        Math.class
    };

    // Export YYY getXXXProperty() methods for dynamic variable access
    private Class[] _dynLib = new Class[]{
        JavaExpr.class
    };

    // stores the value for a variable name
    class VarValue {

        public String name;
        public double value;

        public VarValue(String s, double v) {
            name = s;
            value = v;
        }
    }


    /**
     * Initialize and compile a new expression. Any variables used in the
     * expression must be set with the setVar method of this class.
     *
     * @param exprStr contains the expression string
     */
    public JavaExpr(String exprStr) throws Throwable {
        // The namespace is defined by constructing the library class
        Library lib = new Library(_stLib, _dynLib, null, this);

        // setup the global context and data
        _context[0] = this; // this pointer for YYY getXXXProperty() methods

        // compile the expression
        _expr = Evaluator.compile(exprStr, lib);
    }


    /**
     * Initialize and compile a new expression. Variables used in the
     * expression are resolved using the given resolver object.
     *
     * @param exprStr contains the expression string
     * @param resolver used to resolve variable names in expressions
     */
    public JavaExpr(String exprStr, DVResolver resolver) throws Throwable {
        _dynLib[0] = resolver.getClass();

        // The namespace is defined by constructing the library class
        Library lib = new Library(_stLib, _dynLib, null, resolver);

        // setup the global context and data
        _context[0] = resolver; // this pointer for YYY getXXXProperty() methods

        // compile the expression
        _expr = Evaluator.compile(exprStr, lib);
    }


    /** Implements the DVResolver interface */
    public String getTypeName(String name) {
        return "Double";
    }

    /** Called by reflection for the DVResolver interface to get the value of the named variable */
    public double getDoubleProperty(String name) {
        for (int i = 0; i < _numVars; i++) {
            VarValue v = (VarValue) _vars.get(i);
            if (v.name.equals(name))
                return v.value;
        }
        return 0.0;
    }

    /**
     * Set the value of the given variable to the given value.
     * <p>
     * Note that variable names must conform to Java syntax ("$X" is allowed,
     * but not ${X}, for example).
     */
    public void setVar(String name, double value) {
        for (int i = 0; i < _numVars; i++) {
            VarValue v = (VarValue) _vars.get(i);
            if (v.name.equals(name)) {
                v.value = value;
                return;
            }
        }
        _vars.add(new VarValue(name, value));
        _numVars++;
    }

    /** Evaluate the expression and return the result. */
    public double eval() throws Throwable {
        return _expr.evaluate_double(_context);
    }

    /** Evaluate the expression and return the result as a boolean. */
    public boolean evalBoolean() throws Throwable {
        return _expr.evaluate_boolean(_context);
    }

    /** Evaluate the expression and return the result as an Object. */
    public Object evalObject() throws Throwable {
        return _expr.evaluate(_context);
    }

    /**
     * test main
     */
    public static void main(String[] args) {
        try {
            // use default variable resolver
            String exprStr = "$XYZ*2";
            JavaExpr expr = new JavaExpr(exprStr);
            double x = 10.1;
            expr.setVar("$XYZ", x);
            System.out.println("$XYZ = " + x + ", " + exprStr + " = " + expr.eval());

            x = 20;
            expr.setVar("$XYZ", x);
            System.out.println("$XYZ = " + x + ", " + exprStr + " = " + expr.eval());

            // try a constant double expression
            expr = new JavaExpr("22.0");
            System.out.println("eval 22.0 = " + expr.eval());

            // try a constant integer expression (should convert to double)
            expr = new JavaExpr("123456789");
            System.out.println("eval 123456789 = " + expr.eval());
            Object o = expr.evalObject();
            System.out.println("evalObject 123456789 = " + o + ": " + o.getClass());
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }
}

