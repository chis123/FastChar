package com.fastchar.core;

import com.fastchar.interfaces.IFastJsonProvider;
import com.fastchar.utils.FastBooleanUtils;
import com.fastchar.utils.FastNumberUtils;
import com.fastchar.utils.FastStringUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

@SuppressWarnings("unchecked")
public class FastBaseInfo extends LinkedHashMap<String, Object> {
    private transient int lineNumber = 1;
    private transient List<Field> fields;
    private transient String tagName;
    private transient String fileName;

    public FastBaseInfo() {
        try {
            fields = new ArrayList<>();
            Class tempClass = this.getClass();
            while (tempClass != null) {
                Field[] fieldList = tempClass.getDeclaredFields();
                for (Field field : fieldList) {
                    if (Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    fields.add(field);
                }
                if (FastBaseInfo.class.isAssignableFrom(tempClass.getSuperclass())) {
                    tempClass = tempClass.getSuperclass();
                }else{
                    tempClass = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void clear() {
        super.clear();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                field.set(this, null);
            } catch (Exception ignored) {}
        }
    }


    /**
     * 设置属性值
     *
     * @param attr
     * @param value
     */
    public void set(String attr, Object value) {
        try {
            for (Field field : fields) {
                if (field.getName().equals(attr)) {
                    field.setAccessible(true);
                    field.set(this, value);
                }
            }
            put(attr, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将自定义的属性填充到map中
     */
    public void fromProperty() {
        try {
            for (Field field : fields) {
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(this);
                if (value != null) {
                    super.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置自定义属性值，从map中
     */
    public void toProperty() {
        try {
            for (Field field : fields) {
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                if (this.containsKey(field.getName())) {
                    field.setAccessible(true);
                    field.set(this, this.get(field.getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected String buildErrorInfo(String info,String attr) {
        String error = info;
        StackTraceElement stackTrace = getStackTrace(attr);
        if (stackTrace != null) {
            error += "\n\tat " + stackTrace;
        }
        return error;
    }

    public StackTraceElement getStackTrace(String attrName) {
        if (FastStringUtils.isEmpty(getFileName())) {
            return null;
        }
        return new StackTraceElement(
                getFileName() + "." + getTagName(),
                attrName,
                getFileName(),
                getLineNumber());
    }


    public int getInt(String attr) {
        return FastNumberUtils.formatToInt(get(attr));
    }

    public int getInt(String attr, int defaultValue) {
        return FastNumberUtils.formatToInt(get(attr), defaultValue);
    }

    public String getString(String attr) {
        return String.valueOf(get(attr));
    }

    public boolean getBoolean(String attr) {
        return FastBooleanUtils.formatToBoolean(get(attr));
    }

    public boolean getBoolean(String attr, boolean defaultValue) {
        return FastBooleanUtils.formatToBoolean(get(attr), defaultValue);
    }

    public String toJson() {
        fromProperty();
        return  FastChar.getOverrides().newInstance(IFastJsonProvider.class).toJson(this);
    }


    @Override
    public String toString() {
        return toJson();
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }


}
