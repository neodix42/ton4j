package org.ton.java.smartcontract.wallet.v5;

import static java.util.Objects.isNull;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.*;
import org.ton.java.smartcontract.types.Destination;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV5Config;
import org.ton.java.smartcontract.types.WalletV5InnerRequest;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

@Builder
@Getter
public class WalletV5 implements Contract {

  private static final int SIZE_BOOL = 1;
  private static final int SIZE_SEQNO = 32;
  private static final int SIZE_WALLET_ID = 32;
  private static final int SIZE_VALID_UNTIL = 32;

  private static final int PREFIX_SIGNED_EXTERNAL = 0x7369676E;
  private static final int PREFIX_SIGNED_INTERNAL = 0x73696E74;
  private static final int PREFIX_EXTENSION_ACTION = 0x6578746e;

  TweetNaclFast.Signature.KeyPair keyPair;
  long initialSeqno;
  long walletId;

  long validUntil;
  TonHashMapE extensions;
  boolean isSigAuthAllowed;

  private Tonlib tonlib;
  private long wc;

  private boolean deployAsLibrary;

  public static class WalletV5Builder {}

  public static WalletV5Builder builder() {
    return new CustomWalletV5Builder();
  }

  private static class CustomWalletV5Builder extends WalletV5Builder {
    @Override
    public WalletV5 build() {
      if (isNull(super.keyPair)) {
        super.keyPair = Utils.generateSignatureKeyPair();
      }
      return super.build();
    }
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
    return "V5";
  }

  /**
   *
   *
   * <pre>
   * contract_state$_
   *   is_signature_allowed:(## 1)
   *   seqno:# wallet_id:(## 32)
   *   public_key:(## 256)
   *   extensions_dict:(HashmapE 256 int1)
   *   = ContractState;
   * </pre>
   */
  @Override
  public Cell createDataCell() {
    if (isNull(extensions)) {
      return CellBuilder.beginCell()
          .storeBit(isSigAuthAllowed)
          .storeUint(initialSeqno, 32)
          .storeUint(walletId, 32)
          .storeBytes(keyPair.getPublicKey())
          .storeBit(false) // empty extensions dict
          .endCell();
    } else {
      return CellBuilder.beginCell()
          .storeBit(isSigAuthAllowed)
          .storeUint(initialSeqno, 32)
          .storeUint(walletId, 32)
          .storeBytes(keyPair.getPublicKey())
          .storeDict(
              extensions.serialize(
                  k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                  v -> CellBuilder.beginCell().storeBit((Boolean) v).endCell()))
          .endCell();
    }
  }

  @Override
  public Cell createCodeCell() {
    if (!deployAsLibrary) {
      return CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell();
    } else {
      return CellBuilder.beginCell()
          .storeUint(2, 8)
          .storeBytes(
              CellBuilder.beginCell().fromBoc(WalletCodes.V5R1.getValue()).endCell().getHash())
          .setExotic(true)
          .cellType(CellType.LIBRARY)
          .endCell();
    }
  }

  @Override
  public StateInit getStateInit() {
    return StateInit.builder()
        .code(createCodeCell())
        .data(createDataCell())
        //                .lib(createLibraryCell())
        .build();
  }

  public ExtMessageInfo send(WalletV5Config config) {
    return tonlib.sendRawMessage(prepareExternalMsg(config).toCell().toBase64());
  }

  /** Deploy wallet without any extensions. One can be installed later into the wallet. */
  public ExtMessageInfo deploy() {
    return tonlib.sendRawMessage(prepareDeployMsg().toCell().toBase64());
  }

