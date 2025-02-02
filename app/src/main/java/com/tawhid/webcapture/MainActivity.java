package com.tawhid.webcapture;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    EditText input_url;
    RecyclerView recyclerView;

    private static final int REQUEST_CODE_EXTERNAL_STORAGE = 100;
    private ActivityResultLauncher<Intent> manageStoragePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        input_url = findViewById(R.id.input_url);
        recyclerView = findViewById(R.id.pdfList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedURL = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedURL != null) {
                    startPrinting(sharedURL);
                }
            }
        }

        manageStoragePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            showSnackbar("Manage External Storage permission granted");
                            loadData();
                        } else {
                            showSnackbar("Permission denied. Unable to access files.");
                        }
                    }
                });

        checkAndRequestPermissions();

        // go button
        LinearLayout goButton = findViewById(R.id.go_button);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = input_url.getText().toString();
                startPrinting(url);
            }
        });

        // duck duck go button
        LinearLayout duckDuckGo = findViewById(R.id.duckduckgo);
        duckDuckGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPrinting("https://duckduckgo.com");
            }
        });

        // google button
        LinearLayout googleButton = findViewById(R.id.google);
        googleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPrinting("https://google.com");
            }
        });

        // youTube button
        LinearLayout bingButton = findViewById(R.id.youtube);
        bingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, YouTubeCaptureActivity.class);
                startActivity(intent);
            }
        });

        // EditText enter key listener
        input_url.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String url = input_url.getText().toString();
                    startPrinting(url);
                    return true;
                }
                return false;
            }
        });

    }

    private void checkAndRequestPermissions() {
        if (hasManageExternalStoragePermission()) {
            loadData();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestManageExternalStoragePermission();
            } else {
                requestExternalStoragePermissions();
            }
        }
    }


    private boolean hasManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }


    private void requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    manageStoragePermissionLauncher.launch(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    manageStoragePermissionLauncher.launch(intent);
                }
            } else {
                showSnackbar("Permission already granted");
            }
        }
    }

    private void requestExternalStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE_EXTERNAL_STORAGE);
        } else {
            showSnackbar("External Storage permissions already granted");
            loadData();
        }
    }

    // Handle the result of permission requests (for Android < 11)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showSnackbar("External Storage permission granted");
                loadData();
            } else {
                showSnackbar("Permission denied. Unable to access files.");
            }
        }
    }

    private void loadData() {
        // Load your RecyclerView adapter here
        recyclerView.setAdapter(new AdapterClass(this, pdfFiles()));
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private ArrayList<String> pdfFiles() {
        ContentResolver contentResolver = getContentResolver();
        String mime = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String memeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf");
        String[] args = new String[]{memeType};
        String[] proj = {MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DISPLAY_NAME};
        String sortingOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";
        Cursor cursor = contentResolver.query(MediaStore.Files.getContentUri("external")
                , proj, mime, args, sortingOrder);
        ArrayList<String> pdfFiles = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

                String path = cursor.getString(index);
                pdfFiles.add(path);
            }
            cursor.close();
        }
        return pdfFiles;
    }


    public void startPrinting(String url) {
        if (isUrl(url)) {
            Intent intent = new Intent(MainActivity.this, WebViewActivity.class);
            intent.putExtra("input-url", url);
            startActivity(intent);
        } else {
            Snackbar.make(findViewById(android.R.id.content), "URl is Invalid!", Snackbar.LENGTH_SHORT).show();
        }
    }

    public static boolean isUrl(String input) {
        final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
        Pattern pattern = Pattern.compile(URL_REGEX);
        Matcher matcher = pattern.matcher(input);//replace with string to compare
        if (matcher.find()) {
            System.out.println("String contains URL");
            return true;
        }
        return false;
    }

    // Clear input_url
    public void clear(View view) {
        input_url.setText("");
    }

    // Paste From Clip
    public void pasteFromClip(View view) {
        // Get the clipboard manager
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        // Check if there is any text on the clipboard
        if (clipboardManager.hasPrimaryClip()) {
            // Get the text from the clipboard
            ClipData.Item item = Objects.requireNonNull(clipboardManager.getPrimaryClip()).getItemAt(0);
            CharSequence text = item.getText();

            // Check if the text is not null and not empty
            if (text != null && !text.toString().isEmpty()) {
                // Set the text of the EditText to the clipboard text
                input_url.setText(text);
            } else {
                // Show a message if there is no text on the clipboard
                Snackbar.make(findViewById(android.R.id.content), "Clipboard is empty", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            // Show a message if there is no clipboard available
            Snackbar.make(findViewById(android.R.id.content), "Clipboard unavailable", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about_menu) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    public void showDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_design);
        Button noButton = dialog.findViewById(R.id.no);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        Button yesButton = dialog.findViewById(R.id.yes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        dialog.show();
    }
}