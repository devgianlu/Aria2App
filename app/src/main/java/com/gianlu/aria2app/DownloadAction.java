package com.gianlu.aria2app;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;

import com.gianlu.aria2app.NetIO.JTA2.AFile;
import com.gianlu.aria2app.NetIO.JTA2.Download;
import com.gianlu.aria2app.NetIO.JTA2.JTA2;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Map;

public class DownloadAction {
    private JTA2 jta2;

    DownloadAction(Activity context) throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException, KeyManagementException {
        jta2 = JTA2.newInstance(context);
    }

    void pause(final Context context, final String gid, final IPause handler) {
        jta2.pause(gid, new JTA2.IGID() {
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

    private void forcePause(String gid, final IPause handler) {
        jta2.forcePause(gid, new JTA2.IGID() {
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

    void moveUp(final String gid, final IMove handler) {
        jta2.changePosition(gid, -1, new JTA2.ISuccess() {
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

    void moveDown(final String gid, final IMove handler) {
        jta2.changePosition(gid, 1, new JTA2.ISuccess() {
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

    void unpause(String gid, final IUnpause handler) {
        jta2.unpause(gid, new JTA2.IGID() {
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

    void remove(final Context context, final String gid, Download.STATUS status, final IRemove handler) {
        if (status == Download.STATUS.COMPLETE || status == Download.STATUS.ERROR || status == Download.STATUS.REMOVED) {
            jta2.removeDownloadResult(gid, new JTA2.ISuccess() {
                @Override
                public void onSuccess() {
                    handler.onRemovedResult(gid);
                }

                @Override
                public void onException(Exception ex) {
                    handler.onException(false, ex);
                }
            });
        } else {
            jta2.remove(gid, new JTA2.IGID() {
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

    private void forceRemove(String gid, final IRemove handler) {
        jta2.forceRemove(gid, new JTA2.IGID() {
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

    void restart(final String gid, final IRestart handler) {
        jta2.tellStatus(gid, new JTA2.IDownload() {
            @Override
            public void onDownload(final Download download) {
                jta2.getOption(gid, new JTA2.IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        String url = download.files.get(0).uris.get(AFile.URI_STATUS.USED);

                        jta2.addUri(Collections.singletonList(url), null, options, new JTA2.IGID() {
                            @Override
                            public void onGID(final String newGid) {
                                jta2.removeDownloadResult(gid, new JTA2.ISuccess() {
                                    @Override
                                    public void onSuccess() {
                                        handler.onRestarted();
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

    interface IPause {
        void onPaused(String gid);
        void onException(Exception ex);
    }

    interface IMove {
        void onMoved(String gid);

        void onException(Exception ex);
    }

    interface IUnpause {
        void onUnpaused(String gid);

        void onException(Exception ex);
    }

    interface IRemove {
        void onRemoved(String gid);

        void onRemovedResult(String gid);

        void onException(boolean b, Exception ex);
    }

    interface IRestart {
        void onRestarted();

        void onException(Exception ex);

        void onRemoveResultException(Exception ex);

        void onGatheringInformationException(Exception ex);
    }
}




