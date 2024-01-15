package org.ton.java.hashmaps;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestHashMap {

    @Test
    public void testHashMapDeserialization() {
        // cell containing hashmap
        String t = "B5EE9C7241010A010032000203CE6001020201200303020120030402012005050201CE080802012006060201200707020120080802012009090003006001FFF7D9";

        Cell cell = Cell.fromBoc(t);
        log.info("cell {}", cell.print());
        log.info("cell {}", Utils.bytesToHex(cell.toBocNew()));

        // deserialize hashmap from cell, by providing key-deserializator and value-deserializator
        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap x = cs.loadDict(64,
                k -> k.readUint(64),
                v -> CellSlice.beginParse(v).loadUint(8)
        );
        log.info("Deserialized hashmap from cell {}", x);

        int i = 0;
        // traverse deserialized hashmap
        for (Map.Entry<Object, Object> entry : x.elements.entrySet()) {
            assertThat((BigInteger) entry.getKey()).isEqualTo(i++);
            assertThat((BigInteger) entry.getValue()).isEqualTo(BigInteger.ONE);
        }

        // serialization
        Cell cellSerialized = x.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 64).endCell().bits,
                v -> CellBuilder.beginCell().storeUint((BigInteger) v, 8).endCell()
        );

        log.info("Serialized hashmap in cell");
        log.info("cell {}", Utils.bytesToHex(cellSerialized.toBocNew()));
        log.info("cellSerialized {}", cellSerialized.print());
    }

    @Test
    public void testHashMapSerialization() {
        TonHashMap x = new TonHashMap(9);

        x.elements.put(100L, (byte) 1);
        x.elements.put(200L, (byte) 2);
        x.elements.put(300L, (byte) 3);
        x.elements.put(400L, (byte) 4);

        Cell cell = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap dex = cs.loadDict(9,
                k -> k.readUint(9),
                v -> CellSlice.beginParse(v).loadUint(3)
        );

        log.info("Deserialized hashmap from cell {}", dex);

        assertThat(Utils.bytesToHex(cell.toBocNew())).isEqualTo(Utils.bytesToHex(cell.toBocNew()));
    }

    @Test
    @Ignore  // TODO fails HashMapE serialization with one entry
    public void testHashMapDeserializationOneEntry() {
        int keySizeX = 9;
        TonHashMap x = new TonHashMap(keySizeX);

        x.elements.put(100L, (byte) 1);

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, keySizeX).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cellX {}", cellX.print());
    }

    @Test
    public void testEmptyHashMapSerialization() {
        TonHashMap x = new TonHashMap(9);
        assertThrows(Error.class, () -> x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        ));
        log.info("Deserialized hashmap from cell {}", x);
    }

    @Test
    public void testHashMapDeserializationWithBothEdges() {

        String t = "B5EE9C7241010501001D0002012001020201CF03040009BC0068054C0007B91012180007BEFDF218CFA830D9";

        Cell cell = Cell.fromBoc(t);
        log.info("cell {}", cell.print());
        log.info("cell {}", Utils.bytesToHex(cell.toBocNew()));

        //TonHashMap dex = new TonHashMap(16);
        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap dex = cs.loadDict(16,
                k -> k.readUint(16),
                v -> CellSlice.beginParse(v).loadUint(16)
        );

        log.info("Deserialized hashmap from cell {}", dex);

        // serialize
        dex.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 16).bits,
                v -> CellBuilder.beginCell().storeUint((BigInteger) v, 16)
        );
    }

    @Test // works with bits.writeBitString(bitString); does not - bits.writeBitStringFromRead(bitString);
    public void testHashMapDeserializationWithMixedEmptyEdges() {
        String BOC_NETWORK_CONFIG = "te6cckEBEwEAVwACASABAgIC2QMEAgm3///wYBESAgEgBQYCAWIODwIBIAcIAgHODQ0CAdQNDQIBIAkKAgEgCxACASAQDAABWAIBIA0NAAEgAgEgEBAAAdQAAUgAAfwAAdwXk+eF";
        int[] KEYS_NETWORK_CONFIG = new int[]{0, 1, 9, 10, 12, 14, 15, 16, 17, 32, 34, 36, -1001, -1000};
        Cell cell = Cell.fromBoc(Utils.base64ToUnsignedBytes(BOC_NETWORK_CONFIG));
        log.info("cell:\n{}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        log.info("deserializing hashmap...");
        TonHashMap dex = cs.loadDict(32,
                k -> k.readInt(32),
                v -> v);

        log.info("Deserialized hashmap from cell {}", dex);

        int i = 0;
        // traverse deserialized hashmap
        log.info("Deserialized hashmap from cell");
        for (Map.Entry<Object, Object> entry : dex.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), ((Cell) entry.getValue()).bits.toBitString());
//            assertThat((BigInteger) entry.getKey()).isEqualTo(KEYS_NETWORK_CONFIG[i++]);
//            assertThat(((Cell) entry.getValue()).bits.toBitString().length()).isZero();
        }
    }

    /**
     * test data taken from tonuitils
     * dict_test.go:TestLoadCell_LoadDict
     */
    @Test
    public void testHashMap() {
        // go - skip refs
        String t = "b5ee9c724102340100062200235b9023afe2ffffff1100000000000000000000000000019db8c60000000162c4845200001aab34c426c6014d575c2001020300480101e5415dd4e865179eb82b1edff31c8408a095e7474e3a1d3d68a061fe03b8ac62000102138209bd22c691124a3630043301d90000000000000000ffffffffffffffff826f48b1a444928d8bb1146f3ef442e4900001aab34b4e484014d575ccf1f8c5fab66850786114e421ecc97a16833bbe2b5a034e49fabcb583022173aafcda01a0c373515a9ff299743ebb7bd974a8f82ce51986a23d2831fb034189d83303130104de91634889251b180506330048010179219a4240635a4ad6f3a6a65275701912edea7893798f57af1b4f9778ca721b021503130101b271de28950272b8070809031301011898a010da960e780a0b0c004801010ab44385631582c1108ad75ebfc884e257b8cf6cdfbbe9155b6c12807f9149e4002a00480101bb06f3506745c5f6a6239d132a70b38439cb60ff95f62e45261ba12e844e889b0001031301007443d8525c3939180d0e0f00480101ae5aa36e6c6acae1db9b2ab1a33cf9859af8ecea96fd2e78b60eb24e0f4f2e53003100480101ec90a44eee02bed840c10e88351163ee9e3613eb9dbe8da760783da449714e2800010213010070971eff39146f88101100480101ee5c34562b83c7c32cb6033f90ce4637a9f59073428032d2be0cb276414b1d50002700480101aaed7ccc3904836f362ae06eb234b71d64e02eb4ba6d6b7869197a9ed5c4b0b800010213010044a99d11861d4b68121300480101596621878c7465344345dcefa4803ee2fb224fcb1cbd6aa09a10f53ab38914e90026021301002f239c10ff1cc72814150048010170c159783d1ae77f702595ce6d7a25acd37ffaaa8c12293ec9e6e81206cc6c310023004801012b5f1d1614fcb15ebd3d3d489b2895f5cda5fdbc48e658556643fd8c10c9c2c30024021301002b46ef1908757d6816170048010115bf77a14a73e704bc99a04417ee8985285a2939bb45211b707d533f57ebc10b001b021100fca881a1128a4c0818190048010113b9aff02e187ceba81ee40ca27df256723a0d8780cab4d94fcd6777b44468ad001d021100fc1790148d13b5281a1b00480101150c62b460866814e89011659974790cbc4490e707066c4c6f464ac63c2e7f41001c021100fc1655ff5d35bd881c1d021100fc15b673f38f9fe81e1f0048010161d2396ee5844f18376658a740d0e64c1574e15435d898b11e6895fb9e366c7e00160048010123a38921c8a3df0be51e86008e789246c5d42acfce14e2f92ae20fd323228b0a0014021100fc14f672784a4108202100480101d064c22bd7b908f0583e76124b8f79cd2ae12a2b6c7f314866841ab69f6d08fd0011021100fc14e70d89bdac28222300480101015cf021221ff8bfe080a84c140f55b7df241dacd892dfae49cd21b9b21838450011021100fc14d960a69113c82425004801012bca1f9584151841c927fb8b9a6bf887c09ba1e0cc5330b9427a96e3aee7b8a00010021100fc14d5376aba99082627004801012e4427dfe24435652c5b10f4d6a533aa22b8c6b3b0cf9564843fb3234aadd95c000d021100fc14d29ab7e9aca82829004801011d4467b1885043dd00b94cd83318975cb2d140fdd722084503c5e4f53d6bdd3e000f021100fc14d275986a11482a2b0048010160f1f53a819b9663e6cf4a7f2ad05f14473c1040cf8625144a425db0e3d1fbe10001021100fc14d2678e94cc082c2d0211503f05347a6cd0c5c22e2f00480101528a31734c0cd0914e0e5b24837094ce137ba183f79df2ba90a97d7909b95e9b00090212680fc14d1e633be2523031004801013fb8e8144a95214d6762cd8d7359fa7d8d7ec2fb6965cb839b8e43d624ab60e8000900480101a034fc34e1f147eb3f9c031c44e890a05e7194d2856e55653466736c40edfd95000a019dba14b98dca6d1cbf2f323117af319a45c09562da3b1d49f86e900e83cc6a00fc14cb1acdbfba4c9832d0d1105cb368bb9f085ac369478347a67b0c52b690cc3902a961c799c2500001aa4cd2961c58320048010155d04ccb9e1eef0374eafb7ce62e26fb6b5d1d17353a9ad12bbbe04406241e6b000100480101b3e9649d10ccb379368e81a3a7e8e49c8eb53f6acc69b0ba2ffa80082f70ee390001ee8406d7";

        Cell cell = Cell.fromBoc(t);
        log.info("cell:\n {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        cs.loadRef();
        CellSlice cs1 = CellSlice.beginParse(cs.loadRef());
        for (int i = 0; i < 3; i++) {


            TonHashMapE dex = cs1.loadDictE(256,
                    k -> k.readUint(256),
                    v -> CellSlice.beginParse(v).loadUint(32)
            );

            log.info("Deserialized hashmap from cell {}", dex);

            for (Map.Entry<Object, Object> entry : dex.elements.entrySet()) {

                Address dictAddr = Address.of("0:" + ((BigInteger) entry.getKey()).toString(16));
                String dictAddrStr = dictAddr.toString(true, false, true, false);
                log.info("d {}", dictAddrStr);

                assertThat(dictAddrStr).isEqualTo("EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N");
            }
        }
    }
}