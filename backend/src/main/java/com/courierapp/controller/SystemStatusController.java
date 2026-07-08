package com.courierapp.controller;

import com.courierapp.enums.BookingStatus;
import com.courierapp.enums.PartyStatus;
import com.courierapp.repository.BookingRepository;
import com.courierapp.repository.PartyRepository;
import com.courierapp.repository.UserRepository;
import com.courierapp.security.SessionTrackingService;
import com.courierapp.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.lang.management.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/system")
@Tag(name = "System Status")
@PreAuthorize("hasAuthority('ADMIN_VIEW')")
public class SystemStatusController {

    private final HealthEndpoint healthEndpoint;
    private final BookingRepository bookingRepository;
    private final PartyRepository partyRepository;
    private final UserRepository userRepository;
    private final SessionTrackingService sessionTrackingService;
    private final AuditLogService auditLogService;

    private static final Instant START_TIME = Instant.now();

    public SystemStatusController(HealthEndpoint healthEndpoint,
                                  BookingRepository bookingRepository,
                                  PartyRepository partyRepository,
                                  UserRepository userRepository,
                                  SessionTrackingService sessionTrackingService,
                                  AuditLogService auditLogService) {
        this.healthEndpoint = healthEndpoint;
        this.bookingRepository = bookingRepository;
        this.partyRepository = partyRepository;
        this.userRepository = userRepository;
        this.sessionTrackingService = sessionTrackingService;
        this.auditLogService = auditLogService;
    }

    // ── Basic status (existing endpoint — kept for backwards compat) ──────────

    @GetMapping("/status")
    @Operation(summary = "Basic system status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        long uptimeSeconds = java.time.Duration.between(START_TIME, Instant.now()).getSeconds();
        result.put("startedAt", START_TIME);
        result.put("uptimeSeconds", uptimeSeconds);
        result.put("uptimeHuman", formatUptime(uptimeSeconds));

        HealthComponent health = healthEndpoint.health();
        result.put("status", health.getStatus() == Status.UP ? "UP" : "DOWN");
        result.put("dbStatus", extractComponentStatus(health, "db"));

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        result.put("heapUsedMb", heapUsed);
        result.put("heapMaxMb", heapMax);
        result.put("heapUsedPercent", heapMax > 0 ? (int) (heapUsed * 100 / heapMax) : 0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBookings", bookingRepository.count());
        stats.put("pendingApprovalBookings", bookingRepository.countByStatus(BookingStatus.PENDING_APPROVAL));
        stats.put("approvedBookings", bookingRepository.countByStatus(BookingStatus.APPROVED));
        stats.put("totalParties", partyRepository.count());
        stats.put("pendingApprovalParties",
                partyRepository.findAll().stream()
                        .filter(p -> p.getPartyStatus() == PartyStatus.PENDING_APPROVAL).count());
        stats.put("totalUsers", userRepository.count());
        result.put("businessStats", stats);
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        return result;
    }

    // ── Full server info ──────────────────────────────────────────────────────

    @GetMapping("/info")
    @Operation(summary = "Full server and application information")
    public Map<String, Object> fullInfo() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ── Uptime / health ───────────────────────────────────────────────────
        long uptimeSeconds = java.time.Duration.between(START_TIME, Instant.now()).getSeconds();
        result.put("startedAt", START_TIME);
        result.put("uptimeSeconds", uptimeSeconds);
        result.put("uptimeHuman", formatUptime(uptimeSeconds));
        HealthComponent health = healthEndpoint.health();
        result.put("status", health.getStatus() == Status.UP ? "UP" : "DOWN");
        result.put("dbStatus", extractComponentStatus(health, "db"));
        result.put("redisStatus", extractComponentStatus(health, "redis"));

        // ── JVM / Memory ──────────────────────────────────────────────────────
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long heapCommit = memBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024);
        long nonHeapUsed = memBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
        long nonHeapCommit = memBean.getNonHeapMemoryUsage().getCommitted() / (1024 * 1024);
        long jvmTotalUsed = heapUsed + nonHeapUsed;
        long jvmTotalCommitted = heapCommit + nonHeapCommit;
        result.put("heapUsedMb", heapUsed);
        result.put("heapMaxMb", heapMax);
        result.put("heapCommittedMb", heapCommit);
        result.put("nonHeapUsedMb", nonHeapUsed);
        result.put("heapUsedPercent", heapMax > 0 ? (int) (heapUsed * 100 / heapMax) : 0);
        result.put("jvmTotalUsedMb", jvmTotalUsed);
        result.put("jvmTotalCommittedMb", jvmTotalCommitted);
        result.put("jvmMemUsedPercent", jvmTotalCommitted > 0 ? (int) (jvmTotalUsed * 100 / jvmTotalCommitted) : 0);

