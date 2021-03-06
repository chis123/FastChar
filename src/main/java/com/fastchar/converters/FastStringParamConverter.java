package com.fastchar.converters;

import com.fastchar.asm.FastParameter;
import com.fastchar.core.FastAction;
import com.fastchar.core.FastHandler;
import com.fastchar.interfaces.IFastParamConverter;

import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * FastAction 路由方法中形参类型为String、String[]、List&lt;String&gt;的参数转换器
 */
@SuppressWarnings("unchecked")
public class FastStringParamConverter implements IFastParamConverter {
    @Override
    public Object convertValue(FastAction action, FastParameter parameter, FastHandler handler) throws Exception {
        Object value = null;
        if (parameter.getType() == String.class) {
            value = action.getParam(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == String[].class) {
            value = action.getParamToArray(parameter.getName());
            handler.setCode(1);
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
            if (parameter.getParameterizedType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
                if (parameterizedType.getActualTypeArguments()[0] == String.class) {
                    Collection collection = FastTypeHelper.getCollectionInstance(parameter.getType());
                    if (collection != null) {
                        String[] paramToArray = action.getParamToArray(parameter.getName());
                        collection.addAll(Arrays.asList(paramToArray));
                        value = collection;
                    }
                    handler.setCode(1);
                }
            }
        }
        return value;
    }
}
