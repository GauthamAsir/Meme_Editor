package a.gautham.neweditor;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import a.gautham.neweditor.helper.FileAdapter;
import a.gautham.neweditor.helper.FileModel;

public class EditedFiles extends AppCompatActivity {

    private ListView listView;
    private ProgressBar progressBar;
    private TextView no_files_tv;
    private List<FileModel> list = new ArrayList<>();
    private File editedFolder, reSizedFolder;

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

        editedFolder = new File(getExternalFilesDir(null), "EditedFiles");
        reSizedFolder = new File(getExternalFilesDir(null), "ReSizedImages");

        if (editedFolder.exists() || reSizedFolder.exists())
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

                File[] values1;
                values1 = reSizedFolder.listFiles();
                File[] values2;
                values2 = editedFolder.listFiles();
                list.clear();

                if (values1 != null) {

                    if (values1.length > 0) {
                        addFiles(values1);
                    }

                }

                if (values2 != null) {

                    if (values2.length > 0) {
                        addFiles(values2);
                    }

                }

                if (list.size() > 0) {
                    runOnUiThread(() -> {
                        FileAdapter fileAdapter = new FileAdapter(EditedFiles.this, list);
                        listView.setAdapter(fileAdapter);
                        fileAdapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    });
                } else {
                    runOnUiThread(() -> noFiles());
                }

            }

            private void addFiles(File[] filesArray) {

                Arrays.sort(filesArray, (o1, o2) ->
                        Long.compare((o2).lastModified(), (o1).lastModified()));
                for (File file1 : filesArray) {
                    if (!file1.isDirectory()) {
                        FileModel fileModel = new FileModel(
                                file1.getName(),
                                FileUtils.byteCountToDisplaySize(file1.length()),
                                file1);
                        list.add(fileModel);
                    }
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
