package com.sm.stasversion.utils;

import android.content.Context;
import android.graphics.Typeface;
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

public class EditingTextAdapter extends RecyclerView.Adapter<EditingTextAdapter.ViewHolder> {
    private List<TextModel> mToolList = new ArrayList<>();
    private OnItemSelected mOnItemSelected;
    private ConstraintLayout rView;
    private Context ctx;

    public EditingTextAdapter(OnItemSelected onItemSelected, Context c) {
        mOnItemSelected = onItemSelected;
        ctx = c;

        setAdapter();
    }

    public EditingTextAdapter(Context c) {
        ctx = c;

        setAdapter();
    }

    private void setAdapter() {
        mToolList.add(new TextModel("", ToolType.BRUSH, "", R.dimen.empty_size));
        mToolList.add(new TextModel("STIX \nGeneral", ToolType.BRUSH, "stix.ttf", R.dimen.stix_size));
        mToolList.add(new TextModel("Sweet \nHipster", ToolType.BRUSH, "hipster.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Wire \nOne", ToolType.BRUSH, "wire.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Sweet \nHipster", ToolType.BRUSH, "hipster.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Wire \nOne", ToolType.BRUSH, "wire.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Wire \nOne", ToolType.BRUSH, "wire.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Wire \nOne", ToolType.BRUSH, "wire.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Sweet \nHipster", ToolType.BRUSH, "hipster.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Sweet \nHipster", ToolType.BRUSH, "hipster.ttf", R.dimen.wire_size));
        mToolList.add(new TextModel("Sweet \nHipster", ToolType.BRUSH, "hipster.ttf", R.dimen.wire_size));
    }

    public interface OnItemSelected {
        void onTextSelected(ToolType filterType, Integer position, String font);
    }

    class TextModel {
        private String mFontName;
        private ToolType mToolType;
        private String mFont;
        private Integer mSize;

        TextModel(String toolName, ToolType toolType, String font, Integer size) {
            mFontName = toolName;
            mToolType = toolType;
            mFont = font;
            mSize = size;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_editing_text, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextModel item = mToolList.get(position);
        holder.txtTool.setText(item.mFontName);
        holder.txtTool.setTextSize(ctx.getResources().getDimension(item.mSize));

        if(!item.mFont.equals("")) {
            holder.viewEmpty.setVisibility(View.GONE);

            Typeface type = Typeface.createFromAsset(ctx.getAssets(),item.mFont);
            holder.txtTool.setTypeface(type);
        } else {
            holder.viewEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mToolList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View imgToolIcon;
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
                    mOnItemSelected.onTextSelected(mToolList.get(getLayoutPosition()).mToolType, getLayoutPosition(), mToolList.get(getLayoutPosition()).mFont);
                }
            });
        }
    }
}
