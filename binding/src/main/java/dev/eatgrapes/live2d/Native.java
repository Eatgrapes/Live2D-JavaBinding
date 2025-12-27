package dev.eatgrapes.live2d;

public abstract class Native implements AutoCloseable {
    protected final long _ptr;

    protected Native(long ptr) {
        if (ptr == 0) throw new RuntimeException("Native pointer is null");
        this._ptr = ptr;
    }

    public long getPtr() {
        return _ptr;
    }

    @Override
    public abstract void close();
}