        // ── OS / Hardware ─────────────────────────────────────────────────────
        OperatingSystemMXBean osMx = ManagementFactory.getOperatingSystemMXBean();
        result.put("osName", osMx.getName());
        result.put("osVersion", osMx.getVersion());
        result.put("osArch", osMx.getArch());
        result.put("availableProcessors", osMx.getAvailableProcessors());
        result.put("systemLoadAverage", osMx.getSystemLoadAverage());

        // Use com.sun.management.OperatingSystemMXBean for physical memory
        if (osMx instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            long totalPhys = sunOs.getTotalMemorySize() / (1024 * 1024);
            long freePhys = sunOs.getFreeMemorySize() / (1024 * 1024);
            double cpuLoad = sunOs.getCpuLoad() * 100;
            result.put("totalPhysicalMemoryMb", totalPhys);
            result.put("freePhysicalMemoryMb", freePhys);
            result.put("usedPhysicalMemoryMb", totalPhys - freePhys);
            result.put("physMemUsedPercent", totalPhys > 0 ? (int) ((totalPhys - freePhys) * 100 / totalPhys) : 0);
            result.put("systemCpuLoadPercent", cpuLoad < 0 ? -1 : (int) cpuLoad);
            result.put("processCpuLoadPercent", (int) (sunOs.getProcessCpuLoad() * 100));
        }

        // ── JVM info ─────────────────────────────────────────────────────────
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("javaVendor", System.getProperty("java.vendor"));
        result.put("jvmName", System.getProperty("java.vm.name"));
        result.put("jvmVersion", System.getProperty("java.vm.version"));

        // ── JVM args ─────────────────────────────────────────────────────────
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        result.put("jvmArguments", jvmArgs);

        // ── JAR / App path ────────────────────────────────────────────────────
        result.put("workingDirectory", System.getProperty("user.dir"));
        // java.class.path is reliable for fat JARs (Spring Boot 3.x uses nested: URLs
        // that break getCodeSource().getLocation().toURI())
        String classPath = System.getProperty("java.class.path", "");
        String resolvedJarPath = "unknown";
        String resolvedJarDir = "unknown";
        if (!classPath.isBlank()) {
            // For `java -jar app.jar` the classpath is just the JAR name/path
            String[] entries = classPath.split(java.io.File.pathSeparator);
            for (String entry : entries) {
                if (entry.endsWith(".jar")) {
                    try {
                        java.io.File jarFile = new java.io.File(entry).getAbsoluteFile();
                        resolvedJarPath = jarFile.getAbsolutePath();
                        resolvedJarDir = jarFile.getParent() != null ? jarFile.getParent() : "";
                    } catch (Exception ignored) {
                        resolvedJarPath = entry;
                    }
                    break;
                }
            }
        }
        result.put("jarPath", resolvedJarPath);
        result.put("jarDirectory", resolvedJarDir);

        // ── Network / Hostname ────────────────────────────────────────────────
        result.put("hostname", resolveHostname());
        result.put("localIpAddresses", resolveLocalIps());

