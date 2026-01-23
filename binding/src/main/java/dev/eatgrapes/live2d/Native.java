package dev.eatgrapes.live2d;

/**
 * Base class for objects that wrap a native C++ pointer.
 * <p>
 * This class implements {@link AutoCloseable} to ensure native resources are properly released.
 */
public abstract class Native implements AutoCloseable {
    /**
     * The raw pointer to the native object.
     */
    protected final long _ptr;

    /**
     * Constructs a Native wrapper around the given pointer.
     * @param ptr The native pointer value.
     * @throws RuntimeException if the pointer is null (0).
     */
    protected Native(long ptr) {
        if (ptr == 0) throw new RuntimeException("Native pointer is null");
        this._ptr = ptr;
    }

    /**
     * Gets the raw native pointer.
     * @return The pointer value.
     */
    public long getPtr() {
        return _ptr;
    }

    /**
     * Releases the native resources associated with this object.
     */
    @Override
    public abstract void close();
}
