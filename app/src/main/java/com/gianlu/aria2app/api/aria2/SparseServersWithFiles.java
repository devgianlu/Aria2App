package com.gianlu.aria2app.api.aria2;

public class SparseServersWithFiles {
    public final SparseServers servers;
    public final AriaFiles files;

    public SparseServersWithFiles(SparseServers servers, AriaFiles files) {
        this.servers = servers;
        this.files = files;
    }

    public static SparseServersWithFiles empty() {
        return new SparseServersWithFiles(SparseServers.empty(), null);
    }
}
