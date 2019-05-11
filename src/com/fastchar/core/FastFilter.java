package com.fastchar.core;

import com.fastchar.interfaces.IFastWeb;
import com.fastchar.exception.FastWebException;

import com.fastchar.utils.FastClassUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("unchecked")
public final class FastFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            long time = System.currentTimeMillis();
            FastEngine engine = FastEngine.instance();
            engine.init(filterConfig.getServletContext());

            String web = filterConfig.getInitParameter("web");
            Class<?> aClass = FastClassUtils.getClass(web);
            if (aClass != null) {
                if (!IFastWeb.class.isAssignableFrom(aClass)) {
                    FastWebException fastWebException = new FastWebException(FastChar.getLocal().getInfo("Class_Error1", aClass.getSimpleName(), IFastWeb.class.getSimpleName()) +
                            "\n\tat " + new StackTraceElement(aClass.getName(), aClass.getSimpleName(), aClass.getSimpleName() + ".java", 1));
                    fastWebException.printStackTrace();
                    throw fastWebException;
                }
                engine.getWebs().addFastWeb((Class<? extends IFastWeb>) aClass);
            }
            engine.run();
            engine.getLog().info(FastFilter.class, engine.getLog().lightStyle(FastChar.getLocal().getInfo("FastChar_Error1", (System.currentTimeMillis() - time) / 1000.0)));
        } catch (Exception e) {
            e.printStackTrace();
            throw new FastWebException(e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        httpServletRequest.setCharacterEncoding(FastEngine.instance().getConstant().getEncoding());
        httpServletResponse.setCharacterEncoding(FastEngine.instance().getConstant().getEncoding());

        new FastDispatcher(filterChain, httpServletRequest, httpServletResponse).invoke();
    }

    @Override
    public void destroy() {
        try {
            FastEngine.instance().destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
