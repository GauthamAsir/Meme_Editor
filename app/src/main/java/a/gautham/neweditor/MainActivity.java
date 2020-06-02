package a.gautham.neweditor;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Objects;

import a.gautham.app_updater.AppUpdater;
import a.gautham.app_updater.constants.DISPLAY;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1234;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PICK_IMAGE = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppUpdater appUpdater = new AppUpdater(this);
        appUpdater.setType(DISPLAY.DIALOG);
        appUpdater.setUpGithub("GauthamAsir", "Meme_Editor");
        appUpdater.start();

    }

    public void edit(View view) {
        startActivity(new Intent(getApplicationContext(),EditorActivity.class));
    }

    public void edited_items(View view) {

        if (!arePermissionDenied()) {
            startActivity(new Intent(getApplicationContext(), EditedFiles.class));
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if (arePermissionDenied()) {
                // Clear Data of Application, So that it can request for permissions again
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                startActivity(new Intent(getApplicationContext(), EditedFiles.class));
            }
        }
    }

    private boolean arePermissionDenied() {

        for (String permissions : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), permissions) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    public void reSize(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(intent, "Select a source"), PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK)
            return;

        if (data == null)
            return;

        if (requestCode == PICK_IMAGE) {
            Uri resultUri = data.getData();
            if (resultUri != null) {
                startActivity(new Intent(getApplicationContext(), ReSize.class)
                        .putExtra("path", getPathFromUri(resultUri)));
            }
        }

    }

    private String getPathFromUri(Uri uri) {
        String[] filePathColumn = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String Path = cursor.getString(columnIndex);
        cursor.close();
        return Path;
    }

}
