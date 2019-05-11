package com.fastchar.core;


import com.fastchar.annotation.*;
import com.fastchar.asm.FastParameter;
import com.fastchar.asm.FastMethodRead;
import com.fastchar.exception.FastActionException;
import com.fastchar.exception.FastWebException;
import com.fastchar.interfaces.IFastInterceptor;
import com.fastchar.interfaces.IFastRootInterceptor;

import com.fastchar.out.FastOut;
import com.fastchar.utils.FastMethodUtils;
import com.fastchar.utils.FastStringUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("unchecked")
public final class FastDispatcher {

    private static final Map<String, FastRoute> FAST_ROUTE_MAP = new HashMap<>();
    private static final Set<String> resolved = new HashSet<>();

    static void initDispatcher() {
        FastEngine.instance().getInterceptors().sortRootInterceptor();
        resolved.clear();
    }


    public static List<String> getClassRoutes(Class<? extends FastAction> targetClass) throws Exception {
        FastAction fastAction = targetClass.newInstance();
        FastMethodRead parameterConverter = new FastMethodRead();
        List<FastMethodRead.MethodLine> lineNumber = parameterConverter.getMethodLineNumber(targetClass, "getRoute");
        String classRoute = fastAction.getRoute();
        if (FastStringUtils.isEmpty(classRoute)) {
            int line = 1;
            if (lineNumber.size() > 0) {
                line = lineNumber.get(0).getLastLine();
            }
            throw new FastActionException(FastChar.getLocal().getInfo("Action_Error1") +
                    "\n\tat " + new StackTraceElement(targetClass.getName(), "getRoute",
                    targetClass.getSimpleName() + ".java", line)
            );
        }
        List<String> classRoutes = new ArrayList<>();
        classRoutes.add(classRoute);
        if (targetClass.isAnnotationPresent(AFastRoute.class)) {
            AFastRoute fastRoute = targetClass.getAnnotation(AFastRoute.class);
            if (fastRoute.head()) {
                classRoutes.clear();
                for (String s : fastRoute.value()) {
                    String route = FastStringUtils.stripEnd(s, "/") + "/" +
                            FastStringUtils.stripStart(classRoute, "/");
                    if (classRoutes.contains(route)) {
                        continue;
                    }
                    classRoutes.add(route);
                }
            } else {
                for (String route : fastRoute.value()) {
                    if (classRoutes.contains(route)) {
                        continue;
                    }
                    classRoutes.add(route);
                }
            }
        }
        return classRoutes;
    }

    public static List<String> getClassMethodRoutes(Class<? extends FastAction> targetClass, String declaredMethodName) throws Exception {
        return getClassMethodRoutes(targetClass, targetClass.getDeclaredMethod(declaredMethodName));
    }

    public static List<String> getClassMethodRoutes(Class<? extends FastAction> targetClass, Method declaredMethod) throws Exception {
        List<String> classMethodRoutes = new ArrayList<>();
        if (Modifier.isStatic(declaredMethod.getModifiers())) {
            return classMethodRoutes;
        }
        if (Modifier.isTransient(declaredMethod.getModifiers())) {
            return classMethodRoutes;
        }
        if (Modifier.isAbstract(declaredMethod.getModifiers())) {
            return classMethodRoutes;
        }
        if (!Modifier.isPublic(declaredMethod.getModifiers())) {
            return classMethodRoutes;
        }
        if (declaredMethod.getName().equalsIgnoreCase("getRoute")) {
            return classMethodRoutes;
        }
        List<String> classRoutes = getClassRoutes(targetClass);
        List<String> methodRoutes = new ArrayList<>();
        methodRoutes.add(declaredMethod.getName());
        if (declaredMethod.isAnnotationPresent(AFastRoute.class)) {
            AFastRoute fastRoute = declaredMethod.getAnnotation(AFastRoute.class);
            if (fastRoute.head()) {
                methodRoutes.clear();
                for (String s : fastRoute.value()) {
                    String route = FastStringUtils.stripEnd(s, "/") + "/" + declaredMethod.getName();
                    if (methodRoutes.contains(route)) {
                        continue;
                    }
                    methodRoutes.add(route);
                }
            } else {
                for (String route : fastRoute.value()) {
                    if (methodRoutes.contains(route)) {
                        continue;
                    }
                    methodRoutes.add(route);
                }
            }
        }

        for (String route : classRoutes) {
            if (FastStringUtils.isEmpty(route)) continue;
            route = FastStringUtils.stripEnd(route, "/") + "/";
            for (String methodRoute : methodRoutes) {
                if (FastStringUtils.isEmpty(methodRoute)) continue;
                String classMethodRoute = route + FastStringUtils.strip(methodRoute, "/");
                if (classMethodRoutes.contains(classMethodRoute)) {
                    continue;
                }
                classMethodRoutes.add(classMethodRoute);
            }
        }
        return classMethodRoutes;
    }

