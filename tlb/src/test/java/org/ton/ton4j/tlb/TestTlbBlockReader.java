package org.ton.ton4j.tlb;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.tlb.adapters.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbBlockReader {

  public static final Gson gson =
      new GsonBuilder()
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          .registerTypeAdapter(Cell.class, new CellTypeAdapter())
          .registerTypeAdapter(byte[].class, new ByteArrayToHexTypeAdapter())
          .registerTypeAdapter(TonHashMapAug.class, new TonHashMapAugTypeAdapter())
          .registerTypeAdapter(TonHashMapAugE.class, new TonHashMapAugETypeAdapter())
          .registerTypeAdapter(TonHashMap.class, new TonHashMapTypeAdapter())
          .registerTypeAdapter(TonHashMapE.class, new TonHashMapETypeAdapter())
          .disableHtmlEscaping()
          .setLenient()
          .create();

  @Test
  public void testShouldDeserializeBlockInfo() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c724101020100a10001a09bc7a987000000000001000000010000000000ffffffff000000000000000065a57c5f00000000000f424000000000000f424cf530ba43000000000000000100000000c400000004000000000000002e0100980000000000000000000000005052419cde96601f375f5ff37f0c0958d99d696a6b31a075787a22769006e0575ab4d36de07ce24d78ddc0c37a776ebea7728d08bc5d720cf7ab662a4ffb23e02d1bf94f")
            .endCell();
    log.info("CellType {}", c.getCellType());
    BlockInfo blockInfo = BlockInfo.deserialize(CellSlice.beginParse(c));
    log.info("blockInfo {}", blockInfo);
    assertThat(blockInfo.getEndLt()).isEqualTo(1000012);
    assertThat(blockInfo.getGenValidatorListHashShort()).isEqualTo(4113611331L);
    assertThat(blockInfo.getPrevRef().getPrev1().getFileHash())
        .isEqualTo("5ab4d36de07ce24d78ddc0c37a776ebea7728d08bc5d720cf7ab662a4ffb23e0");
    c.toHex();
  }

  @Test
  public void testShouldDeserializeValueFlow() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c72410106010054000211b8e48dfb4a0eebb0040105022581fa7454b05a2ea2ac0fd3a2a5d348d2954008020202012004030015bfffffffbcbd0efda563d00015be000003bcb355ab466ad0001d43b9aca00250775d8011954fc40008b63e6951")
            .endCell();
    log.info("CellType {}", c.getCellType());
    ValueFlow valueFlow = ValueFlow.deserialize(CellSlice.beginParse(c));
    log.info("valueFlow {}", valueFlow);
    assertThat(valueFlow.getFeesCollected().getCoins()).isEqualTo(2700000000L);
    assertThat(valueFlow.getRecovered().getCoins()).isEqualTo(2700000000L);
    assertThat(valueFlow.getFeesImported().getCoins()).isEqualTo(1000000000L);
    assertThat(valueFlow.getFromPrevBlk().getCoins())
        .isEqualTo(new BigInteger("2280867924805872170"));
    c.toHex();
  }

  @Test
  public void testShouldDeserializeStateUpdate() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c7241024301000be900028a045052419cde96601f375f5ff37f0c0958d99d696a6b31a075787a22769006e057724ab9590a228d7373ef8d28544bea428d454ad789b07c24ceb3fc7a07c32c59001400140301245b9023afe2ffffff1100ffffffff0000000000000000000000010000000065a57c5f00000000000f424c0000000160160224112213821158e21bcc4e71c0300626245b9023afe2ffffff1100ffffffff0000000000000000000000000000000065a57c180000000000000000ffffffff600529041521330000000000000000000000000000000084563886eda33f200028250011000000000000000010231340422b1c437989ce3806090726219fbf955555555555555555555555555555555555555555555555555555555555555502812a05f200393159c085ce242a1fe7dc6f3b4ef0cfbf54eef7ad3f40de588995b86f9c2f46400000000003d09220082271cff555555555555555555555555555555555555555555555555555555555555555533a973dc00000000000000000003d0925409502f90015d02f2d23130108ac710d9ba5bc60180e0a26219fbf66666666666666666666666666666666666666666666666666666666666666660502b95fd5003794d7a89bf0b1b83d2e445fab3781b81d7584baecb59bebf56b2e2f1fb798f3800000000007a121400b2271cff33333333333333333333333333333333333333333333333333333333333333332ce9bcbc00000000000000000003d090d40ae57f54016d0410c015188caa7e2000000000000000000000000000000000000000000000000000000000000000000000000010d001f65a5823b65a57fe3609184e72a000010231460108ac710d4479c1c010f21262265df40845638869fc774100fb887249507b17fc5674e8ad1ae8058eddc490c24e8c0138896214d64c0a4637800000000007a121c26102477cff000000000000000000000000000000000000000000000000000000000000000021881c9400000000000000000003d09121158e21a7f1dd04033c0263c3b3a2455cc26aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaac22b1c4379fc3f1806122e1c260103d0401301eb50000000000000000800000000000000000000000000000005a2f25be03d373e41b62e16fb0d999a97235b40ea71f1522a5fa982a817ea60fd265bcbc973e584eb1eab6b3ea65309a0a32cec006e1bee4f781214122612c4d000000000040000000000000007fffffff800000004cb4af986000000c91400030020284801019fffb8e8af80f298cefbc832633b545151ef8eb59eb427e724054e1cab56efbf000f0211800000000007a124d01817006bb0400000000000000000000000800000000007a1234909ba9b6b469399f9e52775ebeb6c4de942610ad6cc27bdac50ef7fd984a6dbc00213e2000000000000f424983d190173a7ffffffffffffffffd64515aa043f53a3f60eaf663295c8a035537a617a3cc21f5e94859356c021f6400000000003d092c00000000003d092e01a01064606001b00ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffbd01efe92000000000000001e8496cb4af8be4002bf0001f530ba430000000060000000000000000800000000000000000000000282920ce6f4b300f9bafaff9bf8604ac6cceb4b53598d03abc3d113b4803702bad5a69b6f03e7126bc6ee061bd3bb75f53b946845e2eb9067bd5b31527fd91f00be201d0201201f1e00b3bfecabb049ff67c5388c8bbc8010e2e757141a27180b24e1450e6eced5c6273178a32d2be2f8000000000000000800000008000000000000000800000000000000000000000000000000000000000000000000000000000000040073dfe8cb4af8be0000000000000002000000020000000000000002000000000000000000000000000000000000000000000000000000000000000100abd040000000000000002000000000000000000000000a0a48339bd2cc03e6ebebfe6fe1812b1b33ad2d4d66340eaf0f444ed200dc0aeb569a6dbc0f9c49af1bb8186f4eedd7d4ee51a1178bae419ef56cc549ff647c10219bbe8f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac0213ab668031a93e8c798b2cb3e2acc80b1abde06ed7b06270a03fe422373e797c88470ea8c00000000003d092222236fcff04f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac21881f4800000000000000000003d093109d5b34017f0372342004811ef55ac000000000000000000000000000000000000000000000000000000000000000022390000000000000000000000000000000184563886f3139c7009cc4b40282625284801018036bd5aa65bd160ccc4f204a44731afdb62bab1ab21c9496af2fc1b4d226ccd000302012028270015bfffffffbcbd1a94a200100015be000003bcb3670dc155502113821158e21bb68cfc80102a221340422b1c4376d19f9002302b219fbf955555555555555555555555555555555555555555555555555555555555555502812a05f20000000000000000000000000000000000000000000000000000000000000000000000000000000000202c2271cff555555555555555555555555555555555555555555555555555555555555555533a973dc0000000000000000000000001409502f90015d02f2d21490000000013311def76051bbabfb248bd9a90d300716f598ffe8f8b65c8fddd8501b0e8c6402e28480101aa559cb80ee5ce3f4b1e6107ec3ea424c2b89946804aad09f246e96353cc15c5000e2848010160d01ddc9d54e89cf8040b78fab3258c9327109018e4078f7f5c7ff0ba401aa2000c22130108ac710d90c501c0083331219fbf66666666666666666666666666666666666666666666666666666666666666660502540be4000000000000000000000000000000000000000000000000000000000000000000000000000000000040322271cff33333333333333333333333333333333333333333333333333333333333333332cc9ba500000000000000000000000001409502f90016d04140221460108ac710d4643854003834219bbe8f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac021dcd650000000000000000000000000000000000000000000000000000000000000000000000000000000000235236fcff04f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac21881f4800000000000000000000000010ee6b28017f0373642004811ef55aa00000000000000000000000000000000000000000000000000000000000000002848010145910e27fe37d8dcf1fac777ebb3bda38ae1ea8389f81bfb1bc0079f3f67ef5b00022165df40845638869f6816000000000000000000000000000000000000000000000000000000000000000000000000000000000004392377cff0000000000000000000000000000000000000000000000000000000000000000212819f400000000000000000000000021158e21a7da0580013c03c3b3a284801012d8973441aaae38b20c3fda9274f0a97de1447af2ecd7e424267fcca9901866b000300480000000045f1e3dc29d6bf453889c449069a2458b2d56701fc3661d498162f743e5ba6210098ff0020dd2082014c97ba9730ed44d0d70b1fe0a4f260810200d71820d70b1fed44d0d31fd3ffd15112baf2a122f901541044f910f2a2f80001d31f31d307d4d101fb00a4c8cb1fcbffc9ed540173a7e00000000000000020847cbb01de39c3308edf1091131c7dedb780ca3e34d85bdc1a6ab4df16d79e800000000003d092400000000003d092603e01064606003f00ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593fc0000000000000000000000000000000000000000000000000000000000000001017d784000000000000001e8492cb4af8be4000490000000000000000000000000000000000000000000000000000000000000000000000000128480101febfd56fa7c2a5010aea13c738df0c2155d6670e8b055dd312d2372f38f7701b000e28480101986c49971b96062e1fba4410e27249c8d73b0a9380f7ffd44640167e68b215e80003c9f67164")
            .endCell();
    log.info("CellType {}", c.getCellType());
    MerkleUpdate merkleUpdate = MerkleUpdate.deserialize(CellSlice.beginParse(c));
    log.info("stateUpdate {}", merkleUpdate);
    c.toHex();
  }

  @Test
  public void testLoadBlockMaster() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-6.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("block {}", block);
    assertThat(block).isNotNull();

    List<Transaction> txs = block.getAllTransactions();
    log.info("txs {}", txs);
    //    block.toCell();
  }

  @Test
  public void testLoadBlockMaster2() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c7241023201000498000114ff00f4a413f4bcf2c80b010201200302000cf2308048f2f00201480e0402012008050201200706001db9c34f00a5f03802032028307f00580011bbbd182108325e4c380201200d090201200b0a003bb6e53da89a1f401a803a1a7ffe00203e00203a861a1e0026209a8608a810020120310c002bb2fe7c02840917c120c1fd039be864fe800c380c1c200017bb9a5ed44d0d430d0d3ff3080202cb120f0201ce1110005b3e105bc90c0c40b53d01347b5134350c3434ffcc201254452ebcbd087ec120841ca368e840b2333d00104c3c01a000513e105bc90c0c40bd01347b5134350c3434ffcc20125444eebcbd20840764eab600723d00104c3c01a002012023130201201c140201201915020120181601f73e105bc90c80fd01347c02b434c03e8034c7f4c7fd010c2012c97cbd2012d4e4ae7cbd2012d4e4ee7cbd20134920840ee6b2802814032ec6fcbd3e097e0554c1e8483e0454c2e0083d039be864f4c7cc248c083880a94b20083d039be865900720083d05a74c083232c7f274100720083d05b882a9013232c01400e0170038fa02cb1fcb1f17f400c9f00b82101a69387e02c8cb1ff4004130f00600793e105bc90c0c40b53d01347b5134350c3434ffcc201254c52ebcbd08b434ffcc201200aebcbd3c028c54943c02e0843218aeaf40b2333d00104c3c01a00201201b1a00e33e105bc90c0c40b4fff4c7fe803d01347c02887434ffcc20125446eebcbd08e0080a60c1fc014c6011c07cbc94ca3c020a7232ffd50825a0083d10c1291508e43c0240bc02e0840d2212a4497232ffd49032c7d4883d00095110d4a17c01e0841c04df21c0f232ffc4b2c7fd00104c3c01a000ed3e105bc90c0c40b4fff4c7fe803d01347c0288e0080a60c1fc016011c07cbd2011d4c6eebcbd14cc3c0214d2bc020af232ffd5082e20083d10c06951543c0241291509243c025004fc02e084260abfffc97232ffd49032c7d4883d00095110d4a17c01e0840c19b443c0f232ffc4b2c7fd00104c3c01a0020120201d0201201f1e001f3214017e8084fd003d003333327b552000193b51343e803d013d0135350c200201202221003b20128870403cbc8830802672007e8080a0c1fd10e5cc0060c1fd16cc38a0001d0060c1fd039be864fe800c380c1c200201202b2402012028250201202726003d1c20043232c141bc0105b3c594013e808532dab2c7c4b2cff3c4f25c7ec020003d1c20043232c1417c010573c5893e808532da84b2c7f2cff3c4f260103ec0200201202a290023104cfd039be8482540b5c04c00780c5c0ca0001d1c081cb232c072c032c1f2fff274200201202d2c00215fa4001fa46804602c00012f2f4d3ff3080201202f2e001134c1c06a80b5c6006001ed20120871c03cbc807434c0c05c6c2497c0f83c00cc4074c7dc208061a808f00023858cc074c7e01200a0841b5a5b9d2e84bcbd2082c63cd865d6f4cffe801400f880fe0048840d10015bc13e186084100d28f014842ea4cc3c033820842296cbb9d4842ea4cc3c03782082c63cd854842ea4cc3c03f8203000588210982535785210ba9330f00ee08210b766741a5210ba9330f011e0821025d53dfdba92f010e0308048f2f00011b323bc02840d17c12004896818")
            .endCell();
    log.info("CellType {}", c.getCellType());
    c.toHex();
  }

  @Test
  public void testLoadBlockNotMaster() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c72e1021c0100040b00001c00c400de0170020402a0033c036a037c0387039e03b6041c048204ce04ea0536055405a005ec060406200700077007bc080908100817041011ef55aaffffff110102030402a09bc7a98700000000840101c745200000000100000000000000000000000000634e94ec00001d367caaae4000001d367caaae419bbc68ac00058fb00173ed920173bfbec400000003000000000000002e05060211b8e48dfb43b9aca00407080a8a04250ec78adc9d082383679c3289edc662b628be0e34e51a8f7c412e98d24c8a5fb59960f376a6ad4dce93f406ce904add5a2aea140c99b877d02f67f1cd1e5f51021902190c0d03894a33f6fdb1c342502d7261843b4a3bfdbfb766c45705b7c4410af03c358431620ff05a79b1be0d76ede085c08726e04bad3c5779d949364eb56540f06c2c49b98d514111401a1b1b009800001d367c9b6c040173ed92b57df82537164b18661e22f620e1a7a15826a73d7402eef9433d55c030232370a7caa150ac8f2f4c74cb5c77e6671edb6f8accd65c683faf6e48a88720b2c72d009800001d367c9b6c0101c7451f78d2820caf6a5f100a444450ddab2f7754bbce7c6027dce5349269227866124a33b3efd318a7ec75c8f26844fd4dce5f581927f670a0087d7fec56658b487d720225826b977bb75290e16c135cbbddba94870b40080909000d0010ee6b2800080201200a0b0013be000003bc91627aea900013bfffffffbc8b96fc9c50235b9023afe2ffffff110000000000000000000000000001c7451f00000001634e94e900001d367c9b6c010173ed91200e0f10235b9023afe2ffffff110000000000000000000000000001c7452000000001634e94ec00001d367caaae410173ed9220141516284801017e49cb3c190a5033a93c907c6631d4459cf4bf71f57f041dd14270fb919423dc000122138209ae5deedd4a4385b011192848010125e39d851243cee82c062dd588cfa4587461b7869f68023bad26988d33bf8a24000223130104d72ef76ea521c2d81213192848010105a0d0f5cf8e9d2d98f032e935e8de2208463332de6c74af0b9d5cfc2bc2802102162848010157c418ac5021e527850e982354ed5a21fd7a0b0ac719e443fcd3c80f496dc4db003401110000000000000000501722138209ae5deedd4a4385b0181921d90000000000000000ffffffffffffffff826b977bb75290e16bb5f5e54ddd448c900001d367c9b6c040173ed92b57df82537164b18661e22f620e1a7a15826a73d7402eef9433d55c030232370a7caa150ac8f2f4c74cb5c77e6671edb6f8accd65c683faf6e48a88720b2c72d819006bb0400000000000000000b9f6c900000e9b3e4db601ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc0284801012aa19c773967de4112363f58e8331a68fb2b3fcb1d55daf352b93c497a019ce4021728480101b3e9649d10ccb379368e81a3a7e8e49c8eb53f6acc69b0ba2ffa80082f70ee39000100030020000102b1e6b8f1")
            .endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("block {}", block);
    assertThat(block).isNotNull();

    List<Transaction> txs = block.getAllTransactions();
    log.info("txs {}", txs);
    block.toCell();
    block.toCell().toHex();
  }

  @Ignore
  @Test // does not work in pytoniq-core
  public void testShouldDeserializeBlock() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-5")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("block {}", block);
    List<Transaction> txs = block.getAllTransactions();
    log.info("txs {}", txs);
    block.toCell();
    block.toCell().toHex();
  }

  @Test
  public void testShouldDeserializeBlock2() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c72e10211010002b800001c00c400de0170020402a0033c0346035803a4040a0422042a04f20562056a0571041011ef55aaffffff110102030402a09bc7a98700000000800100000001000000000000000000000000000000000065a57c6500000000004c4b4000000000004c4b41f530ba43000000000000000400000000c400000004000000000000002e05060211b8e48dfb43b9aca00407080a8a04b45e4b7c07a6e7c836c5c2df61b33352e46b681d4e3e2a454bf5305502fd4c1fddf467caa9989b5c1f035b5c4e7c5ac8c31d8c2528f8d773b7ae3bf2ecdc2ffe00010002090a03894a33f6fdc1982ce574b6b30d1b8a1574d1fcdb9056434fbcdced45d451545011e0b48651d9576093fecf8a711917790021c5ceae28344e301649c28a1cdd9dab8c4e62f1400f1010009800000000003d0908000000045121fb8e9ad96a839c8b148b7598e70577e0b31449f83731347af3f97e632fc87bb5caea234b002a390f4ab673c842f3466dedcdf838a2a51bf49a0af3d78fab0098000000000000000000000000b45e4b7c07a6e7c836c5c2df61b33352e46b681d4e3e2a454bf5305502fd4c1fa4cb79792e7cb09d63d56d67d4ca613414659d800dc37dc9ef02428244c2589a0005000008000d0010ee6b28000828480101b45e4b7c07a6e7c836c5c2df61b33352e46b681d4e3e2a454bf5305502fd4c1f0001035b9023afe2ffffff1100000000000000000000000000000000010000000065a57c6500000000004c4b4100000004200b0c0d01110000000000000000500e0003001000c300000000000000000000000000000001021dcd6500100000000003d0908000000045121fb8e9ad96a839c8b148b7598e70577e0b31449f83731347af3f97e632fc87bb5caea234b002a390f4ab673c842f3466dedcdf838a2a51bf49a0af3d78fab8006bb040000000000000000000000200000000001e8483ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc000030020000102a62c29b8")
            .endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info(
        "inMsg {}, outMsg {}, block {}",
        block.getExtra().getInMsgDesc().getCount(),
        block.getExtra().getOutMsgDesc().getCount(),
        block);
    block.toCell().toHex();
  }

  @Test
  public void testShouldDeserializeBlock4() {
    Cell c =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c72e10211010002ba00001c00c400de0170020402a0033c0346035803a4040a0422042a04f60566056e0575041011ef55aaffffff110102030402a09bc7a9870000000084010002f9c9000000000000000000000000000000000068bc4b100000003413f320400000003413f32041c5dfece0000004dd000308290003059ac40000000b00000000000003ee05060211b8e48dfb43b9aca00407080a8a042c1004f1a0b76a00ea3200eae279beecf59a7970dfa82fd71aa32cd9dfde9977e7e4850f3b76ddaaf1df8322658d850ca3d93e1961e374e1b54232adbd7f830a00020002090a03894a33f6fdf0540b83317882f0b3b944b6550829bf41162abbde579c78e5e130486b06414f4837e99a11dc5ca3b0f7fede3ff05727f56189a48d205eeb6b0a5578e8aac013400f101000980000003413e3de04000308292c13b029c8a03ffdc7544d47bd50bb6b6e904d355eb87bae5692bb32f145a037eaf48d740cb10a17a74c18adf804d3ce5ae71bd826a5a7954f270dfc5ef8c37a00980000003413e3de010002f9c8fa0a5e09054091ef68cdd157aaaae8e244f04ac2e7fc4108b07210a081e08d72098f1e64e9c3de72802678da46853313f82d8fc500de4383c6229c6a17f26d980005000008000d0010ee6b280008284801012c1004f1a0b76a00ea3200eae279beecf59a7970dfa82fd71aa32cd9dfde99770002035b9023afe2ffffff11000000000000000000000000000002f9c90000000068bc4b100000003413f3204100030829200b0c0d01110000000000000000500e0003001000c70000000000000000ffffffffffffffff0358aef7814d0010000003413e3de04000308292c13b029c8a03ffdc7544d47bd50bb6b6e904d355eb87bae5692bb32f145a037eaf48d740cb10a17a74c18adf804d3ce5ae71bd826a5a7954f270dfc5ef8c37a8006bb04000000000000000000184148000001a09f1ef01ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc000030020000102babad4d6")
            .endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info(
        "inMsg {}, outMsg {}, ShardAccountBlocks {}, block {}",
        block.getExtra().getInMsgDesc().getCount(),
        block.getExtra().getOutMsgDesc().getCount(),
        block.getExtra().getShardAccountBlocks(),
        block);
    block.toCell().toHex();
  }

  @Test
  public void testShouldDeserializeBlock5() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-4.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    //    block.toCell();
  }

  @Test
  public void testShouldDeserializeBlock7() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-3.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    //    block.toCell();
  }

  // Bits overflow. Can't load 32 bits. 1 bits left.
  @Test
  public void testShouldDeserializeBlock8() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-2.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    //    block.toCell(); empty config params?
  }

  // unknown MsgEnvelope flag, found 0x0,
  @Test
  public void testShouldDeserializeBlock9() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-1.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    block.toCell().toHex();
  }

  /** Refs overflow. No more refs., */
  @Test
  public void testShouldDeserializeBlock10() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-7.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    block.toCell().toHex();
    log.info(
        "block, txs {}, msgs {}", block.getAllTransactions().size(), block.getAllMessages().size());
  }

  /** Can't load 256 bits. 235 bits left. */
  @Test
  public void testShouldDeserializeBlock11() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-8.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    block.toCell().toHex();
    log.info(
        "block, txs {}, msgs {}", block.getAllTransactions().size(), block.getAllMessages().size());
  }

  /** BigInteger out of byte range, boc */
  @Test
  public void testShouldDeserializeBlock12() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-9.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    log.info(
        "block, txs {}, msgs {}", block.getAllTransactions().size(), block.getAllMessages().size());
    block.toCell();
    block.toCell().toHex();
    log.info("gson {}", gson.toJson(block));
  }

  /** Wrong magic for MsgAddressInt, found 0 */
  @Test
  public void testShouldDeserializeBlock13() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-10.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    log.info(
        "block, txs {}, msgs {}", block.getAllTransactions().size(), block.getAllMessages().size());
    block.toCell().toHex();
  }

  /** Bits overflow. Can't load 8 bits. 5 bits left. */
  @Test
  public void testShouldDeserializeBlock14() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-11.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    //    block.toCell();
    log.info(
        "block, txs {}, msgs {}", block.getAllTransactions().size(), block.getAllMessages().size());
    // block.toCell().toHex(); // java.lang.Error: TonHashMap does not support empty dict. Consider
    // using TonHashMapE
  }

  /** wrong magic number, can be only [0x5f327da5L, 0x9023afe2L], found 101b099 */
  @Test
  public void testShouldDeserializeBlock15() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-12.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    block.toCell().toHex();
  }

  /** no more refs */
  @Test
  public void testShouldDeserializeBlock16() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-13.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    block.toCell().toHex();
  }

  /** Bits overflow. Can't load 276 bits. 42 bits left. */
  @Test
  public void testShouldDeserializeBlock17() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-14.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    block.toCell().toHex();
  }

  /** no more refs */
  @Test
  public void testShouldDeserializeBlock18() {
    Cell c = CellBuilder.beginCell().fromBoc(getBoc("boc-15.txt")).endCell();
    log.info("CellType {}", c.getCellType());
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("getShardAccountBlocks {}, block {}", block.getExtra().getShardAccountBlocks(), block);
    block.toCell();
    block.toCell().toHex();
  }

  private String getBoc(String fileName) {
    return Utils.streamToString(
        Objects.requireNonNull(
            TestTlbBlockReader.class.getClassLoader().getResourceAsStream(fileName)));
  }
}
