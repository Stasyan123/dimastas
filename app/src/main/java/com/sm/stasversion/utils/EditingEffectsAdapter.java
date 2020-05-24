package com.sm.stasversion.utils;

import android.content.Context;
import android.graphics.Bitmap;
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

public class EditingEffectsAdapter extends RecyclerView.Adapter<EditingEffectsAdapter.ViewHolder> {
    private List<EffectModel> mToolList = new ArrayList<>();
    private OnItemSelected mOnItemSelected;
    private ConstraintLayout rView;

    public EditingEffectsAdapter(OnItemSelected onItemSelected) {
        mOnItemSelected = onItemSelected;

        mToolList.add(new EffectModel("Exposure", EffectType.Exposition, R.drawable.ic_exposure, 0));
        mToolList.add(new EffectModel("Crop", EffectType.Crop, R.drawable.ic_crop, -1));
        mToolList.add(new EffectModel("Brightness", EffectType.Brightness, R.drawable.ic_brightness, 1));
        mToolList.add(new EffectModel("Contrast", EffectType.Contrast, R.drawable.ic_contrast, 2));
        mToolList.add(new EffectModel("Shadows", EffectType.Shadow, R.drawable.ic_shadow, 3));
        mToolList.add(new EffectModel("Highlights", EffectType.Highlight, R.drawable.ic_highlights, 4));
        mToolList.add(new EffectModel("Saturation", EffectType.Saturation, R.drawable.ic_saturation, 5));
        mToolList.add(new EffectModel("Temperature", EffectType.Temperature, R.drawable.ic_temperature, 6));
        mToolList.add(new EffectModel("Grain", EffectType.Grain, R.drawable.ic_grain, 7));
        mToolList.add(new EffectModel("Sharpen", EffectType.Sharpness, R.drawable.ic_sharpen, 8));
    }

    public interface OnItemSelected {
        void onEffectSelected(EffectType filterType, Integer position);
    }

    class EffectModel {
        private String mToolName;
        private EffectType mToolType;
        private Integer effecIcon;
        private Integer mIndex;

        EffectModel(String toolName, EffectType toolType, Integer icon, Integer index) {
            mToolName = toolName;
            mToolType = toolType;
            effecIcon = icon;
            mIndex = index;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_editing_effects, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EffectModel item = mToolList.get(position);
        holder.txtTool.setText(item.mToolName);
        holder.imgToolIcon.setImageResource(item.effecIcon);
    }

    @Override
    public int getItemCount() {
        return mToolList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgToolIcon;
        TextView txtTool;

        ViewHolder(View itemView) {
            super(itemView);
            imgToolIcon = itemView.findViewById(R.id.imgToolIcon);
            txtTool = itemView.findViewById(R.id.txtTool);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemSelected.onEffectSelected(mToolList.get(getLayoutPosition()).mToolType, getLayoutPosition());
                }
            });
        }
    }
}
