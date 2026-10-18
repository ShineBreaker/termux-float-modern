package com.termux.window;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TmuxUtilsTest {

    @Test
    public void parseSessionReadsNameWindowsAndAttached() {
        TmuxUtils.TmuxSession session = TmuxUtils.parseSession("work|3|1");

        assertNotNull(session);
        assertEquals("work", session.name);
        assertEquals(3, session.windows);
        assertTrue(session.attached);
    }

    @Test
    public void parseSessionTreatsTrailingColumnsAsDetached() {
        TmuxUtils.TmuxSession session = TmuxUtils.parseSession("main|1|0");

        assertNotNull(session);
        assertFalse(session.attached);
    }

    @Test
    public void parseSessionSkipsMalformedLines() {
        // stderr is merged into the list output, so "no server running" lines must be ignored
        assertNull(TmuxUtils.parseSession("no server running on /data/data/com.termux/files/usr/tmp/tmux-10123/default"));
        assertNull(TmuxUtils.parseSession(""));
        assertNull(TmuxUtils.parseSession("work|many|1"));
        assertNull(TmuxUtils.parseSession("|2|0"));
        assertNull(TmuxUtils.parseSession(null));
    }

    @Test
    public void sanitizeSessionNameStripsTargetSeparators() {
        assertEquals("my-session", TmuxUtils.sanitizeSessionName("my.session"));
        assertEquals("-a-b-", TmuxUtils.sanitizeSessionName(":a:b:"));
        assertEquals("padded", TmuxUtils.sanitizeSessionName("  padded  "));
    }

    @Test
    public void sanitizeSessionNameFallsBackToDefault() {
        assertEquals(TmuxUtils.DEFAULT_SESSION_NAME, TmuxUtils.sanitizeSessionName("   "));
        assertEquals(TmuxUtils.DEFAULT_SESSION_NAME, TmuxUtils.sanitizeSessionName(null));
    }

    @Test
    public void buildAttachArgumentsAttachOrCreate() {
        assertArrayEquals(new String[]{"new-session", "-A", "-s", "work"},
                TmuxUtils.buildAttachArguments("work"));
    }

    @Test
    public void buildShellNameIsNamespacedByTmux() {
        assertEquals("tmux:work", TmuxUtils.buildShellName("work"));
    }
}
