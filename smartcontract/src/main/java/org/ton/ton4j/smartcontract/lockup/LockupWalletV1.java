package org.ton.ton4j.smartcontract.lockup;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.TonPfxHashMapE;
import org.ton.ton4j.smartcontract.types.LockupConfig;
import org.ton.ton4j.smartcontract.types.LockupWalletV1Config;
import org.ton.ton4j.smartcontract.types.WalletCodes;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawTransaction;
import org.ton.ton4j.tonlib.types.RunResult;
import org.ton.ton4j.tonlib.types.TvmStackEntryNumber;
import org.ton.ton4j.utils.Utils;

/**
 * <a href="https://github.com/toncenter/tonweb/tree/master/src/contract/lockup">lockup contract</a>
 * Funding the wallet with custom time-locks is out of scope for this implementation at the time.
 * This can be performed by specialized software.
 */
@Builder
@Getter
public class LockupWalletV1 implements Contract {

  public static final String LOCKUP_R1_CODE_HEX =
      "B5EE9C7241021E01000261000114FF00F4A413F4BCF2C80B010201200203020148040501F2F28308D71820D31FD31FD31F802403F823BB13F2F2F003802251A9BA1AF2F4802351B7BA1BF2F4801F0BF9015410C5F9101AF2F4F8005057F823F0065098F823F0062071289320D74A8E8BD30731D4511BDB3C12B001E8309229A0DF72FB02069320D74A96D307D402FB00E8D103A4476814154330F004ED541D0202CD0607020120131402012008090201200F100201200A0B002D5ED44D0D31FD31FD3FFD3FFF404FA00F404FA00F404D1803F7007434C0C05C6C2497C0F83E900C0871C02497C0F80074C7C87040A497C1383C00D46D3C00608420BABE7114AC2F6C2497C338200A208420BABE7106EE86BCBD20084AE0840EE6B2802FBCBD01E0C235C62008087E4055040DBE4404BCBD34C7E00A60840DCEAA7D04EE84BCBD34C034C7CC0078C3C412040DD78CA00C0D0E00130875D27D2A1BE95B0C60000C1039480AF00500161037410AF0050810575056001010244300F004ED540201201112004548E1E228020F4966FA520933023BB9131E2209835FA00D113A14013926C21E2B3E6308003502323287C5F287C572FFC4F2FFFD00007E80BD00007E80BD00326000431448A814C4E0083D039BE865BE803444E800A44C38B21400FE809004E0083D10C06002012015160015BDE9F780188242F847800C02012017180201481B1C002DB5187E006D88868A82609E00C6207E00C63F04EDE20B30020158191A0017ADCE76A268699F98EB85FFC00017AC78F6A268698F98EB858FC00011B325FB513435C2C7E00017B1D1BE08E0804230FB50F620002801D0D3030178B0925B7FE0FA4031FA403001F001A80EDAA4";

  TweetNaclFast.Signature.KeyPair keyPair;
  long walletId;
  long initialSeqno;
  byte[] publicKey;

  LockupConfig lockupConfig;

  /**
   *
   *
   * <pre>
   * Interface to <a href="https://github.com/toncenter/tonweb/tree/master/src/contract/lockup">lockup contract</a>
   * <p>
   * Additional options should be populated.
   * options.lockupConfig.configPublicKey
   * options.lockupConfig.allowedDestinations
   * options.lockupConfig.totalRestrictedValue
   * </pre>
   */
  public static class LockupWalletV1Builder {}

  public static LockupWalletV1Builder builder() {
    return new CustomLockupWalletV1Builder();
  }

  private static class CustomLockupWalletV1Builder extends LockupWalletV1Builder {
    @Override
    public LockupWalletV1 build() {
      if (isNull(super.publicKey)) {
        if (isNull(super.keyPair)) {
          super.keyPair = Utils.generateSignatureKeyPair();
        }
      }
      return super.build();
    }
  }

  private Tonlib tonlib;
  private long wc;

  private AdnlLiteClient adnlLiteClient;

  @Override
  public AdnlLiteClient getAdnlLiteClient() {
    return adnlLiteClient;
  }

  @Override
  public void setAdnlLiteClient(AdnlLiteClient pAdnlLiteClient) {
    adnlLiteClient = pAdnlLiteClient;
  }

