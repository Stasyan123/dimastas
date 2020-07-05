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


public class EditingTexturesAdapter extends RecyclerView.Adapter<EditingTexturesAdapter.ViewHolder> {
    private List<TexturesModel> mToolList = new ArrayList<>();
    private OnItemSelected mOnItemSelected;
    private ConstraintLayout rView;
    private TextureType currentType;

    public EditingTexturesAdapter(OnItemSelected onItemSelected, TextureType type) {
        mOnItemSelected = onItemSelected;
        currentType = type;
    }

    public interface OnItemSelected {
        void onTexturesSelected(TextureType textureType, Integer texture, Integer position);
    }

    class TexturesModel {
        private String mToolName;
        private TextureType mTextureType;
        private Integer textureIcon;
        private Integer textureIconBlend;

        TexturesModel(String toolName, TextureType textureType, Integer icon, Integer iconBlend) {
            mToolName = toolName;
            mTextureType = textureType;
            textureIcon = icon;
            textureIconBlend = iconBlend;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_editing_textures, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TexturesModel item = mToolList.get(position);

        if(item.mToolName.equals("")) {
            holder.txtTool.setVisibility(View.GONE);
            holder.viewEmpty.setVisibility(View.VISIBLE);
        } else {
            holder.txtTool.setText(item.mToolName);
            holder.imgToolIcon.setImageResource(item.textureIcon);
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
                    mOnItemSelected.onTexturesSelected(mToolList.get(getLayoutPosition()).mTextureType, mToolList.get(getLayoutPosition()).textureIconBlend, getLayoutPosition());
                }
            });
        }
    }
}
