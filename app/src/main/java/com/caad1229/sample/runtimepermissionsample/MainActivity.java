package com.caad1229.sample.runtimepermissionsample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.caad1229.sample.runtimepermissionsample.utils.RuntimePermissionUtils;

public class MainActivity extends AppCompatActivity {
    private final static String[] PERMISSION_CAMERA = new String[]{Manifest.permission.CAMERA};
    private final static int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.launch_camera_button).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View view) {
                // 権限があるか確認
                if (RuntimePermissionUtils.hasSelfPermissions(MainActivity.this, PERMISSION_CAMERA)) {
                    // 権限がある場合は、そのまま通常処理を行う
                    launchCamera();
                } else {
                    // 権限がない場合は、パーミッション確認アラートを表示する
                    requestPermissions(PERMISSION_CAMERA, PERMISSION_REQUEST_CODE);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // アラート表示中に画面回転すると length ０でコールバックされるのでガードする
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            // 失敗した場合
            if (!RuntimePermissionUtils.checkGrantResults(grantResults)) {
                // 「今後は確認しない」にチェックされているかどうか
                if (RuntimePermissionUtils.shouldShowRequestPermissionRationale(MainActivity.this, PERMISSION_CAMERA[0])) {
                    Toast.makeText(MainActivity.this, "権限ないです", Toast.LENGTH_SHORT).show();
                } else {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            RuntimePermissionUtils.showAlertDialog(getSupportFragmentManager(), "カメラ");
                        }
                    });
                }
            } else {
                // 権限が取れた場合は通常の処理を行う
                launchCamera();
            }
        }
    }

    private void launchCamera() {
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, 0);
    }
}
