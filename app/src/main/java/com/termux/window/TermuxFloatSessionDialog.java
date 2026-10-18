package com.termux.window;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dialog that lists the tmux sessions known to Termux and lets the user attach one in the float
 * window or hand it over to the Termux app.
 */
class TermuxFloatSessionDialog {

    private final TermuxFloatService mService;

    private final Dialog mDialog;
    private final ScrollView mScrollView;
    private final LinearLayout mSessionList;
    private final TextView mHint;
    private final TextView mEmpty;
    private final EditText mNewSessionName;
    private final Button mCreateButton;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    TermuxFloatSessionDialog(@NonNull TermuxFloatService service) {
        mService = service;

        mDialog = new Dialog(service);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.dialog_sessions);

        mScrollView = mDialog.findViewById(R.id.sessions_scroll);
        mSessionList = mDialog.findViewById(R.id.session_list);
        mHint = mDialog.findViewById(R.id.sessions_hint);
        mEmpty = mDialog.findViewById(R.id.sessions_empty);
        mNewSessionName = mDialog.findViewById(R.id.new_session_name);
        mCreateButton = mDialog.findViewById(R.id.create_session_button);

        mCreateButton.setOnClickListener(v -> createSession());

        configureWindow();
    }

    /** Show the dialog and reload the session list. */
    void show() {
        if (!mDialog.isShowing()) mDialog.show();
        refresh();
    }

    void dismiss() {
        mDialog.dismiss();
    }

    /** Release the worker thread; the dialog cannot be reused afterwards. */
    void release() {
        mExecutor.shutdown();
    }

    /**
     * The dialog is opened from a service rather than from an activity, so it needs an overlay
     * window type of its own and an explicit size.
     */
    private void configureWindow() {
        Window window = mDialog.getWindow();
        if (window == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        else
            window.setType(WindowManager.LayoutParams.TYPE_PHONE);

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.5f);

        DisplayMetrics metrics = mService.getResources().getDisplayMetrics();
        window.setLayout((int) (metrics.widthPixels * 0.9f), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Reload the session list from the tmux server without blocking the main thread. */
    private void refresh() {
        if (!TmuxUtils.isTmuxInstalled()) {
            mHint.setText(R.string.sessions_dialog_tmux_missing);
            mNewSessionName.setVisibility(View.GONE);
            mCreateButton.setVisibility(View.GONE);
            mEmpty.setVisibility(View.GONE);
            mSessionList.removeAllViews();
            return;
        }

        mHint.setText(R.string.sessions_dialog_hint);
        mNewSessionName.setVisibility(View.VISIBLE);
        mCreateButton.setVisibility(View.VISIBLE);

        mExecutor.execute(() -> {
            List<TmuxUtils.TmuxSession> sessions = TmuxUtils.listSessions(mService);
            mSessionList.post(() -> populate(sessions));
        });
    }

    private void populate(@NonNull List<TmuxUtils.TmuxSession> sessions) {
        mSessionList.removeAllViews();
        mEmpty.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(mService);
        for (TmuxUtils.TmuxSession session : sessions) {
            View item = inflater.inflate(R.layout.item_session, mSessionList, false);
            ((TextView) item.findViewById(R.id.session_name)).setText(session.name);
            ((TextView) item.findViewById(R.id.session_detail)).setText(buildDetail(session));

            item.setOnClickListener(v -> {
                mService.switchToTmuxSession(session.name);
                dismiss();
            });

            ImageButton openInTermuxButton = item.findViewById(R.id.open_in_termux_button);
            openInTermuxButton.setOnClickListener(v -> mService.openTmuxSessionInTermuxApp(session.name));

            mSessionList.addView(item);
        }

        limitListHeight();
    }

    private CharSequence buildDetail(@NonNull TmuxUtils.TmuxSession session) {
        String windows = mService.getString(R.string.session_detail_windows, session.windows);
        if (!session.attached) return windows;
        return windows + " · " + mService.getString(R.string.session_detail_attached);
    }

    /** Keep long session lists scrollable instead of letting them run off the screen. */
    private void limitListHeight() {
        mScrollView.post(() -> {
            DisplayMetrics metrics = mService.getResources().getDisplayMetrics();
            int maxHeight = (int) (metrics.heightPixels * 0.4f);

            ViewGroup.LayoutParams params = mScrollView.getLayoutParams();
            params.height = Math.min(mSessionList.getHeight(), maxHeight);
            mScrollView.setLayoutParams(params);
        });
    }

    private void createSession() {
        String sessionName = TmuxUtils.sanitizeSessionName(mNewSessionName.getText().toString());
        mNewSessionName.setText("");
        mService.switchToTmuxSession(sessionName);
        dismiss();
    }

}
