package com.fastchar.database.sql;

import com.fastchar.core.FastChar;
import com.fastchar.core.FastEntity;
import com.fastchar.database.info.FastColumnInfo;
import com.fastchar.database.info.FastSqlInfo;
import com.fastchar.exception.FastSqlException;
import com.fastchar.utils.FastStringUtils;

import java.util.*;

public class FastMySql extends FastSql {

    public static boolean isOverride(String type) {
        if (FastStringUtils.isNotEmpty(type)) {
            return type.equalsIgnoreCase("mysql");
        }
        return false;
    }


    public FastMySql() {
        this.type = "mysql";
    }

    @Override
    public FastSqlInfo buildInsertSql(FastEntity<?> entity, String... checks) {
        if (entity == null) {
            return null;
        }
        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        entity.markDefault();
        entity.setDefaultValue();
        entity.unmarkDefault();

        TreeSet<String> treeKeys = new TreeSet<>(entity.allKeys());
        for (String key : treeKeys) {
            FastColumnInfo<?> column = entity.getColumn(key);
            if (column != null) {
                Object columnValue = getColumnValue(entity, column);
                if (column.isNotNull() && columnValue == null) {
                    throw new FastSqlException(FastChar.getLocal().getInfo("Db_Sql_Error3", "'" + column.getName() + "'"));
                }
                columns.add(key);
                values.add(columnValue);
                placeholders.add("?");
            }
        }
        if (values.size() == 0) {
            return null;
        }
        List<String> checkColumns = new ArrayList<>();
        for (String key : checks) {
            FastColumnInfo<?> column = entity.getColumn(key);
            if (column != null) {
                Object columnValue = getColumnValue(entity, column);
                values.add(columnValue);
                checkColumns.add(key);
            }
        }

        FastSqlInfo sqlInfo = newSqlInfo();
        if (checkColumns.size() == 0) {
            String sqlStr = "insert into " + entity.getTableName()
                    + " (" + FastStringUtils.join(columns, ",") + ") values" +
                    " (" + FastStringUtils.join(placeholders, ",") + ") ";
            sqlInfo.setSql(sqlStr);
        } else {
            StringBuilder sqlStr = new StringBuilder("insert into " + entity.getTableName()
                    + " (" + FastStringUtils.join(columns, ",") + ") select " +
                    FastStringUtils.join(placeholders, ",") + " from dual where not exists " +
                    " (select " + FastStringUtils.join(checkColumns, ",") + " from " + entity.getTableName() + " where 1=1 ");

            for (String check : checkColumns) {
                sqlStr.append(" and ").append(check).append(" = ? ");
            }
            sqlStr.append(")");
            sqlInfo.setSql(sqlStr.toString());
        }
        sqlInfo.setLog(entity.getBoolean("log", true));
        sqlInfo.setParams(values);
        return sqlInfo;
    }


    @Override
    public String buildPageSql(String selectSql, int page, int pageSize) {
        if (page > 0) {
            selectSql += " limit " + (page - 1) * pageSize + "," + pageSize;
        }
        return selectSql;
    }
}

