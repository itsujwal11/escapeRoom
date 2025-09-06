package com.example.escaperoom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InstructionAdapter extends RecyclerView.Adapter<InstructionAdapter.ViewHolder> {

    private List<InstructionItem> instructionList;

    public InstructionAdapter(List<InstructionItem> instructionList) {
        this.instructionList = instructionList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_instruction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InstructionItem item = instructionList.get(position);
        holder.titleTextView.setText(item.getTitle());
        holder.descriptionTextView.setText(item.getDescription());
    }

    @Override
    public int getItemCount() {
        return instructionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView descriptionTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.instructionTitle);
            descriptionTextView = itemView.findViewById(R.id.instructionDescription);
        }
    }
}