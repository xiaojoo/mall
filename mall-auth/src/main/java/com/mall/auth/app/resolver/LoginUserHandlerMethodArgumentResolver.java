package com.mall.auth.app.resolver;

import com.mall.auth.app.annotation.LoginUser;
import com.mall.auth.app.entity.UserEntity;
import com.mall.auth.app.interceptor.AuthorizationInterceptor;
import com.mall.auth.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import lombok.RequiredArgsConstructor;

/**
 * 有@LoginUser注解的方法参数，注入当前登录用户
 *
 * @author mall
 */
@Component
@RequiredArgsConstructor
public class LoginUserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(UserEntity.class) && parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                  NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
        //获取用户ID
        Object object = request.getAttribute(AuthorizationInterceptor.USER_KEY, RequestAttributes.SCOPE_REQUEST);
        if(object == null){
            return null;
        }

        //获取用户信息
        UserEntity user = userService.getById((Long)object);

        return user;
    }
}
