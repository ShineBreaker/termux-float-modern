package com.termux.window;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;

import com.termux.shared.data.IntentUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.notification.NotificationUtils;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE;
import com.termux.shared.termux.TermuxConstants.TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

public class TermuxFloatService extends Service {

    private TermuxFloatView mFloatingWindow;

    private TermuxSession mSession;

    private boolean mVisibleWindow = true;

    private static final String LOG_TAG = "TermuxFloatService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        runStartForeground();
        TermuxFloatApplication.setLogConfig(this, false);
        Logger.logVerbose(LOG_TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        if (mFloatingWindow == null && !initializeFloatView())
            return Service.START_NOT_STICKY;

        String action = null;
        if (intent != null) {
            Logger.logVerboseExtended(LOG_TAG, "Received intent:\n" + IntentUtils.getIntentString(intent));
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case TERMUX_FLOAT_SERVICE.ACTION_STOP_SERVICE:
                    actionStopService();
                    break;
                case TERMUX_FLOAT_SERVICE.ACTION_SHOW:
                    setVisible(true);
                    break;
                case TERMUX_FLOAT_SERVICE.ACTION_HIDE:
                    setVisible(false);
                    break;
                default:
                    Logger.logError(LOG_TAG, "Invalid action: \"" + action + "\"");
                    break;
            }
        } else if (!mVisibleWindow) {
            // Show window if hidden when launched through launcher icon.
            setVisible(true);
        }

        return Service.START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.logVerbose(LOG_TAG, "onDestroy");

        if (mFloatingWindow != null)
            mFloatingWindow.closeFloatingWindow();

