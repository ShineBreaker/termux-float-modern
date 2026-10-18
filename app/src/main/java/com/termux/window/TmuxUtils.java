package com.termux.window;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for talking to the tmux server that runs inside Termux.
 *
 * tmux is used as the shared session layer between Termux:Float and the Termux app: both attach to
 * the same tmux session over the tmux socket, which is what makes a session opened in one app
 * visible and usable in the other. A plain shell session cannot be shared this way, since its pty
 * belongs to the process that created it and Termux does not expose an API to adopt foreign
 * sessions.
 */
public final class TmuxUtils {

    private static final String LOG_TAG = "TmuxUtils";

    /** Absolute path of the tmux client binary inside the Termux prefix. */
    public static final String TMUX_EXECUTABLE_PATH = TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/tmux";

    /** Session name used when the user does not pick one. */
    public static final String DEFAULT_SESSION_NAME = "termux-float";

    /** Format used to list sessions in a line based, machine readable way. */
    static final String LIST_FORMAT = "#{session_name}|#{session_windows}|#{session_attached}";

    private TmuxUtils() {}

    /** A tmux session as reported by {@code tmux list-sessions}. */
    public static final class TmuxSession {
        public final String name;
        public final int windows;
        public final boolean attached;

        TmuxSession(@NonNull String name, int windows, boolean attached) {
            this.name = name;
            this.windows = windows;
            this.attached = attached;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    /** Whether the tmux package is installed inside Termux. */
    public static boolean isTmuxInstalled() {
        return new File(TMUX_EXECUTABLE_PATH).exists();
    }

    /** Arguments that attach to {@code sessionName}, creating the session when it is missing. */
    @NonNull
    public static String[] buildAttachArguments(@NonNull String sessionName) {
        return new String[]{"new-session", "-A", "-s", sessionName};
    }

    /**
     * Shell name used for float window sessions. The Termux app can then reuse an existing session
     * with the same name instead of stacking up windows for the same tmux session.
     */
    @NonNull
    public static String buildShellName(@NonNull String sessionName) {
        return "tmux:" + sessionName;
    }

    /**
     * Strip characters that tmux does not accept in session names ({@code .} and {@code :} are
     * target separators) and fall back to {@link #DEFAULT_SESSION_NAME} for empty results.
     */
    @NonNull
    public static String sanitizeSessionName(@Nullable String name) {
        String sanitized = name == null ? "" : name.trim().replaceAll("[.:]", "-");
        return sanitized.isEmpty() ? DEFAULT_SESSION_NAME : sanitized;
    }

    /** Parse one {@link #LIST_FORMAT} line, returning null when the line is not a session entry. */
    @Nullable
    public static TmuxSession parseSession(@Nullable String line) {
        if (line == null) return null;

        String[] parts = line.trim().split("\\|");
        if (parts.length != 3 || parts[0].trim().isEmpty()) return null;

        try {
            return new TmuxSession(parts[0].trim(), Integer.parseInt(parts[1].trim()), "1".equals(parts[2].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * List the sessions known to the tmux server, or an empty list when tmux is not installed or
     * no server is running. Must not be called on the main thread.
     */
    @NonNull
    public static List<TmuxSession> listSessions(@NonNull Context context) {
        List<TmuxSession> sessions = new ArrayList<>();
        if (!isTmuxInstalled()) return sessions;

        try {
            Process process = newProcessBuilder(context, "list-sessions", "-F", LIST_FORMAT).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    TmuxSession session = parseSession(line);
                    if (session != null) sessions.add(session);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to list tmux sessions", e);
        }

        return sessions;
    }

    /** A process builder running the tmux client with the Termux shell environment. */
    @NonNull
    static ProcessBuilder newProcessBuilder(@NonNull Context context, @NonNull String... arguments) {
        List<String> command = new ArrayList<>();
        command.add(TMUX_EXECUTABLE_PATH);
        for (String argument : arguments) command.add(argument);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().clear();
        builder.environment().putAll(new TermuxShellEnvironment().getEnvironment(context, false));
        // Keep stderr in the same stream: tmux reports "no server running" there, and the parser
        // simply skips lines that are not session entries.
        builder.redirectErrorStream(true);
        return builder;
    }

}
