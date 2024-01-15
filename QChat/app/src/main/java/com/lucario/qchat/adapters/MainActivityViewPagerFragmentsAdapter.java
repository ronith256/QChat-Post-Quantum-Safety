package com.lucario.qchat.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.lucario.qchat.fragments.ChatsFragment;
import com.lucario.qchat.fragments.StatusFragment;
import com.lucario.qchat.utilities.Constants;

public class MainActivityViewPagerFragmentsAdapter extends FragmentStateAdapter {


    public MainActivityViewPagerFragmentsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ChatsFragment();
            case 1:
                return new StatusFragment();
            default:
                return new ChatsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return Constants.TITLES.length;
    }
}
