package com.gianlu.aria2app.api.updater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.aria2app.api.aria2.DownloadWithUpdate;
import com.gianlu.aria2app.api.aria2.DownloadsAndGlobalStats;
import com.gianlu.aria2app.api.aria2.Peers;
import com.gianlu.aria2app.api.aria2.SparseServersWithFiles;

import java.util.Objects;

public final class Wants<P> {
    private final Class<P> klass;
    private final String data;

    private Wants(@NonNull Class<P> klass, @Nullable String data) {
        this.klass = klass;
        this.data = data;
    }

    @NonNull
    public static Wants<DownloadsAndGlobalStats> downloadsAndStats() {
        return new Wants<>(DownloadsAndGlobalStats.class, null);
    }

    @NonNull
    public static Wants<DownloadWithUpdate.BigUpdate> bigUpdate(String gid) {
        if (gid == null) throw new IllegalArgumentException("gid is null! Check arguments.");
        return new Wants<>(DownloadWithUpdate.BigUpdate.class, gid);
    }

    @NonNull
    public static Wants<Peers> peers(String gid) {
        if (gid == null) throw new IllegalArgumentException("gid is null! Check arguments.");
        return new Wants<>(Peers.class, gid);
    }

    @NonNull
    public static Wants<SparseServersWithFiles> serversAndFiles(String gid) {
        if (gid == null) throw new IllegalArgumentException("gid is null! Check arguments.");
        return new Wants<>(SparseServersWithFiles.class, gid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wants<?> wants = (Wants<?>) o;
        return klass.equals(wants.klass) && Objects.equals(data, wants.data);
    }

    @Override
    public int hashCode() {
        int result = klass.hashCode();
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Wants{klass=" + klass + ", data='" + data + "'}";
    }
}
