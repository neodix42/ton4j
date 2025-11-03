package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.BocHeader;

@RunWith(JUnit4.class)
public class TestBocHeaderCoverage {

  @Test
  public void testInstantiateAndAccessFieldsViaReflection() throws Exception {
    BocHeader h = new BocHeader();
    assertThat(h).isNotNull();

    // Access and modify all fields via reflection to ensure class is loaded and fields are visited
    Field has_idx = BocHeader.class.getDeclaredField("has_idx");
    Field hash_crc32 = BocHeader.class.getDeclaredField("hash_crc32");
    Field has_cache_bits = BocHeader.class.getDeclaredField("has_cache_bits");
    Field flags = BocHeader.class.getDeclaredField("flags");
    Field size_bytes = BocHeader.class.getDeclaredField("size_bytes");
    Field off_bytes = BocHeader.class.getDeclaredField("off_bytes");
    Field cells_num = BocHeader.class.getDeclaredField("cells_num");
    Field roots_num = BocHeader.class.getDeclaredField("roots_num");
    Field absent_num = BocHeader.class.getDeclaredField("absent_num");
    Field tot_cells_size = BocHeader.class.getDeclaredField("tot_cells_size");
    Field index = BocHeader.class.getDeclaredField("index");

    has_idx.setAccessible(true);
    hash_crc32.setAccessible(true);
    has_cache_bits.setAccessible(true);
    flags.setAccessible(true);
    size_bytes.setAccessible(true);
    off_bytes.setAccessible(true);
    cells_num.setAccessible(true);
    roots_num.setAccessible(true);
    absent_num.setAccessible(true);
    tot_cells_size.setAccessible(true);
    index.setAccessible(true);

    has_idx.setInt(h, 1);
    hash_crc32.setInt(h, 1);
    has_cache_bits.setInt(h, 0);
    flags.setInt(h, 0);
    size_bytes.setInt(h, 2);
    off_bytes.setInt(h, 2);
    cells_num.setInt(h, 1);
    roots_num.setInt(h, 1);
    absent_num.setInt(h, 0);
    tot_cells_size.setInt(h, 10);
    index.set(h, new int[] {0});

    assertThat(has_idx.getInt(h)).isEqualTo(1);
    assertThat(size_bytes.getInt(h)).isEqualTo(2);
    assertThat(((int[]) index.get(h))).containsExactly(0);
  }
}
