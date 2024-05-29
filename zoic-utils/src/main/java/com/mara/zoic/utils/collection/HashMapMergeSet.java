package com.mara.zoic.utils.collection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 使用HashMap实现的并查集。
 * @param <T> 元素类型
 */
public class HashMapMergeSet<T> implements MergeSet<T> {

    private final HashMap<T, T> objs;
    private final HashMap<T, Integer> height;

    public HashMapMergeSet(int size) {
        objs = new HashMap<>(size, 1);
        height = new HashMap<>(size, 1);
    }

    public HashMapMergeSet() {
        this(16);
    }

    @Override
    public boolean isRelated(T t1, T t2) {
        return getRoot(t1) == getRoot(t2);
    }

    @Override
    public void setOrAddRelated(T t1, T t2) {
        if (!objs.containsKey(t1)) {
            add(t1);
        }
        if (!objs.containsKey(t2)) {
            add(t2);
        }
        T t1Root = getRoot(t1);
        T t2Root = getRoot(t2);
        if (Objects.equals(t1Root, t2Root)) {
            return;
        }
        int t1RootHeight = height.get(t1Root);
        int t2RootHeight = height.get(t2Root);
        if (t1RootHeight > t2RootHeight) {
            objs.put(t2Root, t1Root);
        } else {
            objs.put(t1Root, t2Root);
            if (t1RootHeight == t2RootHeight) {
                height.compute(t2Root, (k, v) -> v == null ? -1 : v + 1);
            }
        }
    }

    @Override
    public void add(T t) {
        objs.putIfAbsent(t, t);
        height.putIfAbsent(t, 1);
    }

    @Override
    public void remove(T t) {
        if (t == null || !objs.containsKey(t)) {
            return;
        }
        T tParent = getParent(t);
        objs.entrySet().stream().filter(e -> e.getValue().equals(t)).forEach(e -> {
            if (tParent.equals(t)) {
                // 如果t的父节点是自己，则将所有原本连接到t的节点连接到自己
                e.setValue(e.getKey());
            } else {
                // 如果t的父节点不是自己，则将所有原本连接到t的节点连接过去
                e.setValue(tParent);
            }
        });
        objs.remove(t);
        height.remove(t);
    }

    @Override
    public void addAll(Collection<T> coll) {
        for (T e : coll) {
            add(e);
        }
    }

    @Override
    public void removeRelated(T t1, T t2) {
        if (t1 == null || t2 == null) {
            return;
        }

        T t1Root = getRoot(t1);
        T t2Root = getRoot(t2);
        if (!Objects.equals(t1Root, t2Root)) {
            return;
        }
        int t1RootHeight = height.get(t1Root);
        int t2RootHeight = height.get(t2Root);
        if (getParent(t1).equals(t2)) {
            objs.put(t1, t1);
            if (t1RootHeight == t2RootHeight) {
                height.compute(t2Root, (k, v) -> v == null ? -1 : v + 1);
            }
        } else if (getParent(t2).equals(t1)) {
            objs.put(t2, t2);
            if (t1RootHeight == t2RootHeight) {
                height.compute(t1Root, (k, v) -> v == null ? -1 : v + 1);
            }
        }
    }

    @Override
    public T getParent(T t) {
        return objs.get(t);
    }

    @Override
    public T getRoot(T t) {
        T p = objs.get(t);
        if (p == null) {
            return null;
        }
        if (Objects.equals(p, t)) {
            return p;
        }
        T r = getRoot(p);
        objs.put(p, r);
        return r;
    }

    @Override
    public boolean isEmpty() {
        return objs.isEmpty();
    }

    @Override
    public int size() {
        return objs.size();
    }

    @Override
    public int groupCount() {
        return objs.entrySet().stream()
                .reduce(0, (i, en) -> Objects.equals(en.getKey(), en.getValue()) ? i + 1 : i, (l, r) -> r);
    }

    @Override
    public List<T> getGroupLeaders() {
        return objs.entrySet().stream()
                .filter(e -> Objects.equals(e.getKey(), e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        // HashMap<Integer, Integer> map = new HashMap<>();
        // map.put(3, 2);
        // map.put(1, 9);
        // map.put(4, 1);
        // map.put(2, 2);
        // map.put(7, 7);
        // System.out.println(map.entrySet().stream()
        //         .reduce(0, (i, en) -> Objects.equals(en.getKey(), en.getValue()) ? i + 1 : i, (l, r) -> r));

        HashMapMergeSet<String> names = new HashMapMergeSet<>();
        names.add("Mara");
        names.add("PBL");
        names.add("Bear");
        names.add("Nick");
        names.add("Simba");
        names.add("SuQ");
        names.add("XiaoLi");
        names.setOrAddRelated("Mara", "PBL");
        names.setOrAddRelated("PBL", "XiaoLi");
        names.setOrAddRelated("Nick", "Bear");
        names.setOrAddRelated("Bear", "Simba");

        System.out.println(names.getGroupLeaders());
        System.out.println(names.groupCount());
        System.out.println(names.isRelated("Mara", "Nick"));
        System.out.println(names.isRelated("Mara", "PBL"));
        System.out.println(names.isRelated("Nick", "Simba"));
        System.out.println(names.objs);

        names.removeRelated("Mara", "PBL");
        System.out.println(names.getGroupLeaders());
        System.out.println(names.groupCount());
        System.out.println(names.isRelated("Mara", "Nick"));
        System.out.println(names.isRelated("Mara", "PBL"));
        System.out.println(names.isRelated("Nick", "Simba"));
        System.out.println(names.objs);

        // int[][] isConnected = {
        //         {1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
        //         {1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        //         {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        //         {0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},
        //         {0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0},
        //         {0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
        //         {0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0},
        //         {1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0},
        //         {0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0},
        //         {0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1},
        //         {0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0},
        //         {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0},
        //         {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
        //         {0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0},
        //         {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}
        // };
        // HashMapMergeSet<Integer> cities = new HashMapMergeSet<>(isConnected.length);
        // for (int i = 0; i < isConnected.length; i++) {
        //     cities.add(i);
        //     for (int j = i + 1; j < isConnected.length; j++) {
        //         if (isConnected[i][j] == 1) {
        //             cities.setOrAddRelated(i, j);
        //         }
        //     }
        // }
        // System.out.println(cities.size());
        // System.out.println(cities.getGroupLeaders());
        // System.out.println(cities.groupCount());
        // System.out.println(cities.getParent(7));
        // System.out.println(cities.getRoot(7));
        // System.out.println(cities.objs);
        // System.out.println(cities.height);
    }
}