    static boolean actionResolver(Class<? extends FastAction> actionClass) throws Exception {
        if (FastChar.getServletContext() == null) {
            return false;
        }
        if (resolved.contains(actionClass.getName())) {
            return true;
        }
        resolved.add(actionClass.getName());
        Class<? extends FastOut> defaultOut = FastEngine.instance().getActions().getDefaultOut();

        FastMethodRead parameterConverter = new FastMethodRead();

        if (actionClass.isAnnotationPresent(AFastAction.class)) {
            AFastAction annotation = actionClass.getAnnotation(AFastAction.class);
            if (!annotation.value()) {//被禁止
                return false;
            }
        }
        if (actionClass.isAnnotationPresent(AFastResponse.class)) {
            AFastResponse response = actionClass.getAnnotation(AFastResponse.class);
            defaultOut = FastOut.convertType(response.value());
        }
        Method[] declaredMethods = actionClass.getDeclaredMethods();
        Map<String, Integer> methodCount = new HashMap<>();
        for (Method declaredMethod : declaredMethods) {
            if (Modifier.isStatic(declaredMethod.getModifiers())) {
                continue;
            }
            if (Modifier.isTransient(declaredMethod.getModifiers())) {
                continue;
            }
            if (Modifier.isAbstract(declaredMethod.getModifiers())) {
                continue;
            }
            if (!Modifier.isPublic(declaredMethod.getModifiers())) {
                continue;
            }
            if (!methodCount.containsKey(declaredMethod.getName())) {
                methodCount.put(declaredMethod.getName(), 0);
            }
            methodCount.put(declaredMethod.getName(), methodCount.get(declaredMethod.getName()) + 1);

            List<FastMethodRead.MethodLine> lines = new ArrayList<>();
            List<FastParameter> parameter = parameterConverter.getParameter(declaredMethod, lines);
            List<String> classMethodRoutes = getClassMethodRoutes(actionClass, declaredMethod);

            for (String classMethodRoute : classMethodRoutes) {
                FastRoute fastRoute = new FastRoute();
                fastRoute.actionClass = actionClass;
                fastRoute.method = declaredMethod;
                fastRoute.firstMethodLineNumber = lines.get(methodCount.get(declaredMethod.getName()) - 1).getFirstLine() - 1;
                fastRoute.lastMethodLineNumber = lines.get(methodCount.get(declaredMethod.getName()) - 1).getLastLine();
                fastRoute.methodParameter = parameter;
                fastRoute.route = classMethodRoute;
                if (actionClass.isAnnotationPresent(AFastPriority.class)) {
                    AFastPriority annotation = actionClass.getAnnotation(AFastPriority.class);
                    fastRoute.priority = annotation.value();
                }

                if (FastMethodUtils.isOverride(declaredMethod)) {
                    fastRoute.priority = Math.max(AFastPriority.P_NORMAL, fastRoute.priority);
                }

                if (declaredMethod.isAnnotationPresent(AFastPriority.class)) {
                    AFastPriority annotation = declaredMethod.getAnnotation(AFastPriority.class);
                    fastRoute.priority = annotation.value();
                }

                if (declaredMethod.isAnnotationPresent(AFastResponse.class)) {
                    AFastResponse annotation = declaredMethod.getAnnotation(AFastResponse.class);
                    defaultOut = FastOut.convertType(annotation.value());
                }
                fastRoute.returnOut = defaultOut;

                initInterceptor(null, fastRoute, 0);
                initInterceptor(actionClass, fastRoute, 1);
                initInterceptor(declaredMethod, fastRoute, 2);

                if (FAST_ROUTE_MAP.containsKey(classMethodRoute)) {
                    FastRoute existFastRoute = FAST_ROUTE_MAP.get(classMethodRoute);
                    StackTraceElement newStack = new StackTraceElement(actionClass.getName(), fastRoute.getMethod().getName(),
                            actionClass.getSimpleName() + ".java", fastRoute.firstMethodLineNumber);

                    StackTraceElement currStack = new StackTraceElement(existFastRoute.getActionClass().getName(), existFastRoute.getMethod().getName(),
                            existFastRoute.getActionClass().getSimpleName() + ".java", existFastRoute.firstMethodLineNumber);
                    if (fastRoute.getPriority() == existFastRoute.getPriority()) {
                        throw new FastActionException(FastChar.getLocal().getInfo("Action_Error2", "'" + classMethodRoute + "'") +
                                "\n\tat " + newStack +
                                "\n\tat " + currStack
                        );
                    }
                    if (fastRoute.getPriority() > existFastRoute.getPriority()) {
                        FastChar.getLog().warn(FastAction.class, FastChar.getLog().warnStyle(FastChar.getLocal().getInfo("Action_Error3", "'" + classMethodRoute + "'", newStack)));
                    } else {
                        FastChar.getLog().warn(FastAction.class, FastChar.getLog().warnStyle(FastChar.getLocal().getInfo("Action_Error3", "'" + classMethodRoute + "'", currStack)));
                        continue;
                    }
                }
                fastRoute.sortInterceptors();
                FAST_ROUTE_MAP.put(classMethodRoute, fastRoute);
                if (FastChar.getConstant().isLogRoute()) {
                    FastChar.getLog().info(actionClass, classMethodRoute);
                }
            }
        }
        if (FastAction.class.isAssignableFrom(actionClass.getSuperclass())) {
            if (actionClass.getSuperclass() != FastAction.class) {
                actionResolver((Class<? extends FastAction>) actionClass.getSuperclass());
            }
        }
        return true;
    }


