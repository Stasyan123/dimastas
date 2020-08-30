package com.sm.stasversion.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
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
    private Context context;

    public EditingToolsAdapter(OnItemSelected onItemSelected, Bitmap img, Context ctx, int position) {
        mOnItemSelected = onItemSelected;
        image = img.copy(Bitmap.Config.ARGB_8888,true);
        context = ctx;

        mToolList.add(new ToolModel("Default", 0, FilterType.DEFAULT, "@adjust lut empty.png", false));
        mToolList.add(new ToolModel("1", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut 1.png", false));
        mToolList.add(new ToolModel("2", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut 2.png", false));
        mToolList.add(new ToolModel("3", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut 3.JPG", true));
        mToolList.add(new ToolModel("4", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut 4.png", true));
        mToolList.add(new ToolModel("5", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut 5.png", false));
        mToolList.add(new ToolModel("6", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut 6.png", false));
        mToolList.add(new ToolModel("7", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut 7.png", false));
        mToolList.add(new ToolModel("Azure", ContextCompat.getColor(ctx, R.color.filter1), FilterType.FILTER1, "@adjust lut Azure.JPG", false));
        mToolList.add(new ToolModel("Balmy", ContextCompat.getColor(ctx, R.color.filter2), FilterType.FILTER2, "@adjust lut Balmy.JPG", false));
        mToolList.add(new ToolModel("Basic", ContextCompat.getColor(ctx, R.color.filter3), FilterType.FILTER3, "@adjust lut Basic.JPG", false));
        mToolList.add(new ToolModel("Christmas", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Christmas.JPG", false));
        mToolList.add(new ToolModel("Cinematic", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Cinematic.JPG", false));
        mToolList.add(new ToolModel("Crimson", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Crimson.jpg", false));
        mToolList.add(new ToolModel("Ginger", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Ginger.JPG", false));
        mToolList.add(new ToolModel("Infra Aquatic", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Infra_Aquatic.JPG", false));
        mToolList.add(new ToolModel("Infra F", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Infra_F.JPG", false));
        mToolList.add(new ToolModel("Infra O", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Infra_O.JPG", false));
        mToolList.add(new ToolModel("Infra P", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Infra_P.JPG", false));
        mToolList.add(new ToolModel("Infra R", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Infra_R.JPG", false));
        mToolList.add(new ToolModel("Infra Y", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Infra_Y.JPG", false));
        mToolList.add(new ToolModel("Lagoon", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Lagoon.JPG", false));
        mToolList.add(new ToolModel("Lilac", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Lilac.JPG", false));
        mToolList.add(new ToolModel("Lomo", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Lomo.JPG", false));
        mToolList.add(new ToolModel("Infra B", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Infra_B.JPG", false));
        mToolList.add(new ToolModel("Pop", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Pop.JPG", false));
        mToolList.add(new ToolModel("Rose Gold", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut RoseGold.jpg", false));
        mToolList.add(new ToolModel("Skin", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Skin.JPG", false));
        mToolList.add(new ToolModel("Springles", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Springles.JPG", false));
        mToolList.add(new ToolModel("Sunset", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Sunset.JPG", false));
        mToolList.add(new ToolModel("Travel", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Travel.JPG", false));
        mToolList.add(new ToolModel("Warm", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut Warm.JPG", false));
        mToolList.add(new ToolModel("White", ContextCompat.getColor(ctx, R.color.filter4), FilterType.FILTER3, "@adjust lut White.JPG", false));

        if(position > 0) {
            mToolList.get(position).showBorder = true;
        }
    }

    public interface OnItemSelected {
        void onFilterSelected(FilterType filterType, Integer position, String rule, Integer color, Boolean byLicense);
    }

    public int getScale() {
        return (int)context.getResources().getDisplayMetrics().density;
    }

    public void invalidate() {

    }

    class ToolModel {
        private String mToolName;
        private Integer mColor;
        private FilterType fType;
        private String mRule;
        private Bitmap mThumb;
        private Boolean byLicense;
        private boolean showBorder = false;

        ToolModel(String toolName, Integer color, FilterType type, String rule, Boolean _byLicense) {
            mToolName = toolName;
            mColor = color;
            fType = type;
            mRule = rule;
            byLicense = _byLicense;
            mThumb = CGENativeLibrary.filterImage_MultipleEffects(image, rule + " 1.0", 1.0f);
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
            holder.viewBorder.setVisibility(View.GONE);
            holder.intensityIcon.setVisibility(View.GONE);
        } else {
            holder.txtTool.setVisibility(View.VISIBLE);
            holder.viewEmpty.setVisibility(View.GONE);

            holder.txtTool.setText(item.mToolName);

            GradientDrawable border = new GradientDrawable();
            border.setStroke(2 * getScale(), item.mColor);

            holder.txtTool.setBackgroundColor(item.mColor);
            holder.viewBorder.setBackground(border);

            if(!item.showBorder) {
                holder.viewBorder.setVisibility(View.GONE);
                holder.intensityIcon.setVisibility(View.GONE);
            } else {
                holder.viewBorder.setVisibility(View.VISIBLE);
                holder.intensityIcon.setVisibility(View.VISIBLE);
            }
        }

        holder.imgToolIcon.setImageBitmap(item.mThumb);
    }

    public void switchBorderStatus(int position, boolean status) {
        ToolModel item = mToolList.get(position);

        item.showBorder = status;
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
        ImageView intensityIcon;

        ViewHolder(View itemView) {
            super(itemView);
            imgToolIcon = itemView.findViewById(R.id.imgToolIcon);
            txtTool = itemView.findViewById(R.id.txtTool);
            viewEmpty = itemView.findViewById(R.id.viewEmpty);
            viewBorder = itemView.findViewById(R.id.viewBorder);
            intensityIcon = itemView.findViewById(R.id.intensity_icon);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemSelected.onFilterSelected(mToolList.get(getLayoutPosition()).fType, getLayoutPosition(),
                            mToolList.get(getLayoutPosition()).mRule, mToolList.get(getLayoutPosition()).mColor, mToolList.get(getLayoutPosition()).byLicense);
                }
            });
        }
    }
}
