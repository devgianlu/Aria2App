package com.gianlu.aria2app;

import android.app.Activity;
import android.preference.PreferenceManager;

import com.gianlu.jtitan.Aria2Helper.Download;
import com.gianlu.jtitan.Aria2Helper.File;
import com.gianlu.jtitan.Aria2Helper.IDownload;
import com.gianlu.jtitan.Aria2Helper.IGID;
import com.gianlu.jtitan.Aria2Helper.IOption;
import com.gianlu.jtitan.Aria2Helper.JTA2;

import java.util.Collections;
import java.util.Map;

public class DownloadAction {
    private String downloadGID;
    private IAction handler;
    private JTA2 jta2;
    private Activity context;
    private boolean forceAction;

    public DownloadAction(String gid, IAction handler, Activity context) {
        downloadGID = gid;
        this.handler = handler;
        this.context = context;

        jta2 = Utils.readyJTA2(context);
        forceAction = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_forceAction", true);
    }

    public void pause() {
        jta2.pause(downloadGID, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.done(gid);
            }

            @Override
            public void onException(Exception ex) {
                if (forceAction) {
                    forcePause();
                } else {
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_PAUSE, ex.getMessage());
                    handler.failed(ex);
                }
            }
        });
    }

    // TODO: Move up/down in queue

    public void forcePause() {
        jta2.forcePause(downloadGID, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.done(gid);
            }

            @Override
            public void onException(Exception ex) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_FORCEPAUSE, ex.getMessage());
                handler.failed(ex);
            }
        });
    }

    public void unpause() {
        jta2.unpause(downloadGID, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.done(gid);
            }

            @Override
            public void onException(Exception ex) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_UNPAUSE, ex.getMessage());
                handler.failed(ex);
            }
        });
    }

    public void remove(Download.STATUS status) {
        if (status == Download.STATUS.COMPLETE || status == Download.STATUS.ERROR || status == Download.STATUS.REMOVED) {
            jta2.removeDownloadResult(downloadGID, new IGID() {
                @Override
                public void onGID(String gid) {
                    handler.done(gid);
                }

                @Override
                public void onException(Exception ex) {
                    Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_REMOVERESULT, ex.getMessage());
                    handler.failed(ex);
                }
            });
        } else {
            jta2.remove(downloadGID, new IGID() {
                @Override
                public void onGID(String gid) {
                    handler.done(gid);
                }

                @Override
                public void onException(Exception ex) {
                    if (forceAction) {
                        forceRemove();
                    } else {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_REMOVE, ex.getMessage());
                        handler.failed(ex);
                    }
                }
            });
        }

    }

    public void forceRemove() {
        jta2.forceRemove(downloadGID, new IGID() {
            @Override
            public void onGID(String gid) {
                handler.done(gid);
            }

            @Override
            public void onException(Exception ex) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_FORCEREMOVE, ex.getMessage());
                handler.failed(ex);
            }
        });
    }

    public void restart() {
        jta2.tellStatus(downloadGID, new IDownload() {
            @Override
            public void onDownload(final Download download) {
                jta2.getOption(downloadGID, new IOption() {
                    @Override
                    public void onOptions(Map<String, String> options) {
                        String url = download.files.get(0).uris.get(File.URI_STATUS.USED);

                        jta2.addUri(Collections.singletonList(url), null, options, new IGID() {
                            @Override
                            public void onGID(String gid) {
                                jta2.removeDownloadResult(downloadGID, new IGID() {
                                    @Override
                                    public void onGID(String gid) {
                                        handler.done(gid);
                                    }

                                    @Override
                                    public void onException(Exception ex) {
                                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_REMOVERESULT, ex.getMessage());
                                        handler.failed(ex);
                                    }
                                });
                            }

                            @Override
                            public void onException(Exception ex) {
                                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_ADDDOWNLOAD, ex.getMessage());
                                handler.failed(ex);
                            }
                        });
                    }

                    @Override
                    public void onException(Exception ex) {
                        Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, ex.getMessage());
                        handler.failed(ex);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_GATHERING_INFORMATION, exception.getMessage());
                handler.failed(exception);
            }
        });
    }

    public enum ACTION {
        PAUSE,
        REMOVE,
        RESTART,
        RESUME,
        SHOW_MORE
    }

    public interface IAction {
        void done(String response);

        void failed(Exception exception);
    }
}




