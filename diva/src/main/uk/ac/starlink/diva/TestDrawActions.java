package uk.ac.starlink.diva;

import diva.canvas.JCanvas;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import java.util.ListIterator;

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
        DrawFigureStore store = new DrawFigureStore( "diva",
                                                     "figurestore.xml",
                                                     "drawnfigures" );
        DrawActions drawActions = new DrawActions( canvas,  store );

        //  Add a re-defined character.
        FigureProps props = new FigureProps( 10.0, 10.0, 100.0, 50.0 );
        drawActions.createDrawFigure( DrawFigureFactory.ELLIPSE, props );

        JFrame frame = new JFrame( "TestDrawActions" );
        frame.setSize( new Dimension( 200, 200 ) );
        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( canvas, BorderLayout.CENTER );
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar( menuBar );
        menuBar.add( new DrawGraphicsMenu( drawActions ) );
        frame.setVisible( true );

        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        
        // Exit when window is closed.
        WindowListener closer = new WindowAdapter() {   
                public void windowClosing( WindowEvent e ) {
                    System.exit( 1 );
                }
            };
        frame.addWindowListener( closer );
    }
}
