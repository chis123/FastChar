package com.fastchar.database.operate;

import com.fastchar.core.FastChar;
import com.fastchar.core.FastEntity;
import com.fastchar.database.FastDb;
import com.fastchar.database.info.FastColumnInfo;
import com.fastchar.database.info.FastDatabaseInfo;
import com.fastchar.database.info.FastSqlInfo;
import com.fastchar.database.info.FastTableInfo;
import com.fastchar.interfaces.IFastDatabaseOperateProvider;
import com.fastchar.utils.FastNumberUtils;
import com.fastchar.utils.FastStringUtils;

import java.sql.*;
import java.util.*;

/**
 * MySql数据库操作
 */
public class FastMySqlDatabaseOperateProvider implements IFastDatabaseOperateProvider {

    public static boolean isOverride(Object data) {
        if (data == null) {
            return false;
        }
        return data.toString().equals("mysql");
    }

    private Set<String> tables = null;
    private Map<String, Set<String>> tableColumns = new HashMap<>();
    private FastDb fastDb = new FastDb().setLog(false).setUseCache(false);

    @Override
    public void fetchDatabaseInfo(FastDatabaseInfo databaseInfo) throws Exception{
        Connection connection = fastDb.setDatabase(databaseInfo.getName()).getConnection();
        if (connection == null) return;
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

            ResultSet tables = dmd.getTables(null, null, null, new String[]{"TABLE"});
            while (tables.next()) {
                String table_name = tables.getString("TABLE_NAME");
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
                    FastColumnInfo columnInfo = tableInfo.getColumnInfo(column_name);
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
                PreparedStatement statement = connection.prepareStatement(sqlStr);
                ResultSet columnsRs = statement.executeQuery();
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

                    FastColumnInfo columnInfo = tableInfo.getColumnInfo(columnName);
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
                    boolean index = checkColumnIndex(databaseInfo.getName(), tableInfo.getName(), columnName);
                    columnInfo.setIndex(String.valueOf(index));
                    columnInfo.fromProperty();
                }
                columnsRs.close();
                statement.close();

                tableInfo.fromProperty();
            }
            databaseInfo.fromProperty();
            tables.close();
        } finally {
            fastDb.close(connection);
        }
    }

    @Override
    public void createDatabase(FastDatabaseInfo databaseInfo) throws Exception{
        Connection connection = null;
        try {
            String driverClassName = databaseInfo.getDriver();
            Class.forName(driverClassName);

            connection = DriverManager.getConnection("jdbc:mysql://" + databaseInfo.getHost()
                            + ":" + databaseInfo.getPort() + "/mysql", databaseInfo.getUser(),
                    databaseInfo.getPassword());
            Statement statement = connection.createStatement();
            String sqlStr = String.format("create database if not exists %s default character set utf8 collate utf8_general_ci", databaseInfo.getName());

            statement.executeUpdate(sqlStr);
            statement.close();
            connection.close();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public boolean checkTableExists(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo) throws Exception{
        Connection connection = fastDb.setDatabase(databaseInfo.getName()).getConnection();
        if (connection == null) return true;
        try {
            if (tables == null) {
                tables = new HashSet<>();
                DatabaseMetaData dmd = connection.getMetaData();
                String schemaPattern = null;
                String databaseProductName = dmd.getDatabaseProductName();
                if (databaseProductName.toLowerCase().replace(" ", "").equals("sqlserver")) {
                    schemaPattern = "dbo";
                }
                databaseInfo.setProduct(databaseProductName);
                databaseInfo.setVersion(dmd.getDatabaseProductVersion());
                ResultSet resultSet = dmd.getTables(null, schemaPattern, null, new String[]{"TABLE"});
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    tables.add(tableName);
                }
            }
        } finally {
            fastDb.close(connection);
        }

        return tables.contains(tableInfo.getName());
    }

    @Override
    public void createTable(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo) throws Exception{
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
            String sql = String.format(" create table if not exists %s ( %s );", tableInfo.getName(), FastStringUtils.join(columnSql, ","));

            fastDb.setDatabase(databaseInfo.getName()).update(sql);
            if (databaseInfo.getDefaultData().containsKey(tableInfo.getName())) {
                for (FastSqlInfo sqlInfo : databaseInfo.getDefaultData().get(tableInfo.getName())) {
                    fastDb.setDatabase(databaseInfo.getName()).update(sqlInfo.getSql(), sqlInfo.toParams());
                }
            }
        }finally {
            FastChar.getLog().info(IFastDatabaseOperateProvider.class, FastChar.getLocal().getInfo("Db_Table_Info1", databaseInfo.getName(), tableInfo.getName()));
        }
    }

    @Override
    public boolean checkColumnExists(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo, FastColumnInfo<?> columnInfo) throws Exception{
        Connection connection = fastDb.setDatabase(databaseInfo.getName()).getConnection();
        if (connection == null) {
            return true;
        }
        try {
            if (!tableColumns.containsKey(tableInfo.getName())) {
                try {
                    String sqlStr = String.format("select * from %s where 1=0 ", tableInfo.getName());
                    PreparedStatement statement = connection.prepareStatement(sqlStr);
                    Set<String> columns = new HashSet<>();
                    ResultSet columnsRs = statement.executeQuery();
                    ResultSetMetaData data = columnsRs.getMetaData();
                    for (int i = 1; i < data.getColumnCount() + 1; i++) {
                        String columnName = data.getColumnName(i);
                        columns.add(columnName);
                    }
                    tableColumns.put(tableInfo.getName(), columns);

                    columnsRs.close();
                    statement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return tableColumns.get(tableInfo.getName()).contains(columnInfo.getName());
        }  finally {
            fastDb.close(connection);
        }
    }

    @Override
    public void addColumn(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo, FastColumnInfo<?> columnInfo) throws Exception{
        try {
            String sql = String.format("alter table %s add %s", tableInfo.getName(), buildColumnSql(columnInfo));
            fastDb.setDatabase(databaseInfo.getName()).update(sql);
            alterColumnIndex(databaseInfo, tableInfo.getName(), columnInfo);
        }finally {
            FastChar.getLog().info(FastMySqlDatabaseOperateProvider.class,
                    FastChar.getLocal().getInfo("Db_Table_Info2", databaseInfo.getName(), tableInfo.getName(), columnInfo.getName()));
        }
    }

    @Override
    public void alterColumn(FastDatabaseInfo databaseInfo, FastTableInfo<?> tableInfo, FastColumnInfo<?> columnInfo) throws Exception{
        try {
            String sql = String.format("alter table %s modify %s", tableInfo.getName(), buildColumnSql(columnInfo));
            fastDb.setDatabase(databaseInfo.getName()).update(sql);
            alterColumnIndex(databaseInfo, tableInfo.getName(), columnInfo);
        } finally {
            FastChar.getLog().info(FastMySqlDatabaseOperateProvider.class,
                    FastChar.getLocal().getInfo("Db_Table_Info3", databaseInfo.getName(), tableInfo.getName(), columnInfo.getName()));
        }
    }

    private void alterColumnIndex(FastDatabaseInfo databaseInfo, String tableName, FastColumnInfo<?> columnInfo) throws Exception{
        String convertIndex = convertIndex(columnInfo);
        if (!convertIndex.equalsIgnoreCase("none")) {
            String columnName = columnInfo.getName();

            String indexName = String.format("%sIndex", columnName);

            if (!checkColumnIndex(databaseInfo.getName(), tableName, columnName)) {
                String createIndexSql = String.format("alter table %s add index %s %s (%s%s)", tableName, convertIndex, indexName, columnName, getIndexMaxLength(getType(columnInfo)));
                fastDb.setDatabase(databaseInfo.getName()).update(createIndexSql);
                FastChar.getLog().info(FastMySqlDatabaseOperateProvider.class,
                        FastChar.getLocal().getInfo("Db_Table_Info4", databaseInfo.getName(), tableName, columnInfo.getName(), indexName));
            }
        }
    }


    private boolean checkColumnIndex(String databaseName, String tableName, String columnName) {
        try {
            String checkIndexSql = String.format("select count(1) as countIndex  from information_schema.statistics where table_name = '%s'" +
                    "  and column_name='%s' and table_schema='%s'", tableName, columnName, databaseName);
            FastEntity fastEntity = fastDb.setDatabase(databaseName).selectFirst(checkIndexSql);
            if (fastEntity != null) {
                return fastEntity.getInt("countIndex") > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private String convertIndex(FastColumnInfo columnInfo) {
        String index = columnInfo.getIndex();
        String[] indexArray = new String[]{"normal", "fulltext", "spatial", "unique"};
        if (index.equalsIgnoreCase("true") || index.equalsIgnoreCase("normal")) {
            return "";
        }
        for (String s : indexArray) {
            if (s.equalsIgnoreCase(index)) {
                return index;
            }
        }
        return "none";
    }

    private String buildColumnSql(FastColumnInfo columnInfo) {
        StringBuilder stringBuilder = new StringBuilder(columnInfo.getName());
        stringBuilder.append(" ");
        stringBuilder.append(getType(columnInfo));

        String length = getLength(columnInfo);
        if (FastStringUtils.isNotEmpty(length)) {
            stringBuilder.append(" (").append(length).append(") ");
        }
        if (isStringType(getType(columnInfo))) {
            stringBuilder.append(" character set ").append(FastStringUtils.defaultValue(columnInfo.getCharset(),
                    "utf8mb4"));
        }

        if (columnInfo.isAutoincrement()) {
            stringBuilder.append(" auto_increment ");
        }

        stringBuilder.append(" ");
        stringBuilder.append(FastStringUtils.defaultValue(columnInfo.getNullable(), " null "));

        if (FastStringUtils.isNotEmpty(columnInfo.getValue())) {
            if (isNumberType(getType(columnInfo))) {
                stringBuilder.append(" default ");
                if (FastNumberUtils.isRealNumber(columnInfo.getValue())) {
                    stringBuilder.append(columnInfo.getValue());
                }
            } else if (isStringType(getType(columnInfo))) {
                stringBuilder.append(" default ");
                stringBuilder.append("'").append(columnInfo.getValue()).append("'");
            }
        }

        stringBuilder.append(" comment '");
        stringBuilder.append(columnInfo.getComment());
        stringBuilder.append("'");

        return stringBuilder.toString();
    }


    private String getLength(FastColumnInfo columnInfo) {
        String type = getType(columnInfo);
        String length = columnInfo.getLength();
        if (isIntType(type)) {
            if (FastStringUtils.isEmpty(length)) {
                return "11";
            }
        } else if (isFloatType(type)) {
            if (FastStringUtils.isEmpty(length)) {
                return "11,2";
            }
        } else if (type.equalsIgnoreCase("varchar")) {
            if (FastStringUtils.isEmpty(length)) {
                return "500";
            }
        } else if (isDateType(type)) {
            return null;
        }
        return length;
    }

    private String getType(FastColumnInfo columnInfo) {
        return columnInfo.getType();
    }


    public boolean isDateType(String type) {
        if (type.equals("date")
                || type.equals("time")
                || type.equals("year")
                || type.equals("datetime")
                || type.equals("timestamp")) {
            return true;
        }
        return false;
    }


    public boolean isNumberType(String type) {
        return isIntType(type) || isFloatType(type);
    }

    public boolean isIntType(String type) {
        if (type.equals("int")
                || type.equals("bigint")
                || type.equals("tinyint")
                || type.equals("smallint")
                || type.equals("mediumint")
                || type.equals("double")
                || type.equals("float")
                || type.equals("decimal")) {

            return true;
        }
        return false;
    }

    public boolean isFloatType(String type) {
        if (type.equals("double")
                || type.equals("float")
                || type.equals("decimal")) {

            return true;
        }
        return false;
    }


    public boolean isStringType(String type) {
        return type.contains("varchar")
                || type.contains("text")
                || type.contains("fulltext")
                || type.contains("char")
                || type.contains("mediumtext")
                || type.contains("longtext");
    }

    public boolean isBlobType(String type) {
        return type.contains("tinyblob")
                || type.contains("blob")
                || type.contains("mediumblob")
                || type.contains("longblob");
    }


    public String getIndexMaxLength(String type) {
        if (type.toLowerCase().equals("fulltext")) {
            return "";
        }

        if (isDateType(type) || isNumberType(type) || isBlobType(type)) {
            return "";
        }
        return "(155)";
    }


}
