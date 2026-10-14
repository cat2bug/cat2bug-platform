package com.cat2bug.admin.nativeimage;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import java.io.IOException;
import java.io.InputStream;

/**
 * commons-compress 1.28 的 {@code ZipFile} 直接引用 zstd-jni，未引入该库时 Native 构建期链接失败；
 * 本项目不读取 zstd 压缩的 zip 条目，Native 下改为报不支持。
 */
@TargetClass(className = "org.apache.commons.compress.archivers.zip.ZipFile")
final class Target_CommonsCompressZipFile {

    @Substitute
    InputStream createZstdInputStream(InputStream in) throws IOException {
        throw new IOException("Zstandard compressed zip entries are not supported");
    }
}
