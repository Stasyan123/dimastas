package com.sm.stasversion.imagepicker.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sm.stasversion.R;
import com.sm.stasversion.imagepicker.listener.OnFolderClickListener;
import com.sm.stasversion.imagepicker.model.Folder;
import com.sm.stasversion.imagepicker.ui.common.BaseRecyclerViewAdapter;
import com.sm.stasversion.imagepicker.ui.imagepicker.AssetLoader;

import java.util.ArrayList;
import java.util.List;


public class FolderPickerMenuAdapter extends BaseRecyclerViewAdapter<FolderPickerMenuAdapter.ViewHolder> {

    private List<Folder> folders = new ArrayList<>();
    private OnFolderClickListener itemClickListener;

    public FolderPickerMenuAdapter(Context context, AssetLoader assetLoader, OnFolderClickListener itemClickListener) {
        super(context, assetLoader);
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public FolderPickerMenuAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagepicker_menu_folder, parent, false);

        return new FolderPickerMenuAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderPickerMenuAdapter.ViewHolder holder, int position) {
        final Folder folder = folders.get(position);

        getAssetLoader().loadAsset(folder.getImages().get(0), holder.image);

        holder.name.setText(folder.getFolderName());

        final int count = folder.getImages().size();
        holder.count.setText("" + count);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemClickListener.onFolderClick(folder);
            }
        });
    }

    public void setData(List<Folder> folders) {
        if (folders != null) {
            this.folders.clear();
            this.folders.addAll(folders);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView image;
        private TextView name;
        private TextView count;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_folder_thumbnail);
            name = itemView.findViewById(R.id.text_folder_name);
            count = itemView.findViewById(R.id.text_photo_count);
        }
    }
}
