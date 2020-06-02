package a.gautham.neweditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import a.gautham.neweditor.helper.TextStyleBuilder;

public class EditorActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener {

    private EditText textInput;
    private ImageView add_text, bg_image;
    private View addTextRootView, addImageRootView;
    private HashMap<View,TextStyleBuilder> addedTextViews = new HashMap<>();
    private List<View> addedViews = new ArrayList<>();
    private Context context;
    private RelativeLayout editor_container, delete_layout;
    private LinearLayout image_scale_type;
    private Rect outRect;
    private int PICK_IMAGE_BG = 12, PICK_IMAGE = 13;
    private AlertDialog alertDialog;
    private String TAG = "MainActivity: ";
    private boolean selectBg = false, normalText = true, boldText = false, italicText = false,
            underLineText = false, fontFamilyChanged = false, textBg = false, editing = false;
    private Typeface typeface;
    private androidx.appcompat.app.AlertDialog fontFamilyDialog;
    private TextView fitCenter, center, centerInside, centerCrop, fitEnd, fitStart, fitXy, matrix;
    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        context = getApplicationContext();
        setupUI();

    }

    private void setupUI() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        textInput = findViewById(R.id.textInput);
        editor_container = findViewById(R.id.editor_container);
        delete_layout = findViewById(R.id.delete_layout);
        bg_image = findViewById(R.id.bg_image);
        image_scale_type = findViewById(R.id.image_scale_type);

        fitCenter = findViewById(R.id.fitCenter);
        center = findViewById(R.id.center);
        centerInside = findViewById(R.id.centerInside);
        centerCrop = findViewById(R.id.centerCrop);
        fitEnd = findViewById(R.id.fitEnd);
        fitStart = findViewById(R.id.fitStart);
        fitXy = findViewById(R.id.fitXy);
        matrix = findViewById(R.id.matrix);

        root = findViewById(R.id.root);

        fitCenter.setOnClickListener(this);
        center.setOnClickListener(this);
        centerInside.setOnClickListener(this);
        centerCrop.setOnClickListener(this);
        fitEnd.setOnClickListener(this);
        fitStart.setOnClickListener(this);
        fitXy.setOnClickListener(this);
        matrix.setOnClickListener(this);

        outRect = new Rect(delete_layout.getLeft(), delete_layout.getTop(),
                delete_layout.getRight(), delete_layout.getBottom());

        add_text = findViewById(R.id.add_text);
        add_text.setOnClickListener(this);

    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final  View view = findViewById(R.id.select_bg_color_menu);
                if (view!=null){
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            editor_container.setBackground(ContextCompat.getDrawable(context,R.drawable.editor_bg));
                            return true;
                        }
                    });
                }
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.add_image_menu:
                AddImgDialog();
                return true;
            case R.id.select_bg_color_menu:
                ColorPicker();
                return true;
            case R.id.save_menu:
            case R.id.share_menu:
                String filename = "GMemes_"+random(12);
                saveImageDialog(filename);
                return true;
            case R.id.crop_bg_image_menu:
                if (selectBg){
                    if (bg_image.getTag().toString()!=null){
                        CropImage.activity(Uri.parse(bg_image.getTag().toString()))
                                .start(this);
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveImageDialog(final String filename) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a format");

        builder.setPositiveButton("JPEG", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveImage(filename + ".jpeg", Bitmap.CompressFormat.JPEG);
            }
        });

        builder.setNeutralButton("PNG", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveImage(filename + ".png", Bitmap.CompressFormat.PNG);
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context,R.color.colorAccent));
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(context,R.color.colorAccent));

    }

    private String saveImage(String imageName, final Bitmap.CompressFormat format) {

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Processing");
        dialog.show();

        String selectedOutputPath = "";
        if (isSDCARDMounted()) {
            File folder = new File(getExternalFilesDir(null), "EditedFiles");
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    Log.d("EditorActivity: ", "Failed to Create to Folder");
                }
            }
            // Create a media file name
            selectedOutputPath = folder.getPath() + File.separator + imageName;
            Log.d(TAG, "selected camera path " + selectedOutputPath);
            final File file = new File(selectedOutputPath);
            try {
                FileOutputStream out = new FileOutputStream(file);
                if (editor_container != null) {
                    editor_container.setDrawingCacheEnabled(true);
                    editor_container.getDrawingCache().compress(format, 100, out);
                }
                out.flush();
                out.close();
                dialog.dismiss();
                editing = false;

                final Snackbar snackbar = Snackbar.make(root, "File Saved", Snackbar.LENGTH_LONG);
                snackbar.show();
                snackbar.setAction("Share", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        shareFile(file, format);
                        snackbar.dismiss();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return selectedOutputPath;

    }

    private void shareFile(File file, Bitmap.CompressFormat format){

        Uri uri = FileProvider.getUriForFile(
                context,
                "a.gautham.neweditor.provider",
                file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        if (format==Bitmap.CompressFormat.JPEG){
            shareIntent.setType("image/jpg");
        }else {
            shareIntent.setType("image/png");
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        Intent chooser = Intent.createChooser(shareIntent, "Share image");

        List<ResolveInfo> resInfoList = getPackageManager()
                .queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(chooser);

    }

    private boolean isSDCARDMounted() {
        String status = Environment.getExternalStorageState();
        return status.equals(Environment.MEDIA_MOUNTED);
    }

    public String random(int len) {

        String DATA = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random generator = new Random();

        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            sb.append(DATA.charAt(generator.nextInt(DATA.length())));
        }

        return sb.toString();
    }

    private void AddImgDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.add_image_layout,null);
        builder.setView(view);

        TextView add_bg_image = view.findViewById(R.id.add_bg_image);
        TextView remove_bg_image = view.findViewById(R.id.remove_bg_image);
        TextView add_image = view.findViewById(R.id.add_image);

        alertDialog = builder.create();
        alertDialog.show();

        add_bg_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent, "Select a source"), PICK_IMAGE_BG);
                selectBg = true;

            }
        });

        add_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent, "Select a source"), PICK_IMAGE);
                selectBg = false;

            }
        });

        remove_bg_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bg_image.setBackground(null);
                bg_image.setVisibility(View.GONE);
                alertDialog.dismiss();
                ImageScaleLayout(false);
                selectBg = false;
            }
        });

    }

    private void ImageScaleLayout(boolean value){
        if (value)
            image_scale_type.setVisibility(View.VISIBLE);
        else
            image_scale_type.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result != null ? result.getUri() : null;

                if (selectBg){
                    Picasso.get().load(resultUri).into(bg_image);
                    bg_image.setTag(resultUri.toString());
                    ImageScaleLayout(true);
                }else {
                    AddImage(resultUri);
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                assert result != null;
                Exception error = result.getError();
                Log.e(TAG, Objects.requireNonNull(error.getMessage()));
            }
        }

        if (resultCode!=RESULT_OK)
            return;

        if (data==null)
            return;

        if (requestCode == PICK_IMAGE_BG || requestCode == PICK_IMAGE){

            CropImage.activity(data.getData())
                    .start(this);
            alertDialog.dismiss();

        }

    }

    private void ColorPicker(){

        new ColorPickerDialog.Builder(this)
                .setTitle("Select a color")
                .setPreferenceName("MyColorPickerDialog")
                .setPositiveButton("Confirm",
                        new ColorEnvelopeListener() {
                            @Override
                            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                editor_container.setBackgroundColor(envelope.getColor());
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .show();

    }

    @SuppressLint("InflateParams")
    private void AddText(TextStyleBuilder textStyleBuilder){

        String text = textInput.getText().toString();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        addTextRootView = inflater.inflate(R.layout.textview, null);
        TextView addTextView = addTextRootView.findViewById(R.id.textView);
        addTextView.setGravity(Gravity.CENTER);
        addTextView.setText(text);
        addTextRootView.setOnTouchListener(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editor_container.addView(addTextRootView, params);
        addedTextViews.put(addTextRootView,textStyleBuilder);
        addedViews.add(addTextView);

        hideKeyboard();
        textInput.setText("");

        editing = true;

    }

    @SuppressLint("InflateParams")
    private void AddImage(Uri uri){

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        addImageRootView = inflater.inflate(R.layout.imageview, null);
        ImageView imageView = addImageRootView.findViewById(R.id.image_iv);
        Picasso.get().load(uri).into(imageView);
        addImageRootView.setOnTouchListener(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editor_container.addView(addImageRootView, params);
        addedViews.add(addImageRootView);

        editing = true;

    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(context);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.add_text) {
            int[] padding = {12, 0, 12, 0};

            TextStyleBuilder textStyleBuilder = new TextStyleBuilder(
                    textInput.getText().toString(),
                    40,
                    Color.parseColor("#FFFFFF"),
                    padding,
                    textInput.getTypeface(),
                    Color.parseColor("#3700B3"),
                    false,
                    textInput.getPaintFlags()
            );
            AddText(textStyleBuilder);
        }

        if (v.getId() == R.id.fitCenter){
            bg_image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            fitCenter.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            fitCenter.setBackground(null);
        }

        if (v.getId() == R.id.center){
            bg_image.setScaleType(ImageView.ScaleType.CENTER);
            center.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            center.setBackground(null);
        }

        if (v.getId() == R.id.centerInside){
            bg_image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            centerInside.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            centerInside.setBackground(null);
        }

        if (v.getId() == R.id.centerCrop){
            bg_image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            centerCrop.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            centerCrop.setBackground(null);
        }

        if (v.getId() == R.id.fitEnd){
            bg_image.setScaleType(ImageView.ScaleType.FIT_END);
            fitEnd.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            fitEnd.setBackground(null);
        }

        if (v.getId() == R.id.fitStart){
            bg_image.setScaleType(ImageView.ScaleType.FIT_START);
            fitStart.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            fitStart.setBackground(null);
        }

        if (v.getId() == R.id.fitXy){
            bg_image.setScaleType(ImageView.ScaleType.FIT_XY);
            fitXy.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            fitXy.setBackground(null);
        }

        if (v.getId() == R.id.matrix){
            bg_image.setScaleType(ImageView.ScaleType.MATRIX);
            matrix.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
        }else {
            matrix.setBackground(null);
        }

    }

    private int _xDelta;
    private int _yDelta;
    private int _rightMargin;
    private int _bottomMargin;
    private float mPrevRawX;
    private float mPrevRawY;

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        final int X = (int) event.getRawX();
        final int Y = (int) event.getRawY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                this._xDelta = X - lParams.leftMargin;
                this._yDelta = Y - lParams.topMargin;
                mPrevRawX = event.getRawX();
                mPrevRawY = event.getRawY();
                delete_layout.setVisibility(View.VISIBLE);
                view.bringToFront();
                break;

            case MotionEvent.ACTION_MOVE:
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                layoutParams.leftMargin = X - this._xDelta;
                layoutParams.topMargin = Y - this._yDelta;
                layoutParams.rightMargin = -250;
                layoutParams.bottomMargin = -250;
                view.setLayoutParams(layoutParams);
                break;

            case MotionEvent.ACTION_UP:
                if (isViewInBounds(delete_layout, X, Y)){
                    if (addedViews.size()>0){
                        editor_container.removeView(view);
                    }
                    if (addedViews.size()==0)
                        editing = false;
                } else if (!isViewInBounds(editor_container, X, Y)) {
                    view.animate().translationY(0).translationY(0);
                }

                float mCurrentCancelX = event.getRawX();
                float mCurrentCancelY = event.getRawY();

                if (mCurrentCancelX == mPrevRawX || mCurrentCancelY == mPrevRawY){
                    if (view instanceof TextView){
                        editing = true;
                        showUpdateDialogText(((TextView) view), Objects.requireNonNull(addedTextViews.get(view)));
                    }else if (view instanceof ImageView){
                        editing = true;
                        showUpdateDialogImage((ImageView) view);
                    }
                }

                delete_layout.setVisibility(View.GONE);

        }

        return true;
    }

    private void showUpdateDialogImage(final ImageView imageView){

        int layoutWidth = editor_container.getWidth();
        int layoutHeight = editor_container.getHeight();

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(EditorActivity.this);
        bottomSheetDialog.setContentView(R.layout.update_image);

        TextView seekbar_width_value = bottomSheetDialog.findViewById(R.id.seekbar_width_value);
        SeekBar seekbar_width = bottomSheetDialog.findViewById(R.id.seekbar_width);

        TextView seekbar_height_value = bottomSheetDialog.findViewById(R.id.seekbar_height_value);
        SeekBar seekbar_height = bottomSheetDialog.findViewById(R.id.seekbar_height);

        final TextView fitCenter, center, centerInside, centerCrop, fitEnd, fitStart, fitXy, matrix;
        fitCenter = bottomSheetDialog.findViewById(R.id.fitCenter_img);
        center = bottomSheetDialog.findViewById(R.id.center_img);
        centerInside = bottomSheetDialog.findViewById(R.id.centerInside_img);
        centerCrop = bottomSheetDialog.findViewById(R.id.centerCrop_img);
        fitEnd = bottomSheetDialog.findViewById(R.id.fitEnd_img);
        fitStart = bottomSheetDialog.findViewById(R.id.fitStart_img);
        fitXy = bottomSheetDialog.findViewById(R.id.fitXy_img);
        matrix = bottomSheetDialog.findViewById(R.id.matrix_img);

        final List<View> views = new ArrayList<>();
        views.add(fitCenter);
        views.add(center);
        views.add(centerInside);
        views.add(centerCrop);
        views.add(fitEnd);
        views.add(fitStart);
        views.add(fitXy);
        views.add(matrix);

        setBgNullImgStyle(views);

        switch (imageView.getScaleType()){
            case FIT_CENTER:
                setBgStyle(fitCenter);
                break;
            case CENTER:
                setBgStyle(center);
                break;
            case CENTER_INSIDE:
                setBgStyle(centerInside);
                break;
            case CENTER_CROP:
                setBgStyle(centerCrop);
                break;
            case FIT_END:
                setBgStyle(fitEnd);
                break;
            case FIT_START:
                setBgStyle(fitStart);
                break;
            case FIT_XY:
                setBgStyle(fitXy);
                break;
            case MATRIX:
                setBgStyle(matrix);
                break;
        }

        fitCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(fitCenter);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            }
        });

        center.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(center);
                imageView.setScaleType(ImageView.ScaleType.CENTER);

            }
        });

        centerInside.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(centerInside);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            }
        });

        centerCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(centerCrop);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            }
        });

        fitEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(fitEnd);
                imageView.setScaleType(ImageView.ScaleType.FIT_END);

            }
        });

        fitStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(fitStart);
                imageView.setScaleType(ImageView.ScaleType.FIT_START);

            }
        });

        fitXy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(fitXy);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);

            }
        });

        matrix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setBgNullImgStyle(views);
                setBgStyle(matrix);
                imageView.setScaleType(ImageView.ScaleType.MATRIX);

            }
        });

        int imageWidth = imageView.getWidth();
        int imageHeight = imageView.getHeight();

        seekbar_height.setMax(layoutHeight);
        seekbar_width.setMax(layoutWidth);

        seekbar_height.setProgress(imageHeight);
        seekbar_width.setProgress(imageWidth);

        seekbar_height_value.setText(String.valueOf(imageHeight));
        seekbar_width_value.setText(String.valueOf(imageWidth));

        seekbarProgressImage(seekbar_width, imageView, seekbar_width_value, "width");
        seekbarProgressImage(seekbar_height, imageView, seekbar_height_value, "height");

        bottomSheetDialog.show();

    }

    private void setBgStyle(TextView textView) {
        textView.setBackground(ContextCompat.getDrawable(context,R.drawable.image_style_bg));
    }

    private void setBgNullImgStyle(List<View> views) {
        for (View view : views){
            view.setBackground(null);
        }
    }

    private void seekbarProgressImage(SeekBar seekbar, final ImageView imageView, final TextView value, final String type) {

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText(String.valueOf(progress));

                if (type.equals("width")){

                    imageView.requestLayout();
                    imageView.getLayoutParams().width = progress;

                }else {

                    imageView.requestLayout();
                    imageView.getLayoutParams().height = progress;

                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void showUpdateDialogText(final TextView textView, final TextStyleBuilder textStyleBuilder) {

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.update_text);
        bottomSheetDialog.setCancelable(true);

        final TextView preview = bottomSheetDialog.findViewById(R.id.preview);
        final EditText textInput_update = bottomSheetDialog.findViewById(R.id.textInput_update);

        final TextView text_size_value = bottomSheetDialog.findViewById(R.id.text_size_value);
        SeekBar seekbar = bottomSheetDialog.findViewById(R.id.seekbar);

        TextView select_font_family = bottomSheetDialog.findViewById(R.id.select_font_family);
        final ImageView text_style_normal = bottomSheetDialog.findViewById(R.id.text_style_normal);
        final ImageView text_style_bold = bottomSheetDialog.findViewById(R.id.text_style_bold);
        final ImageView text_style_italic = bottomSheetDialog.findViewById(R.id.text_style_italic);
        final ImageView text_style_underline = bottomSheetDialog.findViewById(R.id.text_style_underline);

        LinearLayout color_picker = bottomSheetDialog.findViewById(R.id.color_picker);
        final TextView color_box = bottomSheetDialog.findViewById(R.id.color_box);

        ImageView text_background_bt = bottomSheetDialog.findViewById(R.id.text_background_bt);
        ImageView text_background_color_bt = bottomSheetDialog.findViewById(R.id.text_background_color_bt);

        final TextView enable_advance = bottomSheetDialog.findViewById(R.id.enable_advance);
        final Switch switch_text_padding = bottomSheetDialog.findViewById(R.id.switch_text_padding);

        final LinearLayout text_padding_seekbar_all = bottomSheetDialog.findViewById(R.id.text_padding_seekbar_all);
        final LinearLayout text_padding_seekbar_one_layout = bottomSheetDialog.findViewById(R.id.text_padding_seekbar_one_layout);

        final TextView text_padding_value = bottomSheetDialog.findViewById(R.id.text_padding_value);
        SeekBar text_padding_seekbar = bottomSheetDialog.findViewById(R.id.text_padding_seekbar);

        TextView text_padding_value_start = bottomSheetDialog.findViewById(R.id.text_padding_value_start);
        SeekBar text_padding_seekbar_start = bottomSheetDialog.findViewById(R.id.text_padding_seekbar_start);

        TextView text_padding_value_end = bottomSheetDialog.findViewById(R.id.text_padding_value_end);
        SeekBar text_padding_seekbar_end = bottomSheetDialog.findViewById(R.id.text_padding_seekbar_end);

        TextView text_padding_value_top = bottomSheetDialog.findViewById(R.id.text_padding_value_top);
        SeekBar text_padding_seekbar_top = bottomSheetDialog.findViewById(R.id.text_padding_seekbar_top);

        TextView text_padding_value_bottom = bottomSheetDialog.findViewById(R.id.text_padding_value_bottom);
        SeekBar text_padding_seekbar_bottom = bottomSheetDialog.findViewById(R.id.text_padding_seekbar_bottom);

        final LinearLayout text_padding_layout = bottomSheetDialog.findViewById(R.id.text_padding_layout);

        String t = textStyleBuilder.getText();
        int tSize = textStyleBuilder.getTextSize();
        Typeface tTypeface = textStyleBuilder.getTypeface();
        final int tColor = textStyleBuilder.getText_color();
        int[] padding = textStyleBuilder.getPadding();
        final int bgColor = textStyleBuilder.getBg_color();
        boolean hasBg = textStyleBuilder.isHasBg();
        int paintFlags = textStyleBuilder.getPaintflags();

        textInput_update.setText(t);

        setColor_box(tColor, color_box);

        preview.setPadding(padding[0],padding[1],padding[2],padding[3]);

        preview.setText(t);
        preview.setTextSize(tSize);
        preview.setTypeface(tTypeface);
        preview.setTextColor(tColor);
        if (hasBg){
            addTextBg(bgColor, preview, textView);
        }
        preview.setPaintFlags(paintFlags);

        text_size_value.setText(String.valueOf(tSize));
        seekbar.setProgress(tSize);

        if (normalText) selected(text_style_normal);
        else deselected(text_style_normal);

        if (boldText) selected(text_style_bold);
        else deselected(text_style_bold);

        if (italicText) selected(text_style_italic);
        else deselected(text_style_italic);

        if (underLineText) selected(text_style_underline);
        else deselected(text_style_underline);

        text_padding_seekbar.setProgress(padding[0]);
        text_padding_value.setText(String.valueOf(padding[0]));

        text_padding_value_start.setText(String.valueOf(padding[0]));
        text_padding_seekbar_start.setProgress(padding[0]);

        text_padding_value_top.setText(String.valueOf(padding[1]));
        text_padding_seekbar_top.setProgress(padding[1]);

        text_padding_value_end.setText(String.valueOf(padding[2]));
        text_padding_seekbar_end.setProgress(padding[2]);

        text_padding_value_bottom.setText(String.valueOf(padding[3]));
        text_padding_seekbar_bottom.setProgress(padding[3]);

        textInput_update.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = textInput_update.getText().toString();
                preview.setText(text);
                textView.setText(text);
                textStyleBuilder.setText(text);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                text_size_value.setText(String.valueOf(progress));
                preview.setTextSize(progress);
                textView.setTextSize(progress);
                textStyleBuilder.setTextSize(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        select_font_family.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final androidx.appcompat.app.AlertDialog.Builder fontFamilyBuilder = new
                        androidx.appcompat.app.AlertDialog.Builder(EditorActivity.this);
                View view1 = LayoutInflater.from(context).inflate(R.layout.fonts_list_static,null);
                fontFamilyBuilder.setView(view1);

                CardView font_family_chewy = view1.findViewById(R.id.font_family_chewy);
                CardView font_family_lobster = view1.findViewById(R.id.font_family_lobster);
                CardView font_family_open_sans = view1.findViewById(R.id.font_family_open_sans);
                CardView font_family_oswald = view1.findViewById(R.id.font_family_oswald);
                CardView font_family_roboto = view1.findViewById(R.id.font_family_roboto);
                CardView font_family_ubuntu = view1.findViewById(R.id.font_family_ubuntu);
                CardView font_family_default = view1.findViewById(R.id.font_family_default);

                fontFamilyDialog = fontFamilyBuilder.create();
                fontFamilyDialog.show();

                font_family_chewy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setFont(R.font.chewy);
                    }
                });

                font_family_lobster.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setFont(R.font.lobster);
                    }
                });

                font_family_open_sans.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setFont(R.font.open_sans);
                    }
                });

                font_family_oswald.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setFont(R.font.oswald);
                    }
                });

                font_family_roboto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setFont(R.font.roboto);
                    }
                });

                font_family_ubuntu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setFont(R.font.ubuntu);
                    }
                });

                font_family_default.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        setFont(R.font.open_sans);
                    }
                });

            }

            private void setFont(int font) {

                typeface = ResourcesCompat.getFont(context, font);
                preview.setTypeface(typeface);
                textView.setTypeface(typeface);
                fontFamilyChanged = true;
                fontFamilyDialog.dismiss();

            }
        });

        text_style_normal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (boldText){
                    boldText = false;
                    italicText = false;
                    underLineText = false;
                    normalText = true;
                    selected(text_style_normal);
                    deselected(text_style_bold);
                    deselected(text_style_italic);
                    deselected(text_style_underline);
                    if (fontFamilyChanged){
                        preview.setTypeface(typeface, Typeface.NORMAL);
                        textView.setTypeface(typeface, Typeface.NORMAL);
                    }else {
                        preview.setTypeface(null, Typeface.NORMAL);
                        textView.setTypeface(null, Typeface.NORMAL);
                    }
                    preview.setPaintFlags(0);
                    textView.setPaintFlags(0);
                    return;
                }

                if (fontFamilyChanged){
                    preview.setTypeface(typeface, Typeface.NORMAL);
                    textView.setTypeface(typeface, Typeface.NORMAL);
                }else {
                    textView.setTypeface(null, Typeface.NORMAL);
                    preview.setTypeface(null, Typeface.NORMAL);
                }
                selected(text_style_normal);
                normalText = true;

            }
        });

        text_style_bold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (boldText){
                    boldText = false;
                    deselected(text_style_bold);
                    if (italicText) {
                        preview.setTypeface(null, Typeface.ITALIC);
                        textView.setTypeface(null, Typeface.ITALIC);
                    } else {
                        preview.setTypeface(null, Typeface.NORMAL);
                        textView.setTypeface(null, Typeface.NORMAL);
                    }
                    return;
                }

                boldText = true;

                if (normalText){
                    normalText = false;
                    selected(text_style_bold);
                    deselected(text_style_normal);
                    preview.setTypeface(preview.getTypeface(), Typeface.BOLD);
                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                    return;
                }

                if (italicText){
                    preview.setTypeface(preview.getTypeface(), Typeface.BOLD_ITALIC);
                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD_ITALIC);
                    selected(text_style_bold);
                    return;
                }

                preview.setTypeface(preview.getTypeface(), Typeface.BOLD);
                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
                selected(text_style_bold);
            }
        });

        text_style_italic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (italicText){
                    italicText = false;
                    deselected(text_style_italic);
                    if (boldText) {
                        preview.setTypeface(null, Typeface.BOLD);
                        textView.setTypeface(null, Typeface.BOLD);
                    } else {
                        preview.setTypeface(null, Typeface.NORMAL);
                        textView.setTypeface(null, Typeface.NORMAL);
                    }
                    return;
                }

                italicText = true;

                if (boldText){
                    preview.setTypeface(preview.getTypeface(), Typeface.BOLD_ITALIC);
                    textView.setTypeface(textView.getTypeface(), Typeface.BOLD_ITALIC);
                    selected(text_style_italic);
                    return;
                }

                preview.setTypeface(preview.getTypeface(), Typeface.ITALIC);
                textView.setTypeface(textView.getTypeface(), Typeface.ITALIC);
                selected(text_style_italic);
            }
        });

        text_style_underline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (underLineText) {
                    preview.setPaintFlags(0);
                    textView.setPaintFlags(0);
                    deselected(text_style_underline);
                    underLineText = false;
                } else {
                    preview.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                    textView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
                    selected(text_style_underline);
                    underLineText = true;
                }
            }
        });

        color_picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new ColorPickerDialog.Builder(EditorActivity.this)
                        .setTitle("Select a color")
                        .setPreferenceName("MyColorPickerDialog")
                        .setPositiveButton("Confirm",
                                new ColorEnvelopeListener() {
                                    @Override
                                    public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                        preview.setTextColor(envelope.getColor());
                                        textView.setTextColor(envelope.getColor());
                                        textStyleBuilder.setText_color(envelope.getColor());
                                        setColor_box(envelope.getColor(), color_box);
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .attachAlphaSlideBar(true)
                        .attachBrightnessSlideBar(true)
                        .show();

            }
        });

        text_background_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (textBg){

                    preview.setBackground(null);
                    textView.setBackground(null);
                    textStyleBuilder.setHasBg(false);
                    textBg = false;

                    return;
                }

                textBg = true;

                addTextBg(bgColor, preview, textView);
                textStyleBuilder.setHasBg(true);
                textStyleBuilder.setBg_color(bgColor);

            }
        });

        text_background_color_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ColorPickerDialog.Builder(EditorActivity.this)
                        .setTitle("Select a color")
                        .setPreferenceName("MyColorPickerDialog")
                        .setPositiveButton("Confirm",
                                new ColorEnvelopeListener() {
                                    @Override
                                    public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                        addTextBg(envelope.getColor(), preview, textView);
                                        textStyleBuilder.setHasBg(true);
                                        textStyleBuilder.setBg_color(envelope.getColor());
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .attachAlphaSlideBar(true)
                        .attachBrightnessSlideBar(true)
                        .show();
            }
        });

        enable_advance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (text_padding_layout.getVisibility() == View.GONE){
                    enable_advance.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_keyboard_arrow_up,0);
                    text_padding_layout.setVisibility(View.VISIBLE);
                    return;
                }

                enable_advance.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.ic_keyboard_arrow_down,0);
                text_padding_layout.setVisibility(View.GONE);

            }
        });

        switch_text_padding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switch_text_padding.isChecked()){
                    text_padding_seekbar_all.setVisibility(View.VISIBLE);
                    text_padding_seekbar_one_layout.setVisibility(View.GONE);
                }else {
                    text_padding_seekbar_all.setVisibility(View.GONE);
                    text_padding_seekbar_one_layout.setVisibility(View.VISIBLE);
                }
            }
        });

        seekbarProgressText(text_padding_seekbar, preview, textView, text_padding_value, textStyleBuilder, "all");

        seekbarProgressText(text_padding_seekbar_start, preview, textView, text_padding_value_start, textStyleBuilder,"start");
        seekbarProgressText(text_padding_seekbar_end, preview, textView, text_padding_value_end, textStyleBuilder,"end");
        seekbarProgressText(text_padding_seekbar_top, preview, textView, text_padding_value_top, textStyleBuilder,"top");
        seekbarProgressText(text_padding_seekbar_bottom, preview, textView, text_padding_value_bottom, textStyleBuilder,"bottom");


        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                textStyleBuilder.setText(preview.getText().toString());
                textStyleBuilder.setTypeface(preview.getTypeface());
                textStyleBuilder.setPaintflags(preview.getPaintFlags());
            }
        });

        bottomSheetDialog.show();

    }

    private void seekbarProgressText(SeekBar seekBar, final TextView preview,
                                     final TextView textView, final TextView value, final TextStyleBuilder textStyleBuilder, final String type) {

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                int[] padding = textStyleBuilder.getPadding();

                if (type.equals("all")){
                    preview.setPadding(progress,progress,progress,progress);
                    textView.setPadding(progress,progress,progress,progress);
                    int[] allPadding = {progress,progress,progress,progress};
                    textStyleBuilder.setPadding(allPadding);
                }

                if (type.equals("start")){
                    padding[0] = progress;
                    preview.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textView.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textStyleBuilder.setPadding(padding);
                }

                if (type.equals("top")){
                    padding[1] = progress;
                    preview.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textView.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textStyleBuilder.setPadding(padding);
                }

                if (type.equals("end")){
                    padding[2] = progress;
                    preview.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textView.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textStyleBuilder.setPadding(padding);
                }

                if (type.equals("bottom")){
                    padding[3] = progress;
                    preview.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textView.setPadding(padding[0],padding[1],padding[2],padding[3]);
                    textStyleBuilder.setPadding(padding);
                }

                value.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void addTextBg(int color, TextView preview, TextView textView) {

        Drawable drawable = DrawableCompat.wrap(
                Objects.requireNonNull(getDrawable(R.drawable.text_bg_box)));
        DrawableCompat.setTint(drawable,color);
        preview.setBackground(drawable);
        textView.setBackground(drawable);

    }

    private void selected(ImageView imageView){
        imageView.setBackgroundColor(ContextCompat.getColor(context,R.color.dark));
    }

    private void deselected(ImageView imageView){
        imageView.setBackgroundColor(Color.TRANSPARENT);
    }

    private void setColor_box(int color, TextView color_box){
        Drawable drawable = DrawableCompat.wrap(
                Objects.requireNonNull(getDrawable(R.drawable.color_picker_box)));
        DrawableCompat.setTint(drawable,color);
        color_box.setBackground(drawable);
    }

    private int[] location = new int[2];
    private boolean isViewInBounds(View view, int x, int y) {
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }

    @Override
    public void onBackPressed() {
        if (editing){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Are you sure");
            builder.setMessage("You have some unsaved changes, still want to go back ?");

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditorActivity.super.onBackPressed();
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.colorAccent));

        }else {
            super.onBackPressed();
        }
    }

}
