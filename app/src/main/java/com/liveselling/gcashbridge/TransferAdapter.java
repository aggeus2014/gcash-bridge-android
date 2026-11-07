package com.liveselling.gcashbridge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class TransferAdapter extends RecyclerView.Adapter<TransferAdapter.ViewHolder> {
    private final List<TransferRecord> list;

    public TransferAdapter(List<TransferRecord> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransferRecord tr = list.get(position);
        holder.tvAmount.setText(String.format(Locale.US, "₱ %.2f", tr.getAmount()));
        holder.tvSender.setText("From: " + (tr.getSenderName() != null ? tr.getSenderName() : "GCash Sender"));
        holder.tvRefNo.setText("Ref: " + tr.getRefNo());
        holder.tvTimestamp.setText(tr.getTimestamp() != null ? tr.getTimestamp() : "");

        String status = tr.getStatus() != null ? tr.getStatus().toUpperCase(Locale.US) : "QUEUED";
        if ("SYNCED".equals(status)) {
            holder.tvStatusBadge.setText("SYNCED ✅");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_bg_green);
        } else if ("QUEUED".equals(status)) {
            holder.tvStatusBadge.setText("QUEUED ⏳");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_bg_amber);
        } else {
            holder.tvStatusBadge.setText("FAILED ❌");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_bg_red);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAmount, tvStatusBadge, tvSender, tvRefNo, tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvSender = itemView.findViewById(R.id.tvSender);
            tvRefNo = itemView.findViewById(R.id.tvRefNo);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
