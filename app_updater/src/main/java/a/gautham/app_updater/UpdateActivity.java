package a.gautham.app_updater;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.squareup.picasso.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import a.gautham.app_updater.constants.Constants;
import a.gautham.app_updater.helper.ServiceGenerator;
import a.gautham.app_updater.models.AssestsModel;
import a.gautham.app_updater.models.GitRelease;
import a.gautham.app_updater.service.GithubService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateActivity extends AppCompatActivity {

    private static final String TAG = "UpdateActivity: ";
    private ImageView icon_update, update_refresh;
    private TextView header1, header2, changelogs, changelogs_txt;
    private ProgressBar pg;
    private AlertDialog pgDialog;
    private Context context;
    private DownloadTask downloadTask;
    private TextView download_name, download_size, percent_pg;
    private Button update_bt;
    private Call<GitRelease> callAsync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        context = getApplicationContext();

        init();

        if (getIntent() != null) {

            setUpDownloadDialog();

            String username = getIntent().getStringExtra("username");
            String reponame = getIntent().getStringExtra("repoName");

            GithubService githubService = ServiceGenerator.build().create(GithubService.class);
            callAsync = githubService.getReleases(username, reponame);
            getInfo();

            update_refresh.setOnClickListener(v -> getInfo());

        } else {
            Log.e(TAG, "Git Username/Repo is empty");
        }

    }

    public void init() {

        icon_update = findViewById(R.id.icon_update);
        update_refresh = findViewById(R.id.update_refresh);
        header1 = findViewById(R.id.header1);
        header2 = findViewById(R.id.header2);
        update_bt = findViewById(R.id.update_bt);
        changelogs = findViewById(R.id.changelogs);
        changelogs_txt = findViewById(R.id.changelogs_txt);

    }

    private void getInfo() {

        callAsync.enqueue(new Callback<GitRelease>() {
            @Override
            public void onResponse(Call<GitRelease> call, Response<GitRelease> response) {

                if (response.code() == 200) {

                    GitRelease releases = response.body();
                    AssestsModel assestsModel = releases.getAssestsModels().get(0);

                    double currentAppversion = Double.parseDouble(getAppVersion());
                    double latestAppversion = Double.parseDouble(releases.getRelease_tag_name());

                    download_name.setText(assestsModel.getAssest_name().replace(".apk", ""));
                    double size = assestsModel.getSize();
                    double fileSizeInKB = size / 1024;
                    double fileSizeInMB = fileSizeInKB / 1024;
                    download_size.setText(String.format("%.2f Mb", fileSizeInMB));

                    if (currentAppversion < latestAppversion) {

                        update_bt.setVisibility(View.VISIBLE);
                        header2.setText(R.string.new_update_available);

                        changelogs.setVisibility(View.VISIBLE);
                        changelogs_txt.setVisibility(View.VISIBLE);
                        changelogs.setText(releases.getRelease_description());

                        update_bt.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                downloadTask = new DownloadTask(assestsModel.getAssest_name());
                                downloadTask.execute(assestsModel.getDownload_url());
                            }
                        });

                    } else {
                        header2.setText(R.string.no_updates);
                        update_bt.setVisibility(View.GONE);
                        changelogs.setVisibility(View.GONE);
                        changelogs_txt.setVisibility(View.GONE);
                    }

                } else {
                    Log.e(TAG + R.string.error, response.message());
                }

            }

            @Override
            public void onFailure(Call<GitRelease> call, Throwable t) {
                Log.e(TAG + R.string.failed, t.getMessage());
            }
        });

    }

    private void setUpDownloadDialog() {

        View progressBar = LayoutInflater.from(this).inflate(R.layout.download_layout, null);
        pg = progressBar.findViewById(R.id.progressBar);
        download_name = progressBar.findViewById(R.id.download_name);
        download_size = progressBar.findViewById(R.id.download_size);
        percent_pg = progressBar.findViewById(R.id.percent_pg);

        MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(this, Constants.dialogAlertStyle);
        alertDialog.setTitle(R.string.downloading);
        alertDialog.setIcon(R.drawable.ic_system_update_black_24dp);
        alertDialog.setView(progressBar);
        alertDialog.setCancelable(false);

        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadTask.cancel(true);
                dialog.dismiss();
            }
        });

        pgDialog = alertDialog.create();

    }

    private String getAppVersion() {

        String result = "";
        try {
            result = context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
                    .versionName;
            result = result.replaceAll("[a-zA-Z]|-", "");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private String app_name;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(String app_name) {
            this.app_name = app_name;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(context.getExternalFilesDir(null).getAbsolutePath() + "/" + app_name);

                byte[] data = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);

                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            pgDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

            percent_pg.setText(progress[0] + "%");

            // if we get here, length is known, now set indeterminate to false
            pg.setIndeterminate(false);
            pg.setMax(100);
            pg.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {

            mWakeLock.release();
            pgDialog.dismiss();
            if (result != null) {
                Toast.makeText(context, R.string.download_error + result, Toast.LENGTH_LONG).show();
            } else {

                File file = new File(context.getExternalFilesDir(null).getAbsolutePath()
                        + "/" + app_name);
                Uri data = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);

                Intent installAPK = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                installAPK.setDataAndType(data, "application/vnd.android.package-archive");
                installAPK.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(installAPK);

                Toast.makeText(context, R.string.file_downloaded, Toast.LENGTH_SHORT).show();

                getInfo();

            }
        }
    }

}
