package com.mara.zoic.exchain.core;

/**
 * 校验模式。
 * @author Mara.X.Ma
 * @since 1.0.0 2022-06-14
 */
public enum ValidationMode {
    /**
     * 可持续模式，任意校验器在中途汇报了任何信息都会继续在校验流程中走下去，不会中断校验。
     * <p>此模式适用于需要完全校验的场景。
     * <p>这是默认的校验模式。在此模式下仍然能通过 {@link DataWrapper} 和 {@link ValidationContext} 提供的相关方法来控制后续校验流程。
     */
    SUSTAINABLE,

    /**
     * 即停模式，任意校验器在中途汇报了指定类型的信息即会导致<b>该数据</b>的后续校验中断。
     * 默认情况下遇到 {@link ValidationMessage.MessageType#ERROR} 及比其更严重的消息级别时即会中断校验。
     * <p>此模式适用于不需要完全校验的场景。
     */
    IMMEDIATE_STOP_SINGLE_DATA,

    /**
     * 即停模式，任意校验器在中途汇报了指定类型的信息即会导致<b>所有数据</b>的后续校验中断。
     * 默认情况下遇到 {@link ValidationMessage.MessageType#ERROR} 及比其更严重的消息级别时即会中断校验。
     * <p>此模式适用于不需要完全校验的场景。
     */
    IMMEDIATE_STOP_ALL_DATA;

    /**
     * 获得默认的 {@link ValidationMode}。
     * @return 默认的{@link ValidationMode}。固定为 {@link #SUSTAINABLE}。
     */
    public static ValidationMode getDefault() {
        return SUSTAINABLE;
    }
}
