package com.fastchar.core;

import com.fastchar.annotation.AFastRoute;
import com.fastchar.asm.FastMethodRead;
import com.fastchar.exception.FastActionException;
import com.fastchar.out.FastOut;
import com.fastchar.utils.FastStringUtils;
import com.sun.xml.internal.messaging.saaj.packaging.mime.util.LineInputStream;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FastActions {
    private Class<? extends FastOut> defaultOut;
    private List<String> excludeUrls = new ArrayList<>();//排除拦截url
    private boolean excludeServlet = true;//排除拦截servlet

    public FastActions add(Class<? extends FastAction> actionClass) throws Exception {
        if (Modifier.isAbstract(actionClass.getModifiers())) {
            return this;
        }
        if (Modifier.isInterface(actionClass.getModifiers())) {
            return this;
        }
        if (!Modifier.isPublic(actionClass.getModifiers())) {
            return this;
        }
        FastDispatcher.actionResolver(actionClass);
        return this;
    }

    public Class<? extends FastOut> getDefaultOut() {
        return defaultOut;
    }

    public FastActions setDefaultOut(Class<? extends FastOut> defaultOut) {
        this.defaultOut = defaultOut;
        return this;
    }

    /**
     * 排除路径，例如：/druid/*,/user/servlet.action
     * @param urlPatterns
     * @return
     */
    public FastActions addExcludeUrls(String... urlPatterns) {
        this.excludeUrls.addAll(Arrays.asList(urlPatterns));
        return this;
    }

    public List<String> getExcludeUrls() {
        return excludeUrls;
    }

    public boolean isExcludeServlet() {
        return excludeServlet;
    }

    public FastActions setExcludeServlet(boolean excludeServlet) {
        this.excludeServlet = excludeServlet;
        return this;
    }
}
