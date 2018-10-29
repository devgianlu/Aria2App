package com.gianlu.aria2app.Adapters;

import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class PagerAdapter<F extends Fragment> extends FragmentStatePagerAdapter {
    private final List<F> fragments;

    public PagerAdapter(FragmentManager fm, List<F> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @SafeVarargs
    public PagerAdapter(FragmentManager fm, F... fragments) {
        super(fm);
        this.fragments = Arrays.asList(fragments);
    }

    public int indexOf(Class fragmentClass) {
        for (int i = 0; i < fragments.size(); i++)
            if (fragments.get(i).getClass() == fragmentClass)
                return i;

        return -1;
    }

    @Override
    public F getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Bundle args = getItem(position).getArguments();
        return args != null ? args.getString("title") : null;
    }

    public List<F> getFragments() {
        return fragments;
    }
}
