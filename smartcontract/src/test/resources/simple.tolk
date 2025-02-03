tolk 0.7

import "@stdlib/gas-payments"

@inline
fun onInternalMessage(myTonBalance: int, msgValue: int, inMsgFull: cell, inMsgBody: slice) {
    debugPrint(40);
    debugPrint(myTonBalance);

    var cs: slice = inMsgFull.beginParse();
    var flags: int = cs.loadUint(4);

    var senderAddress: slice = cs.loadAddress();
    var x = inMsgBody.loadUint(32);
    debugPrint(x);
    setContractData(beginCell().storeUint(x, 32).endCell());
}

@inline
fun onExternalMessage(inMsgBody: slice) {
    debugPrint(41);

    var msgSeqno = inMsgBody.loadUint(32);
    var ds = getContractData().beginParse();
    var storedSeqno = ds.loadUint(32);
    var storedUnique = ds.loadUint(32);

    assert(!(msgSeqno != storedSeqno)) throw 33;

    acceptExternalMessage();

    setContractData(beginCell().storeUint(storedSeqno + 1, 32).storeUint(storedUnique, 32).endCell());
}

get seqno(): int {
    debugPrint(42);
    return getContractData().beginParse().preloadUint(32);
}

get unique(): int {
    debugPrint(42);
    return getContractData().beginParse().skipBits(32).preloadUint(32);
}
