package com.dianping.cascade.resolver.factory;

import com.dianping.cascade.ContextParams;
import com.dianping.cascade.ParameterResolver;
import com.dianping.cascade.ParameterResolverFactory;
import com.dianping.cascade.annotation.Entity;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.lang.annotation.Annotation;

/**
 * Created by yangjie on 1/24/16.
 */
public class EntityResolverFactory implements ParameterResolverFactory {
    private static ObjectMapper m = new ObjectMapper();

    {
        m.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ParameterResolver create(Annotation[] annotations, final Class type) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Entity) {
                return new ParameterResolver() {
                    @Override
                    public Object resolve(ContextParams params) {
                        try {
                            return m.convertValue(params.getAll(), type);
                        } catch (Exception ex) {
                            throw new RuntimeException(String.format("@Entity param can not create instance for type [%s]", type.getSimpleName()));
                        }
                    }
                };
            }
        }

        return null;
    }
}
