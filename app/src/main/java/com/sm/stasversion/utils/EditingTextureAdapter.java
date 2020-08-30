package com.sm.stasversion.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.sm.stasversion.R;

import java.util.ArrayList;
import java.util.List;

public class EditingTextureAdapter extends RecyclerView.Adapter<EditingTextureAdapter.ViewHolder> {
    private List<TextureModel> mToolList = new ArrayList<>();
    private OnItemSelected mOnItemSelected;
    private ConstraintLayout rView;

    public EditingTextureAdapter(OnItemSelected onItemSelected) {
        mOnItemSelected = onItemSelected;

        mToolList.add(new TextureModel("", "def", null));
        mToolList.add(new TextureModel("Dust 001", "01.png", R.drawable.mini_1));
        mToolList.add(new TextureModel("Dust 002", "02.png", R.drawable.mini_2));
        mToolList.add(new TextureModel("Dust 003", "03.png", R.drawable.mini_3));
        mToolList.add(new TextureModel("Dust 004", "04.png", R.drawable.mini_4));
        mToolList.add(new TextureModel("Dust 005", "05.png", R.drawable.mini_5));
        mToolList.add(new TextureModel("Dust 006", "06.png", R.drawable.mini_6));
        mToolList.add(new TextureModel("Dust 007", "07.png", R.drawable.mini_7));
        mToolList.add(new TextureModel("Dust 008", "08.png", R.drawable.mini_8));
        mToolList.add(new TextureModel("Dust 009", "09.png", R.drawable.mini_9));
        mToolList.add(new TextureModel("Dust 010", "09.png", R.drawable.mini_9));
    }

    public interface OnItemSelected {
        void onTextureSelected(String name, Integer position);
    }

    class TextureModel {
        private String mToolName;
        private String texName;
        private Integer effecIcon;

        TextureModel(String toolName, String _texName, Integer icon) {
            mToolName = toolName;
            texName = _texName;
            effecIcon = icon;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_editing_texture, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextureModel item = mToolList.get(position);

        if(item.mToolName.equals("")) {
            holder.txtTool.setVisibility(View.GONE);
            holder.viewEmpty.setVisibility(View.VISIBLE);
        } else {
            holder.txtTool.setText(item.mToolName);
            holder.imgToolIcon.setImageResource(item.effecIcon);
        }
    }

    @Override
    public int getItemCount() {
        return mToolList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgToolIcon;
        TextView txtTool;
        View viewEmpty;

        ViewHolder(View itemView) {
            super(itemView);
            imgToolIcon = itemView.findViewById(R.id.imgToolIcon);
            txtTool = itemView.findViewById(R.id.txtTool);
            viewEmpty = itemView.findViewById(R.id.viewEmpty);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemSelected.onTextureSelected(mToolList.get(getLayoutPosition()).texName, 10);
                }
            });
        }
    }
}
