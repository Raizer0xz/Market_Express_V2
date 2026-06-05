package com.example.MS_sucursales.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class UsuarioContext {

    private UsuarioContext() {}

    private static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
    }

    public static Long getUsuarioId() {
        String val = getRequest().getHeader("X-Usuario-Id");
        return (val != null && !val.isBlank()) ? Long.parseLong(val) : null;
    }

    public static String getEmail() {
        return getRequest().getHeader("X-Usuario-Email");
    }

    public static String getRol() {
        return getRequest().getHeader("X-Usuario-Rol");
    }

    public static boolean esAdmin()         { return "ADMIN".equalsIgnoreCase(getRol()); }
    public static boolean esCliente()       { return "CLIENTE".equalsIgnoreCase(getRol()); }
    public static boolean esRepartidor()    { return "REPARTIDOR".equalsIgnoreCase(getRol()); }
    public static boolean esAdminSucursal() { return "ADMIN_SUCURSAL".equalsIgnoreCase(getRol()); }
}