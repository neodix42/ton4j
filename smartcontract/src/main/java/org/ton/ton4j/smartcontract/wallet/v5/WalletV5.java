package org.ton.ton4j.smartcontract.wallet.v5;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.gson.internal.LinkedTreeMap;
import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.tl.liteserver.responses.RunMethodResult;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.toncenter.TonCenter;
import org.ton.ton4j.toncenter.TonResponse;
import org.ton.ton4j.toncenter.model.RunGetMethodResponse;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.tonlib.types.*;
import org.ton.ton4j.utils.Utils;

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
  byte[] publicKey;
  long validUntil;
  TonHashMapE extensions;
  boolean isSigAuthAllowed;

  private Tonlib tonlib;
  private long wc;

  private AdnlLiteClient adnlLiteClient;
  private TonCenter tonCenterClient;

  @Override
  public AdnlLiteClient getAdnlLiteClient() {
    return adnlLiteClient;
  }

  @Override
  public void setAdnlLiteClient(AdnlLiteClient pAdnlLiteClient) {
    adnlLiteClient = pAdnlLiteClient;
  }

  @Override
  public org.ton.ton4j.toncenter.TonCenter getTonCenterClient() {
    return tonCenterClient;
  }

  @Override
  public void setTonCenterClient(org.ton.ton4j.toncenter.TonCenter pTonCenterClient) {
    tonCenterClient = pTonCenterClient;
  }

  private boolean deployAsLibrary;

  public static class WalletV5Builder {}

  public static WalletV5Builder builder() {
    return new CustomWalletV5Builder();
  }

  private static class CustomWalletV5Builder extends WalletV5Builder {
    @Override
    public WalletV5 build() {
      if (isNull(super.publicKey)) {
        if (isNull(super.keyPair)) {
          super.keyPair = Utils.generateSignatureKeyPair();
        }
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
          .storeBytes(isNull(keyPair) ? publicKey : keyPair.getPublicKey())
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

  public SendResponse send(WalletV5Config config) {
    return send(prepareExternalMsg(config));
  }

  /**
   * Sends amount of nano toncoins to destination address and waits till message found among
   * account's transactions
   */
  //  public RawTransaction sendWithConfirmation(WalletV5Config config) throws Exception {
  //    if (nonNull(adnlLiteClient)) {
  //      adnlLiteClient.sendRawMessageWithConfirmation(prepareExternalMsg(config), getAddress());
  //      return null;
  //    } else {
  //      return tonlib.sendRawMessageWithConfirmation(
  //          prepareExternalMsg(config).toCell().toBase64(), getAddress());
  //    }
  //  }

  /** Deploy wallet without any extensions. One can be installed later into the wallet. */
  public SendResponse deploy() {
    return send(prepareDeployMsg());
  }

  public SendResponse deploy(byte[] signedBody) {
    return send(prepareDeployMsg(signedBody));
  }

  public Message prepareDeployMsg(byte[] signedBodyHash) {
    Cell body = createDeployMsg();
    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(CellBuilder.beginCell().storeCell(body).storeBytes(signedBodyHash).endCell())
        .build();
  }

  public SendResponse send(WalletV5Config config, byte[] signedBodyHash) {
    return send(prepareExternalMsg(config, signedBodyHash));
  }

  public Message prepareExternalMsg(WalletV5Config config, byte[] signedBodyHash) {
    Cell body = createExternalTransferBody(config);

    return Message.builder()
        .info(ExternalMessageInInfo.builder().dstAddr(getAddressIntStd()).build())
        .init(getStateInit())
        .body(CellBuilder.beginCell().storeCell(body).storeBytes(signedBodyHash).endCell())
        .build();
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
                .value(
                    CurrencyCollection.builder()
                        .coins(config.getAmount())
                        .extraCurrencies(
                            convertExtraCurrenciesToHashMap(config.getExtraCurrencies()))
                        .build())
                .build())
        .body(body)
        .build();
  }

  public MessageRelaxed prepareInternalMsgRelaxed(WalletV5Config config) {
    Cell body = createInternalSignedBody(config);

    return MessageRelaxed.builder()
        .info(
            InternalMessageInfoRelaxed.builder()
                .dstAddr(getAddressIntStd())
                .value(
                    CurrencyCollection.builder()
                        .coins(config.getAmount())
                        .extraCurrencies(
                            convertExtraCurrenciesToHashMap(config.getExtraCurrencies()))
                        .build())
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
  public Cell createDeployMsg() {
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

    if (actionList.getActions().isEmpty()) {

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
        .mode(
            isNull(destination.getSendMode())
                ? ((destination.getMode() == 0) ? 3 : destination.getMode())
                : destination.getSendMode().getValue())
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
                        .value(
                            CurrencyCollection.builder()
                                .coins(destination.getAmount())
                                .extraCurrencies(
                                    convertExtraCurrenciesToHashMap(
                                        destination.getExtraCurrencies()))
                                .build())
                        //
                        // .value(CurrencyCollection.builder().coins(destination.getAmount()).build())
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
    if (nonNull(tonCenterClient)) {
      try {
        return tonCenterClient.getSubWalletId(getAddress().toBounceable());
      } catch (Exception e) {
        throw new Error(e);
      }
    }
    if (nonNull(adnlLiteClient)) {
      return adnlLiteClient.getSubWalletId(getAddress());
    }
    return tonlib.getSubWalletId(getAddress());
  }

  public byte[] getPublicKey() {
    if (nonNull(tonCenterClient)) {
      try {
        return Utils.to32ByteArray(tonCenterClient.getPublicKey(getAddress().toBounceable()));
      } catch (Exception e) {
        throw new Error(e);
      }
    } else if (nonNull(adnlLiteClient)) {
      return Utils.to32ByteArray(adnlLiteClient.getPublicKey(getAddress()));
    } else if (nonNull(tonlib)) {
      return Utils.to32ByteArray(tonlib.getPublicKey(getAddress()));
    } else {
      throw new Error("Provider not set");
    }
  }

  public boolean getIsSignatureAuthAllowed() {
    if (nonNull(tonCenterClient)) {
      TonResponse<RunGetMethodResponse> runMethodResult =
          tonCenterClient.runGetMethod(
              getAddress().toBounceable(), "is_signature_allowed", new ArrayList<>());
      if (runMethodResult.isSuccess()) {
        try {
          List<Object> elements = runMethodResult.getResult().getStack().get(0);
          return Long.decode(String.valueOf(elements.get(1))) == -1;
        } catch (Throwable e) {
          throw new Error("Error getting isSignatureAuthAllowed", e);
        }
      } else {
        throw new Error("Error getting isSignatureAuthAllowed " + runMethodResult.getError());
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult =
          adnlLiteClient.runMethod(getAddress(), "is_signature_allowed");
      BigInteger signatureAllowed = runMethodResult.getIntByIndex(0);
      return signatureAllowed.longValue() != 0;
    } else if (nonNull(tonlib)) {
      RunResult result =
          tonlib.runMethod(getAddress(), Utils.calculateMethodId("is_signature_allowed"));
      TvmStackEntryNumber signatureAllowed = (TvmStackEntryNumber) result.getStack().get(0);
      return signatureAllowed.getNumber().longValue() != 0;
    } else {
      throw new Error("Provider not set");
    }
  }

  public TonHashMap getRawExtensions() {
    if (nonNull(tonCenterClient)) {
      TonResponse<RunGetMethodResponse> runMethodResult =
          tonCenterClient.runGetMethod(
              getAddress().toBounceable(), "get_extensions", new ArrayList<>());
      if (runMethodResult.isSuccess()) {
        try {
          List<Object> elements = new ArrayList<>(runMethodResult.getResult().getStack().get(0));
          LinkedTreeMap<String, String> t = (LinkedTreeMap<String, String>) elements.get(1);
          CellSlice cs = CellSlice.beginParse(Cell.fromBoc(Utils.base64ToBytes(t.get("bytes"))));
          return cs.loadDict(256, k -> k.readUint(256), v -> v);
        } catch (Throwable e) {
          throw new Error("Error executing getRawExtensions", e);
        }
      } else {
        return new TonHashMap(256);
      }
    } else if (nonNull(adnlLiteClient)) {
      RunMethodResult runMethodResult = adnlLiteClient.runMethod(getAddress(), "get_extensions");
      Cell cellExtensions = runMethodResult.getCellByIndex(0);
      CellSlice cs = CellSlice.beginParse(cellExtensions);

      return cs.loadDict(256, k -> k.readUint(256), v -> v);
    } else if (nonNull(tonlib)) {

      RunResult result = tonlib.runMethod(getAddress(), Utils.calculateMethodId("get_extensions"));
      if (result.getStack().get(0) instanceof TvmStackEntryList) {
        return new TonHashMap(256);
      }
      TvmStackEntryCell tvmStackEntryCell = (TvmStackEntryCell) result.getStack().get(0);

      String base64Msg = tvmStackEntryCell.getCell().getBytes();
      CellSlice cs = CellSlice.beginParse(Cell.fromBocBase64(base64Msg));

      return cs.loadDict(256, k -> k.readUint(256), v -> v);
    } else {
      throw new Error("Provider not set");
    }
  }
}