  @Override
  public Tonlib getTonlib() {
    return tonlib;
  }

  @Override
  public void setTonlib(Tonlib pTonlib) {
    tonlib = pTonlib;
  }

  @Override
  public long getWorkchain() {
    return wc;
  }

  @Override
  public String getName() {
    return "lockupR1";
  }

  public Cell createDeployMessage() {
    return CellBuilder.beginCell()
        .storeUint(walletId, 32)
        .storeInt(-1, 32)
        .storeUint(initialSeqno, 32) // seqno
        .endCell();
  }

  public Cell createTransferBody(LockupWalletV1Config config) {

    Cell order =
        Message.builder()
            .info(
                InternalMessageInfo.builder()
                    .bounce(config.isBounce())
                    .srcAddr(getAddressIntStd())
                    .dstAddr(
                        MsgAddressIntStd.builder()
                            .workchainId(config.getDestination().wc)
                            .address(config.getDestination().toBigInteger())
                            .build())
                    .value(
                        CurrencyCollection.builder()
                            .coins(config.getAmount())
                            .extraCurrencies(
                                convertExtraCurrenciesToHashMap(config.getExtraCurrencies()))
                            .build())
                    .build())
            .init(config.getStateInit())
            .body(
                (isNull(config.getBody()) && nonNull(config.getComment()))
                    ? CellBuilder.beginCell()
                        .storeUint(0, 32)
                        .storeString(config.getComment())
                        .endCell()
                    : config.getBody())
            .build()
            .toCell();

    return CellBuilder.beginCell()
        .storeUint(config.getWalletId(), 32)
        .storeUint(
            (config.getValidUntil() == 0)
                ? Instant.now().getEpochSecond() + 60
                : config.getValidUntil(),
            32)
        .storeUint(config.getSeqno(), 32)
        .storeUint(
            isNull(config.getSendMode()) // for backward compatibility
                ? ((config.getMode() == 0) ? 3 : config.getMode())
                : config.getSendMode().getValue(),
            8)
        .storeRef(order)
        .endCell();
  }

  /**
   * from lockup-wallet.fc: store_int(seqno, 32) store_int(subwallet_id, 32) store_uint(public_key,
   * 256) store_uint(config_public_key, 256) store_dict(allowed_destinations)
   * store_grams(total_locked_value) store_dict(locked) store_grams(total_restricted_value)
   * store_dict(restricted).end_cell();
   *
   * @return Cell
   */
  @Override
  public Cell createDataCell() {

    CellBuilder cell = CellBuilder.beginCell();
    cell.storeUint(0, 32); // seqno
    cell.storeUint(walletId, 32);
    cell.storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey());
    cell.storeBytes(Utils.hexToSignedBytes(lockupConfig.getConfigPublicKey())); // 256

    int dictKeySize = 267;
    TonPfxHashMapE dictAllowedDestinations = new TonPfxHashMapE(dictKeySize);

    if (nonNull(lockupConfig.getAllowedDestinations())
        && (!lockupConfig.getAllowedDestinations().isEmpty())) {
      for (String addr : lockupConfig.getAllowedDestinations()) {
        dictAllowedDestinations.elements.put(Address.of(addr), (byte) 1);
      }
    }

