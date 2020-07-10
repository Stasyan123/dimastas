package com.sm.stasversion.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.sm.stasversion.videoUtils.FilterType;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sm.stasversion.R;

import org.wysaid.nativePort.CGENativeLibrary;

import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLookupFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter;

/**
 * @author <a href="https://github.com/burhanrashid52">Burhanuddin Rashid</a>
 * @version 0.1.2
 * @since 5/23/2018
 */
public class EditingToolsAdapter extends RecyclerView.Adapter<EditingToolsAdapter.ViewHolder> {

    private List<ToolModel> mToolList = new ArrayList<>();
    private OnItemSelected mOnItemSelected;
    private Bitmap image;

    public EditingToolsAdapter(OnItemSelected onItemSelected, Bitmap img, Context ctx) {
        mOnItemSelected = onItemSelected;
        image = img.copy(Bitmap.Config.ARGB_8888,true);

        mToolList.add(new ToolModel("Default", 0, FilterType.DEFAULT, "@adjust lut empty.png"));
        mToolList.add(new ToolModel("Vintage", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut ping.png"));
        mToolList.add(new ToolModel("Filter2", ContextCompat.getColor(ctx, R.color.filter2), FilterType.FILTER2, "@adjust lut gazon_filter.JPG"));
        mToolList.add(new ToolModel("Filter3", ContextCompat.getColor(ctx, R.color.filter3), FilterType.FILTER3, "@adjust lut lookup_pink.JPG"));
        mToolList.add(new ToolModel("Filter4", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut soft_warming.png"));
        mToolList.add(new ToolModel("Filter5", ContextCompat.getColor(ctx, R.color.filter5), FilterType.FILTER3, "@adjust lut foggy_night.png"));
        mToolList.add(new ToolModel("Filter6", ContextCompat.getColor(ctx, R.color.filter3), FilterType.FILTER3, "@adjust lut edgy_amber.png"));
        mToolList.add(new ToolModel("Filter7", ContextCompat.getColor(ctx, R.color.filter3), FilterType.FILTER3, "@adjust lut filmstock.png"));
        mToolList.add(new ToolModel("Filter8", ContextCompat.getColor(ctx, R.color.filter3), FilterType.FILTER3, "@adjust lut late_sunset.png"));
        mToolList.add(new ToolModel("Filter9", ContextCompat.getColor(ctx, R.color.filter3), FilterType.FILTER3, "@adjust lut wildbird.png"));
    }

    public interface OnItemSelected {
        void onFilterSelected(FilterType filterType, Integer position, String rule, Integer color);
    }

    class ToolModel {
        private String mToolName;
        private Integer mColor;
        private FilterType fType;
        private String mRule;

        ToolModel(String toolName, Integer color, FilterType type, String rule) {
            mToolName = toolName;
            mColor = color;
            fType = type;
            mRule = rule;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_editing_filters, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ToolModel item = mToolList.get(position);

        if(item.mToolName.equals("Default")) {
            holder.txtTool.setVisibility(View.GONE);
            holder.viewEmpty.setVisibility(View.VISIBLE);
        } else {
            holder.txtTool.setVisibility(View.VISIBLE);
            holder.viewEmpty.setVisibility(View.GONE);

            holder.txtTool.setText(item.mToolName);
        }

        holder.txtTool.setBackgroundColor(item.mColor);

        holder.imgToolIcon.setImageBitmap(CGENativeLibrary.filterImage_MultipleEffects(image, item.mRule, 1.0f));
    }

    @Override
    public int getItemCount() {
        return mToolList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgToolIcon;
        TextView txtTool;
        View viewEmpty;
        View viewBorder;

        ViewHolder(View itemView) {
            super(itemView);
            imgToolIcon = itemView.findViewById(R.id.imgToolIcon);
            txtTool = itemView.findViewById(R.id.txtTool);
            viewEmpty = itemView.findViewById(R.id.viewEmpty);
            viewBorder = itemView.findViewById(R.id.viewBorder);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemSelected.onFilterSelected(mToolList.get(getLayoutPosition()).fType, getLayoutPosition(),
                            mToolList.get(getLayoutPosition()).mRule, mToolList.get(getLayoutPosition()).mColor);
                }
            });
        }
    }
}
