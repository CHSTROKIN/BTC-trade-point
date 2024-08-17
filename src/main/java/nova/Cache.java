package nova;

import java.util.concurrent.TimeUnit;

public interface Cache<K,V> {


    public V put(K key, V value);
    public V get(K key);
    public boolean contain(K key);
    public void clear();
    public int size();
}