    private static void initInterceptor(GenericDeclaration target, FastRoute fastRoute, int index) throws Exception {
        FastMethodRead read = new FastMethodRead();
        if (target != null) {
            AFastBefore beforeInterceptor = null;
            AFastAfter afterInterceptor = null;
            if (target instanceof Class<?>) {
                beforeInterceptor = ((Class<?>) target).getAnnotation(AFastBefore.class);
                afterInterceptor = ((Class<?>) target).getAnnotation(AFastAfter.class);
            } else if (target instanceof Method) {
                beforeInterceptor = ((Method) target).getAnnotation(AFastBefore.class);
                afterInterceptor = ((Method) target).getAnnotation(AFastAfter.class);
            }
            if (beforeInterceptor != null) {
                for (Class<? extends IFastInterceptor> aClass : beforeInterceptor.value()) {
                    List<FastMethodRead.MethodLine> lineNumber = read.getMethodLineNumber(aClass, "onInterceptor");
                    FastRoute.RouteInterceptor routeInterceptor = new FastRoute.RouteInterceptor();
                    routeInterceptor.index = index;
                    routeInterceptor.priority = beforeInterceptor.priority();
                    routeInterceptor.interceptorClass = aClass;
                    if (lineNumber.size() > 0) {
                        routeInterceptor.firstMethodLineNumber = lineNumber.get(0).getFirstLine();
                        routeInterceptor.lastMethodLineNumber = lineNumber.get(0).getLastLine();
                    }
                    fastRoute.addBeforeInterceptor(routeInterceptor);
                }
            }

            if (afterInterceptor != null) {
                for (Class<? extends IFastInterceptor> aClass : afterInterceptor.value()) {
                    List<FastMethodRead.MethodLine> lineNumber = read.getMethodLineNumber(aClass, "onInterceptor");
                    FastRoute.RouteInterceptor routeInterceptor = new FastRoute.RouteInterceptor();
                    routeInterceptor.index = index;
                    routeInterceptor.priority = afterInterceptor.priority();
                    routeInterceptor.interceptorClass = aClass;
                    if (lineNumber.size() > 0) {
                        routeInterceptor.firstMethodLineNumber = lineNumber.get(0).getFirstLine();
                        routeInterceptor.lastMethodLineNumber = lineNumber.get(0).getLastLine();
                    }
                    fastRoute.addAfterInterceptor(routeInterceptor);
                }
            }
        } else {
            List<FastInterceptors.InterceptorInfo<IFastInterceptor>> beforeInterceptors = FastEngine.instance().getInterceptors().getBeforeInterceptors(fastRoute.getRoute());
            for (FastInterceptors.InterceptorInfo<IFastInterceptor> beforeInterceptor : beforeInterceptors) {
                List<FastMethodRead.MethodLine> lineNumber = read.getMethodLineNumber(beforeInterceptor.getInterceptor(), "onInterceptor");
                FastRoute.RouteInterceptor routeInterceptor = new FastRoute.RouteInterceptor();
                routeInterceptor.index = index;
                routeInterceptor.priority = beforeInterceptor.getPriority();
                routeInterceptor.interceptorClass = beforeInterceptor.getInterceptor();
                if (lineNumber.size() > 0) {
                    routeInterceptor.firstMethodLineNumber = lineNumber.get(0).getFirstLine();
                    routeInterceptor.lastMethodLineNumber = lineNumber.get(0).getLastLine();
                }
                fastRoute.addBeforeInterceptor(routeInterceptor);
            }

            List<FastInterceptors.InterceptorInfo<IFastInterceptor>> afterInterceptors = FastEngine.instance().getInterceptors().getAfterInterceptors(fastRoute.getRoute());
            for (FastInterceptors.InterceptorInfo<IFastInterceptor> afterInterceptor : afterInterceptors) {
                List<FastMethodRead.MethodLine> lineNumber = read.getMethodLineNumber(afterInterceptor.getInterceptor(), "onInterceptor");
                FastRoute.RouteInterceptor routeInterceptor = new FastRoute.RouteInterceptor();
                routeInterceptor.index = index;
                routeInterceptor.priority = afterInterceptor.getPriority();
                routeInterceptor.interceptorClass = afterInterceptor.getInterceptor();
                if (lineNumber.size() > 0) {
                    routeInterceptor.firstMethodLineNumber = lineNumber.get(0).getFirstLine();
                    routeInterceptor.lastMethodLineNumber = lineNumber.get(0).getLastLine();
                }
                fastRoute.addAfterInterceptor(routeInterceptor);
            }
        }
    }


