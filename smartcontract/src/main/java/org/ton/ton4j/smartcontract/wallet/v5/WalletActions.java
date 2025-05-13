package org.ton.ton4j.smartcontract.wallet.v5;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.tlb.*;

@Builder
@Data
public class WalletActions {
  OutList wallet;
  ActionList extended;

  public static class WalletActionsBuilder {}

  public static WalletActionsBuilder builder() {
    return new CustomWalletActionsBuilder();
  }

  private static class CustomWalletActionsBuilder extends WalletActionsBuilder {
    @Override
    public WalletActions build() {
      return super.build();
    }
  }

  public Cell toCell() {
    CellBuilder cb = CellBuilder.beginCell();

    if (ObjectUtils.isNotEmpty(wallet)) {
      cb.storeRef(wallet.toCell());
    } else {
      cb.storeBit(false);
    }
    if (ObjectUtils.isNotEmpty(extended)) {
      cb.storeBit(true);
      cb.storeRef(extended.toCell());
    } else {
      cb.storeBit(false);
    }

    return cb.endCell();
  }
}
