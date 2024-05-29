package com.mara.zoic.exchain.core;

/**
 * {@link ValidationContext} 保持器，有助于快速获取 {@link ValidationContext}。
 * <p>当且仅当在校验环境下才设定值，不在校验环境下将会获得null。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-03-31
 */
class ValidationContextHolder {
    
    static final InheritableThreadLocal<ValidationContext> HOLDER = new InheritableThreadLocal<>();
    
}