        // ── Database info (host + name only — NO password) ────────────────────
        String dsUrl = System.getProperty("spring.datasource.url",
                System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                        org.springframework.core.env.SystemEnvironmentPropertySource.class.getName()));
        String dbUrl = resolveDbUrl();
        result.put("dbUrl", maskPassword(dbUrl));
        result.put("dbUsername", resolveDbUsername());
        result.put("dbDriver", "PostgreSQL / Flyway");

        // ── Application info ──────────────────────────────────────────────────
        result.put("appName", "ShipDesk Courier Booking");
        result.put("springProfilesActive", System.getProperty("spring.profiles.active", "default"));

        // ── Threads ───────────────────────────────────────────────────────────
        ThreadMXBean threadMx = ManagementFactory.getThreadMXBean();
        result.put("threadCount", threadMx.getThreadCount());
        result.put("peakThreadCount", threadMx.getPeakThreadCount());
        result.put("daemonThreadCount", threadMx.getDaemonThreadCount());

        // ── Business stats ────────────────────────────────────────────────────
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBookings", bookingRepository.count());
        stats.put("pendingApprovalBookings", bookingRepository.countByStatus(BookingStatus.PENDING_APPROVAL));
        stats.put("approvedBookings", bookingRepository.countByStatus(BookingStatus.APPROVED));
        stats.put("totalParties", partyRepository.count());
        stats.put("pendingApprovalParties",
                partyRepository.findAll().stream()
                        .filter(p -> p.getPartyStatus() == PartyStatus.PENDING_APPROVAL).count());
        stats.put("totalUsers", userRepository.count());
        result.put("businessStats", stats);

        return result;
    }

    // ── Active sessions ───────────────────────────────────────────────────────

    @GetMapping("/sessions")
    @Operation(summary = "List all currently active user sessions")
    public List<Map<String, String>> activeSessions() {
        return sessionTrackingService.listActiveSessions();
    }

    @PostMapping("/sessions/{userId}/terminate")
    @PreAuthorize("hasAuthority('ADMIN_UPDATE')")
    @Operation(summary = "Force-terminate a user session (admin only)")
    public ResponseEntity<Map<String, Object>> terminateSession(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {
        boolean removed = sessionTrackingService.terminateSession(userId);
        auditLogService.log("AUTH", "SESSION_TERMINATED_BY_ADMIN", userId,
                String.valueOf(userId), principal.getUsername(), "Force session termination");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("terminated", removed);
        response.put("message", removed ? "Session terminated successfully" : "No active session found");
        return ResponseEntity.ok(response);
    }

    // ── JVM threads detail ────────────────────────────────────────────────────

    @GetMapping("/threads")
    @Operation(summary = "JVM thread state breakdown")
    public Map<String, Object> threadInfo() {
        ThreadMXBean threadMx = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threads = threadMx.getThreadInfo(threadMx.getAllThreadIds(), 0);

        Map<String, Long> byState = Arrays.stream(threads)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(t -> t.getThreadState().name(), Collectors.counting()));

        List<Map<String, Object>> topThreads = Arrays.stream(threads)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(t -> t.getThreadState().name()))
                .limit(50)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getThreadId());
                    m.put("name", t.getThreadName());
                    m.put("state", t.getThreadState().name());
                    m.put("blockedCount", t.getBlockedCount());
                    m.put("waitedCount", t.getWaitedCount());
                    return m;
                }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalThreads", threadMx.getThreadCount());
        result.put("peakThreadCount", threadMx.getPeakThreadCount());
        result.put("byState", byState);
        result.put("threads", topThreads);
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractComponentStatus(HealthComponent health, String component) {
        try {
            if (health instanceof CompositeHealth ch) {
                HealthComponent comp = ch.getComponents().get(component);
                if (comp != null) return comp.getStatus().getCode();
            }
        } catch (Exception ignored) {}
        return "UNKNOWN";
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m " + (seconds % 60) + "s";
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private List<String> resolveLocalIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().contains(".")) {
                        ips.add(addr.getHostAddress() + " (" + ni.getDisplayName() + ")");
                    }
                }
            }
        } catch (Exception ignored) {}
        return ips;
    }

    private String resolveDbUrl() {
        // Try env var, then system property
        String url = System.getenv("SPRING_DATASOURCE_URL");
        if (url != null) return url;
        url = System.getProperty("spring.datasource.url");
        if (url != null) return url;
        return "jdbc:postgresql://localhost:5432/courierdb";
    }

    private String resolveDbUsername() {
        String u = System.getenv("SPRING_DATASOURCE_USERNAME");
        if (u != null) return u;
        return System.getProperty("spring.datasource.username", "courier_user");
    }

    private String maskPassword(String url) {
        if (url == null) return "";
        return url.replaceAll("password=[^&;]+", "password=***")
                  .replaceAll(":[^:@/]+@", ":***@");
    }
}
