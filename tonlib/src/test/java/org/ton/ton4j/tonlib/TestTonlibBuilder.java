package org.ton.ton4j.tonlib;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.tonlib.types.VerbosityLevel;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)

public class TestTonlibBuilder {
    @Test
    public void testTonlibBuilderWindows() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToTonlibSharedLib("G:/tonlibjson.dll")
                    .pathToGlobalConfig("G:/testnet-global.config.json")
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderWindowsIntegratedLib() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToGlobalConfig("G:/testnet-global.config.json")
                    .verbosityLevel(VerbosityLevel.FATAL)
                    .build();
            assertThat(tonlib.getLast()).isNotNull();
        }
    }

    @Test
    public void testTonlibBuilderWindowsIntegratedConfig() {
        if (SystemUtils.IS_OS_WINDOWS) {
            Tonlib tonlib = Tonlib.builder()
                    .pathToTonlibSharedLib("G:/tonlibjson.dll")
                    .keystorePath("G:/")
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
                    .pathToTonlibSharedLib("/mnt/tonlibjson-linux-x86_64.so")
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
                    .pathToTonlibSharedLib("/mnt/tonlibjson-linux-x86_64.so")
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

    String globalConfigAsString = "{\n" +
            "  \"@type\": \"config.global\",\n" +
            "  \"dht\": {\n" +
            "    \"@type\": \"dht.config.global\",\n" +
            "    \"k\": 6,\n" +
            "    \"a\": 3,\n" +
            "    \"static_nodes\": {\n" +
            "      \"@type\": \"dht.nodes\",\n" +
            "      \"nodes\": [\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"6PGkPQSbyFp12esf1NqmDOaLoFA8i9+Mp5+cAx5wtTU=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -1185526007,\n" +
            "                \"port\": 22096\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"L4N1+dzXLlkmT5iPnvsmsixzXU0L6kPKApqMdcrGP5d9ssMhn69SzHFK+yIzvG6zQ9oRb4TnqPBaKShjjj2OBg==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"4R0C/zU56k+x2HGMsLWjX2rP/SpoTPIHSSAmidGlsb8=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -1952265919,\n" +
            "                \"port\": 14395\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"0uwWyCFn2KjPnnlbSFYXLZdwIakaSgI9WyRo87J3iCGwb5TvJSztgA224A9kNAXeutOrXMIPYv1b8Zt8ImsrCg==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"/YDNd+IwRUgL0mq21oC0L3RxrS8gTu0nciSPUrhqR78=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -1402455171,\n" +
            "                \"port\": 14432\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"6+oVk6HDtIFbwYi9khCc8B+fTFceBUo1PWZDVTkb4l84tscvr5QpzAkdK7sS5xGzxM7V7YYQ6gUQPrsP9xcLAw==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"DA0H568bb+LoO2LGY80PgPee59jTPCqqSJJzt1SH+KE=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -1402397332,\n" +
            "                \"port\": 14583\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"cL79gDTrixhaM9AlkCdZWccCts7ieQYQBmPxb/R7d7zHw3bEHL8Le96CFJoB1KHu8C85iDpFK8qlrGl1Yt/ZDg==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"MJr8xja0xpu9DoisFXBrkNHNx1XozR7HHw9fJdSyEdo=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -2018147130,\n" +
            "                \"port\": 6302\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"XcR5JaWcf4QMdI8urLSc1zwv5+9nCuItSE1EDa0dSwYF15R/BtJoKU5YHA4/T8SiO18aVPQk2SL1pbhevuMrAQ==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"Fhldu4zlnb20/TUj9TXElZkiEmbndIiE/DXrbGKu+0c=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -2018147075,\n" +
            "                \"port\": 6302\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"nUGB77UAkd2+ZAL5PgInb3TvtuLLXJEJ2icjAUKLv4qIGB3c/O9k/v0NKwSzhsMP0ljeTGbcIoMDw24qf3goCg==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"gzUNJnBJhdpooYCE8juKZo2y4tYDIQfoCvFm0yBr7y0=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": 89013260,\n" +
            "                \"port\": 54390\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"LCrCkjmkMn6AZHW2I+oRm1gHK7CyBPfcb6LwsltskCPpNECyBl1GxZTX45n0xZtLgyBd/bOqMPBfawpQwWt1BA==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"jXiLaOQz1HPayilWgBWhV9xJhUIqfU95t+KFKQPIpXg=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": 94452896,\n" +
            "                \"port\": 12485\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"fKSZh9nXMx+YblkQXn3I/bndTD0JZ1yAtK/tXPIGruNglpe9sWMXR+8fy3YogPhLJMdjNiMom1ya+tWG7qvBAQ==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"vhFPq+tgjJi+4ZbEOHBo4qjpqhBdSCzNZBdgXyj3NK8=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": 85383775,\n" +
            "                \"port\": 36752\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"kBwAIgJVkz8AIOGoZcZcXWgNmWq8MSBWB2VhS8Pd+f9LLPIeeFxlDTtwAe8Kj7NkHDSDC+bPXLGQZvPv0+wHCg==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"sbsuMcdyYFSRQ0sG86/n+ZQ5FX3zOWm1aCVuHwXdgs0=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": 759132846,\n" +
            "                \"port\": 50187\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"9FJwbFw3IECRFkb9bA54YaexjDmlNBArimWkh+BvW88mjm3K2i5V2uaBPS3GubvXWOwdHLE2lzQBobgZRGMyCg==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"aeMgdMdkkbkfAS4+n4BEGgtqhkf2/zXrVWWECOJ/h3A=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -1481887565,\n" +
            "                \"port\": 25975\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"z5ogivZWpQchkS4UR4wB7i2pfOpMwX9Nd/USxinL9LvJPa+/Aw3F1AytR9FX0BqDftxIYvblBYAB5JyAmlj+AA==\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"@type\": \"dht.node\",\n" +
            "          \"id\": {\n" +
            "            \"@type\": \"pub.ed25519\",\n" +
            "            \"key\": \"rNzhnAlmtRn9rTzW6o2568S6bbOXly7ddO1olDws5wM=\"\n" +
            "          },\n" +
            "          \"addr_list\": {\n" +
            "            \"@type\": \"adnl.addressList\",\n" +
            "            \"addrs\": [\n" +
            "              {\n" +
            "                \"@type\": \"adnl.address.udp\",\n" +
            "                \"ip\": -2134428422,\n" +
            "                \"port\": 45943\n" +
            "              }\n" +
            "            ],\n" +
            "            \"version\": 0,\n" +
            "            \"reinit_date\": 0,\n" +
            "            \"priority\": 0,\n" +
            "            \"expire_at\": 0\n" +
            "          },\n" +
            "          \"version\": -1,\n" +
            "          \"signature\": \"sn/+ZfkfCSw2bHnEnv04AXX/Goyw7+StHBPQOdPr+wvdbaJ761D7hyiMNdQGbuZv2Ep2cXJpiwylnZItrwdUDg==\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  },\n" +
            "  \"liteservers\": [\n" +
            "    {\n" +
            "      \"ip\": 84478511,\n" +
            "      \"port\": 19949,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"n4VDnSCUuSpjnCyUk9e3QOOd6o0ItSWYbTnW3Wnn8wk=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 84478479,\n" +
            "      \"port\": 48014,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"3XO67K/qi+gu3T9v8G2hx1yNmWZhccL3O7SoosFo8G0=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -2018135749,\n" +
            "      \"port\": 53312,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"aF91CuUHuuOv9rm2W5+O/4h38M3sRm40DtSdRxQhmtQ=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -2018145068,\n" +
            "      \"port\": 13206,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"K0t3+IWLOXHYMvMcrGZDPs+pn58a17LFbnXoQkKc2xw=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -2018145059,\n" +
            "      \"port\": 46995,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"wQE0MVhXNWUXpWiW5Bk8cAirIh5NNG3cZM1/fSVKIts=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 1091931625,\n" +
            "      \"port\": 30131,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"wrQaeIFispPfHndEBc0s0fx7GSp8UFFvebnytQQfc6A=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 1091931590,\n" +
            "      \"port\": 47160,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"vOe1Xqt/1AQ2Z56Pr+1Rnw+f0NmAA7rNCZFIHeChB7o=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 1091931623,\n" +
            "      \"port\": 17728,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"BYSVpL7aPk0kU5CtlsIae/8mf2B/NrBi7DKmepcjX6Q=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 1091931589,\n" +
            "      \"port\": 13570,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"iVQH71cymoNgnrhOT35tl/Y7k86X5iVuu5Vf68KmifQ=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -1539021362,\n" +
            "      \"port\": 52995,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"QnGFe9kihW+TKacEvvxFWqVXeRxCB6ChjjhNTrL7+/k=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -1539021936,\n" +
            "      \"port\": 20334,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"gyLh12v4hBRtyBygvvbbO2HqEtgl+ojpeRJKt4gkMq0=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -1136338705,\n" +
            "      \"port\": 19925,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"ucho5bEkufbKN1JR1BGHpkObq602whJn3Q3UwhtgSo4=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 868465979,\n" +
            "      \"port\": 19434,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"J5CwYXuCZWVPgiFPW+NY2roBwDWpRRtANHSTYTRSVtI=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 868466060,\n" +
            "      \"port\": 23067,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"vX8d0i31zB0prVuZK8fBkt37WnEpuEHrb7PElk4FJ1o=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -2018147130,\n" +
            "      \"port\": 53560,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"NlYhh/xf4uQpE+7EzgorPHqIaqildznrpajJTRRH2HU=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -2018147075,\n" +
            "      \"port\": 46529,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"jLO6yoooqUQqg4/1QXflpv2qGCoXmzZCR+bOsYJ2hxw=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": 908566172,\n" +
            "      \"port\": 51565,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"TDg+ILLlRugRB4Kpg3wXjPcoc+d+Eeb7kuVe16CS9z8=\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"ip\": -1185526007,\n" +
            "      \"port\": 4701,\n" +
            "      \"id\": {\n" +
            "        \"@type\": \"pub.ed25519\",\n" +
            "        \"key\": \"G6cNAr6wXBBByWDzddEWP5xMFsAcp6y13fXA8Q7EJlM=\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"validator\": {\n" +
            "    \"@type\": \"validator.config.global\",\n" +
            "    \"zero_state\": {\n" +
            "      \"workchain\": -1,\n" +
            "      \"shard\": -9223372036854775808,\n" +
            "      \"seqno\": 0,\n" +
            "      \"root_hash\": \"F6OpKZKqvqeFp6CQmFomXNMfMj2EnaUSOXN+Mh+wVWk=\",\n" +
            "      \"file_hash\": \"XplPz01CXAps5qeSWUtxcyBfdAo5zVb1N979KLSKD24=\"\n" +
            "    },\n" +
            "    \"init_block\": {\n" +
            "      \"root_hash\": \"VpWyfNOLm8Rqt6CZZ9dZGqJRO3NyrlHHYN1k1oLbJ6g=\",\n" +
            "      \"seqno\": 34835953,\n" +
            "      \"file_hash\": \"8o12KX54BtJM8RERD1J97Qe1ZWk61LIIyXydlBnixK8=\",\n" +
            "      \"workchain\": -1,\n" +
            "      \"shard\": -9223372036854775808\n" +
            "    },\n" +
            "    \"hardforks\": [\n" +
            "      {\n" +
            "        \"file_hash\": \"t/9VBPODF7Zdh4nsnA49dprO69nQNMqYL+zk5bCjV/8=\",\n" +
            "        \"seqno\": 8536841,\n" +
            "        \"root_hash\": \"08Kpc9XxrMKC6BF/FeNHPS3MEL1/Vi/fQU/C9ELUrkc=\",\n" +
            "        \"workchain\": -1,\n" +
            "        \"shard\": -9223372036854775808\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
}