    private FilterChain filterChain;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private String contentUrl;
    private List<Class<? extends IFastRootInterceptor>> rootInterceptor;
    private int interceptorIndex = -1;
    private FastAction forwarder;

    public FastDispatcher(HttpServletRequest request, HttpServletResponse response) {
        this(null, request, response);
    }

    public FastDispatcher(FilterChain filterChain, HttpServletRequest request, HttpServletResponse response) {
        this.filterChain = filterChain;
        this.request = request;
        this.response = response;
        this.init();
    }

    private void init() {
        String requestUrl = request.getRequestURL().toString();
        this.contentUrl = FastUrlParser.getContentPath(requestUrl);
        this.rootInterceptor = FastEngine.instance().getInterceptors().getRootInterceptors(contentUrl);
        this.interceptorIndex = -1;
    }

    public FilterChain getFilterChain() {
        return filterChain;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public FastDispatcher setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
        return this;
    }

    public FastAction getForwarder() {
        return forwarder;
    }

    public FastDispatcher setForwarder(FastAction forwarder) {
        this.forwarder = forwarder;
        return this;
    }

    /**
     * 验证路径是否可被转发到FastChar
     *
     * @param contentPath
     * @return
     */
    private boolean validateUrl(String contentPath) {
        List<String> excludeUrls = FastChar.getActions().getExcludeUrls();
        for (String excludeUrl : excludeUrls) {
            if (FastStringUtils.matches(excludeUrl, contentPath)) {
                return false;
            }
        }
        File localFile = new File(FastChar.getPath().getWebRootPath(), contentPath);
        if (localFile.exists()) {//存在于webroot下
            return false;
        }
        return true;
    }


