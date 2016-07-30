package com.gianlu.aria2app;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.File;
import com.gianlu.aria2app.NetIO.JTA2.IDownload;
import com.gianlu.aria2app.NetIO.JTA2.IGID;
import com.gianlu.aria2app.NetIO.JTA2.IOption;
import com.gianlu.aria2app.NetIO.JTA2.ISuccess;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;

public class DownloadAction {
    private JTA2 jta2;

    public DownloadAction(Activity context) throws IOException, NoSuchAlgorithmException {
        jta2 = Utils.readyJTA2(context);
    }

    public void pause(final Context context, final String gid, final IPause handler) {
        jta2.pause(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onPaused(gid);
            }

            @Override
            public void onException(Exception ex) {
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_forceAction", true)) {
                    forcePause(gid, handler);
                } else {
                    handler.onException(ex);
                }
            }
        });
    }
    public void forcePause(String gid, final IPause handler) {
        jta2.forcePause(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onPaused(gid);
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public void moveUp(final String gid, final IMove handler) {
        jta2.changePosition(gid, -1, JTA2.POSITION_HOW.POS_CUR, new ISuccess() {
            @Override
            public void onSuccess() {
                handler.onMoved(gid);
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public void moveDown(final String gid, final IMove handler) {
        jta2.changePosition(gid, 1, JTA2.POSITION_HOW.POS_CUR, new ISuccess() {
            @Override
            public void onSuccess() {
                handler.onMoved(gid);
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public void unpause(String gid, final IUnpause handler) {
        jta2.unpause(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onUnpaused(gid);
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(ex);
            }
        });
    }

    public void remove(final Context context, final String gid, Download.STATUS status, final IRemove handler) {
        if (status == Download.STATUS.COMPLETE || status == Download.STATUS.ERROR || status == Download.STATUS.REMOVED) {
            jta2.removeDownloadResult(gid, new IGID() {
                @Override
                public void onGID(String gid) {
                    handler.onRemovedResult(gid);
                }

                @Override
                public void onException(Exception ex) {
                    handler.onException(false, ex);
                }
            });
        } else {
            jta2.remove(gid, new IGID() {
                @Override
                public void onGID(String gid) {
                    handler.onRemoved(gid);
                }

                @Override
                public void onException(Exception ex) {
                    if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_forceAction", true)) {
                        forceRemove(gid, handler);
                    } else {
                        handler.onException(true, ex);
                    }
                }
            });
        }

    }
    public void forceRemove(String gid, final IRemove handler) {
        jta2.forceRemove(gid, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.onRemoved(gid);
            }

            @Override
            public void onException(Exception ex) {
                handler.onException(true, ex);
            }
        });
    }

    public void restart(final String gid, final IRestart handler) {
        jta2.tellStatus(gid, new IDownload() {
            @Override
            public void onDownload(final Download download) {
                jta2.getOption(gid, new IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        String url = download.files.get(0).uris.get(File.URI_STATUS.USED);

                        jta2.addUri(Collections.singletonList(url), null, options, new IGID() {
                            @Override
                            public void onGID(final String newGid) {
                                jta2.removeDownloadResult(gid, new IGID() {
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
        RESUME
    }

    public interface IPause {
        void onPaused(String gid);
        void onException(Exception ex);
    }

    public interface IMove {
        void onMoved(String gid);

        void onException(Exception ex);
    }

    public interface IUnpause {
        void onUnpaused(String gid);

        void onException(Exception ex);
    }

    public interface IRemove {
        void onRemoved(String gid);

        void onRemovedResult(String gid);

        void onException(boolean b, Exception ex);
    }

    public interface IRestart {
        void onRestarted(String gid);

        void onException(Exception ex);

        void onRemoveResultException(Exception ex);

        void onGatheringInformationException(Exception ex);
    }
}




