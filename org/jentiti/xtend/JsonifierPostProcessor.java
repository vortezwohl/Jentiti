package org.jentiti.xtend;

import java.lang.reflect.Modifier;
import org.jentiti.annotation.Jsonify;
import org.jentiti.xtend.intf.EntityPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonifierPostProcessor implements EntityPostProcessor {

    @Override
    public Object postInstantiation(Object entity) {

        if (entity.getClass().isAnnotationPresent(Jsonify.class)) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(entity.getClass());
            enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
                if (
                        method.getName().equals("toString")
                                && method.getParameterCount() == 0
                                && method.getGenericReturnType() == String.class
                                && method.getModifiers() == Modifier.PUBLIC
                ) {
                    return new ObjectMapper().writeValueAsString(obj);
                }
                return proxy.invokeSuper(obj, args);
            });
            return enhancer.create();
        }
        else
            return entity;
    }
}
