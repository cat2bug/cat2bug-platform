package com.cat2bug.quarkus.admin.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 服务器监控数据，字段与前端 monitor/server 页面一致。
 */
@ApplicationScoped
public class ServerInfoService {

    private static final Logger LOG = Logger.getLogger(ServerInfoService.class);
    private static final long CPU_SAMPLE_MILLIS = 1000L;
    private static final long KB = 1024L;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;
    private static final long START_MILLIS = resolveStartMillis();

    private static final Set<String> PSEUDO_FS = Set.of(
            "proc", "sysfs", "devtmpfs", "devpts", "cgroup", "cgroup2", "mqueue", "securityfs", "debugfs",
            "tracefs", "pstore", "bpf", "autofs", "hugetlbfs", "fusectl", "configfs", "binfmt_misc", "nsfs",
            "rpc_pipefs", "selinuxfs", "efivarfs", "tmpfs", "shm", "devfs", "squashfs", "nullfs", "fdescfs");

    public Map<String, Object> collect() {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("cpu", cpu());
        server.put("mem", mem());
        server.put("jvm", jvm());
        server.put("sys", sys());
        server.put("sysFiles", sysFiles());
        return server;
    }

    private Map<String, Object> cpu() {
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("cpuNum", Runtime.getRuntime().availableProcessors());
        long[] ticks = sampleProcStat();
        if (ticks != null) {
            // user nice system idle iowait irq softirq steal
            double total = 0;
            for (long t : ticks) {
                total += t;
            }
            cpu.put("total", round(total * 100, 2));
            cpu.put("sys", percent(ticks[2], total));
            cpu.put("used", percent(ticks[0], total));
            cpu.put("wait", percent(ticks[4], total));
            cpu.put("free", percent(ticks[3], total));
            return cpu;
        }
        double load = processCpuLoad();
        cpu.put("total", 100.0);
        cpu.put("sys", 0.0);
        cpu.put("used", round(load * 100, 2));
        cpu.put("wait", 0.0);
        cpu.put("free", round((1 - load) * 100, 2));
        return cpu;
    }

