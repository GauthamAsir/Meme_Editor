package a.gautham.neweditor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void edit(View view) {
        startActivity(new Intent(getApplicationContext(),EditorActivity.class));
    }

    public void edited_items(View view) {
        Toast.makeText(this, "W.I.P", Toast.LENGTH_SHORT).show();
    }
}
