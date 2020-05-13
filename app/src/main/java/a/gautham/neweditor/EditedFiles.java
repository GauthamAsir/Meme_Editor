package a.gautham.neweditor;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import a.gautham.neweditor.helper.FileAdapter;
import a.gautham.neweditor.helper.FileModel;

public class EditedFiles extends AppCompatActivity {

    private ListView listView;
    private ProgressBar progressBar;
    private TextView no_files_tv;
    private List<FileModel> list = new ArrayList<>();
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edited_files);

        listView = findViewById(R.id.listView);
        progressBar = findViewById(R.id.progressBar);
        no_files_tv = findViewById(R.id.no_files_tv);

    }

    @Override
    protected void onResume() {
        super.onResume();

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                "Pictures" + File.separator + "GMemes Creator";

        file = new File(path);
        if (file.exists())
            loadFiles();
        else
            noFiles();

    }

    private void loadFiles() {

        progressBar.setVisibility(View.VISIBLE);
        no_files_tv.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {

                File[] values = file.listFiles();
                list.clear();

                if (values != null && values.length > 0) {

                    Arrays.sort(values, new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            return Long.compare((o2).lastModified(), (o1).lastModified());
                        }
                    });
                    for (File file1 : values) {
                        if (!file1.isDirectory()) {
                            FileModel fileModel = new FileModel(
                                    file1.getName(),
                                    FileUtils.byteCountToDisplaySize(file1.length()),
                                    file1);
                            list.add(fileModel);
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (list.size() > 0) {
                                FileAdapter fileAdapter = new FileAdapter(EditedFiles.this, list);
                                listView.setAdapter(fileAdapter);
                                progressBar.setVisibility(View.GONE);
                            } else {
                                noFiles();
                            }

                        }
                    });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noFiles();
                        }
                    });
                }

            }
        }).start();

    }

    private void noFiles() {
        progressBar.setVisibility(View.GONE);
        no_files_tv.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

}
