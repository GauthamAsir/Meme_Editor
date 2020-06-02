package a.gautham.neweditor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.appbar.MaterialToolbar;
import com.theartofdev.edmodo.cropper.CropImage;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ReSize extends AppCompatActivity implements View.OnClickListener {

    String path, backupPath;
    MaterialToolbar toolbar;
    ImageView image_reSize;
    TextView fitCenter, center, centerInside, centerCrop, fitEnd, fitStart, fitXy, matrix, image_size;
    EditText max_height, max_weight;
    String name;
    RelativeLayout root;
    LinearLayout image_scale_type;
    HorizontalScrollView img_size_container;
    Button enableAdvance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_size);

        if (getIntent() == null) {
            Toast.makeText(this, "Something went wrong, Intent extra was null", Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        path = getIntent().getStringExtra("path");
        backupPath = getIntent().getStringExtra("path");
        if (path == null || backupPath == null) {
            Toast.makeText(this, "Something went wrong, Uri was null", Toast.LENGTH_SHORT).show();
            onBackPressed();
            return;
        }

        initUI();
        loadImage();

    }

    private void loadImage() {

        if (name == null) {
            Glide.with(this)
                    .load(Uri.parse("file://" + path))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(image_reSize);
            return;
        }

        if (!getSavedImageFile().exists()) {
            Toast.makeText(this, "File does not exists", Toast.LENGTH_SHORT).show();
            return;
        }
        Glide.with(this)
                .load(Uri.parse("file://" + getSavedImageFile().getAbsolutePath()))
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(image_reSize);

    }

    private void initUI() {

        root = findViewById(R.id.root);
        image_scale_type = findViewById(R.id.image_scale_type);
        img_size_container = findViewById(R.id.img_size_container);
        toolbar = findViewById(R.id.toolbar);
        image_reSize = findViewById(R.id.image_reSize);
        fitCenter = findViewById(R.id.fitCenter);
        center = findViewById(R.id.center);
        centerInside = findViewById(R.id.centerInside);
        centerCrop = findViewById(R.id.centerCrop);
        fitEnd = findViewById(R.id.fitEnd);
        fitStart = findViewById(R.id.fitStart);
        fitXy = findViewById(R.id.fitXy);
        matrix = findViewById(R.id.matrix);
        max_height = findViewById(R.id.max_height);
        max_weight = findViewById(R.id.max_weight);
        image_size = findViewById(R.id.image_size);
        enableAdvance = findViewById(R.id.enableAdvance);

        fitCenter.setOnClickListener(this);
        center.setOnClickListener(this);
        centerInside.setOnClickListener(this);
        centerCrop.setOnClickListener(this);
        fitEnd.setOnClickListener(this);
        fitStart.setOnClickListener(this);
        fitXy.setOnClickListener(this);
        matrix.setOnClickListener(this);

        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        getImageSize();

    }

    private void getImageSize() {

        if (name == null) {
            String size = getRealPathFromURI(path);
            File file = new File(size);
            image_size.setText(FileUtils.byteCountToDisplaySize(file.length()));
            return;
        }

        image_size.setText(FileUtils.byteCountToDisplaySize(getSavedImageFile().length()));

    }

    public float getMaxWeight() {
        return Float.parseFloat(max_weight.getText().toString());
    }

    public float getMaxHeight() {
        return Float.parseFloat(max_height.getText().toString());
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.fitCenter) {
            image_reSize.setScaleType(ImageView.ScaleType.FIT_CENTER);
            fitCenter.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            fitCenter.setBackground(null);

        if (v.getId() == R.id.center) {
            image_reSize.setScaleType(ImageView.ScaleType.CENTER);
            center.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            center.setBackground(null);

        if (v.getId() == R.id.centerInside) {
            image_reSize.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            centerInside.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            centerInside.setBackground(null);

        if (v.getId() == R.id.centerCrop) {
            image_reSize.setScaleType(ImageView.ScaleType.CENTER_CROP);
            centerCrop.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            centerCrop.setBackground(null);

        if (v.getId() == R.id.fitEnd) {
            image_reSize.setScaleType(ImageView.ScaleType.FIT_END);
            fitEnd.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            fitEnd.setBackground(null);

        if (v.getId() == R.id.fitStart) {
            image_reSize.setScaleType(ImageView.ScaleType.FIT_START);
            fitStart.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            fitStart.setBackground(null);

        if (v.getId() == R.id.fitXy) {
            image_reSize.setScaleType(ImageView.ScaleType.FIT_XY);
            fitXy.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            fitXy.setBackground(null);

        if (v.getId() == R.id.matrix) {
            image_reSize.setScaleType(ImageView.ScaleType.MATRIX);
            matrix.setBackground(ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.image_style_bg));
        } else
            matrix.setBackground(null);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.string.save, 0, R.string.save).setIcon(R.drawable.ic_save);
        menu.add(0, R.string.share, 0, R.string.share).setIcon(R.drawable.ic_share);
        menu.add(0, R.string.restore, 0, R.string.restore).setIcon(R.drawable.ic_restore);
        menu.add(0, R.string.crop, 0, "").setIcon(R.drawable.ic_crop)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    private File getSavedImageFile() {
        return new File(Objects.requireNonNull(getExternalFilesDir(null))
                .getAbsolutePath() + File.separator +
                "ReSizedImages" + File.separator + name + ".jpg");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.string.share:

                Uri uri = FileProvider.getUriForFile(
                        getApplicationContext(),
                        "a.gautham.neweditor.provider",
                        getSavedImageFile());

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                Intent chooser = Intent.createChooser(shareIntent, "Share image");
                List<ResolveInfo> resInfoList = getPackageManager()
                        .queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivity(chooser);

                return true;
            case R.string.save:
                compressImage(path);
                getImageSize();
                loadImage();
                return true;
            case R.string.crop:

                if (name != null) {
                    CropImage.activity(Uri.fromFile(getSavedImageFile()))
                            .start(this);
                } else {
                    CropImage.activity(Uri.fromFile(new File(path)))
                            .start(this);
                }

                return true;

            case R.string.restore:
                path = backupPath;

                if (name != null) {
                    if (!getSavedImageFile().delete()) {
                        Toast.makeText(this, "Error Restoring Image", Toast.LENGTH_SHORT).show();
                    }
                    name = null;
                }

                max_height.setText(R.string._1080);
                max_weight.setText(R.string._720);
                getImageSize();
                loadImage();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri;
                if (result == null) {
                    onBackPressed();
                    Toast.makeText(this, "Error Cropping Image", Toast.LENGTH_SHORT).show();
                    return;
                }
                resultUri = result.getUri();
                File file = new File(getRealPathFromURI(resultUri.toString()));
                path = file.getAbsolutePath();
                if (name != null) {
                    compressImage(path);
                }
                loadImage();
            }
        }

    }

    public void compressImage(String filePath) {

        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        //by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
        //you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        // max Height and width values of the compressed image is taken as 1080x720

        float maxHeight = getMaxHeight();
        float maxWidth = getMaxWeight();
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        //width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

        //setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

        //inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

        //this options allow android to claim the bitmap memory if it runs low on memory
        options.inTempStorage = new byte[16 * 1024];

        try {
            //load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        assert scaledBitmap != null;
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        //check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out;
        String filename = getFilename();
        try {
            out = new FileOutputStream(filename);

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public String getFilename() {
        File folder = new File(getExternalFilesDir(null), "ReSizedImages");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.d("ReSize: ", "Failed to Create to Folder");
            }
        }
        if (name == null) {
            name = String.valueOf(System.currentTimeMillis());
        }
        return (folder.getAbsolutePath() + "/" + name + ".jpg");

    }

    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        @SuppressLint("Recycle") Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = Math.min(heightRatio, widthRatio);
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public void enable_advance(View view) {

        if (image_scale_type.getVisibility() == View.VISIBLE) {
            image_scale_type.setVisibility(View.GONE);
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) img_size_container.getLayoutParams();
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            enableAdvance.setCompoundDrawablesWithIntrinsicBounds
                    (0, 0, R.drawable.ic_keyboard_arrow_down, 0);
            return;
        }

        image_scale_type.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) img_size_container.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        rlp.addRule(RelativeLayout.ABOVE, R.id.image_scale_type);
        enableAdvance.setCompoundDrawablesWithIntrinsicBounds
                (0, 0, R.drawable.ic_keyboard_arrow_up, 0);

    }
}