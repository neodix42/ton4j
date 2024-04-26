package org.ton.java.tonlib;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.tonlib.types.VerbosityLevel;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)

public class TestTonlibBuilder {
    @Test
    public void testTonlibBuilderWindows() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToTonlibSharedLib("G:\\DOCKER\\mnt\\tonlibjson.dll")
                    .pathToGlobalConfig("G:\\DOCKER\\mnt\\testnet-global.config.json")
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderWindowsIntegratedLib() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToGlobalConfig("G:\\DOCKER\\mnt\\testnet-global.config.json")
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderWindowsIntegratedConfig() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToTonlibSharedLib("G:\\DOCKER\\mnt\\tonlibjson.dll")
                    .keystorePath("G:\\DOCKER\\mnt\\")
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderWindowsIgnoreCache() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .ignoreCache(false)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();

            tonlib = Tonlib.builder()
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .ignoreCache(true)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderWindowsConfigAsString() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .globalConfigAsString(globalConfigAsString)
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderWindowsIntegratedLibAndConfig() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder().build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderUbuntu() {
        if (SystemUtils.IS_OS_LINUX) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToTonlibSharedLib("/mnt/tonlibjson.so")
                    .pathToGlobalConfig("/mnt/testnet-global.config.json")
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderUbuntuIntegratedLib() {
        if (SystemUtils.IS_OS_LINUX) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToGlobalConfig("/mnt/testnet-global.config.json")
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderUbuntuIntegratedConfig() {
        if (SystemUtils.IS_OS_LINUX) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToTonlibSharedLib("/mnt/tonlibjson.so")
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderUbuntuIntegratedLibAndConfig() {
        if (SystemUtils.IS_OS_LINUX) {
            Tonlib tonlib = Tonlib.builder().build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    String globalConfigAsString = "";
}
