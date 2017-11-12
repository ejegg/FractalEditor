package com.ejegg.android.fractaleditor;

public interface ProgressListener {
        void started();
        void progressed(int progress);
        void finished();
}