    public void invoke() throws FastWebException {
        try {
            interceptorIndex++;
            if (interceptorIndex < rootInterceptor.size()) {
                IFastRootInterceptor interceptor = rootInterceptor.get(interceptorIndex).newInstance();
                interceptor.onInterceptor(request, response, this);
                return;
            }

            Date inTime = new Date();
            List<FastUrl> parse = FastUrlParser.parse(contentUrl);
            for (int i = 0; i < parse.size(); i++) {
                FastUrl fastUrl = parse.get(i);
                if (FAST_ROUTE_MAP.containsKey(fastUrl.getMethodRoute())) {
                    FastRoute fastRoute = FAST_ROUTE_MAP.get(fastUrl.getMethodRoute()).copy();
                    fastRoute.inTime = inTime;
                    fastRoute.request = request;
                    fastRoute.response = response;
                    fastRoute.fastUrl = fastUrl;
                    fastRoute.forwarder = forwarder;
                    fastRoute.rootInterceptor = rootInterceptor;
                    fastRoute.stackTraceElements.addAll(Arrays.asList(Thread.currentThread().getStackTrace()));
                    fastRoute.invoke();
                    return;
                }
                if (FAST_ROUTE_MAP.containsKey(fastUrl.getMethodRouteIndex())) {
                    FastRoute fastRoute = FAST_ROUTE_MAP.get(fastUrl.getMethodRouteIndex()).copy();
                    fastRoute.inTime = inTime;
                    fastRoute.request = request;
                    fastRoute.response = response;
                    fastRoute.fastUrl = fastUrl;
                    fastRoute.forwarder = forwarder;
                    fastRoute.rootInterceptor = rootInterceptor;
                    fastRoute.stackTraceElements.addAll(Arrays.asList(Thread.currentThread().getStackTrace()));
                    fastRoute.invoke();
                    return;
                }
                if (i == 0) {
                    if (!validateUrl(contentUrl)) {
                        doFilter();
                        return;
                    }
                }

            }

            contentUrl = FastStringUtils.stripEnd(contentUrl, "/");
            if (contentUrl.lastIndexOf(".") > 0) {
                doFilter();
                return;
            }

            FastRoute fastRoute404 = new FastRoute();
            fastRoute404.inTime = inTime;
            fastRoute404.route = contentUrl;


            FastAction fastErrorAction = new FastAction() {
                @Override
                public String getRoute() {
                    return null;
                }
            };
            fastRoute404.fastAction = fastErrorAction;
            fastErrorAction.fastRoute = fastRoute404;
            fastErrorAction.request = request;
            fastErrorAction.response = response;
            fastErrorAction.fastUrl = new FastUrl();
            fastErrorAction.response404("the route '" + contentUrl + "' not found!");
        } catch (Exception e) {
            throw new FastWebException(e);
        }
    }

    private void doFilter() throws Exception {
        if (this.filterChain != null) {
            this.filterChain.doFilter(this.request, this.response);
        }
    }
}