        runStopForeground();
    }
    /** Request to stop service. */
    public void requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service");
        runStopForeground();
        stopSelf();
    }

    /** Process action to stop service. */
    private void actionStopService() {
        if (mSession != null)
            mSession.killIfExecuting(this, false);
        requestStopService();
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }



    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationUtils.setupNotificationChannel(this, TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID,
                TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    private Notification buildNotification() {
        final Resources res = getResources();

        final String notificationText = res.getString(mVisibleWindow ? R.string.notification_message_visible : R.string.notification_message_hidden);

        final String intentAction = mVisibleWindow ? TERMUX_FLOAT_SERVICE.ACTION_HIDE : TERMUX_FLOAT_SERVICE.ACTION_SHOW;
        Intent notificationIntent = new Intent(this, TermuxFloatService.class).setAction(intentAction);
        PendingIntent contentIntent = PendingIntent.getService(this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        // Build the notification
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(this,
                TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID, Notification.PRIORITY_LOW,
                TermuxConstants.TERMUX_FLOAT_APP_NAME, notificationText, null,
                contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.mipmap.ic_service_notification);

        // Set background color for small notification icon
        builder.setColor(0xFF000000);

        // TermuxSessions are always ongoing
        builder.setOngoing(true);

        // Set Exit button action
        Intent exitIntent = new Intent(this, TermuxFloatService.class).setAction(TERMUX_FLOAT_SERVICE.ACTION_STOP_SERVICE);
        builder.addAction(android.R.drawable.ic_delete, res.getString(R.string.notification_action_exit),
                PendingIntent.getService(this, 0, exitIntent,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0));

        return builder.build();
    }



    @SuppressLint("InflateParams")
    private boolean initializeFloatView() {
        boolean floatWindowWasNull = false;
        if (mFloatingWindow == null) {
            mFloatingWindow = (TermuxFloatView) ((LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_main, null);
            floatWindowWasNull = true;
        }

        mFloatingWindow.initFloatView(this);

        // The initial session attaches to the shared tmux session when tmux is installed,
        // so the Termux app can see and attach it too; a plain shell is the fallback.
        if (TmuxUtils.isTmuxInstalled())
            switchToTmuxSession(TmuxUtils.DEFAULT_SESSION_NAME);
        if (mSession == null) {
            mSession = createTermuxSession(
                    new ExecutionCommand(0, null, null, null, mFloatingWindow.getProperties().getDefaultWorkingDirectory(), ExecutionCommand.Runner.TERMINAL_SESSION.getName(), false), null);
            if (mSession == null)
                return false;
            mFloatingWindow.getTerminalView().attachSession(mSession.getTerminalSession());
        }

        try {
            mFloatingWindow.launchFloatingWindow();
        } catch (Exception e) {
            Logger.logStackTrace(LOG_TAG, e);
            // Settings.canDrawOverlays() does not work (always returns false, perhaps due to sharedUserId?).
            // So instead we catch the exception and prompt here.
            startActivity(new Intent(this, TermuxFloatPermissionActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            requestStopService();
            return false;
        }

        if (floatWindowWasNull)
            Logger.showToast(this, getString(R.string.initial_instruction_toast), true);

        return true;
    }

    private void setVisible(boolean newVisibility) {
        mVisibleWindow = newVisibility;
        mFloatingWindow.setVisibility(newVisibility ? View.VISIBLE : View.GONE);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_ID, buildNotification());
    }



    /** Create a {@link TermuxSession}. */
    @Nullable
    public synchronized TermuxSession createTermuxSession(ExecutionCommand executionCommand, String sessionName) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession");

        if (ExecutionCommand.Runner.APP_SHELL.getName().equals(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring a background execution command passed to createTermuxSession()");
            return null;
        }

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        executionCommand.shellName = sessionName;
        executionCommand.terminalTranscriptRows = mFloatingWindow.getProperties().getTerminalTranscriptRows();
        TermuxSession newTermuxSession = TermuxSession.execute(this, executionCommand,
                mFloatingWindow.getTermuxFloatSessionClient(), null, new TermuxShellEnvironment(),
                null, executionCommand.isPluginExecutionCommand);
        if (newTermuxSession == null) {
            Logger.logError(LOG_TAG, "Failed to execute new TermuxSession command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            return null;
        }

        // Emulator won't be set at this point so colors won't be set by TermuxFloatSessionClient.checkForFontAndColors()
        mFloatingWindow.reloadViewStyling();

        return newTermuxSession;
    }

    public TermuxSession getTermuxSession() {
        return mSession;
    }

    public TerminalSession getCurrentSession() {
        return mSession != null ? mSession.getTerminalSession() : null;
    }

    /**
     * Attach the float window to the given tmux session, creating the session when it does not
     * exist yet. The session is shared with the Termux app, which can attach to the same name.
     */
    public void switchToTmuxSession(@NonNull String sessionName) {
        if (mFloatingWindow == null) return;

        Logger.logDebug(LOG_TAG, "Switching float window to tmux session \"" + sessionName + "\"");

        ExecutionCommand executionCommand = new ExecutionCommand(0, TmuxUtils.TMUX_EXECUTABLE_PATH,
                TmuxUtils.buildAttachArguments(sessionName), null,
                mFloatingWindow.getProperties().getDefaultWorkingDirectory(),
                ExecutionCommand.Runner.TERMINAL_SESSION.getName(), false);

        TermuxSession oldSession = mSession;
        TermuxSession newSession = createTermuxSession(executionCommand, TmuxUtils.buildShellName(sessionName));
        if (newSession == null) {
            Logger.logError(LOG_TAG, "Failed to attach the float window to tmux session \"" + sessionName + "\"");
            return;
        }

        mSession = newSession;
        mFloatingWindow.getTerminalView().attachSession(newSession.getTerminalSession());
        mFloatingWindow.showTouchKeyboard();

        // Kill the previous session only once the new one is in place, so that its
        // onSessionFinished() callback cannot stop the service mid-switch.
        if (oldSession != null)
            oldSession.killIfExecuting(this, false);
    }

    /**
     * Ask the Termux app to open - or focus - a session attached to the given tmux session, so the
     * session can be used from the main app while it stays attached here.
     */
    public void openTmuxSessionInTermuxApp(@NonNull String sessionName) {
        TermuxAppSharedProperties termuxAppProperties = TermuxAppSharedProperties.init(this);
        if (termuxAppProperties == null || !termuxAppProperties.shouldAllowExternalApps()) {
            Logger.showToast(this, getString(R.string.sessions_dialog_open_in_termux_failed), true);
            return;
        }

        Intent intent = new Intent();
        intent.setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME);
        intent.setAction(RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, TmuxUtils.TMUX_EXECUTABLE_PATH);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, TmuxUtils.buildAttachArguments(sessionName));
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_WORKDIR, TermuxConstants.TERMUX_HOME_DIR_PATH);
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_RUNNER, ExecutionCommand.Runner.TERMINAL_SESSION.getName());
        // The shell name is derived from the tmux session so that repeated requests focus the
        // existing Termux session instead of stacking up new ones.
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_SHELL_NAME, TmuxUtils.buildShellName(sessionName));
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_SHELL_CREATE_MODE, ExecutionCommand.ShellCreateMode.NO_SHELL_WITH_NAME.getMode());
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_SESSION_ACTION,
                String.valueOf(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY));
        intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_LABEL, getString(R.string.sessions_dialog_open_in_termux));

        try {
            startService(intent);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to hand tmux session over to the Termux app", e);
            Logger.showToast(this, getString(R.string.sessions_dialog_open_in_termux_failed), true);
        }
    }

}
