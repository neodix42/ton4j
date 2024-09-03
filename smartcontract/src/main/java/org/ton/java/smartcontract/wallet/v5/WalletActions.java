package org.ton.java.smartcontract.wallet.v5;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.tlb.types.ActionSendMsg;

import java.util.List;

@Builder
@Data
public class WalletActions {
    List<ActionSendMsg> outSendMessageAction;
    List<ExtendedAction> extendedActions;

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

    public static class ExtendedAction {
        public Action type;
        public Address dstAddress;
        public boolean isSigAllowed;

        public ExtendedAction(Action type, Address dstAddress) {
            this.type = type;
            this.dstAddress = dstAddress;
        }

        public Cell toCell() {
            if (type.value == Action.SIG_AUTH.value) {
                return CellBuilder.beginCell()
                        .storeUint(type.value, 8)
                        .storeBit(isSigAllowed)
                        .endCell();
            }
            return CellBuilder.beginCell()
                    .storeUint(type.value, 8)
                    .storeAddress(dstAddress)
                    .endCell();
        }

    }

    @Getter
    public enum Action {
        ADD_EXTENSION(2), REMOVE_EXTENSION(3), SIG_AUTH(4);
        final int value;

        Action(int value) {
            this.value = value;
        }
    }


    // actions: {
    //                    wallet: [
    //                        {
    //                            type: 'sendMsg',
    //                            outMsg: internal_relaxed({
    //                                to: newAddress,
    //                                value: toNano('100'),
    //                                init: {
    //                                    code,
    //                                    data: newWalletData
    //                                }
    //                            }),
    //                            mode: defaultExternalMode
    //                        }
    //                    ]
    //                },

    // actions: {
    //                    extended: [
    //                        {
    //                            type: 'add_extension',
    //                            address: extensionAddr
    //                        }
    //                    ]
    //                },
}
