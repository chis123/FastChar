package com.fastchar.extend.gson;

import com.fastchar.annotation.AFastClassFind;
import com.fastchar.core.FastChar;
import com.fastchar.interfaces.IFastJsonProvider;
import com.fastchar.utils.FastClassUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * Gson https://github.com/google/gson
 */
@AFastClassFind("com.google.gson.Gson")
public class FastGsonProvider implements IFastJsonProvider {


    @Override
    public String toJson(Object value) {
        Gson gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .setDateFormat(FastChar.getConstant().getDateFormat())
                .create();
        return gson.toJson(value);
    }

    @Override
    public <T> T fromJson(String json, Class<T> targetClass) {
        return new Gson().fromJson(json, targetClass);
    }

    @Override
    public <T> T fromJson(String json, Type targetType) {
        return new Gson().fromJson(json, targetType);
    }

}
