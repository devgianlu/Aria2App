package com.gianlu.aria2app;

import android.app.Activity;
import android.preference.PreferenceManager;

import com.gianlu.jtitan.Aria2Helper.Download;
import com.gianlu.jtitan.Aria2Helper.File;
import com.gianlu.jtitan.Aria2Helper.IDownload;
import com.gianlu.jtitan.Aria2Helper.IGID;
import com.gianlu.jtitan.Aria2Helper.IOption;
import com.gianlu.jtitan.Aria2Helper.ISuccess;
import com.gianlu.jtitan.Aria2Helper.JTA2;

import java.util.Collections;
import java.util.Map;

public class DownloadAction {
    public static void pause(final Activity context, final String gid, final IPause handler) {
        Utils.readyJTA2(context).pause(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onPaused();
            }

            @Override
            public void onException(Exception ex) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_forceAction", true)) {
                    forcePause(context, gid, handler);
                } else {
                    handler.onException(ex);
                }
            }
        });
    }

    public static void forcePause(final Activity context, String gid, final IPause handler) {
        Utils.readyJTA2(context).forcePause(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onPaused();
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public static void moveUp(final Activity context, String gid, final IMove handler) {
        Utils.readyJTA2(context).changePosition(gid, -1, JTA2.POSITION_HOW.POS_CUR, new ISuccess() {
            @Override
            public void onSuccess() {
                handler.onMoved();
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public static void moveDown(final Activity context, String gid, final IMove handler) {
        Utils.readyJTA2(context).changePosition(gid, 1, JTA2.POSITION_HOW.POS_CUR, new ISuccess() {
            @Override
            public void onSuccess() {
                handler.onMoved();
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public static void unpause(final Activity context, String gid, final IUnpause handler) {
        Utils.readyJTA2(context).unpause(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onUnpaused();
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public static void remove(final Activity context, final String gid, Download.STATUS status, final IRemove handler) {
        if (status == Download.STATUS.COMPLETE || status == Download.STATUS.ERROR || status == Download.STATUS.REMOVED) {
            Utils.readyJTA2(context).removeDownloadResult(gid, new IGID() {
                @Override
                public void onGID(String gid) {
                    handler.onRemovedResult();
                }

                @Override
                public void onException(Exception ex) {
                    handler.onException(false, ex);
                }
            });
        } else {
            Utils.readyJTA2(context).remove(gid, new IGID() {
                @Override
                public void onGID(String gid) {
                    handler.onRemoved();
                }

                @Override
                public void onException(Exception ex) {
                    if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_forceAction", true)) {
                        forceRemove(context, gid, handler);
                    } else {
                        handler.onException(true, ex);
                    }
                }
            });
        }

    }

    public static void forceRemove(final Activity context, String gid, final IRemove handler) {
        Utils.readyJTA2(context).forceRemove(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onRemoved();
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(true, ex);
            }
        });
    }

    public static void restart(final Activity context, final String gid, final IRestart handler) {
        Utils.readyJTA2(context).tellStatus(gid, new IDownload() {
            @Override
            public void onDownload(final Download download) {
                Utils.readyJTA2(context).getOption(gid, new IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        String url = download.files.get(0).uris.get(File.URI_STATUS.USED);

                        Utils.readyJTA2(context).addUri(Collections.singletonList(url), null, options, new IGID() {
                            @Override
                            public void onGID(final String newGid) {
                                Utils.readyJTA2(context).removeDownloadResult(gid, new IGID() {
                                    @Override
                                    public void onGID(String gid) {
                                        handler.onRestarted(newGid);
                                    }

                                    @Override
                                    public void onException(Exception ex) {
                                        handler.onRemoveResultException(ex);
                                    }
                                });
                            }

                            @Override
                            public void onException(Exception ex) {
                                handler.onException(ex);
                            }
                        });
                    }

                    @Override
                    public void onException(Exception ex) {
                        handler.onGatheringInformationException(ex);
                    }
                });
            }

            @Override
            public void onException(final Exception ex) {
                handler.onGatheringInformationException(ex);
            }
        });
    }

    public enum ACTION {
        PAUSE,
        MOVE_UP,
        MOVE_DOWN,
        REMOVE,
        RESTART,
        RESUME,
        SHOW_MORE
    }

    public interface IPause {
        void onPaused();

        void onException(Exception ex);
    }

    public interface IMove {
        void onMoved();

        void onException(Exception ex);
    }

    public interface IUnpause {
        void onUnpaused();

        void onException(Exception ex);
    }

    public interface IRemove {
        void onRemoved();

        void onRemovedResult();

        void onException(boolean b, Exception ex);
    }

    public interface IRestart {
        void onRestarted(String gid);

        void onException(Exception ex);

        void onRemoveResultException(Exception ex);

        void onGatheringInformationException(Exception ex);
    }
}




