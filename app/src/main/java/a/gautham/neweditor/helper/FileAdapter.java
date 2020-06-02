package a.gautham.neweditor.helper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.util.List;

import a.gautham.neweditor.R;

public class FileAdapter extends ArrayAdapter<FileModel> {

    public FileAdapter(@NonNull Context context, List<FileModel> arrayList) {
        super(context, 0, arrayList);
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull final ViewGroup parent) {

        final FileModel fileModel = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.edited_items, parent, false);
        }

        ImageView file_thumbnail = convertView.findViewById(R.id.file_thumbnail);
        ImageView delete_file = convertView.findViewById(R.id.delete_file);
        ImageView share_file = convertView.findViewById(R.id.share_file);
        TextView file_name = convertView.findViewById(R.id.file_name);
        TextView file_size = convertView.findViewById(R.id.file_size);
        MaterialCardView rootCard = convertView.findViewById(R.id.rootCard);

        assert fileModel != null;
        Picasso.get().load(fileModel.getFile()).into(file_thumbnail);

        file_name.setText(fileModel.getName());
        file_size.setText(fileModel.getSize());

        delete_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (fileModel.getFile().exists() && fileModel.getFile().delete()) {
                    remove(fileModel);
                }
            }
        });

        share_file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Uri uri = FileProvider.getUriForFile(
                        parent.getContext(),
                        "a.gautham.neweditor.provider",
                        fileModel.getFile());

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                if (fileModel.getName().endsWith(".jpeg")) {
                    shareIntent.setType("image/jpg");
                } else {
                    shareIntent.setType("image/png");
                }
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                Intent chooser = Intent.createChooser(shareIntent, "Share image");

                List<ResolveInfo> resInfoList = parent.getContext().getPackageManager()
                        .queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    parent.getContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                parent.getContext().startActivity(chooser);

            }
        });

        return convertView;

    }
}
