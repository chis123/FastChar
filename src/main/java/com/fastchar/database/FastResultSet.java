package com.fastchar.database;

import com.fastchar.core.FastEntity;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FastResultSet {

    private ResultSet resultSet;
    private boolean ignoreCase;

    public FastResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public FastEntity getFirstResult() {
        if (resultSet != null) {
            try {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                final String tableName = resultSetMetaData.getTableName(1);
                if (resultSet.next()) {
                    FastRecord fastEntity = new FastRecord();
                    fastEntity.setTableName(tableName);
                    int columnCount = resultSetMetaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        try {
                            String key = resultSetMetaData.getColumnLabel(i);
                            Object value = resultSet.getObject(key);
                            if (isIgnoreCase()) {
                                fastEntity.put(key.toLowerCase(), value);
                            } else {
                                fastEntity.put(key, value);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    return fastEntity;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public FastEntity getLastResult() {
        if (resultSet != null) {
            try {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                final String tableName = resultSetMetaData.getTableName(1);
                if (resultSet.last()) {
                    FastRecord fastEntity = new FastRecord();
                    fastEntity.setTableName(tableName);
                    int columnCount = resultSetMetaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        try {
                            String key = resultSetMetaData.getColumnLabel(i);
                            Object value = resultSet.getObject(key);
                            fastEntity.put(key, value);
                        } catch (Exception ignored) {
                        }
                    }
                    return fastEntity;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public List<FastEntity<?>> getListResult() {
        if (resultSet != null) {
            try {
                List<FastEntity<?>> list = new ArrayList<>();
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                final String tableName = resultSetMetaData.getTableName(1);
                int columnCount = resultSetMetaData.getColumnCount();
                while (resultSet.next()) {
                    FastRecord fastEntity = new FastRecord();
                    fastEntity.setTableName(tableName);
                    Map<String, Integer> keyCount = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        try {
                            String key = resultSetMetaData.getColumnLabel(i);
                            Object value = resultSet.getObject(key);
                            if (keyCount.containsKey(key)) {
                                keyCount.put(key, keyCount.get(key) + 1);
                                key = key + "(" + keyCount.get(key) + ")";
                            } else {
                                keyCount.put(key, 0);
                            }

                            fastEntity.put(key, value);
                        } catch (Exception ignored) {
                        }
                    }
                    list.add(fastEntity);
                }
                return list;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public Map<String, Object> getMap() {
        if (resultSet != null) {
            try {
                Map<String, Object> map = new HashMap<>();
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                if (resultSet.last()) {
                    int columnCount = resultSetMetaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        try {
                            String key = resultSetMetaData.getColumnLabel(i);
                            Object value = resultSet.getObject(key);
                            map.put(key, value);
                        } catch (Exception ignored) {
                        }
                    }
                }
                return map;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public FastResultSet setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
        return this;
    }
}
