package org.ton.ton4j.address.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.address.AddressType;
import org.ton.ton4j.utils.Utils;

@RunWith(JUnit4.class)
public class TestAddressCoverage {

  private static byte[] bytes(int len, int value) {
    byte[] b = new byte[len];
    Arrays.fill(b, (byte) value);
    return b;
  }

  private static String makeBase64Address(int tag, byte wc, byte[] hash) {
    // addr = [tag (1), wc (1), hash (32)], crc16 over addr, total 36 bytes
    byte[] addr = new byte[34];
    addr[0] = (byte) tag;
    addr[1] = wc;
    System.arraycopy(hash, 0, addr, 2, 32);
    byte[] crc = Utils.getCRC16ChecksumAsBytes(addr);
    byte[] full = new byte[36];
    System.arraycopy(addr, 0, full, 0, 34);
    System.arraycopy(crc, 0, full, 34, 2);
    return Utils.bytesToBase64(full);
  }

  @Test
  public void testFactoryOfVariantsAndFlags() {
    byte[] hash = bytes(32, 0x11);

    Address a1 = Address.of(Address.BOUNCEABLE_TAG, -1, hash);
    assertThat(a1.isBounceable()).isTrue();
    assertThat(a1.isWallet()).isFalse();
    assertThat(a1.isTestOnly()).isFalse();
    assertThat(a1.wc).isEqualTo((byte) -1);

    Address a2 = Address.of(Address.NON_BOUNCEABLE_TAG, 0, hash);
    assertThat(a2.isBounceable()).isFalse();
    assertThat(a2.isWallet()).isTrue();
    assertThat(a2.wc).isEqualTo((byte) 0);

    // TEST_FLAG path, wc 0xFF -> -1
    byte testBounceTag = (byte) (Address.BOUNCEABLE_TAG | Address.TEST_FLAG);
    Address a3 = Address.of(testBounceTag, 0xFF, hash);
    assertThat(a3.isTestOnly()).isTrue();
    assertThat(a3.isBounceable()).isTrue();
    assertThat(a3.wc).isEqualTo((byte) -1);

    // of(byte[]) delegates to bounceable, wc default -1
    Address a4 = Address.of(hash);
    assertThat(a4.isBounceable()).isTrue();
    assertThat(a4.wc).isEqualTo((byte) -1);

    // of(Address) copy
    Address a5 = Address.of(a3);
    assertThat(a5.wc).isEqualTo(a3.wc);
    assertThat(a5.isBounceable()).isEqualTo(a3.isBounceable());
  }

  @Test
  public void testConstructorsAndNulls() {
    // String null guards
    assertThrows(IllegalArgumentException.class, () -> new Address((String) null));
    // Copy null guard
    assertThrows(IllegalArgumentException.class, () -> new Address((Address) null));
  }

  @Test
  public void testRawParsingAndEdgeHexLengths() {
    byte[] hash = Utils.hexToSignedBytes("00" + "11".repeat(31)); // 64 hex chars starting with 00
    Address ok0 = new Address("0:" + Utils.bytesToHex(hash));
    assertThat(ok0.isWallet()).isTrue(); // raw marks wallet
    assertThat(ok0.isUserFriendly()).isFalse();

    // length 63 -> prefixed with 0
    String hex63 = ("1" + "2".repeat(62)); // 63 chars
    Address ok63 = new Address("-1:" + hex63);
    assertThat(ok63.hashPart[0]).isEqualTo((byte) 0x01);

    // length 1 -> left pad to 64
    Address ok1 = new Address("0:1");
    assertThat(ok1.hashPart).hasSize(32);

    // invalid: multiple colons
    assertThrows(Error.class, () -> new Address("0:00:11"));
    // invalid wc
    assertThrows(Error.class, () -> new Address("1:"
        + "0".repeat(64)));
    // invalid hex length (not 64, not 63, not 1)
    assertThrows(Error.class, () -> new Address("0:" + "1".repeat(2)));
  }

