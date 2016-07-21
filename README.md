## はじめに

今更感ですが、Runtime Permissionを実装する機会があったので、そのためのメモ。

![ttest.gif](https://qiita-image-store.s3.amazonaws.com/0/77816/019e986a-96ab-dd08-dada-260839fd4d14.gif)


## Runtime Permissionとは

Runtime Permissionは名前の通り、対象権限が必要な操作が行われる前にユーザの許可をもらう。インストール時には必要最低限の権限しか聞かれない。ユーザは、どのタイミングでアプリが権限を必要としているのかわかるし、すべての権限を許可しなくても一部の機能が使えたりもする。`targetSdkVersion` を 23以上にする必要がある。

## SDKによる挙動の違い

| targetSdkVersion | 端末バージョン | 挙動 |
| ---- | ---- | ---- |
| 22以下 | 全機種 | インストール時に全権限確認。拒否した場合インストール出来ない。 |
| 23 | 5.1以下 | インストール時に全権限確認。拒否した場合インストール出来ない。 |
| 23 | 6.0 | 実行時にユーザから権限許可をもらう。開発者で実装する必要がある。 |

targetSdkVersionが23でなくても `M` 以降の機種ではアプリ別に権限の設定ができる。 (設定->アプリ->対象アプリ->許可)


## 簡単な処理の流れ

- 権限確認が必要な処理の前に権限があるかどうか確認する ([`checkSelfPermission()`](https://developer.android.com/reference/android/support/v4/content/PermissionChecker.html?hl=ja#checkSelfPermission(android.content.Context, java.lang.String)))
- 権限がなければダイアログを表示する ([`requestPermissions()`](https://developer.android.com/reference/android/support/v4/app/ActivityCompat.html#requestPermissions(android.app.Activity, java.lang.String[], int)))
    - Fragment にも [`requestPermissions()`](https://developer.android.com/reference/android/support/v4/app/Fragment.html#requestPermissions(java.lang.String[], int)) は実装されている
- システムがダイアログを表示、ユーザが操作する
- ユーザ操作によりコールバックが呼ばれる (`onRequestPermissionsResult`)
- 許可されていたらば目的の処理を行う


## 実装

### 基本的な実装

まずはクラッシュしないように、権限が必要な処理の前に確認処理を追加する。カメラ起動を例に実装してみる。

#### `targetSdkVersion` を `23` 以降にする

デフォルトでプロジェクトを作ると `targetSdkVersion 24` になっているはず。

``` build.gradle
    targetSdkVersion 24
```

#### RuntimePermissionのUtil実装

権限のリクエストや結果などRuntimePermission関連の処理を任せるクラスを作っておく

```java:RuntimePermissionUtils.java
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
}
```


#### 権限確認処理

カメラ起動の前に権限確認のメソッドを呼ぶ。


```java:MainActivity.java
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

private void launchCamera() {
    Intent intent = new Intent();
    intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    startActivityForResult(intent, 0);
}
```

#### 権限確認の結果

`onRequestPermissionsResult` をオーバライドして結果をチェックする。ちなみにダイアログ表示中に画面回転すると `grantResults` のサイズが０な状態で `onRequestPermissionsResult` がコールされてしまう。条件文にサイズチェックを追加してガードする。

```java:MainActivity.java
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // アラート表示中に画面回転すると length ０でコールバックされるのでガードする
    if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
        // 失敗した場合
        if (!RuntimePermissionUtils.checkGrantResults(grantResults)) {
            Toast.makeText(this, "権限ないです", Toast.LENGTH_SHORT).show();
        } else {
            // 権限が取れた場合は通常の処理を行う
            launchCamera();
        }
    }
}
```

### 「今後は確認しない」が有効な場合を考慮した対応

`今後は確認しない` をチェックして許可しなかった場合、 `checkSelfPermission` はダイアログ表示せずに権限なしが返ってくる。

もし、ユーザの気が変わり、権限を許可して機能を使おうとしても、常に権限なしが返ってくるため許可することができない。
詳しいユーザならばシステムのアプリ設定から許可するだろうが、全員ができるかというと微妙だ。せめてシステムのアプリ設定画面に誘導するポップアップはアプリ側で出してみる。


#### `今後は確認しない` をチェックしているしているか判断する

[`shouldShowRequestPermissionRationale`]("https://developer.android.com/reference/android/support/v4/app/ActivityCompat.html#shouldShowRequestPermissionRationale(android.app.Activity, java.lang.String)") メソッドを呼ぶ。権限要求を表示する必要があるかどうかを判定するメソッドで、これが `false` ならば表示する必要が無いと判断でき、かつ「今後は確認しない」状態であると判断することができる。

```java:RuntimePermissionUtils.java
public class RuntimePermissionUtils {
                    :
    public static boolean shouldShowRequestPermissionRationale(@NonNull Activity activity, @NonNull String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activity.shouldShowRequestPermissionRationale(permission);
        }
        return true;
    }
}
```

#### アプリ設定画面に誘導するための対応

システムのアプリ設定画面へ誘導するためのダイアログを表示するので、そのダイアログをUtilsに実装する。


```java:RuntimePermissionUtils.java
public class RuntimePermissionUtils {
            :
            :
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
                    .setPositiveButton( "アプリ情報", new DialogInterface.OnClickListener() {
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
```

#### アプリ情報画面への誘導ダイアログを表示する対応

権限確認結果は `許可しない` かつ `今後は確認しない` 場合は、ダイアログを出だす。

```diff:MainActivity.java:
-                Toast.makeText(this, "権限ないです", Toast.LENGTH_SHORT).show();
+                // 「今後は確認しない」にチェックされているかどうか
+                if (RuntimePermissionUtils.shouldShowRequestPermissionRationale(MainActivity.this, PERMISSION_CAMERA[0])) {
+                    Toast.makeText(MainActivity.this, "権限ないです", Toast.LENGTH_SHORT).show();
+                } else {
+                    new Handler().post(new Runnable() {
+                        @Override
+                        public void run() {
+                            RuntimePermissionUtils.showAlertDialog(getSupportFragmentManager(), "カメラ");
+                        }
+                    });
+                }

```
