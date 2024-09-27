package uk.ac.starlink.ttools.task;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.StringContent;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;

/**
 * Handles export of StiltsCommand objects to external
 * serialization formats.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2024
 */
public class CommandFormatter {

    private final int continueIndent_;
    private final int levelIndent_;
    private final int cwidth_;
    private final CredibleString invocation_;
    private final LineEnder lineEnder_;
    private final boolean includeDflts_;
    private final Color syntaxColor_;
    private boolean forceError_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     *
     * @param  invocation     display text to introduce the STILTS command
     * @param  includeDflts   if true, all parameters are included;
     *                        if false, only those with non-default values
     * @param  lineEnder      line end presentation policy
     * @param  levelIndent    number of spaces per indentation level
     * @param  cwidth         nominal formatting width in characters;
     *                        this affects line wrapping, but actual
     *                        wrapping may depend on other factors too
     */
    public CommandFormatter( CredibleString invocation, boolean includeDflts,
                             LineEnder lineEnder, int levelIndent,
                             int cwidth ) {
        invocation_ = invocation;
        includeDflts_ = includeDflts;
        lineEnder_ = lineEnder;
        levelIndent_ = levelIndent;
        cwidth_ = cwidth;
        continueIndent_ = 1;
        syntaxColor_ = new Color( 0xa0a0ff );
    }

    /**
     * Returns the invocation used to introduce the STILTS command.
     *
     * @return  STILTS invocation string
     */
    public CredibleString getInvocation() {
        return invocation_;
    }

    /**
     * Sets whether the generated stilts commands will be made to
     * produce a gratuitous error.  This is only useful for debugging
     * purposes.
     *
     * @param  forceError  true to force an error from generated commands
     */
    public void setForceError( boolean forceError ) {
        forceError_ = forceError;
    }

    /**
     * Creates a task Executable based on the state of this formatter.
     * Various exceptions may be thrown if there is some error.
     * Such errors are quite possible.
     *
     * <p>If this method returns without error there is a fair chance
     * that the serializations predicted from this object will represent
     * legal STILTS commands.
     *
     * @param  command  command specification
     * @return  executable
     * @throws  TaskException  if there is some other error in setting up
     *                         the executable; probably incorrect parameter
     *                         assignments of some kind
     */
    public Executable createExecutable( StiltsCommand command )
            throws TaskException {

        /* Populate an execution environment that will throw an
         * exception if there is an attempt to add the same parameter twice. */
        MapEnvironment env = new MapEnvironment() {
            final Map<String,Object> map = getMap();
            @Override
            public MapEnvironment setValue( String paramName, Object value ) {
                if ( map.containsKey( paramName ) ) {
                    throw new IllegalStateException( "Duplicate parameter: "
                                                   + paramName );
                }
                return super.setValue( paramName, value );
            }
        };
        try {
            populateEnvironment( command, env );
        }
        catch ( RuntimeException e ) {
            throw new TaskException( e.getMessage(), e );
        }

        /* Create the executable. */
        Task task = command.getTask();
        Executable exec = task.createExecutable( env );

        /* Complain if any arguments are unused, otherwise return. */
        String[] unused = stripExpectedUnused( task, env.getUnused() );
        if ( unused.length > 0 ) {
            throw new TaskException( LineInvoker.getUnusedWarning( unused ) );
        }
        else {
            return exec;
        }
    }

    /**
     * Returns a Document, suitable for use with a JTextPane,
     * formatting the given plot specification.  This may include
     * coloured highlighting etc. depending on configuration.
     *
     * @param   command   command specification
     */
    public StyledDocument createShellDocument( StiltsCommand command ) {
        StyleContext context = StyleContext.getDefaultStyleContext();
        AttributeSet plain = context.getEmptySet();
        AttributeSet faint =
            context.addAttribute( plain, StyleConstants.Foreground,
                                  Color.LIGHT_GRAY );
        AttributeSet syntax =
            syntaxColor_ == null
                ? plain
                : context.addAttribute( plain, StyleConstants.Foreground,
                                        syntaxColor_ );
        AttributeSet warning1 =
            context.addAttribute( plain, StyleConstants.Foreground,
                                  Color.BLUE );
        AttributeSet warning2 =
            context.addAttribute( plain, StyleConstants.Foreground,
                                  Color.RED );
        StyledDocument doc = new DefaultStyledDocument();
        AttributeSet invokeStyle =
                 getStyle( invocation_.getCredibility(),
                           plain, warning1, warning2 );
        String invokeTxt = invocation_.getValue();
        addText( doc, invokeTxt + " ", invokeStyle );
        if ( invokeTxt.length() > 16 ) {
            addText( doc, getPrefix( 0 ), faint );
        }
        addText( doc, command.getTaskName() + " ", plain );
        for ( SettingGroup g : getGroups( command ) ) {
            List<Setting> settings = getSettings( g );
            if ( settings.size() > 0 ) {
                String prefix = getPrefix( g.getLevel() );
                int icr = prefix.lastIndexOf( "\n" );
                boolean hasNewline = icr >= 0;
                int npre = prefix.length() - icr - 1;
                int npost = hasNewline ? icr : 0;
                String prefixCont = hasNewline
                                  ? prefix + spaces( continueIndent_ )
                                  : prefix;
                int npreCont = hasNewline ? npre + continueIndent_ : npre;
                addText( doc, prefix, faint );
                int nc = npre;
                for ( Setting s : settings ) {
                    boolean isDflt = s.isDefaultValue();
                    AttributeSet keyStyle = isDflt ? faint : plain;
                    AttributeSet equStyle = isDflt ? faint : syntax;
                    AttributeSet valStyle =
                        getStyle( s.getCredibility(),
                                  keyStyle, warning1, warning2 );
                    String key = s.getKey();
                    String val = Setting.shellQuote( s.getStringValue() );
                    int wleng = key.length() + 1 + val.length() + 1;
                    if ( nc + wleng + npre > cwidth_ && nc > npre ) {
                        addText( doc, prefixCont, faint );
                        nc = npreCont;
                    }
                    addText( doc, key, keyStyle );
                    addText( doc, "=", equStyle );
                    addText( doc, val, valStyle );
                    addText( doc, " ", plain );
                    nc += wleng;
                }
            }
        }
        return doc;
    }

