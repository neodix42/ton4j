package org.ton.java.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Optimized utility class for network operations
 */
@Slf4j
public final class NetworkUtils {
    private static final Map<Long, String> INT_TO_IP_CACHE = new ConcurrentHashMap<>(1024);
    private static final Map<String, Integer> IP_TO_INT_CACHE = new ConcurrentHashMap<>(1024);
    
    private NetworkUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Convert an integer to an IP address string
     * @param ip The IP address as an integer
     * @return The IP address as a string
     */
    public static String int2ip(long ip) {
        String cachedIp = INT_TO_IP_CACHE.get(Long.valueOf(ip));
        if (cachedIp != null) {
            return cachedIp;
        }
        
        StringBuilder sb = new StringBuilder(15);
        sb.append((ip >> 24) & 0xff).append('.')
          .append((ip >> 16) & 0xff).append('.')
          .append((ip >> 8) & 0xff).append('.')
          .append(ip & 0xff);
        
        String result = sb.toString();
        INT_TO_IP_CACHE.put(Long.valueOf(ip), result);
        return result;
    }
    
    /**
     * Convert an IP address string to an integer
     * @param ip The IP address as a string
     * @return The IP address as an integer
     */
    public static int ip2int(String ip) {
        if (ip == null || ip.isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        Integer cachedInt = IP_TO_INT_CACHE.get(ip);
        if (cachedInt != null) {
            return cachedInt;
        }
        
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IP address format: " + ip);
        }
        
        int result = 0;
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(octets[i]);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid IP address octet: " + octet);
            }
            result <<= 8;
            result |= octet & 0xff;
        }
        
        IP_TO_INT_CACHE.put(ip, result);
        return result;
    }
    
    /**
     * Detect the operating system
     * @return The detected operating system
     */
    public static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        
        if (os.contains("win")) {
            return arch.contains("aarch64") ? OS.WINDOWS_ARM : OS.WINDOWS;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return arch.contains("aarch64") || arch.contains("arm") ? OS.LINUX_ARM : OS.LINUX;
        } else if (os.contains("mac")) {
            return arch.contains("aarch64") || arch.contains("arm") ? OS.MAC_ARM64 : OS.MAC;
        }
        return OS.UNKNOWN;
    }
    
    /**
     * Enum for operating system types
     */
    public enum OS {
        WINDOWS,
        WINDOWS_ARM,
        LINUX,
        LINUX_ARM,
        MAC,
        MAC_ARM64,
        UNKNOWN
    }
    
    /**
     * Disable native output
     * @param verbosityLevel The verbosity level
     */
    public static void disableNativeOutput(int verbosityLevel) {
        log.info("Disabling native output with verbosity level {}", verbosityLevel);
        // Implementation would depend on the native library integration
    }
    
    /**
     * Enable native output
     * @param verbosityLevel The verbosity level
     */
    public static void enableNativeOutput(int verbosityLevel) {
        log.info("Enabling native output with verbosity level {}", verbosityLevel);
        // Implementation would depend on the native library integration
    }
}