    Cell cellDict =
        dictAllowedDestinations.serialize(
            k -> CellBuilder.beginCell().storeAddress((Address) k).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 8).endCell());
    cell.storeDict(cellDict);

    cell.storeCoins(
        isNull(lockupConfig.getTotalLockedValue())
            ? BigInteger.ZERO
            : lockupConfig.getTotalLockedValue());
    cell.storeBit(false); // empty locked dict
    cell.storeCoins(
        isNull(lockupConfig.getTotalRestrictedValue())
            ? BigInteger.ZERO
            : lockupConfig.getTotalRestrictedValue());
    cell.storeBit(false); // empty restricted dict

    return cell.endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(WalletCodes.lockup.getValue()).endCell();
  }

  /**
   * @return long
   */
  public long getWalletId() {

    Address myAddress = getAddress();
    RunResult result = tonlib.runMethod(myAddress, "wallet_id");
    TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStack().get(0);

    return subWalletId.getNumber().longValue();
  }

  public String getPublicKey() {

    Address myAddress = getAddress();
    RunResult result = tonlib.runMethod(myAddress, "get_public_key");
    TvmStackEntryNumber pubkey = (TvmStackEntryNumber) result.getStack().get(0);

    return pubkey.getNumber().toString(16);
  }

  public boolean check_destination(String destination) {

    Address myAddress = getAddress();

    Deque<String> stack = new ArrayDeque<>();

    CellBuilder c = CellBuilder.beginCell();
    c.storeAddress(Address.of(destination));
    stack.offer("[slice, " + c.endCell().toHex(true) + "]");

    RunResult result = tonlib.runMethod(myAddress, "check_destination", stack);
    TvmStackEntryNumber found = (TvmStackEntryNumber) result.getStack().get(0);

    return (found.getNumber().intValue() == -1);
  }

  /**
   * @return BigInteger Amount of nano-coins that can be spent immediately.
   */
  public BigInteger getLiquidBalance() {
    List<BigInteger> balances = getBalances();
    return balances.get(0).subtract(balances.get(1)).subtract(balances.get(2));
  }

  /**
   * @return BigInteger Amount of nano-coins that can be spent after the time-lock OR to the
   *     whitelisted addresses.
   */
  public BigInteger getNominalRestrictedBalance() {
    return getBalances().get(1);
  }

  /**
   * @return BigInteger Amount of nano-coins that can be spent after the time-lock only (whitelisted
   *     addresses not used).
   */
  public BigInteger getNominalLockedBalance() {
    return getBalances().get(2);
  }

  /**
   * @return BigInteger Total amount of nano-coins on the contract nominal liquid value nominal
   *     restricted value nominal locked value
   */
  public List<BigInteger> getBalances() {
    Address myAddress = getAddress();
    RunResult result = tonlib.runMethod(myAddress, "get_balances");
    TvmStackEntryNumber balance = (TvmStackEntryNumber) result.getStack().get(0); // ton balance
    TvmStackEntryNumber restrictedValue =
        (TvmStackEntryNumber) result.getStack().get(1); // total_restricted_value
    TvmStackEntryNumber lockedValue =
        (TvmStackEntryNumber) result.getStack().get(2); // total_locked_value

    return Arrays.asList(balance.getNumber(), restrictedValue.getNumber(), lockedValue.getNumber());
  }

  public ExtMessageInfo deploy() {
    Cell body = createDeployMessage();

    Message externalMessage =
        Message.builder()
            .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
            .init(getStateInit())
            .body(
                CellBuilder.beginCell()
                    .storeBytes(
                        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                    .storeCell(body)
                    .endCell())
            .build();

    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }

  public ExtMessageInfo deploy(byte[] signedBody) {
    return tonlib.sendRawMessage(prepareDeployMsg(signedBody).toCell().toBase64());
  }

  public Message prepareDeployMsg(byte[] signedBodyHash) {
    Cell body = createDeployMessage();
    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(CellBuilder.beginCell().storeBytes(signedBodyHash).storeCell(body).endCell())
        .build();
  }

  public ExtMessageInfo send(LockupWalletV1Config config, byte[] signedBodyHash) {
    return tonlib.sendRawMessage(prepareExternalMsg(config, signedBodyHash).toCell().toBase64());
  }

  public Message prepareExternalMsg(LockupWalletV1Config config, byte[] signedBodyHash) {
    Cell body = createTransferBody(config);
    return MsgUtils.createExternalMessageWithSignedBody(signedBodyHash, getAddress(), null, body);
  }

  public ExtMessageInfo send(LockupWalletV1Config config) {
    Cell body = createTransferBody(config);

    Message externalMessage =
        Message.builder()
            .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
            .body(
                CellBuilder.beginCell()
                    .storeBytes(
                        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                    .storeCell(body)
                    .endCell())
            .build();

    return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  public RawTransaction sendWithConfirmation(LockupWalletV1Config config) {
    Cell body = createTransferBody(config);

    Message externalMessage =
        Message.builder()
            .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
            .body(
                CellBuilder.beginCell()
                    .storeBytes(
                        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash()))
                    .storeCell(body)
                    .endCell())
            .build();
    return tonlib.sendRawMessageWithConfirmation(externalMessage.toCell().toBase64(), getAddress());
  }
}
