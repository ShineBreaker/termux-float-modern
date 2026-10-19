package com.termux.window;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

public class TermuxFloatPermissionActivity extends Activity {

    public static int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        applyColorScheme();

        findViewById(R.id.grant_permission_button).setOnClickListener(v -> requestOverlayPermission());
    }

    /** Tint the permission card to the active color scheme. */
    private void applyColorScheme() {
        WindowColorScheme scheme = WindowColorScheme.load(this);

        getWindow().getDecorView().setBackgroundColor(scheme.surface);
        WindowColorScheme.tintShape(findViewById(R.id.permission_card).getBackground(), scheme.surfaceContainerHigh, 0, 0);
        ((TextView) findViewById(R.id.permission_title)).setTextColor(scheme.onSurface);
        ((TextView) findViewById(R.id.permission_explanation)).setTextColor(scheme.onSurfaceVariant);

        Button grantButton = findViewById(R.id.grant_permission_button);
        grantButton.setTextColor(scheme.onPrimary);
        Drawable buttonBackground = grantButton.getBackground();
        if (buttonBackground instanceof RippleDrawable) {
            RippleDrawable ripple = (RippleDrawable) buttonBackground;
            ripple.setColor(ColorStateList.valueOf(scheme.onPrimaryStateLayer()));
            WindowColorScheme.tintShape(ripple, scheme.primary, 0, 0);
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            startService(new Intent(this, TermuxFloatService.class));
            finish();
        }
    }
}