  @Test
  public void testUserFriendlyParsingValidAndFlags() {
    byte[] hash = bytes(32, 0x22);

    // Valid bounceable mainnet, wc 0
    String b64 = makeBase64Address(Address.BOUNCEABLE_TAG, (byte) 0, hash);
    Address a = new Address(b64);
    assertThat(a.isUserFriendly()).isTrue();
    assertThat(a.isUrlSafe()).isFalse();
    assertThat(a.isBounceable()).isTrue();
    assertThat(a.isWallet()).isFalse();
    // addressType is set within parseFriendlyAddress result
    assertThat(Address.parseFriendlyAddress(b64).addressType).isEqualTo(AddressType.STD_ADDRESS);

    // Valid non-bounceable testnet, wc -1, url-safe input
    // Ensure produced base64 contains '+' or '/' so url-safe branch is triggered
    String b64tn;
    {
      byte[] h = Arrays.copyOf(hash, hash.length);
      int attempts = 0;
      while (true) {
        b64tn = makeBase64Address(Address.NON_BOUNCEABLE_TAG | Address.TEST_FLAG, (byte) -1, h);
        if (b64tn.indexOf('+') >= 0 || b64tn.indexOf('/') >= 0) {
          break;
        }
        // change hash deterministically
        h[attempts % h.length] ^= (byte) (1 + (attempts & 7));
        attempts++;
        if (attempts > 1024) {
          // fallback to avoid infinite loop; test can skip urlSafe assert in worst case
          break;
        }
      }
    }
    String urlSafe = b64tn.replace('+', '-').replace('/', '_');
    Address b = new Address(urlSafe);
    assertThat(b.isUserFriendly()).isTrue();
    // If original contained '+' or '/', then url-safe flag must be true
    assertThat(urlSafe.indexOf('-') >= 0 || urlSafe.indexOf('_') >= 0).isTrue();
    assertThat(b.isUrlSafe()).isTrue();
    assertThat(b.isBounceable()).isFalse();
    assertThat(b.isWallet()).isTrue();
    assertThat(b.isTestOnly()).isTrue();
  }

  @Test
  public void testParseFriendlyAddressErrors() {
    byte[] hash = bytes(32, 0x33);

    // wrong length 47 or 49
    assertThrows(Error.class, () -> Address.parseFriendlyAddress("A".repeat(47)));

    // wrong decoded bytes length: construct a 48-char base64 that decodes to 35 bytes
    String invalidLen = Utils.bytesToBase64(bytes(35, 1)); // 48 chars, but 35 bytes
    assertThrows(Error.class, () -> Address.parseFriendlyAddress(invalidLen));

    // wrong crc
    String good = makeBase64Address(Address.BOUNCEABLE_TAG, (byte) 0, hash);
    byte[] corrupted = Utils.base64ToBytes(good);
    corrupted[35] ^= 0x01; // flip a crc bit
    String wrongCrc = Utils.bytesToBase64(corrupted);
    assertThrows(Error.class, () -> Address.parseFriendlyAddress(wrongCrc));

    // unknown tag (after removing TEST_FLAG)
    String badTag = makeBase64Address(0x00, (byte) 0, hash);
    assertThrows(Error.class, () -> Address.parseFriendlyAddress(badTag));

    // invalid wc (not 0 or -1)
    String badWc = makeBase64Address(Address.BOUNCEABLE_TAG, (byte) 1, hash);
    assertThrows(Error.class, () -> Address.parseFriendlyAddress(badWc));
  }

  @Test
  public void testToStringVariantsAndRoundTrip() {
    byte[] hash = bytes(32, 0x44);
    Address a = Address.of(Address.BOUNCEABLE_TAG, -1, hash);

    String bounce = a.toBounceable();
    assertThat(bounce).hasSize(48);
    assertThat(Address.of(bounce)).isEqualTo(a);

    String bounceT = a.toBounceableTestnet();
    assertThat(bounceT).hasSize(48);

    String nonB = a.toNonBounceable();
    assertThat(nonB).hasSize(48);

    String nonBT = a.toNonBounceableTestnet();
    assertThat(nonBT).hasSize(48);

    String raw = a.toRaw();
    assertThat(raw).contains(":");

    // default toString delegates to bounceable
    assertThat(a.toString()).isEqualTo(bounce);

    // explicit overloads
    assertThat(a.toString(false)) // raw
        .isEqualTo(raw);
    assertThat(a.toString(true)) // user-friendly
        .hasSize(48);
    assertThat(a.toString(true, false)) // non-URL-safe base64 (same length)
        .hasSize(48);
    assertThat(a.toString(true, true, true)) // same as bounceable
        .isEqualTo(bounce);

    // Ensure raw constructed from raw string round-trips
    Address fromRaw = Address.of(raw);
    assertThat(fromRaw.toRaw()).isEqualTo(raw);
  }

