package com.techno.bobackwifi;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


public class FilesViewHolder extends RecyclerView.ViewHolder {

  public ProgressBar progressBar;

  private TextView tvItemTitle;
  private ImageView ivItemType;
  private View view;
  private Context context;
  private FileModel fileModel;

  FilesViewHolder(View itemView, Context context) {
    super(itemView);
    this.view = itemView;
    this.context = context;

    tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
    ivItemType = itemView.findViewById(R.id.ivItemType);
    progressBar = itemView.findViewById(R.id.progressBar);
  }

  void bind(FileModel fileModel) {
    this.fileModel = fileModel;

    tvItemTitle.setText(FilesUtil.getFileName(fileModel.getFile().getPath()));

    view.setOnClickListener(v -> {
      FilesUtil.openFile(context, this.fileModel.getFile());
    });

//      switch (fileModel.getType()) {
//        case FileModel.TYPE_PHOTO: {
//          ivItemType.setImageResource(R.drawable.d_icon_photo);
//          break;
//        }
//        case FileModel.TYPE_APPLICATION: {
//          ivItemType.setImageResource(R.drawable.d_icon_app);
//          break;
//        }
//        case FileModel.TYPE_COMMON: {
//          ivItemType.setImageResource(R.drawable.d_icon_file);
//          break;
//        }
//      }
  }
}
