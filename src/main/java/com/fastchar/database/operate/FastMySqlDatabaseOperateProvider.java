package com.fastchar.database.operate;

import com.fastchar.core.FastChar;
import com.fastchar.core.FastEntity;
import com.fastchar.database.FastDb;
import com.fastchar.database.FastScriptRunner;
import com.fastchar.database.FastType;
import com.fastchar.database.info.FastColumnInfo;
import com.fastchar.database.info.FastDatabaseInfo;
import com.fastchar.database.info.FastSqlInfo;
import com.fastchar.database.info.FastTableInfo;
import com.fastchar.interfaces.IFastDatabaseOperate;
import com.fastchar.utils.FastNumberUtils;
import com.fastchar.utils.FastStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.*;

/**
 * MySql数据库操作
 */
public class FastMySqlDatabaseOperateProvider implements IFastDatabaseOperate {

    public static boolean isOverride(Object data) {
        if (data == null) {
            return false;
        }
        return data.toString().equals("mysql");
    }

    private Set<String> tables = null;
    private Map<String, Set<String>> tableColumns = new HashMap<>();
    private FastDb fastDb = new FastDb().setLog(false).setIgnoreCase(true).setUseCache(false);

    @Override
    public void fetchDatabaseInfo(FastDatabaseInfo databaseInfo) throws Exception {
        Connection connection = fastDb.setDatabase(databaseInfo.getName()).getConnection();
        if (connection == null) return;
        ResultSet resultSet = null;
        try {
            DatabaseMetaData dmd = connection.getMetaData();
            String databaseProductName = dmd.getDatabaseProductName();
            databaseInfo.setProduct(databaseProductName);
            databaseInfo.setVersion(dmd.getDatabaseProductVersion());
            String userName = dmd.getUserName();
            databaseInfo.setUser(userName.split("@")[0]);
            databaseInfo.setHost(userName.split("@")[1]);
            databaseInfo.setType("mysql");
            databaseInfo.setName(connection.getCatalog());
            databaseInfo.setUrl(dmd.getURL());

            resultSet = dmd.getTables(null, null, null, new String[]{"TABLE"});
            while (resultSet.next()) {
                String table_name = resultSet.getString("TABLE_NAME");
                FastTableInfo<?> tableInfo = databaseInfo.getTableInfo(table_name);
                if (tableInfo == null) {
                    tableInfo = FastTableInfo.newInstance();
                    tableInfo.setName(table_name);
                    databaseInfo.getTables().add(tableInfo);
                }

                //检索主键
                ResultSet keyRs = dmd.getPrimaryKeys(null, null, tableInfo.getName());
                while (keyRs.next()) {
                    String column_name = keyRs.getString("COLUMN_NAME");
                    FastColumnInfo<?> columnInfo = tableInfo.getColumnInfo(column_name);
                    if (columnInfo == null) {
                        columnInfo = FastColumnInfo.newInstance();
                        columnInfo.setName(column_name);
                        tableInfo.getColumns().add(columnInfo);
                    }
                    columnInfo.setPrimary("true");
                }
                keyRs.close();

                //检索列
                String sqlStr = String.format("select * from %s where 1=0 ", tableInfo.getName());
                PreparedStatement statement = null;
                ResultSet columnsRs = null;
                try {
                    statement = connection.prepareStatement(sqlStr);
                    columnsRs = statement.executeQuery();
                    ResultSetMetaData data = columnsRs.getMetaData();
                    for (int i = 1; i < data.getColumnCount() + 1; i++) {

                        String columnName = data.getColumnName(i);
                        String type = data.getColumnTypeName(i).toLowerCase();
                        int displaySize = data.getColumnDisplaySize(i);
                        int nullable = data.isNullable(i);
                        boolean isAutoIncrement = data.isAutoIncrement(i);
                        int precision = data.getPrecision(i);
                        int scale = data.getScale(i);

                        if (displaySize >= 715827882) {
                            type = "longtext";
                        } else if (displaySize >= 21845) {
                            type = "text";
                        }

                        FastColumnInfo<?> columnInfo = tableInfo.getColumnInfo(columnName);
                        if (columnInfo == null) {
                            columnInfo = FastColumnInfo.newInstance();
                            columnInfo.setName(columnName);
                            tableInfo.getColumns().add(columnInfo);
                        }
                        if (scale > 0) {
                            columnInfo.setLength(precision + "," + scale);
                        } else {
                            columnInfo.setLength(String.valueOf(precision));
                        }
                        columnInfo.setType(type);
                        columnInfo.setAutoincrement(String.valueOf(isAutoIncrement));
                        if (nullable == ResultSetMetaData.columnNoNulls) {
                            columnInfo.setNullable("not null");
                        } else {
                            columnInfo.setNullable("null");
                        }
                        String index = getColumnIndex(databaseInfo.getName(), tableInfo.getName(), columnName);
                        columnInfo.setIndex(formatIndex(index));
                        columnInfo.fromProperty();
                    }
                } finally {
                    fastDb.close(statement, columnsRs);
                }

                tableInfo.fromProperty();
            }
            databaseInfo.fromProperty();
        } finally {
            fastDb.close(connection, resultSet);
        }
    }

