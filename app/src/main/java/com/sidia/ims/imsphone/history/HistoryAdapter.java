package com.sidia.ims.imsphone.history;

import android.content.Context;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.model.ImsPhoneCallLog;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final Context mContext;
    private final List<ImsPhoneCallLog> mLogs;
    private final View.OnClickListener mListener;

    public HistoryAdapter(Context ctx, List<ImsPhoneCallLog> logs, View.OnClickListener listener) {
        this.mLogs = logs;
        this.mContext = ctx;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImsPhoneCallLog log = mLogs.get(position);
        String address = log.getAddress();

        holder.contact.setSelected(true); // For automated horizontal scrolling of long texts
        holder.callDirection.setImageResource(R.drawable.call_status_outgoing);

        if (log.getType() == CallLog.Calls.INCOMING_TYPE) {
            holder.callDirection.setImageResource(R.drawable.call_status_incoming);
            if (log.getType() == CallLog.Calls.MISSED_TYPE) {
                holder.callDirection.setImageResource(R.drawable.call_status_missed);
            } else {
                holder.callDirection.setImageResource(R.drawable.call_status_incoming);
            }
        }

        holder.contact.setText(address);
        holder.detail.setOnClickListener(mListener);
    }

    @Override
    public int getItemCount() {
        return mLogs.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView contact;
        public final ImageView detail;
        public final ImageView callDirection;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contact = itemView.findViewById(R.id.contact_name);
            detail = itemView.findViewById(R.id.detail);
            callDirection = itemView.findViewById(R.id.icon);
        }
    }
}
