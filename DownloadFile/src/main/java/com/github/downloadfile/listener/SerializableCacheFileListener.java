package com.github.downloadfile.listener;

import java.io.IOException;
import java.io.Serializable;

public interface SerializableCacheFileListener<T>  {
    void writeObject(T object) throws IOException;
}
