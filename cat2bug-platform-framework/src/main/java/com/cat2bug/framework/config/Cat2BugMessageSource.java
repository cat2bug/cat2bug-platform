package com.cat2bug.framework.config;

import org.springframework.context.support.AbstractMessageSource;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 classpath {@code i18n/messages*.properties} 加载文案。
 * GraalVM Native 下不依赖 {@link org.springframework.context.support.ReloadableResourceBundleMessageSource} 的文件刷新机制。
 */
public class Cat2BugMessageSource extends AbstractMessageSource
{
    private static final String BASE_PATH = "i18n/messages";

    private final ConcurrentHashMap<String, Properties> cachedBundles = new ConcurrentHashMap<>();

    private Charset defaultEncoding = StandardCharsets.UTF_8;

    public Cat2BugMessageSource()
    {
        loadBundleFile(bundlePath(""), "default");
    }

    public void setDefaultEncoding(String encoding)
    {
        this.defaultEncoding = Charset.forName(encoding);
    }

    @Override
    @Nullable
    protected MessageFormat resolveCode(String code, Locale locale)
    {
        String msg = resolveCodeWithoutArguments(code, locale);
        if (msg == null)
        {
            return null;
        }
        return new MessageFormat(msg, locale);
    }

    @Override
    @Nullable
    protected String resolveCodeWithoutArguments(String code, Locale locale)
    {
        for (String suffix : calculateSuffixes(locale))
        {
            Properties bundle = bundleForSuffix(suffix);
            if (bundle == null)
            {
                continue;
            }
            String value = bundle.getProperty(code);
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }

    private Properties bundleForSuffix(String suffix)
    {
        String cacheKey = suffix.isEmpty() ? "default" : suffix;
        Properties cached = cachedBundles.get(cacheKey);
        if (cached != null)
        {
            return cached;
        }
        loadBundleFile(bundlePath(suffix), cacheKey);
        return cachedBundles.get(cacheKey);
    }

    private void loadBundleFile(String path, String cacheKey)
    {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null)
        {
            return;
        }
        try (InputStream inputStream = classLoader.getResourceAsStream(path))
        {
            if (inputStream == null)
            {
                return;
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(inputStream, defaultEncoding));
            cachedBundles.put(cacheKey, properties);
        }
        catch (IOException ignored)
        {
            // 缺失的语言包在后续 suffix 链上继续解析
        }
    }

    private static String bundlePath(String suffix)
    {
        return suffix.isEmpty() ? BASE_PATH + ".properties" : BASE_PATH + "_" + suffix + ".properties";
    }

    private static List<String> calculateSuffixes(Locale locale)
    {
        List<String> suffixes = new ArrayList<>(4);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        if (!variant.isEmpty())
        {
            suffixes.add(language + "_" + country + "_" + variant);
        }
        if (!country.isEmpty())
        {
            suffixes.add(language + "_" + country);
        }
        if (!language.isEmpty())
        {
            suffixes.add(language);
        }
        suffixes.add("");
        return suffixes;
    }
}
