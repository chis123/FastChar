package com.fastchar.converters;

import com.fastchar.asm.FastParameter;
import com.fastchar.core.FastAction;
import com.fastchar.core.FastHandler;
import com.fastchar.interfaces.IFastParamConverter;
import com.fastchar.utils.FastArrayUtils;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;

/**
 * FastAction 路由方法中形参类型为Number(int/double/float/long/short)、Number[]、List&lt;Number&gt;的参数转换器
 */
@SuppressWarnings("unchecked")
public class FastNumberParamConverter implements IFastParamConverter {
    @Override
    public Object convertValue(FastAction action, FastParameter parameter, FastHandler handler) throws Exception {
        Object value = checkInt(action, parameter, handler);
        if (handler.getCode() == 1) {
            return value;
        }

        value = checkShort(action,parameter, handler);
        if (handler.getCode() == 1) {
            return value;
        }

        value = checkLong(action, parameter, handler);
        if (handler.getCode() == 1) {
            return value;
        }
        value = checkDouble(action, parameter, handler);
        if (handler.getCode() == 1) {
            return value;
        }
        value = checkFloat(action, parameter, handler);
        if (handler.getCode() == 1) {
            return value;
        }
        return value;
    }

    private Object checkInt(FastAction action,  FastParameter parameter,  FastHandler handler) throws InstantiationException, IllegalAccessException {
        Object value = null;
        if (parameter.getType() == int.class
                || parameter.getType() == Integer.class) {
            value = action.getParamToInt(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == Integer[].class) {
            value = action.getParamToIntArray(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == int[].class) {
            value = FastArrayUtils.toPrimitive(action.getParamToIntArray(parameter.getName()));
            handler.setCode(1);
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
            if ( parameter.getParameterizedType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)  parameter.getParameterizedType();
                if (parameterizedType.getActualTypeArguments()[0] == Integer.class) {
                    Collection collection = FastTypeHelper.getCollectionInstance(parameter.getType());
                    if (collection != null) {
                        Integer[] paramToArray = action.getParamToIntArray(parameter.getName());
                        collection.addAll(Arrays.asList(paramToArray));
                        value = collection;
                    }
                    handler.setCode(1);
                }
            }
        }
        return value;
    }

    private Object checkShort(FastAction action, FastParameter parameter, FastHandler handler) throws InstantiationException, IllegalAccessException {
        Object value = null;
        if (parameter.getType() == short.class
                || parameter.getType() == Short.class) {
            value = action.getParamToShort(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == Short[].class) {
            value = action.getParamToShortArray(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == short[].class) {
            value = FastArrayUtils.toPrimitive(action.getParamToShortArray(parameter.getName()));
            handler.setCode(1);
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
            if (parameter.getParameterizedType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
                if (parameterizedType.getActualTypeArguments()[0] == Short.class) {
                    Collection collection = FastTypeHelper.getCollectionInstance(parameter.getType());
                    if (collection != null) {
                        Short[] paramToArray = action.getParamToShortArray(parameter.getName());
                        collection.addAll(Arrays.asList(paramToArray));
                        value = collection;
                    }
                    handler.setCode(1);
                }
            }
        }
        return value;
    }



    private Object checkLong(FastAction action,FastParameter parameter, FastHandler handler) throws InstantiationException, IllegalAccessException {
        Object value = null;
        if (parameter.getType() == long.class
                || parameter.getType() == Long.class) {
            value = action.getParamToLong(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == Long[].class) {
            value = action.getParamToLongArray(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == long[].class) {
            value = FastArrayUtils.toPrimitive(action.getParamToLongArray(parameter.getName()));
            handler.setCode(1);
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
            if ( parameter.getParameterizedType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)  parameter.getParameterizedType();
                if (parameterizedType.getActualTypeArguments()[0] == Long.class) {
                    Collection collection = FastTypeHelper.getCollectionInstance(parameter.getType());
                    if (collection != null) {
                        Long[] paramToArray = action.getParamToLongArray(parameter.getName());
                        collection.addAll(Arrays.asList(paramToArray));
                        value = collection;
                    }
                    handler.setCode(1);
                }
            }
        }
        return value;
    }



    private Object checkDouble(FastAction action, FastParameter parameter,  FastHandler handler) throws InstantiationException, IllegalAccessException {
        Object value = null;
        if (parameter.getType() == double.class
                || parameter.getType() == Double.class) {
            value = action.getParamToDouble(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == Double[].class) {
            value = action.getParamToDoubleArray(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == double[].class) {
            value = FastArrayUtils.toPrimitive(action.getParamToDoubleArray(parameter.getName()));
            handler.setCode(1);
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
            if ( parameter.getParameterizedType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)  parameter.getParameterizedType();
                if (parameterizedType.getActualTypeArguments()[0] == Double.class) {
                    Collection collection = FastTypeHelper.getCollectionInstance(parameter.getType());
                    if (collection != null) {
                        Double[] paramToArray = action.getParamToDoubleArray(parameter.getName());
                        collection.addAll(Arrays.asList(paramToArray));
                        value = collection;
                    }
                    handler.setCode(1);
                }
            }
        }
        return value;
    }


    private Object checkFloat(FastAction action, FastParameter parameter,  FastHandler handler) throws InstantiationException, IllegalAccessException {
        Object value = null;
        if (parameter.getType() == float.class
                || parameter.getType() == Float.class) {
            value = action.getParamToFloat(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == Float[].class) {
            value = action.getParamToFloatArray(parameter.getName());
            handler.setCode(1);
        } else if (parameter.getType() == float[].class) {
            value = FastArrayUtils.toPrimitive(action.getParamToFloatArray(parameter.getName()));
            handler.setCode(1);
        } else if (Collection.class.isAssignableFrom(parameter.getType())) {
            if ( parameter.getParameterizedType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType)  parameter.getParameterizedType();
                if (parameterizedType.getActualTypeArguments()[0] == Float.class) {
                    Collection collection = FastTypeHelper.getCollectionInstance(parameter.getType());
                    if (collection != null) {
                        Float[] paramToArray = action.getParamToFloatArray(parameter.getName());
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