    /**
     * Returns the list of settings from a group that should be
     * actually formatted by this formatter.
     * Settings with default values may be omitted according to
     * formatter configuration.
     *
     * @param  group   setting group
     * @return  settings to format
     */
    private List<Setting> getSettings( SettingGroup group ) {
        List<Setting> list = new ArrayList<>();
        for ( Setting s : group.getSettings() ) {
            if ( includeDflts_ || ! s.isDefaultValue() ) {
                list.add( s );
            }
        }
        return list;
    }

    /**
     * Remove words from the list of unused words that are harmless.
     *
     * <p>This is a hook for a hack.
     * Some of the parameter settings can be unused when object values
     * for other parameter settings are used.
     * If that happens, it looks like there is a problem because settings
     * have not been used.  This routine can pull them out so
     * the warning goes away.
     *
     * @param  task   task
     * @param  words  input command word list
     * @return   list apart from any words that shouldn't be there
     */
    protected String[] stripExpectedUnused( Task task, String[] words ) {
        return words;
    }

    /**
     * Returns the groups associated with a given plot.
     *
     * @param  command   command specification
     * @return   groups in command specification
     */
    private List<SettingGroup> getGroups( StiltsCommand command ) {
        List<SettingGroup> list =
            new ArrayList<>( Arrays.asList( command.getGroups() ) );
        if ( forceError_ ) {
            list.add( new SettingGroup( 0, new Setting[] {
                new Setting( "force_error", "true", null ),
            } ) );
        }
        return list;
    }

    /**
     * Adds entries to a supplied execution environment corresponding to
     * the STILTS parameters defined by this specification.
     *
     * @param  command  command specification
     * @param  env  execution environment to populate;
     *              should probably be empty on entry
     */
    public void populateEnvironment( StiltsCommand command,
                                     MapEnvironment env ) {
        for ( SettingGroup g : getGroups( command ) ) {
            for ( Setting s : getSettings( g ) ) {
                String key = s.getKey();
                Object objVal = s.getObjectValue();
                env.setValue( key, objVal == null ? s.getStringValue()
                                                  : objVal );
            }
        }
    }

    /**
     * Adds a collection of setting objects to a MapEnvironment.
     *
     * @param  env  execution environment to modify
     * @param  settings  settings to add; nulls may be included
     */
    private void envSettings( MapEnvironment env,
                              Collection<Setting> settings ) {
        for ( Setting s : settings ) {
            if ( s != null && ! s.isDefaultValue() ) {
                String key = s.getKey();
                Object objVal = s.getObjectValue();
                env.setValue( key, objVal == null ? s.getStringValue()
                                                  : objVal );
            }
        }
    }

    /**
     * Appends a given string to a given document using a given attribute set.
     *
     * @param  doc  target document
     * @param  text   text to append
     * @param  attSet   style
     */
    private static void addText( Document doc, String text,
                                 AttributeSet attSet ) {
        try {
            doc.insertString( doc.getLength(), text, attSet );
        }
        catch ( BadLocationException e ) {
            logger_.log( Level.SEVERE, "Can't insert string?", e );
            assert false;
        }
    }

    /**
     * Returns a style switched by credibility.
     *
     * @param   cred   credibility, not null
     * @param   yesStyle  style for YES
     * @param   maybeStyle  style for MAYBE
     * @param   noStyle  style for NO
     * @return  chosen style
     */
    private static AttributeSet getStyle( Credibility cred,
                                          AttributeSet yesStyle,
                                          AttributeSet maybeStyle,
                                          AttributeSet noStyle ) {
        switch ( cred ) {
            case YES:
                return yesStyle;
            case MAYBE:
                return maybeStyle;
            case NO:
                return noStyle;
            default:
                assert false;
                return yesStyle;
        }
    }

    /**
     * Returns the line prefix for a given level.
     * This includes an end-of-line string, so terminating the
     * previous line.
     *
     * @param  level   grouping level
     * @return     line end/start prefix text
     * @see    <i>Goldfarb's First Law of Text Processing</i>
     */
    private String getPrefix( int level ) {
        if ( lineEnder_.includesNewline() ) {
            return new StringBuffer()
                  .append( lineEnder_.getEndOfLine() )
                  .append( spaces( level * levelIndent_ ) )
                  .toString();
        }
        else {
            return "";
        }
    }

    /**
     * Utility method returning a StyledDocument instance with some
     * supplied plain text.
     *
     * @param  txt   text content
     * @return  document
     */
    public static StyledDocument createBasicDocument( String txt ) {
        StringContent content = new StringContent();
        try {
            content.insertString( 0, txt );
        }
        catch ( BadLocationException e ) {
            assert false;
        }
        StyleContext context = StyleContext.getDefaultStyleContext();
        return new DefaultStyledDocument( content, context );
    }

    /**
     * Utility method to return a fixed-length string of spaces.
     *
     * @param   n   required length
     * @return  <code>n</code>-character string
     */
    private static String spaces( int n ) {
        StringBuffer sbuf = new StringBuffer( n );
        for ( int i = 0; i < n; i++ ) {
            sbuf.append( ' ' );
        }
        return sbuf.toString();
    }
}
