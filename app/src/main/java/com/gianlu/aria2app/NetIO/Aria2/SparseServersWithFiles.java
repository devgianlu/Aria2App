package com.gianlu.aria2app.NetIO.Aria2;

import java.util.List;

public class SparseServersWithFiles {
    public final SparseServers servers;
    public final List<AriaFile> files;

    public SparseServersWithFiles(SparseServers servers, List<AriaFile> files) {
        this.servers = servers;
        this.files = files;
    }

    public static SparseServersWithFiles empty() {
        return new SparseServersWithFiles(SparseServers.empty(), null);
    }
}
