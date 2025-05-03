package org.ton.java.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Utility class for file and resource operations
 */
@Slf4j
public final class FileUtils {
    
    private FileUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Convert an input stream to a string
     * @param is The input stream
     * @return The string
     */
    public static String streamToString(InputStream is) {
        if (is == null) {
            return null;
        }
        
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            return br.lines().collect(Collectors.joining());
        } catch (Exception e) {
            log.error("Error converting stream to string", e);
            return null;
        }
    }
    
    /**
     * Get the absolute directory of a resource
     * @param cl The class loader
     * @param resource The resource
     * @return The absolute directory
     */
    public static String getResourceAbsoluteDirectory(ClassLoader cl, String resource) {
        try {
            URL res = cl.getResource(resource);
            if (res == null) {
                throw new Error("Cannot get integrated resource " + resource);
            }
            return Paths.get(res.toURI()).toFile().getAbsolutePath();
        } catch (Exception e) {
            throw new Error("Cannot get absolute directory of resource " + resource);
        }
    }
    
    /**
     * Download a file or return the local path
     * @param linkToFile The link to the file
     * @return The local path
     */
    public static String getLocalOrDownload(String linkToFile) {
        if (linkToFile.contains("http") && linkToFile.contains("://")) {
            try {
                URL url = new URL(linkToFile);
                String filename = FilenameUtils.getName(url.getPath());
                File tmpFile = new File(filename);
                if (!tmpFile.exists()) {
                    log.info("downloading {}", linkToFile);
                    org.apache.commons.io.FileUtils.copyURLToFile(url, tmpFile);
                    tmpFile.setExecutable(true);
                }
                return tmpFile.getAbsolutePath();
            } catch (Exception e) {
                log.error("Error downloading file", e);
                throw new Error("Cannot download file. Error " + e.getMessage());
            }
        } else {
            return linkToFile;
        }
    }
    
    /**
     * Get the library extension for the current OS
     * @return The library extension
     */
    public static String getLibraryExtension() {
        NetworkUtils.OS os = NetworkUtils.getOS();
        if (os == NetworkUtils.OS.WINDOWS || os == NetworkUtils.OS.WINDOWS_ARM) {
            return "dll";
        } else if (os == NetworkUtils.OS.MAC || os == NetworkUtils.OS.MAC_ARM64) {
            return "dylib";
        } else {
            return "so";
        }
    }
    
    /**
     * Get the artifact extension for the current OS
     * @param artifactName The artifact name
     * @return The artifact extension
     */
    public static String getArtifactExtension(String artifactName) {
        NetworkUtils.OS os = NetworkUtils.getOS();
        if (artifactName.contains("emulator") || artifactName.contains("tonlib")) {
            if (os == NetworkUtils.OS.WINDOWS || os == NetworkUtils.OS.WINDOWS_ARM) {
                return ".dll";
            } else if (os == NetworkUtils.OS.MAC || os == NetworkUtils.OS.MAC_ARM64) {
                return ".dylib";
            } else {
                return ".so";
            }
        } else {
            if (os == NetworkUtils.OS.WINDOWS || os == NetworkUtils.OS.WINDOWS_ARM) {
                return ".exe";
            } else {
                return "";
            }
        }
    }
    
    /**
     * Detect the absolute path of an executable
     * @param appName The application name
     * @param library Whether it's a library
     * @return The absolute path
     */
    public static String detectAbsolutePath(String appName, boolean library) {
        try {
            if (library) {
                appName = appName + "." + getLibraryExtension();
            }
            ProcessBuilder pb;
            NetworkUtils.OS os = NetworkUtils.getOS();
            if (os == NetworkUtils.OS.WINDOWS || os == NetworkUtils.OS.WINDOWS_ARM) {
                pb = new ProcessBuilder("where", appName).redirectErrorStream(true);
            } else {
                pb = new ProcessBuilder("which", appName).redirectErrorStream(true);
            }
            Process p = pb.start();
            p.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
            String output =
                    new BufferedReader(new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))
                            .lines()
                            .collect(Collectors.joining("\n"));
            String[] paths = output.split("\n");
            if (paths.length == 1) {
                return paths[0];
            } else {
                for (String path : paths) {
                    if (path.contains("ton")) {
                        return org.apache.commons.lang3.StringUtils.trim(path);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new Error(
                    "Cannot detect absolute path to executable " + appName + ", " + e.getMessage());
        }
    }
    
    /**
     * Get the URL for the lite client
     * @return The URL
     */
    public static String getLiteClientGithubUrl() {
        return getArtifactGithubUrl("lite-client", "");
    }
    
    /**
     * Get the URL for the emulator
     * @return The URL
     */
    public static String getEmulatorGithubUrl() {
        return getArtifactGithubUrl("libemulator", "");
    }
    
    /**
     * Get the URL for tonlib
     * @return The URL
     */
    public static String getTonlibGithubUrl() {
        return getArtifactGithubUrl("tonlibjson", "");
    }
    
    /**
     * Get the URL for func
     * @return The URL
     */
    public static String getFuncGithubUrl() {
        return getArtifactGithubUrl("func", "");
    }
    
    /**
     * Get the URL for tolk
     * @return The URL
     */
    public static String getTolkGithubUrl() {
        return getArtifactGithubUrl("tolk", "");
    }
    
    /**
     * Get the URL for fift
     * @return The URL
     */
    public static String getFiftGithubUrl() {
        return getArtifactGithubUrl("fift", "");
    }
    
    /**
     * Get the URL for an artifact
     * @param artifactName The artifact name
     * @param release The release
     * @return The URL
     */
    public static String getArtifactGithubUrl(String artifactName, String release) {
        return getArtifactGithubUrl(artifactName, release, "ton-blockchain", "ton");
    }
    
    /**
     * Get the URL for an artifact
     * @param artifactName The artifact name
     * @param release The release
     * @param githubUsername The GitHub username
     * @param githubRepository The GitHub repository
     * @return The URL
     */
    public static String getArtifactGithubUrl(
            String artifactName, String release, String githubUsername, String githubRepository) {
        String baseUrl;
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(release) && !release.contains("latest")) {
            baseUrl =
                    "https://github.com/"
                            + githubUsername
                            + "/"
                            + githubRepository
                            + "/releases/download/"
                            + release
                            + "/";
        } else {
            baseUrl =
                    "https://github.com/"
                            + githubUsername
                            + "/"
                            + githubRepository
                            + "/releases/latest/download/";
        }
        
        NetworkUtils.OS os = NetworkUtils.getOS();
        if (os == NetworkUtils.OS.WINDOWS || os == NetworkUtils.OS.WINDOWS_ARM) {
            return baseUrl + artifactName + getArtifactExtension(artifactName);
        } else if (os == NetworkUtils.OS.MAC) {
            return baseUrl + artifactName + "-mac-x86-64" + getArtifactExtension(artifactName);
        } else if (os == NetworkUtils.OS.MAC_ARM64) {
            return baseUrl + artifactName + "-mac-arm64" + getArtifactExtension(artifactName);
        } else if (os == NetworkUtils.OS.LINUX) {
            return baseUrl + artifactName + "-linux-x86_64" + getArtifactExtension(artifactName);
        } else if (os == NetworkUtils.OS.LINUX_ARM) {
            return baseUrl + artifactName + "-linux-arm64" + getArtifactExtension(artifactName);
        } else {
            throw new Error("unknown requested OS");
        }
    }
    
    /**
     * Get the URL for the mainnet global config
     * @return The URL
     */
    public static String getGlobalConfigUrlMainnet() {
        return "https://ton.org/global-config.json";
    }
    
    /**
     * Get the URL for the testnet global config
     * @return The URL
     */
    public static String getGlobalConfigUrlTestnet() {
        return "https://ton.org/testnet-global.config.json";
    }
    
    /**
     * Get the URL for the mainnet global config from GitHub
     * @return The URL
     */
    public static String getGlobalConfigUrlMainnetGithub() {
        return "https://raw.githubusercontent.com/ton-blockchain/ton-blockchain.github.io/main/global.config.json";
    }
    
    /**
     * Get the URL for the testnet global config from GitHub
     * @return The URL
     */
    public static String getGlobalConfigUrlTestnetGithub() {
        return "https://raw.githubusercontent.com/ton-blockchain/ton-blockchain.github.io/main/testnet-global.config.json";
    }
}
