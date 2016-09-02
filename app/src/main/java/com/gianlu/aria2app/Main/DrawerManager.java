package com.gianlu.aria2app.Main;

import android.animation.Animator;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gianlu.aria2app.LetterIconBig;
import com.gianlu.aria2app.LetterIconSmall;
import com.gianlu.aria2app.Main.Profile.AddProfileActivity;
import com.gianlu.aria2app.Main.Profile.MultiModeProfileItem;
import com.gianlu.aria2app.Main.Profile.ProfileItem;
import com.gianlu.aria2app.Main.Profile.ProfilesAdapter;
import com.gianlu.aria2app.Main.Profile.SingleModeProfileItem;
import com.gianlu.aria2app.R;
import com.gianlu.aria2app.Utils;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DrawerManager {
    private static String lastProfile;
    private Activity context;
    private DrawerLayout drawerLayout;
    private LinearLayout drawerList;
    private LinearLayout drawerFooterList;
    private ListView drawerProfiles;
    private LinearLayout drawerProfilesFooter;
    private IDrawerListener listener;
    private ProfilesAdapter profilesAdapter;
    private boolean isProfilesLockedUntilSelected;
    private LetterIconBig currentAccount;
    private LetterIconSmall firstAccount;
    private LetterIconSmall secondAccount;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    public DrawerManager(Activity context, DrawerLayout drawerLayout) {
        this.context = context;
        this.drawerLayout = drawerLayout;
        this.drawerList = (LinearLayout) drawerLayout.findViewById(R.id.mainDrawer_list);
        this.drawerFooterList = (LinearLayout) drawerLayout.findViewById(R.id.mainDrawer_footerList);
        this.drawerProfiles = (ListView) drawerLayout.findViewById(R.id.mainDrawer_profiles);
        this.drawerProfilesFooter = (LinearLayout) drawerLayout.findViewById(R.id.mainDrawer_profilesFooter);

        this.currentAccount = (LetterIconBig) drawerLayout.findViewById(R.id.mainDrawerHeader_currentAccount);
        this.firstAccount = (LetterIconSmall) drawerLayout.findViewById(R.id.mainDrawerHeader_firstAccount);
        this.secondAccount = (LetterIconSmall) drawerLayout.findViewById(R.id.mainDrawerHeader_secondAccount);

        context.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        context.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    public DrawerManager setToolbar(Toolbar toolbar) {
        actionBarDrawerToggle = new ActionBarDrawerToggle(context, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer);
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        actionBarDrawerToggle.syncState();
        return this;
    }

    public void syncTogglerState() {
        if (actionBarDrawerToggle != null)
            actionBarDrawerToggle.syncState();
    }

    public void onTogglerConfigurationChanged(Configuration conf) {
        if (actionBarDrawerToggle != null)
            actionBarDrawerToggle.onConfigurationChanged(conf);
    }

    public void reloadRecentProfiles() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String first = preferences.getString("recentProfiles_first", null);
        String second = preferences.getString("recentProfiles_second", null);

        firstAccount.setVisibility(first == null ? View.GONE : View.VISIBLE);
        secondAccount.setVisibility(second == null ? View.GONE : View.VISIBLE);

        firstAccount.setProfileName(first)
                .setTextColor(R.color.white)
                .setShapeColor(R.color.colorPrimary, R.color.colorPrimary_shadow)
                .build();
        secondAccount.setProfileName(second)
                .setTextColor(R.color.white)
                .setShapeColor(R.color.colorPrimary, R.color.colorPrimary_shadow)
                .build();

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isProfilesLockedUntilSelected) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    drawerLayout.findViewById(R.id.mainDrawerHeader_dropdown).setEnabled(true);

                    isProfilesLockedUntilSelected = false;
                }

                try {
                    if (ProfileItem.isSingleMode(context, ((LetterIconSmall) view).getProfileName()))
                        listener.onProfileItemSelected(SingleModeProfileItem.fromString(context, ((LetterIconSmall) view).getProfileName()), true);
                    else
                        listener.onProfileItemSelected(MultiModeProfileItem.fromString(context, ((LetterIconSmall) view).getProfileName()).getCurrentProfile(context), true);
                } catch (JSONException | IOException ex) {
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
                }
            }
        };

        firstAccount.setOnClickListener(clickListener);
        secondAccount.setOnClickListener(clickListener);
    }

    public DrawerManager setCurrentProfile(SingleModeProfileItem profile) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String oldFirst = preferences.getString("recentProfiles_first", null);

        if (!Objects.equals(oldFirst, profile.getGlobalProfileName())) {
            preferences.edit()
                    .putString("recentProfiles_second", oldFirst)
                    .putString("recentProfiles_first", lastProfile)
                    .apply();
        }

        currentAccount.setProfileName(profile.getGlobalProfileName())
                .setProfileAddress(profile.getServerAddr())
                .setProfilePort(profile.getServerPort())
                .setBigTextColor(R.color.colorAccent)
                .setSmallTextColor(R.color.colorPrimary)
                .setShapeColor(R.color.white, R.color.colorPrimary_shadow)
                .build();
        ((TextView) drawerLayout.findViewById(R.id.mainDrawerHeader_profileName)).setText(profile.getGlobalProfileName());
        ((TextView) drawerLayout.findViewById(R.id.mainDrawerHeader_profileAddr)).setText(profile.getFullServerAddr());

        lastProfile = profile.getGlobalProfileName();

        reloadRecentProfiles();

        return this;
    }

    public DrawerManager setDrawerListener(IDrawerListener listener) {
        this.listener = listener;
        return this;
    }

    public DrawerManager openProfiles(boolean lockUntilSelected) {
        setDrawerState(true, true);
        setProfilesState(true);

        isProfilesLockedUntilSelected = lockUntilSelected;
        if (lockUntilSelected) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            drawerLayout.findViewById(R.id.mainDrawerHeader_dropdown).setEnabled(false);
        }

        return this;
    }

    public DrawerManager setProfilesState(boolean open) {
        if ((drawerLayout.findViewById(R.id.mainDrawer_profileContainer).getVisibility() == View.INVISIBLE && open)
                || (drawerLayout.findViewById(R.id.mainDrawer_profileContainer).getVisibility() == View.VISIBLE && !open)) {
            drawerLayout.findViewById(R.id.mainDrawerHeader_dropdown).callOnClick();
        }

        return this;
    }

    public DrawerManager setDrawerState(boolean open, boolean animate) {
        if (open)
            drawerLayout.openDrawer(GravityCompat.START, animate);
        else
            drawerLayout.closeDrawer(GravityCompat.START, animate);

        return this;
    }

    private View newItem(@DrawableRes int icon, String title, boolean primary) {
        return newItem(icon, title, primary, -1, -1, -1);
    }

    private View newItem(@DrawableRes int icon, String title, boolean primary, int badgeNumber, @ColorRes int textColorRes, @ColorRes int tintRes) {
        int textColor;
        if (textColorRes != -1)
            textColor = ContextCompat.getColor(context, textColorRes);
        else if (primary)
            textColor = Color.BLACK;
        else
            textColor = ContextCompat.getColor(context, R.color.colorPrimary_ripple);

        View view = View.inflate(context, R.layout.material_drawer_item_primary, null);
        if (tintRes != -1)
            view.setBackgroundColor(ContextCompat.getColor(context, tintRes));
        ((ImageView) view.findViewById(R.id.materialDrawer_itemIcon)).setImageResource(icon);
        ((TextView) view.findViewById(R.id.materialDrawer_itemName)).setText(title);
        ((TextView) view.findViewById(R.id.materialDrawer_itemName)).setTextColor(textColor);
        if (badgeNumber == -1) {
            view.findViewById(R.id.materialDrawer_itemBadgeContainer).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.materialDrawer_itemBadgeContainer).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.materialDrawer_itemBadge)).setText(String.valueOf(badgeNumber));
        }

        return view;
    }

    public DrawerManager updateBadge(int num) {
        View view = drawerList.getChildAt(0);

        if (num == -1) {
            view.findViewById(R.id.materialDrawer_itemBadgeContainer).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.materialDrawer_itemBadgeContainer).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.materialDrawer_itemBadge)).setText(String.valueOf(num));
        }

        return this;
    }

    public DrawerManager buildMenu() {
        drawerList.removeAllViews();

        View home = newItem(R.drawable.ic_home_black_48dp, context.getString(R.string.home), true, 0, R.color.colorAccent, R.color.colorPrimary_drawerSelected);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    setDrawerState(false, listener.onListItemSelected(DrawerListItems.HOME));
            }
        });
        drawerList.addView(home, 0);

        View terminal = newItem(R.drawable.ic_developer_board_black_48dp, context.getString(R.string.terminal), true);
        terminal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    setDrawerState(false, listener.onListItemSelected(DrawerListItems.TERMINAL));
            }
        });
        drawerList.addView(terminal);

        View globalOptions = newItem(R.drawable.ic_list_black_48dp, context.getString(R.string.menu_globalOptions), true);
        globalOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    setDrawerState(false, listener.onListItemSelected(DrawerListItems.GLOBAL_OPTIONS));
            }
        });
        drawerList.addView(globalOptions);

        View aboutAria2 = newItem(R.drawable.ic_cloud_black_48dp, context.getString(R.string.about_aria2), true);
        aboutAria2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    setDrawerState(false, listener.onListItemSelected(DrawerListItems.ABOUT_ARIA2));
            }
        });
        drawerList.addView(aboutAria2);

        // Footer group
        drawerFooterList.removeAllViews();

        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundResource(R.color.colorPrimary_ripple);
        drawerFooterList.addView(divider, 0);

        View preferences = newItem(R.drawable.ic_settings_black_48dp, context.getString(R.string.menu_preferences), false);
        preferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    setDrawerState(false, listener.onListItemSelected(DrawerListItems.PREFERENCES));
            }
        });
        drawerFooterList.addView(preferences);

        View support = newItem(R.drawable.ic_report_problem_black_48dp, context.getString(R.string.support), false);
        support.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    setDrawerState(false, listener.onListItemSelected(DrawerListItems.SUPPORT));
            }
        });
        drawerFooterList.addView(support);

        final ImageView dropdownToggle = (ImageView) drawerLayout.findViewById(R.id.mainDrawerHeader_dropdown);

        dropdownToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View profileContainer = drawerLayout.findViewById(R.id.mainDrawer_profileContainer);
                final View menuContainer = drawerLayout.findViewById(R.id.mainDrawer_menuContainer);

                if (profileContainer.getVisibility() == View.INVISIBLE) {
                    dropdownToggle.animate()
                            .rotation(180)
                            .setDuration(200)
                            .start();
                    profileContainer.setVisibility(View.VISIBLE);
                    profileContainer.setAlpha(0);
                    profileContainer.animate()
                            .alpha(1)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animator) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    profileContainer.setAlpha(1);
                                    profilesAdapter.startProfilesTest(null);
                                }

                                @Override
                                public void onAnimationCancel(Animator animator) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animator) {

                                }
                            })
                            .setDuration(200)
                            .start();

                    menuContainer.animate()
                            .alpha(0)
                            .setDuration(200)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animator) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    menuContainer.setVisibility(View.INVISIBLE);
                                }

                                @Override
                                public void onAnimationCancel(Animator animator) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animator) {

                                }
                            })
                            .start();
                } else {
                    dropdownToggle.animate()
                            .rotation(0)
                            .setDuration(200)
                            .start();

                    menuContainer.setVisibility(View.VISIBLE);
                    menuContainer.setAlpha(0);
                    menuContainer.animate()
                            .alpha(1)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animator) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    menuContainer.setAlpha(1);
                                }

                                @Override
                                public void onAnimationCancel(Animator animator) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animator) {

                                }
                            })
                            .setDuration(200)
                            .start();

                    profileContainer.animate()
                            .alpha(0)
                            .setDuration(200)
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animator) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    profileContainer.setVisibility(View.INVISIBLE);
                                }

                                @Override
                                public void onAnimationCancel(Animator animator) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animator) {

                                }
                            })
                            .start();
                }
            }
        });

        return this;
    }

    public DrawerManager buildProfiles() {
        drawerProfilesFooter.removeAllViews();

        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundResource(R.color.colorPrimary_ripple);
        drawerProfilesFooter.addView(divider, 0);

        View add = newItem(R.drawable.ic_add_black_48dp, context.getString(R.string.addProfile), false);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onAddProfile();
            }
        });
        drawerProfilesFooter.addView(add);

        View manage = newItem(R.drawable.ic_settings_black_48dp, context.getString(R.string.manageProfiles), false);
        manage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (profilesAdapter == null || profilesAdapter.getCount() == 0) return;

                new AlertDialog.Builder(context)
                        .setTitle(R.string.editProfile)
                        .setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, profilesAdapter.getItemsName()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    context.startActivity(new Intent(context, AddProfileActivity.class)
                                            .putExtra("edit", true)
                                            .putExtra("isSingleMode", ProfileItem.isSingleMode(context, profilesAdapter.getItem(i).getGlobalProfileName()))
                                            .putExtra("name", profilesAdapter.getItem(i).getGlobalProfileName()));
                                } catch (JSONException | IOException ex) {
                                    Utils.UIToast(context, Utils.TOAST_MESSAGES.CANNOT_EDIT_PROFILE, ex);
                                    context.deleteFile(profilesAdapter.getItem(i).getGlobalProfileName() + ".profile");
                                }
                            }
                        }).create().show();
            }
        });
        drawerProfilesFooter.addView(manage);

        // Load profiles
        List<ProfileItem> profiles = new ArrayList<>();
        File files[] = context.getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toLowerCase().endsWith(".profile");
            }
        });

        for (File profile : files) {
            try {
                if (ProfileItem.isSingleMode(context, profile)) {
                    profiles.add(SingleModeProfileItem.fromFile(context, profile));
                } else {
                    profiles.add(MultiModeProfileItem.fromFile(context, profile));
                }
            } catch (FileNotFoundException ex) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FILE_NOT_FOUND, ex);
            } catch (JSONException | IOException ex) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FATAL_EXCEPTION, ex);
                ex.printStackTrace();
            }
        }

        ((SwipeRefreshLayout) drawerLayout.findViewById(R.id.mainDrawer_profilesRefresh)).setColorSchemeResources(R.color.colorAccent, R.color.colorMetalink, R.color.colorTorrent);
        ((SwipeRefreshLayout) drawerLayout.findViewById(R.id.mainDrawer_profilesRefresh)).setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                buildProfiles();
                if (profilesAdapter != null)
                    profilesAdapter.startProfilesTest(new ProfilesAdapter.IFinished() {
                        @Override
                        public void onFinished() {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((SwipeRefreshLayout) drawerLayout.findViewById(R.id.mainDrawer_profilesRefresh)).setRefreshing(false);
                                }
                            });
                        }
                    });
            }
        });

        profilesAdapter = new ProfilesAdapter(context, profiles, new ProfilesAdapter.IProfile() {
            @Override
            public void onProfileSelected(SingleModeProfileItem which) {
                if (isProfilesLockedUntilSelected) {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    drawerLayout.findViewById(R.id.mainDrawerHeader_dropdown).setEnabled(true);

                    isProfilesLockedUntilSelected = false;
                }

                if (listener != null)
                    listener.onProfileItemSelected(which, false);
            }
        });
        drawerProfiles.setAdapter(profilesAdapter);

        reloadRecentProfiles();

        return this;
    }

    public enum DrawerListItems {
        HOME,
        TERMINAL,
        GLOBAL_OPTIONS,
        ABOUT_ARIA2,
        PREFERENCES,
        SUPPORT
    }

    public interface IDrawerListener {
        boolean onListItemSelected(DrawerListItems which);

        void onProfileItemSelected(SingleModeProfileItem profile, boolean fromRecent);

        void onAddProfile();
    }
}