    @Override
    public void createDatabase(FastDatabaseInfo databaseInfo) throws Exception {
        Connection connection = null;
        Statement statement = null;
        try {
            String driverClassName = databaseInfo.getDriver();
            Class.forName(driverClassName);

            connection = DriverManager.getConnection("jdbc:mysql://" + databaseInfo.getHost()
                            + ":" + databaseInfo.getPort() + "/mysql", databaseInfo.getUser(),
                    databaseInfo.getPassword());
            statement = connection.createStatement();
            String sqlStr = String.format("create database if not exists %s default character set utf8 collate utf8_general_ci", databaseInfo.getName());
            statement.executeUpdate(sqlStr);
        } finally {
            fastDb.close(connection, statement);
        }
    }

    @Override
    public boolean checkTableExists(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo) throws Exception {
        Connection connection = fastDb.setDatabase(databaseInfo.getName()).getConnection();
        if (connection == null) return true;
        ResultSet resultSet = null;
        try {
            if (tables == null) {
                tables = new HashSet<>();
                DatabaseMetaData dmd = connection.getMetaData();
                String databaseProductName = dmd.getDatabaseProductName();
                databaseInfo.setProduct(databaseProductName);
                databaseInfo.setVersion(dmd.getDatabaseProductVersion());
                resultSet = dmd.getTables(null, null, null, new String[]{"TABLE"});
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    tables.add(tableName);
                }
            }
        } finally {
            fastDb.close(connection, resultSet);
        }