  @Test
  public void testNumericConversionsAndShard() {
    byte[] hash = bytes(32, 0x00);
    hash[0] = (byte) 0xF0; // to control shard high nibble (0xF)
    Address a = Address.of(Address.BOUNCEABLE_TAG, 0, hash);

    assertThat(a.toHex()).isEqualTo(Utils.bytesToHex(hash));
    assertThat(a.toBigInteger()).isEqualTo(new BigInteger(Utils.bytesToHex(hash), 16));
    assertThat(a.toDecimal()).isEqualTo(a.toBigInteger().toString(10));

    long shardL = a.getShardAsLong();
    BigInteger shardB = a.getShardAsBigInt();
    assertThat(shardB.longValue()).isEqualTo(shardL);

    // getHash returns unsigned values
    int[] unsigned = a.getHash();
    assertThat(unsigned[0]).isEqualTo(0xF0);
  }

  @Test
  public void testEqualsAndHashCode() {
    byte[] h1 = bytes(32, 0x55);
    byte[] h2 = bytes(32, 0x56);

    Address a1 = Address.of(Address.BOUNCEABLE_TAG, -1, h1);
    Address a2 = Address.of(Address.BOUNCEABLE_TAG, -1, h1);
    Address a3 = Address.of(Address.BOUNCEABLE_TAG, 0, h1);
    Address a4 = Address.of(Address.BOUNCEABLE_TAG, -1, h2);

    assertThat(a1).isEqualTo(a2);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    assertThat(a1).isNotEqualTo(a3);
    assertThat(a1).isNotEqualTo(a4);
    assertThat(a1).isNotEqualTo(null);
    assertThat(a1).isNotEqualTo("str");

    // self-equality to cover (this == obj) branch
    assertThat(a1.equals(a1)).isTrue();
  }

  @Test
  public void testSaveToFileAndIsValid() throws IOException {
    byte[] hash = bytes(32, 0x66);
    Address a = Address.of(Address.NON_BOUNCEABLE_TAG, -1, hash);

    File tmp = File.createTempFile("addr", ".bin");
    tmp.deleteOnExit();
    a.saveToFile(tmp.getAbsolutePath());

    byte[] stored = Files.readAllBytes(tmp.toPath());
    // Expect 32 bytes hash + 4 bytes of wc as int (big-endian)
    assertThat(stored.length).isEqualTo(36);
    assertThat(Arrays.copyOf(stored, 32)).containsExactly(hash);
    // last 4 bytes should represent -1 (0xFF FF FF FF)
    assertThat(stored[32]).isEqualTo((byte) 0xFF);
    assertThat(stored[33]).isEqualTo((byte) 0xFF);
    assertThat(stored[34]).isEqualTo((byte) 0xFF);
    assertThat(stored[35]).isEqualTo((byte) 0xFF);

    // isValid true/false
    String good = a.toString();
    assertThat(Address.isValid(good)).isTrue();
    assertThat(Address.isValid("bad")) .isFalse();
  }

  @Test
  public void testOfFactoryUnknownTagError() {
    byte[] hash = bytes(32, 0x77);
    // Flags that are not bounceable or non-bounceable after TEST_FLAG removal
    byte badFlags = 0x00; // clearly invalid tag
    assertThrows(Error.class, () -> Address.of(badFlags, 0, hash));

    // Also try with TEST_FLAG set; after removal remains invalid
    byte badWithTest = (byte) (0x03 | Address.TEST_FLAG);
    assertThrows(Error.class, () -> Address.of(badWithTest, -1, hash));
  }
}
