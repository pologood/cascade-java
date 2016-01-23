package com.dianping.cascade;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by yangjie on 12/27/15.
 */
public class MethodParametersResolver {

    private List<ParameterResolver> parameterResolvers = Lists.newArrayList();
    private String methodName;

    public MethodParametersResolver(Method method, List<ParameterResolverFactory> parameterResolverFactories) {
        methodName = method.getName();
        int parameterIndex = 0;

        Class<?>[] types = method.getParameterTypes();

        for (Annotation[] annotations : method.getParameterAnnotations()) {
            Class type = types[parameterIndex];

            ParameterResolver parameterResolver = getParameterResolver(annotations, type, parameterResolverFactories);

            if (parameterResolver == null) {
                throw new IllegalArgumentException(methodName + ": every argument must have @Param or @Entity annotation");
            }

            parameterResolvers.add(parameterResolver);

            parameterIndex += 1;
        }
    }

    private ParameterResolver getParameterResolver(Annotation[] annotations, Class type, List<ParameterResolverFactory> parameterResolverFactories) {
        for (ParameterResolverFactory parameterResolverFactory : parameterResolverFactories) {
            ParameterResolver parameterResolver = parameterResolverFactory.create(annotations, type);
            if (parameterResolver != null) {
                return parameterResolver;
            }
        }
        return null;
    }

    public List<Object> resolve(final ContextParams params) {
        try {
            return Lists.transform(parameterResolvers, new Function<ParameterResolver, Object>() {
                @Override
                public Object apply(ParameterResolver input) {
                    return input.resolve(params);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException(methodName + ": illegal argument: " + ex.getMessage());
        }
    }
}