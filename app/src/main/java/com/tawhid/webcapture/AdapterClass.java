package com.tawhid.webcapture;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterClass extends RecyclerView.Adapter<AdapterClass.AdapterViewholder> {
    Context context;
    List<String> pdfFiles;

    public AdapterClass(Context context, List<String> pdfFiles) {
        this.context = context;
        this.pdfFiles = pdfFiles;
    }

    @NonNull
    @Override
    public AdapterViewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.pdf_files_design, parent, false);
        return new AdapterViewholder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterViewholder holder, @SuppressLint("RecyclerView") int position) {

        String path = pdfFiles.get(position);
        File pdfFile = new File(path);
        String filename = pdfFile.getName();
        holder.filename.setText(filename);
        String date = formatDate(pdfFile.lastModified());
        holder.fileDate.setText(date);

        holder.fileCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPdf(pdfFile, holder.itemView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    static class AdapterViewholder extends RecyclerView.ViewHolder {
        TextView filename;
        TextView fileDate;
        CardView fileCard;

        public AdapterViewholder(@NonNull View itemView) {
            super(itemView);
            filename = itemView.findViewById(R.id.fileName);
            fileDate = itemView.findViewById(R.id.fileDate);
            fileCard = itemView.findViewById(R.id.fileCard);
        }
    }

    private void openPdf(File file, View view) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(Intent.createChooser(intent, "Open PDF with"));
        } catch (Exception e) {
            Snackbar.make(view, "No PDF viewer found!", Snackbar.LENGTH_SHORT).show();
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}