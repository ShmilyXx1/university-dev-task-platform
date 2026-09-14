package com.svtu.config;

//import com.svtu.filter.JwtAuthenticationTokenFilter;
//import org.springframework.beans.factory.annotation.Autowired;
import com.svtu.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.Filter;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   //开启方法级权限注解（prePostEnabled = true 已包含）
public class SecurityConfig {

    //    @Autowired
//    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
//
    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private AccessDeniedHandler accessDeniedHandler;

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    // 认证管理器（从 AuthenticationConfiguration 获取）
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //关闭csrf
                .csrf().disable()
                /*不通过session获取SecurityContext，不生成HttpSession
                //服务器不会为用户登录创建任何 Session，也不会读取请求中的 JSESSIONID；
                客户端每次请求都必须携带完整的认证凭证（如 JWT 令牌放在 Header 中），服务器通过解析令牌完成身份认证，
                而非依赖 Session；
                */
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                //继续配置
                .and()
                //认证请求
                .authorizeRequests()
                //anonymous()对登录接口允许匿名访问，登录可访问，未登录不可以访问，这个permitAll()不管登没登录都可以访问
                .antMatchers("/user/userLogin"
                        ,"/user/registerGetCode","/user/registerCheckCode","/user/register"
                        ,"/user/updatePassword","/user/forgetPasswordGetCode","/user/forgetPasswordCheckCode","/ws/chat/**","/user/adminLogin").permitAll()
                // 放行图片、静态资源
                .antMatchers("/images/**", "/static/**", "/css/**", "/js/**").permitAll()
                // 放行所有 OPTIONS 预检请求（解决跨域403）
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                //出上面外的所有请求都需要鉴权认证
                .anyRequest().authenticated()
                .and()
        //添加过滤器,这个jwtAuthenticationTokenFilter过滤器在这个UsernamePasswordAuthenticationFilter过滤器前执行
                .addFilterBefore(loginInterceptor, UsernamePasswordAuthenticationFilter.class);

               //配置异常处理器
        http.exceptionHandling()
                //认证异常
                .authenticationEntryPoint(authenticationEntryPoint)
                //权限异常
                .accessDeniedHandler(accessDeniedHandler);

//                支持跨域
        http.cors();

        return http.build();
    }

    // ========== 跨域配置（Java 8 兼容版）==========
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有前端域名（生产环境替换为具体域名，如 http://localhost:8080）
        // Java 8 兼容：使用 Collections.singletonList 代替 List.of
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        // 允许所有请求方法（GET/POST/PUT/DELETE/OPTIONS）
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头（包括自定义的token等）
        config.setAllowedHeaders(Collections.singletonList("*"));
        // 允许携带Cookie（如需前后端共享Cookie则开启）
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 所有接口生效
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}