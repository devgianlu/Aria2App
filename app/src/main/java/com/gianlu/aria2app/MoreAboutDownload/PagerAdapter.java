package com.gianlu.aria2app.MoreAboutDownload;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class PagerAdapter extends FragmentPagerAdapter {
    private List<CommonFragment> fragments;

    public PagerAdapter(FragmentManager fm, List<CommonFragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    public void stopAllUpdater() {
        for (CommonFragment fragment : fragments) {
            fragment.stopUpdater();
        }
    }

    @Override
    public Fragment getItem(int position) {
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


}
