package com.dianping.cascade;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Created by yangjie on 9/22/15.
 */
public class Cascade {
    private Map<String, Invokable> invokableMap = Maps.newHashMap();

    public Map process(Collection<Field> fields, Object input) {
        return buildFields(Maps.newHashMap(), fields, new ContextParams(toMap(input), null));
    }

    public Map process(Field field, Object input) {
        return process(Lists.newArrayList(field), input);
    }

    @SuppressWarnings("unchecked")
    private Map buildFields(Map results, Collection<Field> fields, ContextParams parentContextParams) {
        ContextParams contextParams = new ContextParams(results, parentContextParams);

        for (Field field : fields) {
            if (field.getType() == null) {
                results.putAll(buildFields(field.getParams(), field.getChildren(), contextParams));
            } else {
                results.put(field.getComputedAs(), buildField(field, contextParams));
            }
        }
        return results;
    }

    private Object buildField(final Field field, ContextParams parentContextParams) {

        Invokable invokable = invokableMap.get(field.getType());

        if (invokable == null) {
            throw new RuntimeException(String.format("Type [%s] not registered", field.getType()));
        }

        final ContextParams contextParams = new ContextParams(field.getParams(), parentContextParams);

        Object result = invokable.invoke(field.getCategory(), contextParams);

        if (CollectionUtils.isEmpty(field.getChildren()) || result == null) {
            return result;
        }

        if (result instanceof Collection) {
            return Collections2.transform((Collection) result, new Function() {
                @Override
                public Object apply(Object input) {
                    return buildFields(toMap(input), field.getChildren(), contextParams);
                }
            });
        } else {
            return buildFields(toMap(result), field.getChildren(), contextParams);
        }

    }

    private Map toMap(Object bean) {
        if (bean instanceof Map) {
            return (Map) bean;
        }

        Map resultMap;
        try {
            resultMap = PropertyUtils.describe(bean);
            resultMap.remove("class");
            return resultMap;
        } catch (Exception e) {
            throw new RuntimeException(String.format("[%s] can not convert to Map", bean.getClass().getName()));
        }
    }

    public void register(Collection beans) {
        for (Object obj : beans) {
            register(obj.getClass().getSimpleName(), obj);
        }
    }

    public void register(String type, Object bean) {
        if (invokableMap.containsKey(type)) {
            throw new RuntimeException(String.format("Type [%s] has already registered", type));
        }
        invokableMap.put(type, new Invokable(bean));
    }
}