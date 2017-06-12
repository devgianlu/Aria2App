package com.gianlu.aria2app.Adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.Arrays;
import java.util.List;

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
        return getItem(position).getArguments().getString("title");
    }

    public List<F> getFragments() {
        return fragments;
    }
}