  public Message prepareDeployMsg() {
    Cell body = createDeployMsg();
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell())
        .build();
  }

  public Message prepareExternalMsg(WalletV5Config config) {
    Cell body = createExternalTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell())
        .build();
  }

  public Message prepareInternalMsg(WalletV5Config config) {
    Cell body = createInternalSignedBody(config);

    return Message.builder()
        .info(
            InternalMessageInfo.builder()
                .srcAddr(getAddressIntStd())
                .dstAddr(getAddressIntStd())
                .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                .build())
        .body(body)
        .build();
  }

  /**
   *
   *
   * <pre>
   * signed_request$_             // 32 (opcode from outer)
   *   wallet_id:    #            // 32
   *   valid_until:  #            // 32
   *   msg_seqno:    #            // 32
   *   inner:        InnerRequest //
   *   signature:    bits512      // 512
   *   = SignedRequest;
   * </pre>
   */
  private Cell createDeployMsg() {
    if (isNull(extensions)) {
      return CellBuilder.beginCell()
          .storeUint(PREFIX_SIGNED_EXTERNAL, 32)
          .storeUint(walletId, SIZE_WALLET_ID)
          .storeUint(
              (validUntil == 0) ? Instant.now().getEpochSecond() + 60 : validUntil,
              SIZE_VALID_UNTIL)
          .storeUint(0, SIZE_SEQNO)
          .storeBit(false) // empty extensions dict
          .endCell();
    } else {
      return CellBuilder.beginCell()
          .storeUint(PREFIX_SIGNED_EXTERNAL, 32)
          .storeUint(walletId, SIZE_WALLET_ID)
          .storeUint(
              (validUntil == 0) ? Instant.now().getEpochSecond() + 60 : validUntil,
              SIZE_VALID_UNTIL)
          .storeUint(0, SIZE_SEQNO)
          .storeDict(
              extensions.serialize(
                  k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                  v -> CellBuilder.beginCell().storeBit((Boolean) v).endCell()))
          .endCell();
    }
  }

  public Cell createExternalTransferBody(WalletV5Config config) {
    return CellBuilder.beginCell()
        .storeUint(PREFIX_SIGNED_EXTERNAL, 32)
        .storeUint(config.getWalletId(), SIZE_WALLET_ID)
        .storeUint(
            (config.getValidUntil() == 0)
                ? Instant.now().getEpochSecond() + 60
                : config.getValidUntil(),
            SIZE_VALID_UNTIL)
        .storeUint(config.getSeqno(), SIZE_SEQNO)
        .storeCell(config.getBody()) // innerRequest
        .endCell();
  }

  public Cell createInternalTransferBody(WalletV5Config config) {
    return CellBuilder.beginCell()
        .storeUint(PREFIX_SIGNED_INTERNAL, 32)
        .storeUint(config.getWalletId(), SIZE_WALLET_ID)
        .storeUint(
            (config.getValidUntil() == 0)
                ? Instant.now().getEpochSecond() + 60
                : config.getValidUntil(),
            SIZE_VALID_UNTIL)
        .storeUint(config.getSeqno(), SIZE_SEQNO)
        .storeCell(config.getBody()) // innerRequest
        .endCell();
  }

  public Cell createInternalSignedBody(WalletV5Config config) {
    Cell body = createInternalTransferBody(config);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public Cell createInternalExtensionTransferBody(BigInteger queryId, Cell body) {
    return CellBuilder.beginCell()
        .storeUint(PREFIX_EXTENSION_ACTION, 32)
        .storeUint(queryId, 64)
        .storeCell(body) // innerRequest
        .endCell();
  }

  public Cell createInternalExtensionSignedBody(BigInteger queryId, Cell body) {
    Cell body1 = createInternalExtensionTransferBody(queryId, body);
    byte[] signature = Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), body1.hash());

    return CellBuilder.beginCell().storeCell(body).storeBytes(signature).endCell();
  }

  public WalletV5InnerRequest manageExtensions(ActionList actionList) {

    return WalletV5InnerRequest.builder()
        .outActions(OutList.builder().build())
        .hasOtherActions(true)
        .otherActions(actionList)
        .build();
  }

  public WalletV5InnerRequest createBulkTransfer(List<Destination> recipients) {
    if (recipients.size() > 255) {
      throw new IllegalArgumentException("Maximum number of recipients should be less than 255");
    }

    List<OutAction> messages = new ArrayList<>();
    for (Destination recipient : recipients) {
      messages.add(convertDestinationToOutAction(recipient));
    }

    return WalletV5InnerRequest.builder()
        .outActions(OutList.builder().actions(messages).build())
        .hasOtherActions(false)
        .build();
  }

  public WalletV5InnerRequest createBulkTransferAndManageExtensions(
      List<Destination> recipients, ActionList actionList) {
    if (recipients.size() > 255) {
      throw new IllegalArgumentException("Maximum number of recipients should be less than 255");
    }

    List<OutAction> messages = new ArrayList<>();
    for (Destination recipient : recipients) {
      messages.add(convertDestinationToOutAction(recipient));
    }

    if (actionList.getActions().size() == 0) {

      return WalletV5InnerRequest.builder()
          .outActions(OutList.builder().actions(messages).build())
          .hasOtherActions(false)
          .build();
    } else {
      return WalletV5InnerRequest.builder()
          .outActions(OutList.builder().actions(messages).build())
          .hasOtherActions(true)
          .otherActions(actionList)
          .build();
    }
  }

  private OutAction convertDestinationToOutAction(Destination destination) {
    Address dstAddress = Address.of(destination.getAddress());
    return ActionSendMsg.builder()
        .mode((destination.getMode() == 0) ? 3 : destination.getMode())
        .outMsg(
            MessageRelaxed.builder()
                .info(
                    InternalMessageInfoRelaxed.builder()
                        .bounce(destination.isBounce())
                        .dstAddr(
                            MsgAddressIntStd.builder()
                                .workchainId(dstAddress.wc)
                                .address(dstAddress.toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(destination.getAmount()).build())
                        .build())
                .init(getStateInit())
                .body(
                    (isNull(destination.getBody())
                            && StringUtils.isNotEmpty(destination.getComment()))
                        ? CellBuilder.beginCell()
                            .storeUint(0, 32) // 0 opcode means we have a comment
                            .storeString(destination.getComment())
                            .endCell()
                        : destination.getBody())
                .build())
        .build();
  }

  // Get Methods
  // --------------------------------------------------------------------------------------------------

  public long getWalletId() {
    RunResult result = tonlib.runMethod(getAddress(), Utils.calculateMethodId("get_subwallet_id"));
    TvmStackEntryNumber subWalletId = (TvmStackEntryNumber) result.getStack().get(0);
    return subWalletId.getNumber().longValue();
  }

  public byte[] getPublicKey() {
    RunResult result = tonlib.runMethod(getAddress(), Utils.calculateMethodId("get_public_key"));
    TvmStackEntryNumber pubKey = (TvmStackEntryNumber) result.getStack().get(0);
    return pubKey.getNumber().toByteArray();
  }

  public boolean getIsSignatureAuthAllowed() {
    RunResult result =
        tonlib.runMethod(getAddress(), Utils.calculateMethodId("is_signature_allowed"));
    TvmStackEntryNumber signatureAllowed = (TvmStackEntryNumber) result.getStack().get(0);
    return signatureAllowed.getNumber().longValue() != 0;
  }

  public TonHashMap getRawExtensions() {
    RunResult result = tonlib.runMethod(getAddress(), Utils.calculateMethodId("get_extensions"));
    if (result.getStack().get(0) instanceof TvmStackEntryList) {
      return new TonHashMap(256);
    }
    TvmStackEntryCell tvmStackEntryCell = (TvmStackEntryCell) result.getStack().get(0);

    String base64Msg = tvmStackEntryCell.getCell().getBytes();
    CellSlice cs = CellSlice.beginParse(Cell.fromBocBase64(base64Msg));

    return cs.loadDict(256, k -> k.readUint(256), v -> v);
  }
}
