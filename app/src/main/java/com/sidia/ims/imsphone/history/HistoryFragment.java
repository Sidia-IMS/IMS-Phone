package com.sidia.ims.imsphone.history;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sidia.ims.imsphone.R;
import com.sidia.ims.imsphone.activities.ui.mainactivity.ImsPhoneViewModel;
import com.sidia.ims.imsphone.model.ImsPhoneCallLog;
import com.sidia.ims.imsphone.utils.ImsPhoneUtils;

import java.util.List;

public class HistoryFragment extends Fragment implements  View.OnClickListener{
    private RecyclerView historyList;
    private View viewInflate;

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    int[] buttonList = {
            R.id.all_calls,
            R.id.missed_calls,
            R.id.delete
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewInflate =  inflater.inflate(R.layout.fragment_history, container, false);
        return viewInflate;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        historyList = viewInflate.findViewById(R.id.history_list);
        historyList.setLayoutManager(new LinearLayoutManager(viewInflate.getContext()));

        List<ImsPhoneCallLog> call_list = ImsPhoneUtils.readCallLog(getActivity());
        historyList.setAdapter(new HistoryAdapter(getContext(), call_list, this));

        for (int id : buttonList) {
            ImageButton btn = viewInflate.findViewById(id);
            btn.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.all_calls:
                break;
            case R.id.missed_calls:
                break;
            case R.id.delete:
                break;
            default:
                break;
        }
    }
}
