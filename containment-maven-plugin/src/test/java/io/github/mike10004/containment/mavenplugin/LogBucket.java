package io.github.mike10004.containment.mavenplugin;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.Logger;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class LogBucket extends DefaultLog {

    private final CollectingLogger collector;

    public LogBucket() {
        this(new CollectingLogger());
    }

    private LogBucket(CollectingLogger collector) {
        super(collector);
        this.collector = requireNonNull(collector);
    }

    public List<LogEntry> getEntries() {
        return Collections.unmodifiableList(collector.entries);
    }

    public String dump() {
        return collector.dump();
    }

    /**
     * Reimplementation of {@link org.codehaus.plexus.logging.console.ConsoleLogger}
     * that captures everything in a buffer.
     */
    private static class CollectingLogger extends org.codehaus.plexus.logging.AbstractLogger {

        private final StringWriter bucket;
        private final PrintWriter bucketWriter;
        private final PrintWriter echoWriter;
        public final List<LogEntry> entries;

        // ----------------------------------------------------------------------
        // Constants
        // ----------------------------------------------------------------------

        private static final String[] TAGS = { "[DEBUG] ", "[INFO] ", "[WARNING] ", "[ERROR] ", "[FATAL ERROR] " };

        // ----------------------------------------------------------------------
        // Constructors
        // ----------------------------------------------------------------------

        public CollectingLogger( final int threshold, final String name, PrintStream echoStream)
        {
            super( threshold, name );
            bucket = new StringWriter(1024);
            bucketWriter = new PrintWriter(bucket, true);
            echoWriter = new PrintWriter(echoStream, true);
            entries = new ArrayList<>();
        }

        public CollectingLogger()
        {
            this( Logger.LEVEL_INFO, "console", System.out );
        }

        public String dump() {
            bucket.flush();
            return bucket.toString();
        }

        // ----------------------------------------------------------------------
        // Public methods
        // ----------------------------------------------------------------------

        public void debug( final String message, final Throwable throwable )
        {
            if ( isDebugEnabled() )
            {
                log( LEVEL_DEBUG, message, throwable );
            }
        }

        public void info( final String message, final Throwable throwable )
        {
            if ( isInfoEnabled() )
            {
                log( LEVEL_INFO, message, throwable );
            }
        }

        public void warn( final String message, final Throwable throwable )
        {
            if ( isWarnEnabled() )
            {
                log( LEVEL_WARN, message, throwable );
            }
        }

        public void error( final String message, final Throwable throwable )
        {
            if ( isErrorEnabled() )
            {
                log( LEVEL_ERROR, message, throwable );
            }
        }

        public void fatalError( final String message, final Throwable throwable )
        {
            if ( isFatalErrorEnabled() )
            {
                log( LEVEL_FATAL, message, throwable );
            }
        }

        public Logger getChildLogger( final String name )
        {
            return this;
        }

        // ----------------------------------------------------------------------
        // Implementation methods
        // ----------------------------------------------------------------------

        private void log(int level, String message, @Nullable Throwable throwable )
        {
            entries.add(new LogEntry(level, message, throwable));
            log(bucketWriter, level, message, throwable);
            log(echoWriter, level, message, throwable);
        }

        private static void log(PrintWriter out, int level, String message, @Nullable Throwable throwable )
        {
            out.println( TAGS[level].concat( message ) );
            if ( throwable != null )
            {
                throwable.printStackTrace( out );
            }
        }
    }

    public static class LogEntry {
        public final int level;
        public final String message;
        public final Throwable throwable;

        public LogEntry(int level, String message, Throwable throwable) {
            this.level = level;
            this.message = message;
            this.throwable = throwable;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", LogEntry.class.getSimpleName() + "[", "]")
                    .add("level=" + level)
                    .add("message='" + message + "'")
                    .add("throwable=" + throwable)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LogEntry)) return false;
            LogEntry logEntry = (LogEntry) o;
            return level == logEntry.level &&
                    Objects.equals(message, logEntry.message) &&
                    Objects.equals(throwable, logEntry.throwable);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, message, throwable);
        }
    }
}
