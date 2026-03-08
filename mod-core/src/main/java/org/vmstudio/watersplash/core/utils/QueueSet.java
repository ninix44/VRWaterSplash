package org.vmstudio.watersplash.core.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class QueueSet<T> extends LinkedList<T> {
    private final Set<T> set = new HashSet<>();

    @Override
    public boolean add(T t) {
        if (set.add(t)) {
            return super.add(t);
        }
        return false;
    }

    @Override
    public void clear() {
        set.clear();
        super.clear();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean modified = false;
        for (T element : c) {
            if (add(element)) {
                modified = true;
            }
        }
        return modified;
    }
}
