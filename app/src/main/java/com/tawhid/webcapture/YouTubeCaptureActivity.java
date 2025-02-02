package com.tawhid.webcapture;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YouTubeCaptureActivity extends AppCompatActivity {

    EditText editText, edit_text_tag;
    ImageView thumbnail_img;
    LinearLayout fetch_button, thumbnail_download_button, reload_thumbnail, keyword_tool;
    ClipboardManager clipboard;
    ClipData clip;
    public String thumbnail_url_global = "";
    private static final int REQUEST_CODE_EXTERNAL_STORAGE = 100;
    private NestedScrollView nested_scroll_view;
    private ImageButton bt_toggle_title, bt_toggle_desc, bt_toggle_hash, bt_toggle_tags;
    View lyt_expand_title, lyt_expand_desc, lyt_expand_hash, lyt_expand_tag;
    TextView tv_title_head, tv_title, tv_desc_head, tv_desc, tv_hash_head, tv_hash, tv_tags_head;
    ProgressDialog fetching_dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_youtube_capture);

        initComponent();

        editText = findViewById(R.id.input_url);
        fetch_button = findViewById(R.id.fetch_button);
        thumbnail_img = findViewById(R.id.thumbnail_image);
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        thumbnail_download_button = findViewById(R.id.thumbnail_download_button);
        reload_thumbnail = findViewById(R.id.reload_thumbnail);

        fetch_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String video_url = editText.getText().toString();
                if (isYoutubeUrl(video_url)) {
                    String video_Id = extractVideoId(video_url);
                    setThumbnail(video_Id);
                    new FetchVideoDataTask().execute(video_Id);
                    fetchingDataDialog();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Enter a YouTube video URL to fetch data.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        thumbnail_download_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Objects.equals(thumbnail_url_global, "")) {
                    downloadImage();
                } else {
                    Snackbar.make(findViewById(android.R.id.content), "Fetch a thumbnail first.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        reload_thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String video_url = editText.getText().toString();
                String video_Id = extractVideoId(video_url);
                setThumbnail(video_Id);
            }
        });

    }

    public class FetchVideoDataTask extends AsyncTask<String, Void, String> {
        private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/videos";

        @Override
        protected String doInBackground(String... videoIds) {
            String videoId = videoIds[0];
            String url = buildUrl(videoId);
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    Log.e("FetchVideoDataTask", "Error fetching video data: " + response.code());
                }
            } catch (IOException e) {
                Log.e("FetchVideoDataTask", "Error fetching video data: ", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {

                if (fetching_dialog.isShowing()) {
                    fetching_dialog.dismiss();
                }

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    // Video details
                    JSONArray items = jsonObject.getJSONArray("items");
                    JSONObject snippet = items.getJSONObject(0).getJSONObject("snippet");
                    // Title
                    tv_title_head = findViewById(R.id.tv_title_head);
                    tv_title = findViewById(R.id.tv_title);
                    String title = snippet.getString("title");
                    tv_title_head.setText("Title: " + title);
                    tv_title.setText(title);

                    Button bt_copy_title = (Button) findViewById(R.id.bt_copy_title);
                    bt_copy_title.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            copy_to_clipboard(title);
                        }
                    });

                    // Description
                    tv_desc_head = findViewById(R.id.tv_desc_head);
                    tv_desc = findViewById(R.id.tv_desc);
                    String description = snippet.getString("description");
                    tv_desc_head.setText("Description: " + description);
                    tv_desc.setText(description);

                    Button bt_copy_desc = (Button) findViewById(R.id.bt_copy_desc);
                    bt_copy_desc.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            copy_to_clipboard(description);
                        }
                    });


                    // Hashtag
                    tv_hash_head = findViewById(R.id.tv_hash_head);
                    tv_hash = findViewById(R.id.tv_hash);
                    String hashtags = extractHashtags(description);

                    if (!Objects.equals(hashtags, "")) {
                        tv_hash_head.setText("Hashtags: " + hashtags);
                        tv_hash.setText(hashtags);
                        bt_toggle_hash.setVisibility(View.VISIBLE);
                    } else {
                        tv_hash_head.setText("Hashtags: Not found!");
                        tv_hash.setText("");
                        bt_toggle_hash.setVisibility(View.GONE);
                    }

                    Button bt_copy_hash = (Button) findViewById(R.id.bt_copy_hash);
                    bt_copy_hash.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            copy_to_clipboard(hashtags);
                        }
                    });

                    JSONArray tagsArray = snippet.getJSONArray("tags");

                    StringBuilder tags = new StringBuilder();
                    for (int i = 0; i < tagsArray.length(); i++) {
                        tags.append(tagsArray.getString(i));
                        if (i < tagsArray.length() - 1) {
                            tags.append(", ");
                        }
                    }

                    tv_tags_head = findViewById(R.id.tv_tags_head);
                    edit_text_tag = findViewById(R.id.edit_text_tag);

                    String tag_tamp = String.valueOf(tags);

                    if (tag_tamp.length() == 0) {
                        tv_tags_head.setText("Video Tag(s): Not found!");
                        edit_text_tag.setText("");
                        bt_toggle_tags.setVisibility(View.GONE);
                    } else {
                        tv_tags_head.setText("Video Tag(s): " + tags);
                        edit_text_tag.setText(tags);
                        bt_toggle_tags.setVisibility(View.VISIBLE);
                    }

                    Button bt_copy_tag = (Button) findViewById(R.id.bt_copy_tag);
                    bt_copy_tag.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            copy_to_clipboard(String.valueOf(tags));
                        }
                    });

                } catch (JSONException e) {
                    Log.e("FetchVideoDataTask", "Error parsing JSON response: ", e);
                }
            }
        }

        private String buildUrl(String videoId) {
            return BASE_URL + "?id=" + videoId + "&key=" + Config.youtube_api + "&part=snippet,statistics";
        }
    }


    private void initComponent() {

        bt_toggle_title = (ImageButton) findViewById(R.id.bt_toggle_title);
        bt_toggle_desc = (ImageButton) findViewById(R.id.bt_toggle_desc);
        bt_toggle_hash = (ImageButton) findViewById(R.id.bt_toggle_hash);
        bt_toggle_tags = (ImageButton) findViewById(R.id.bt_toggle_tags);

        lyt_expand_title = (View) findViewById(R.id.lyt_expand_title);
        lyt_expand_desc = (View) findViewById(R.id.lyt_expand_desc);
        lyt_expand_hash = (View) findViewById(R.id.lyt_expand_hash);
        lyt_expand_tag = (View) findViewById(R.id.lyt_expand_tag);

        Button bt_hide_title = (Button) findViewById(R.id.bt_hide_title);
        Button bt_hide_desc = (Button) findViewById(R.id.bt_hide_desc);
        Button bt_hide_hash = (Button) findViewById(R.id.bt_hide_hash);
        Button bt_hide_tag = (Button) findViewById(R.id.bt_hide_tag);

        bt_toggle_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSectionText(bt_toggle_title);
            }
        });

        bt_toggle_desc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSectionDes(bt_toggle_desc);
            }
        });

        bt_toggle_hash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSectionHash(bt_toggle_hash);
            }
        });

        bt_toggle_tags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSectionTag(bt_toggle_tags);
            }
        });


        bt_hide_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSectionText(bt_toggle_title);
            }
        });

        bt_hide_desc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSectionDes(bt_toggle_desc);
            }
        });

        bt_hide_hash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSectionHash(bt_toggle_hash);
            }
        });

        bt_hide_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSectionTag(bt_toggle_tags);
            }
        });

        // nested scrollview
        nested_scroll_view = (NestedScrollView) findViewById(R.id.nested_scroll_view);
    }

    private void toggleSectionText(View view) {
        boolean show = toggleArrow(view);
        if (show) {
            ViewAnimation.expand(lyt_expand_title, new ViewAnimation.AnimListener() {
                @Override
                public void onFinish() {
                    Tools.nestedScrollTo(nested_scroll_view, lyt_expand_title);
                }
            });
        } else {
            ViewAnimation.collapse(lyt_expand_title);
        }
    }

    private void toggleSectionDes(View view) {
        boolean show = toggleArrow(view);
        if (show) {
            ViewAnimation.expand(lyt_expand_desc, new ViewAnimation.AnimListener() {
                @Override
                public void onFinish() {
                    Tools.nestedScrollTo(nested_scroll_view, lyt_expand_desc);
                }
            });
        } else {
            ViewAnimation.collapse(lyt_expand_desc);
        }
    }

    private void toggleSectionHash(View view) {
        boolean show = toggleArrow(view);
        if (show) {
            ViewAnimation.expand(lyt_expand_hash, new ViewAnimation.AnimListener() {
                @Override
                public void onFinish() {
                    Tools.nestedScrollTo(nested_scroll_view, lyt_expand_hash);
                }
            });
        } else {
            ViewAnimation.collapse(lyt_expand_hash);
        }
    }


    private void toggleSectionTag(View view) {
        boolean show = toggleArrow(view);
        if (show) {
            ViewAnimation.expand(lyt_expand_tag, new ViewAnimation.AnimListener() {
                @Override
                public void onFinish() {
                    Tools.nestedScrollTo(nested_scroll_view, lyt_expand_tag);
                }
            });
        } else {
            ViewAnimation.collapse(lyt_expand_tag);
        }
    }


    public boolean toggleArrow(View view) {
        if (view.getRotation() == 0) {
            view.animate().setDuration(200).rotation(180);
            return true;
        } else {
            view.animate().setDuration(200).rotation(0);
            return false;
        }
    }

    public boolean isYoutubeUrl(String url) {
        // Regular expression for a YouTube URL
        String regex = "^(http(s)?:\\/\\/)?((w){3}\\.)?youtu(be|.be)?(\\.com)?\\/.+";
        // Compile the regular expression
        Pattern pattern = Pattern.compile(regex);
        // Match the URL against the regular expression
        Matcher matcher = pattern.matcher(url);
        // Return true if the URL matches, false otherwise
        return matcher.matches();
    }

    private String extractVideoId(String url) {
        // Regex pattern for matching YouTube URLs
        String regex = "(?:https?:\\/\\/)?(?:www\\.)?(?:youtu\\.be\\/|youtube\\.com\\/(?:watch\\?v=|embed\\/))([\\w-]+)(?:\\S+|$)";

        Matcher matcher = Pattern.compile(regex).matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            // Try parsing URL components for alternative formats
            Uri uri = Uri.parse(url);
            if ("youtu.be".equals(uri.getHost())) {
                return uri.getPathSegments().get(0);
            } else if ("youtube.com".equals(uri.getHost())) {
                return uri.getQueryParameter("v");
            } else {
                return null;
            }
        }
    }

    // Set thumbnail
    public void setThumbnail(String video_id) {
        String thumbnail_url = "https://img.youtube.com/vi/" + video_id + "/maxresdefault.jpg";
        thumbnail_url_global = thumbnail_url;
        Glide.with(thumbnail_img.getContext()).load(thumbnail_url).into(thumbnail_img);
    }

    public void copy_to_clipboard(String str) {
        clip = ClipData.newPlainText("text", str);
        clipboard.setPrimaryClip(clip);
        Snackbar.make(findViewById(android.R.id.content), "Copied", Snackbar.LENGTH_SHORT).show();
    }

    public void fetchingDataDialog() {
        fetching_dialog = new ProgressDialog(YouTubeCaptureActivity.this);
        fetching_dialog.setMessage("Fetching data...");
        Objects.requireNonNull(fetching_dialog.getWindow()).setGravity(Gravity.CENTER);
        fetching_dialog.setCancelable(false);
        fetching_dialog.setCanceledOnTouchOutside(false);

        if (!fetching_dialog.isShowing()) {
            fetching_dialog.show();
        }
    }

    public String extractHashtags(String text) {
        // Define the regular expression pattern to match hashtags
        String regex = "#\\w+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        // Create an empty string to store the extracted hashtags
        StringBuilder hashtags = new StringBuilder();

        // Loop through all matches and extract hashtags
        while (matcher.find()) {
            hashtags.append(matcher.group());
            hashtags.append(" ");
        }

        // Remove the trailing space if there are hashtags
        if (hashtags.length() > 0) {
            hashtags.deleteCharAt(hashtags.length() - 1);
        }

        return hashtags.toString();
    }


    // Clear everything
    public void clear(View view) {
        editText.setText("");
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
                editText.setText(text);
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
            Intent intent = new Intent(YouTubeCaptureActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // Thumbnail Download
    private void downloadImage() {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        // Get current date and time formatted as yyyyMMdd_HHmmss
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateAndTime = sdf.format(new Date());
        String destinationFileName = "Thumbnail_" + currentDateAndTime + ".jpg";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(thumbnail_url_global));
        String customFolder = "WebCapture";
        // Create a directory path including your custom folder
        String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + customFolder + "/" + destinationFileName;

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setTitle("Image Download")
                .setDescription("Downloading")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.parse("file://" + fullPath)); // Set the complete file path

        downloadManager.enqueue(request);
        Snackbar.make(findViewById(android.R.id.content), "Download Completed: file://Pictures/WebCapture", Snackbar.LENGTH_SHORT).show();
    }
}