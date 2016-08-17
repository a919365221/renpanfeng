package com.stock.util;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by 907703 on 2016/3/28.
 */
public class SimpleCache<K,V> implements Serializable{
    //private final transient  Lock lock = new ReentrantLock();
    private final int maxCapacity;
    private final Map<K,V> recents;
    //private final Map<K,V> past;

    public static void deleteCache(File file){
        file.delete();
    }
    public  static <K,V>  SimpleCache  getCache(File file,K k,V v){
        SimpleCache sc = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            sc = (SimpleCache) ois.readObject();

            ois.close();
            fis.close();
        }catch (IOException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if(sc == null){
            sc = new SimpleCache<K,V>(2000);
        }
        return sc;
    }
    public static void writeCache(SimpleCache cache,File file){

        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(cache);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public SimpleCache(int maxCapacity){
        this.maxCapacity = maxCapacity;
        this.recents = new ConcurrentHashMap<>();
        //this.past = new WeakHashMap<K,V>();
    }
    public V get(K k){
        V v = this.recents.get(k);
        /*if(v == null){
            lock.lock();
            try{
                v = this.past.get(k);
            }finally {
                lock.unlock();
            }
            if(v != null){
                this.recents.put(k,v);
            }
        }*/
        return v;
    }
    public void put(K k, V v) {
        /*if (this.recents.size() >= maxCapacity) {
            lock.lock();
            try{
                this.past.putAll(this.recents);
            }finally{
                lock.unlock();
            }
            this.recents.clear();
        }*/
        this.recents.put(k, v);
    }
}

