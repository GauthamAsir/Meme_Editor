package a.gautham.app_updater;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

import a.gautham.app_updater.constants.Constants;
import a.gautham.app_updater.constants.DISPLAY;
import a.gautham.app_updater.helper.ServiceGenerator;
import a.gautham.app_updater.models.AssestsModel;
import a.gautham.app_updater.models.GitRelease;
import a.gautham.app_updater.service.GithubService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.NOTIFICATION_SERVICE;

public class AppUpdater {

    private static final String TAG = "AppUpdater: ";
    private static Context context;
    private static int type = 0;
    private static ProgressBar pg;
    private static TextView download_name, download_size, percent_pg;
    private static AlertDialog pgDialog;
    private static DownloadTask downloadTask;
    private static String gitUsername = "", repo_name = "";
    private static boolean ignoreUpdateCheck = false;
    private Call<GitRelease> callAsync;
    private UpdateListener updateListener;

    public AppUpdater(Context context) {
        AppUpdater.context = context;
        setUpDownloadDialog();
    }

    static void activity() {

        if (gitUsername.isEmpty()) {
            Toast.makeText(context, "Git Username can't be empty", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Git Username is empty");
            return;
        }

        if (repo_name.isEmpty()) {
            Toast.makeText(context, "Git Repo Name can't be empty", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Git Repo Name is empty");
            return;
        }

        context.startActivity(new Intent(context, UpdateActivity.class)
                .putExtra("username", gitUsername)
                .putExtra("repoName", repo_name));

    }

    static void showUpdateDialog(String tag_version, final String app_name, final String downloadUrl) {

        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(context, Constants.dialogAlertStyle)
                .setTitle(Constants.dialogTitle)
                .setCancelable(Constants.dialogCancelable)
                .setIcon(Constants.dialog_icon);

        if (Constants.dialogMessage.isEmpty()) {
            materialAlertDialogBuilder.setMessage(String.format("New Update %s is Available. By Downloading the latest " +
                    "update, you will get the latest features, improvements and bug fixes. Do you want to Update App?", tag_version));
        } else {
            materialAlertDialogBuilder.setMessage(Constants.dialogMessage);
        }

        materialAlertDialogBuilder.setPositiveButton(Constants.dialogBtnPositiveText, (dialog, which) -> {
            downloadTask = new DownloadTask(app_name);
            downloadTask.execute(downloadUrl);
            dialog.dismiss();
        });

        materialAlertDialogBuilder.setNegativeButton(Constants.dialogBtnNegativeText, (dialog, which) -> dialog.dismiss());

        materialAlertDialogBuilder.show();

    }

    static void snack(String content, final String downloadUrl, final String app_name) {
        Activity activity = (Activity) context;

        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), content, Snackbar.LENGTH_LONG);
        snackbar.setAction(context.getResources().getString(R.string.update), view -> {
            downloadTask = new DownloadTask(app_name);
            downloadTask.execute(downloadUrl);

        });
        snackbar.show();
    }