        return tables.contains(tableInfo.getName());
    }

    @Override
    public void createTable(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo) throws Exception {
        try {
            List<String> columnSql = new ArrayList<>();
            List<String> primaryKey = new ArrayList<>();
            for (FastColumnInfo<?> column : tableInfo.getColumns()) {
                columnSql.add(buildColumnSql(column));
                if (column.isPrimary()) {
                    primaryKey.add(column.getName());
                }
            }
            if (primaryKey.size() > 0) {
                columnSql.add(" primary key (" + FastStringUtils.join(primaryKey, ",") + ")");
            }
            String sql = String.format(" create table if not exists %s ( %s ) comment = '%s' ;", tableInfo.getName(), FastStringUtils.join(columnSql, ","), tableInfo.getComment());

            fastDb.setLog(true).setDatabase(databaseInfo.getName()).update(sql);
            if (databaseInfo.getDefaultData().containsKey(tableInfo.getName())) {
                for (FastSqlInfo sqlInfo : databaseInfo.getDefaultData().get(tableInfo.getName())) {
                    fastDb.setLog(true).setDatabase(databaseInfo.getName()).update(sqlInfo.getSql(), sqlInfo.toParams());
                }
            }
            if (FastStringUtils.isNotEmpty(tableInfo.getData())) {
                File file = new File(tableInfo.getData());
                if (file.exists() && file.getName().toLowerCase().endsWith(".sql")) {
                    FastScriptRunner scriptRunner = new FastScriptRunner(fastDb.setLog(true).setDatabase(databaseInfo.getName()).getConnection());
                    scriptRunner.setLogWriter(null);
                    scriptRunner.runScript(new FileReader(file));
                    scriptRunner.closeConnection();
                }
            }

        } finally {
            FastChar.getLog().info(IFastDatabaseOperate.class, FastChar.getLocal().getInfo("Db_Table_Info1", databaseInfo.getName(), tableInfo.getName()));
        }
    }

    @Override
    public boolean checkColumnExists(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo, FastColumnInfo<?> columnInfo) throws Exception {
        Connection connection = fastDb.setDatabase(databaseInfo.getName()).getConnection();
        if (connection == null) {
            return true;
        }
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            if (!tableColumns.containsKey(tableInfo.getName())) {
                try {
                    String sqlStr = String.format("select * from %s where 1=0 ", tableInfo.getName());
                    statement = connection.prepareStatement(sqlStr);
                    Set<String> columns = new HashSet<>();
                    resultSet = statement.executeQuery();
                    ResultSetMetaData data = resultSet.getMetaData();
                    for (int i = 1; i < data.getColumnCount() + 1; i++) {
                        String columnName = data.getColumnName(i);
                        columns.add(columnName);
                    }
                    tableColumns.put(tableInfo.getName(), columns);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return tableColumns.get(tableInfo.getName()).contains(columnInfo.getName());
        } finally {
            fastDb.close(connection, statement, resultSet);
        }
    }

    @Override
    public void addColumn(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo, FastColumnInfo<?> columnInfo) throws Exception {
        try {
            String sql = String.format("alter table %s add %s", tableInfo.getName(), buildColumnSql(columnInfo));
            if (columnInfo.isPrimary()) {
                List<String> keys = getKeys(databaseInfo.getName(), tableInfo.getName());
                if (!keys.contains(columnInfo.getName())) {
                    sql += ",";
                    if (keys.size() > 0) {
                        sql += "drop primary key,";
                    }
                    keys.add(columnInfo.getName());
                    sql += "add primary key (" + FastStringUtils.join(keys, ",") + ") using btree;";
                }
            }
            fastDb.setLog(true).setDatabase(databaseInfo.getName()).update(sql);
            alterColumnIndex(databaseInfo, tableInfo.getName(), columnInfo);
        } finally {
            FastChar.getLog().info(FastMySqlDatabaseOperateProvider.class,
                    FastChar.getLocal().getInfo("Db_Table_Info2", databaseInfo.getName(), tableInfo.getName(), columnInfo.getName()));
        }
    }

    @Override
    public void alterColumn(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo, FastColumnInfo<?> columnInfo) throws Exception {
        try {
            String sql = String.format("alter table %s modify %s ", tableInfo.getName(), buildColumnSql(columnInfo));
            if (columnInfo.isPrimary()) {
                List<String> keys = getKeys(databaseInfo.getName(), tableInfo.getName());
                if (!keys.contains(columnInfo.getName())) {
                    sql += ",";
                    if (keys.size() > 0) {
                        sql += "drop primary key,";
                    }
                    keys.add(columnInfo.getName());
                    sql += "add primary key (" + FastStringUtils.join(keys, ",") + ") using btree;";
                }
            }
            fastDb.setLog(true).setDatabase(databaseInfo.getName()).update(sql);
            alterColumnIndex(databaseInfo, tableInfo.getName(), columnInfo);
        } finally {
            FastChar.getLog().info(FastMySqlDatabaseOperateProvider.class,
                    FastChar.getLocal().getInfo("Db_Table_Info3", databaseInfo.getName(),
                            tableInfo.getName(), columnInfo.getName()));
        }
    }

    private void alterColumnIndex(FastDatabaseInfo databaseInfo, String tableName, FastColumnInfo<?> columnInfo) throws Exception {
        String convertIndex = convertIndex(columnInfo);
        if (!convertIndex.equalsIgnoreCase("none")) {
            String columnName = columnInfo.getName();

            String indexName = String.format("%s_OF_%s", columnName, convertIndex.toUpperCase());

            String oldIndexName = getColumnIndex(databaseInfo.getName(), tableName, columnName);
            //如果数据库存在了此列索引，则跳过
            if (FastStringUtils.isNotEmpty(oldIndexName)) {
                return;
            }

            if (StringUtils.isNotEmpty(oldIndexName)) {
                String deleteIndexStr = "drop index " + oldIndexName + " on " + tableName + ";";
                fastDb.setLog(true).setDatabase(databaseInfo.getName()).update(deleteIndexStr);
            }
            String createIndexSql = String.format("alter table %s add %s index %s (%s%s)", tableName, convertIndex,
                    indexName, columnName, getIndexMaxLength(getType(columnInfo)));
            fastDb.setLog(true).setDatabase(databaseInfo.getName()).update(createIndexSql);
            FastChar.getLog().info(FastMySqlDatabaseOperateProvider.class,
                    FastChar.getLocal().getInfo("Db_Table_Info4", databaseInfo.getName(), tableName, columnInfo.getName(), indexName));
        }
    }


    private String getColumnIndex(String databaseName, String tableName, String columnName) {
        try {
            String checkIndexSql = String.format("select index_name  from information_schema.statistics where table_name = '%s'" +
                    "  and column_name='%s' and table_schema='%s' ", tableName, columnName, databaseName);

            FastEntity<?> fastEntity = fastDb.setLog(false)
                    .setDatabase(databaseName)
                    .selectFirst(checkIndexSql);
            if (fastEntity != null) {
                return fastEntity.getString("index_name", "");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private List<String> getKeys(String databaseName, String tableName) {
        List<String> keys = new ArrayList<>();
        try {
            String checkKeysSql = String.format("select column_name  from information_schema.key_column_usage where table_name = '%s'" +
                    "  and table_schema='%s'", tableName, databaseName);
            List<FastEntity<?>> select = fastDb.setLog(false)
                    .setDatabase(databaseName)
                    .select(checkKeysSql);
            for (FastEntity<?> fastEntity : select) {
                String column_name = fastEntity.getString("column_name", "");
                if (FastStringUtils.isNotEmpty(column_name)) {
                    keys.add(column_name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keys;
    }


    private String convertIndex(FastColumnInfo<?> columnInfo) {
        String index = columnInfo.getIndex();
        if (FastStringUtils.isNotEmpty(index)) {
            String[] indexArray = new String[]{"normal", "fulltext", "spatial", "unique"};
            if (index.equalsIgnoreCase("true") || index.equalsIgnoreCase("normal")) {
                return "";
            }
            for (String s : indexArray) {
                if (s.equalsIgnoreCase(index)) {
                    return index;
                }
            }
        }
        return "none";
    }

    private String formatIndex(String indexName) {
        if (FastStringUtils.isEmpty(indexName)) {
            return "false";
        }
        String[] of_s = indexName.split("_OF_");
        if (of_s.length > 1) {
            indexName = of_s[1];
        }
        if (FastStringUtils.isEmpty(indexName)) {
            return "true";
        }
        return indexName;
    }

    private String buildColumnSql(FastColumnInfo<?> columnInfo) {
        StringBuilder stringBuilder = new StringBuilder(columnInfo.getName());
        stringBuilder.append(" ");
        stringBuilder.append(getType(columnInfo));

        String length = getLength(columnInfo);
        if (FastStringUtils.isNotEmpty(length)) {
            stringBuilder.append(" (").append(length).append(") ");
        }
        if (FastType.isStringType(getType(columnInfo))) {
            stringBuilder.append(" character set ").append(FastStringUtils.defaultValue(columnInfo.getCharset(),
                    "utf8mb4"));
        }

        if (columnInfo.isAutoincrement()) {
            stringBuilder.append(" auto_increment ");
        }

        stringBuilder.append(" ");
        stringBuilder.append(FastStringUtils.defaultValue(columnInfo.getNullable(), " null "));

        if (FastStringUtils.isNotEmpty(columnInfo.getValue())) {
            if (FastType.isNumberType(getType(columnInfo))) {
                stringBuilder.append(" default ");
                if (FastNumberUtils.isRealNumber(columnInfo.getValue())) {
                    stringBuilder.append(columnInfo.getValue());
                }
            } else if (FastType.isStringType(getType(columnInfo))) {
                stringBuilder.append(" default ");
                stringBuilder.append("'").append(columnInfo.getValue()).append("'");
            }
        }

        stringBuilder.append(" comment '");
        stringBuilder.append(columnInfo.getComment());
        stringBuilder.append("'");

        return stringBuilder.toString();
    }


    private String getLength(FastColumnInfo<?> columnInfo) {
        String type = getType(columnInfo);
        String length = columnInfo.getLength();
        if (FastType.isIntType(type)) {
            if (FastStringUtils.isEmpty(length)) {
                return "11";
            }
        } else if (FastType.isFloatType(type) || FastType.isDoubleType(type)) {
            if (FastStringUtils.isEmpty(length)) {
                return "11,2";
            }
        } else if (FastType.isBigStringType(type)) {
            return null;
        } else if (FastType.isStringType(type)) {
            if (FastStringUtils.isEmpty(length)) {
                return "500";
            }
            if (columnInfo.isPrimary()) {
                return "250";
            }
        } else if (FastType.isSqlDateType(type) || FastType.isSqlTimeType(type) || FastType.isTimestampType(type)) {
            return null;
        }
        return length;
    }

    private String getType(FastColumnInfo<?> columnInfo) {
        return FastType.convertType("mysql", columnInfo.getType());
    }


    public String getIndexMaxLength(String type) {
        if (type.toLowerCase().equals("fulltext")) {
            return "";
        }

        if (FastType.isSqlDateType(type)
                || FastType.isSqlTimeType(type)
                || FastType.isTimestampType(type)
                || FastType.isNumberType(type)
                || FastType.isByteArrayType(type)) {
            return "";
        }
        return "(155)";
    }


}
