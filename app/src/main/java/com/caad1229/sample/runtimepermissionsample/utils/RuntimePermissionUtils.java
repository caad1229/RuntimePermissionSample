package com.caad1229.sample.runtimepermissionsample.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

/**
 * Created by s.s on 2016/07/21.
 */
public class RuntimePermissionUtils {
    private RuntimePermissionUtils() {
    }

    public static boolean hasSelfPermissions(@NonNull Context context, @NonNull String... permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkGrantResults(@NonNull int... grantResults) {
        if (grantResults.length == 0) throw new IllegalArgumentException("grantResults is empty");
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity, @NonNull String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activity.shouldShowRequestPermissionRationale(permission);
        }
        return true;
    }

    // ダイアログ表示
    public static void showAlertDialog(FragmentManager fragmentManager, String permission) {
        RuntimePermissionAlertDialogFragment dialog = RuntimePermissionAlertDialogFragment.newInstance(permission);
        dialog.show(fragmentManager, RuntimePermissionAlertDialogFragment.TAG);
    }

    // ダイアログ本体
    public static class RuntimePermissionAlertDialogFragment extends DialogFragment {
        public static final String TAG = "RuntimePermissionApplicationSettingsDialogFragment";
        private static final String ARG_PERMISSION_NAME = "permissionName";

        public static RuntimePermissionAlertDialogFragment newInstance(@NonNull String permission) {
            RuntimePermissionAlertDialogFragment fragment = new RuntimePermissionAlertDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_PERMISSION_NAME, permission);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String permission = getArguments().getString(ARG_PERMISSION_NAME);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                    .setMessage(permission + " の権限がないので、アプリ情報の「許可」から設定してください")
                    .setPositiveButton("アプリ情報", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                            // システムのアプリ設定画面
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getActivity().getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getActivity().startActivity(intent);
                        }
                    })
                    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    });
            return dialogBuilder.create();
        }
    }
}
