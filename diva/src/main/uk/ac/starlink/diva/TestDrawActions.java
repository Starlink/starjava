package uk.ac.starlink.diva;

import diva.canvas.JCanvas;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

public class TestDrawActions
    extends JCanvas
    implements Draw
{
    protected DrawGraphicsPane graphicsPane = null;

    public TestDrawActions()
    {
        //  Add a DrawGraphicsPane to use for displaying
        //  interactive graphics elements.
        graphicsPane =
            new DrawGraphicsPane( DrawActions.getTypedDecorator() );
        setCanvasPane( graphicsPane );
    }

    public DrawGraphicsPane getGraphicsPane()
    {
        return graphicsPane;
    }
    
    public Component getComponent()
    {
        return this;
    }

    public static void main( String[] args )
    {
        TestDrawActions canvas = new TestDrawActions();
        DrawActions drawActions = new DrawActions( canvas );
        JFrame frame = new JFrame( "TestDrawActions" );
        frame.setSize( new Dimension( 200, 200 ) );
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( canvas, BorderLayout.CENTER );
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar( menuBar );
        menuBar.add( new DrawGraphicsMenu( drawActions ) );
        frame.setVisible( true );
    }
}