    /** /proc/stat 两次采样的差值；非 Linux 返回 null。 */
    private static long[] sampleProcStat() {
        long[] first = readProcStat();
        if (first == null) {
            return null;
        }
        try {
            Thread.sleep(CPU_SAMPLE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        long[] second = readProcStat();
        if (second == null) {
            return null;
        }
        long[] diff = new long[8];
        for (int i = 0; i < diff.length; i++) {
            diff[i] = Math.max(0, second[i] - first[i]);
        }
        return diff;
    }

    private static long[] readProcStat() {
        Path stat = Path.of("/proc/stat");
        if (!Files.isReadable(stat)) {
            return null;
        }
        try {
            String line = Files.readAllLines(stat).get(0);
            String[] parts = line.trim().split("\\s+");
            long[] ticks = new long[8];
            for (int i = 0; i < ticks.length && i + 1 < parts.length; i++) {
                ticks[i] = Long.parseLong(parts[i + 1]);
            }
            return ticks;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static double processCpuLoad() {
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
                double load = os.getCpuLoad();
                return load >= 0 ? load : 0;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // 部分运行时不提供系统 CPU 负载
        }
        return 0;
    }

    private Map<String, Object> mem() {
        long total = 0;
        long available = 0;
        Map<String, Long> meminfo = readMeminfo();
        if (meminfo.containsKey("MemTotal") && meminfo.containsKey("MemAvailable")) {
            total = meminfo.get("MemTotal") * KB;
            available = meminfo.get("MemAvailable") * KB;
        } else {
            try {
                if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
                    total = os.getTotalMemorySize();
                    available = os.getFreeMemorySize();
                }
            } catch (RuntimeException | LinkageError ignored) {
                // 部分运行时不提供物理内存信息
            }
        }
        long used = total - available;
        Map<String, Object> mem = new LinkedHashMap<>();
        mem.put("total", div(total, GB, 2));
        mem.put("used", div(used, GB, 2));
        mem.put("free", div(available, GB, 2));
        mem.put("usage", total > 0 ? round(div(used, total, 4) * 100, 2) : 0.0);
        return mem;
    }

    private static Map<String, Long> readMeminfo() {
        Map<String, Long> values = new LinkedHashMap<>();
        Path path = Path.of("/proc/meminfo");
        if (!Files.isReadable(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String[] parts = line.split(":\\s*");
                if (parts.length == 2) {
                    values.put(parts[0].trim(), Long.parseLong(parts[1].replace("kB", "").trim()));
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.debugf("读取 /proc/meminfo 失败: %s", e.getMessage());
        }
        return values;
    }

    private Map<String, Object> jvm() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("total", div(total, MB, 2));
        jvm.put("max", div(runtime.maxMemory(), MB, 2));
        jvm.put("free", div(free, MB, 2));
        jvm.put("used", div(total - free, MB, 2));
        jvm.put("usage", total > 0 ? round(div(total - free, total, 4) * 100, 2) : 0.0);
        jvm.put("name", System.getProperty("java.vm.name", ""));
        jvm.put("version", System.getProperty("java.version", ""));
        jvm.put("home", System.getProperty("java.home", ""));
        jvm.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(START_MILLIS)));
        jvm.put("runTime", timeDistance(System.currentTimeMillis() - START_MILLIS));
        jvm.put("inputArgs", inputArgs());
        return jvm;
    }

    private static String inputArgs() {
        try {
            return ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
        } catch (RuntimeException | LinkageError e) {
            return "[]";
        }
    }

    private static long resolveStartMillis() {
        try {
            long start = ManagementFactory.getRuntimeMXBean().getStartTime();
            if (start > 0) {
                return start;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // 回退为类加载时间
        }
        return System.currentTimeMillis();
    }

    private Map<String, Object> sys() {
        Map<String, Object> sys = new LinkedHashMap<>();
        String hostName = "未知";
        String hostIp = "127.0.0.1";
        try {
            InetAddress local = InetAddress.getLocalHost();
            hostName = local.getHostName();
            hostIp = local.getHostAddress();
        } catch (IOException ignored) {
            // 无法解析本机名时保持默认值
        }
        sys.put("computerName", hostName);
        sys.put("computerIp", hostIp);
        sys.put("osName", System.getProperty("os.name"));
        sys.put("osArch", System.getProperty("os.arch"));
        sys.put("userDir", System.getProperty("user.dir"));
        return sys;
    }

    private List<Map<String, Object>> sysFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            try {
                String type = store.type();
                long total = store.getTotalSpace();
                if (total <= 0 || PSEUDO_FS.contains(type)) {
                    continue;
                }
                long free = store.getUsableSpace();
                long used = total - free;
                Map<String, Object> file = new LinkedHashMap<>();
                file.put("dirName", mountPoint(store));
                file.put("sysTypeName", type);
                file.put("typeName", store.name());
                file.put("total", convertFileSize(total));
                file.put("free", convertFileSize(free));
                file.put("used", convertFileSize(used));
                file.put("usage", round(div(used, total, 4) * 100, 2));
                files.add(file);
            } catch (IOException | RuntimeException e) {
                LOG.debugf("读取磁盘信息失败 %s: %s", store, e.getMessage());
            }
        }
        return files;
    }

    /** Unix 下 FileStore#toString 形如 {@code /mount/point (device)}。 */
    private static String mountPoint(FileStore store) {
        String text = store.toString();
        String suffix = " (" + store.name() + ")";
        return text.endsWith(suffix) ? text.substring(0, text.length() - suffix.length()) : text;
    }

    static String convertFileSize(long size) {
        if (size >= GB) {
            return String.format("%.1f GB", (float) size / GB);
        }
        if (size >= MB) {
            float f = (float) size / MB;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        }
        if (size >= KB) {
            float f = (float) size / KB;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        }
        return String.format("%d B", size);
    }

    static String timeDistance(long diffMillis) {
        long nd = 1000L * 24 * 60 * 60;
        long nh = 1000L * 60 * 60;
        long nm = 1000L * 60;
        return diffMillis / nd + "天" + diffMillis % nd / nh + "小时" + diffMillis % nd % nh / nm + "分钟";
    }

    private static double percent(double part, double total) {
        return total > 0 ? round(part / total * 100, 2) : 0.0;
    }

    private static double div(double a, double b, int scale) {
        if (b == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), scale, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