    static void showNotification(String title, String content, String tag_version, final String downloadUrl) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            initNotificationChannel(context, notificationManager);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                context.getString(R.string.appupdater_channel));

        Intent intent = new Intent(context, UpdateActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, intent, 0);

        builder.setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setSmallIcon(R.drawable.ic_system_update_black_24dp)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);

        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        assert notificationManager != null;
        notificationManager.notify(1, builder.build());

    }

    private static void initNotificationChannel(Context context, NotificationManager notificationManager) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    context.getString(R.string.appupdater_channel),
                    context.getString(R.string.appupdater_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }
    }

    private static void setUpDownloadDialog() {

        Activity activity = (Activity) context;

        @SuppressLint("InflateParams") View progressBar = LayoutInflater.from(activity).inflate(R.layout.download_layout, null);
        pg = progressBar.findViewById(R.id.progressBar);
        download_name = progressBar.findViewById(R.id.download_name);
        download_size = progressBar.findViewById(R.id.download_size);
        percent_pg = progressBar.findViewById(R.id.percent_pg);

        MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(activity, Constants.dialogAlertStyle);
        alertDialog.setTitle(R.string.downloading);
        alertDialog.setIcon(R.drawable.ic_system_update_black_24dp);
        alertDialog.setView(progressBar);
        alertDialog.setCancelable(false);

        alertDialog.setNegativeButton(R.string.cancel, (dialog, which) -> {
            downloadTask.cancel(true);
            dialog.dismiss();
        });

        pgDialog = alertDialog.create();

    }

    private static String getAppVersion() {

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

    public void setType(int type) {
        AppUpdater.type = type;
    }

    public void setUpGithub(String username, String repoName) {

        GithubService githubService = ServiceGenerator.build().create(GithubService.class);
        callAsync = githubService.getReleases(username, repoName);
        AppUpdater.gitUsername = username;
        AppUpdater.repo_name = repoName;

    }

    public void setIgnoreUpdateCheck(boolean ignoreUpdateCheck) {
        AppUpdater.ignoreUpdateCheck = ignoreUpdateCheck;
    }

    public AppUpdater withListener(UpdateListener updateListener) {
        this.updateListener = updateListener;
        return this;
    }

    public void start() {

        callAsync.enqueue(new Callback<GitRelease>() {
            @Override
            public void onResponse(Call<GitRelease> call, Response<GitRelease> response) {

                if (response.code() == 200) {

                    GitRelease releases = response.body();
                    assert releases != null;
                    AssestsModel assestsModel = releases.getAssestsModels().get(0);

                    double currentAppversion = Double.parseDouble(getAppVersion());
                    double latestAppversion = Double.parseDouble(releases.getRelease_tag_name());

                    download_name.setText(assestsModel.getAssest_name().replace(".apk", ""));
                    double size = assestsModel.getSize();
                    double fileSizeInKB = size / 1024;
                    double fileSizeInMB = fileSizeInKB / 1024;
                    download_size.setText(String.format(Locale.ENGLISH, "%.2f Mb", fileSizeInMB));

                    if (type == DISPLAY.ACTIVITY) {
                        if (ignoreUpdateCheck)
                            activity();
                    }

                    if (currentAppversion < latestAppversion) {

                        if (updateListener != null) {
                            updateListener.onSuccess(releases, true);
                        }

                        if (type == DISPLAY.SNACKBAR) {
                            snack(context.getString(R.string.new_update_available), assestsModel.getDownload_url(), assestsModel.getAssest_name());
                            return;
                        }

                        if (type == DISPLAY.ACTIVITY) {
                            activity();
                            return;
                        }

                        if (type == DISPLAY.NOTIFICATION) {
                            showNotification(context.getString(R.string.new_update_available),
                                    "Download Latest Update to enjoy all features",
                                    releases.getRelease_tag_name(), assestsModel.getDownload_url());
                            return;
                        }

                        showUpdateDialog(releases.getRelease_tag_name(), assestsModel.getAssest_name(),
                                assestsModel.getDownload_url());
                    } else {
                        if (updateListener != null) {
                            updateListener.onSuccess(releases, false);
                        }
                    }

                } else {
                    Log.e(TAG + R.string.error, response.message());
                }

            }

            @Override
            public void onFailure(Call<GitRelease> call, Throwable t) {
                Log.e(TAG + R.string.failed, Objects.requireNonNull(t.getMessage()));
            }
        });
    }

    public void setDialogPositiveButtonText(String positiveButtonText) {
        Constants.dialogBtnPositiveText = positiveButtonText;
    }

    public void setDialogTitle(String title) {
        Constants.dialogTitle = title;
    }

    public void setDialogNegativeButtonText(String negativeButtonText) {
        Constants.dialogBtnNegativeText = negativeButtonText;
    }

    public void setDialogMessage(String message) {
        Constants.dialogMessage = message;
    }

    public void setDialogCancelable(boolean cancelable) {
        Constants.dialogCancelable = cancelable;
    }

    public void setDialogIcon(int dialogIcon) {
        Constants.dialog_icon = dialogIcon;
    }

    public void setDialogStyle(int dialogAlertStyle) {

        Constants.dialogAlertStyle = dialogAlertStyle;

    }

    public interface UpdateListener {

        void onSuccess(GitRelease releases, boolean isUpdateAvailable);

        void onFailed();

    }

    private static class DownloadTask extends AsyncTask<String, Integer, String> {

        private String app_name;
        private PowerManager.WakeLock mWakeLock;

        DownloadTask(String app_name) {
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
                output = new FileOutputStream(Objects.requireNonNull(context.getExternalFilesDir(null)).getAbsolutePath() + "/" + app_name);

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
            assert pm != null;
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

                File file = new File(Objects.requireNonNull(context.getExternalFilesDir(null)).getAbsolutePath()
                        + "/" + app_name);
                Uri data = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);

                Intent installAPK = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                installAPK.setDataAndType(data, "application/vnd.android.package-archive");
                installAPK.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(installAPK);

                Toast.makeText(context, R.string.file_downloaded, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
