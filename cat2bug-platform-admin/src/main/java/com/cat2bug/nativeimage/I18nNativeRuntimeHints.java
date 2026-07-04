package com.cat2bug.nativeimage;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native 下 i18n 资源与 ResourceBundle 提示。
 */
public class I18nNativeRuntimeHints implements RuntimeHintsRegistrar
{
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader)
    {
        hints.resources().registerPattern("i18n/messages.properties");
        hints.resources().registerPattern("i18n/messages_*.properties");
        hints.resources().registerResourceBundle("i18n.messages");
    }
}
