package org.ton.java.address;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.utils.Utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

@RunWith(JUnit4.class)
public class TestAddress {

    public static final String TEST_ADDRESS_0 = "0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO";
    public static final String TEST_ADDRESS_1 = "kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL";
    public static final String TEST_ADDRESS_3 = "0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3";
    public static final String TEST_ADDRESS_4 = "kf_8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15-KsQHFLbKSMiYIny";


    @Test
    public void testAddress() {

        Address address01 = new Address(TEST_ADDRESS_0);

        assertThat(address01.toString()).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");

        Address address02 = new Address(TEST_ADDRESS_1);
        assertThat(address02.toString()).isEqualTo(TEST_ADDRESS_1);
        assertThat(address02.isBounceable).isTrue();

        Address address03 = new Address(TEST_ADDRESS_3);
        assertThat(address03.toString()).isEqualTo(TEST_ADDRESS_3);

        Address address04 = new Address("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        assertThat(address04.toString(true, true, false)).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        assertThat(address04.isBounceable).isFalse();

        Address address05 = new Address("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        assertThat(address05.toString(true, true, true)).isEqualTo(TEST_ADDRESS_1);
        assertThat(address05.isBounceable).isFalse();

        Address address06 = new Address("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        assertThat(address06.toString(false)).isEqualTo(TEST_ADDRESS_3);
        assertThat(address06.isBounceable).isFalse();

        Address address07 = new Address(TEST_ADDRESS_1);
        assertThat(address07.toString(true, true, false)).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        assertThat(address07.isBounceable).isTrue();

        Address address08 = new Address(TEST_ADDRESS_1);
        assertThat(address08.toString(true, true, true)).isEqualTo(TEST_ADDRESS_1);
        assertThat(address08.isBounceable).isTrue();

        Address address09 = new Address(TEST_ADDRESS_1);
        assertThat(address09.toString(false)).isEqualTo(TEST_ADDRESS_3);
        assertThat(address09.isBounceable).isTrue();

        Address address10 = new Address(TEST_ADDRESS_3);
        assertThat(address10.toString(true, true, false, true)).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");

        Address address11 = new Address(TEST_ADDRESS_3);
        assertThat(address11.toString(true, true, true, true)).isEqualTo(TEST_ADDRESS_1);

        Address address12 = new Address(TEST_ADDRESS_3);
        assertThat(address12.toString(false)).isEqualTo(TEST_ADDRESS_3);

        Address address13 = new Address("-1:3333333333333333333333333333333333333333333333333333333333333333");
        assertThat(address13.toString(true, false, true, false)).isEqualTo("Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF");
        assertThat(address13.toString(true, false, false, false)).isEqualTo("Uf8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMxYA");
        assertThat(Utils.bytesToHex(address13.hashPart)).isEqualTo("3333333333333333333333333333333333333333333333333333333333333333");
        assertThat(address13.hashPart.length).isEqualTo(32);
        assertThat(address13.wc).isEqualTo((byte) -1);

        Address address14 = new Address(TEST_ADDRESS_4);
        assertThat(address14.isTestOnly).isTrue();

        assertThat(Address.isValid(TEST_ADDRESS_0)).isTrue();
        assertThat(Address.isValid(TEST_ADDRESS_1)).isTrue();
        assertThat(Address.isValid(TEST_ADDRESS_3)).isTrue();
        assertThat(Address.isValid(TEST_ADDRESS_4)).isTrue();
        assertThat(Address.isValid("bad_address")).isFalse();
    }

    @Test
    public void testBadAddress() {
        assertThrows(Error.class, () -> {
            new Address("");
            new Address("bad_input");
            new Address("kf_8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15-KsQHFLbKSMiYInz");
            new Address("ov_8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15-KsQHFLbKSMiYMg3");
        });
    }

    @Test
    public void testCompareAddress() {
        Address a = new Address(TEST_ADDRESS_0);
        Address b = new Address(TEST_ADDRESS_4);
        assertThat(a).isNotEqualTo(b);

        b = new Address(TEST_ADDRESS_1);
        assertThat(a).isNotEqualTo(b);

    }
}
