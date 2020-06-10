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

        mToolList.add(new TextureModel("", TextureType.DEFAULT, null));
        mToolList.add(new TextureModel("Dust", TextureType.SCUFFED, R.drawable.ic_dust));
        mToolList.add(new TextureModel("Light leak", TextureType.GLARE, R.drawable.ic_leak));
        mToolList.add(new TextureModel("Bokeh", TextureType.RAINBOW, R.drawable.ic_bokeh));
        mToolList.add(new TextureModel("Tatto", TextureType.DUST, R.drawable.ic_tatto));
        mToolList.add(new TextureModel("Snow", TextureType.DUST, R.drawable.ic_snow));
    }

    public interface OnItemSelected {
        void onTextureSelected(TextureType textureType, Integer position);
    }

    class TextureModel {
        private String mToolName;
        private TextureType mTextureType;
        private Integer effecIcon;

        TextureModel(String toolName, TextureType textureType, Integer icon) {
            mToolName = toolName;
            mTextureType = textureType;
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
                    mOnItemSelected.onTextureSelected(mToolList.get(getLayoutPosition()).mTextureType, getLayoutPosition());
                }
            });
        }
    }
}