import "@stdlib/gas-payments"
tolk 1.0

struct Storage {
    id: uint32
    counter: uint32
}

fun Storage.load() {
    return Storage.fromCell(contract.getData())
}

fun Storage.save(self) {
    contract.setData(self.toCell())
}

struct (0x7e8764ef) IncreaseCounter {
    queryId: uint64  // query id, typically included in messages
    increaseBy: uint32
}

struct (0x3a752f06) ResetCounter {
    queryId: uint64
}

type AllowedMessage = IncreaseCounter | ResetCounter

fun onExternalMessage(extBody: slice) {
    var storage = lazy Storage.load();
    val msgId = extBody.loadUint(32);

    assert (msgId == storage.id) throw 33;
    acceptExternalMessage();
    storage.id += 1;
    // commitContractDataAndActions();
    storage.save();
}

fun onInternalMessage(in: InMessage) {
    val msg = lazy AllowedMessage.fromSlice(in.body);

    match (msg) {
        IncreaseCounter => {
            var storage = lazy Storage.load();

            storage.counter += msg.increaseBy;
            storage.save();
        }

        ResetCounter => {
            var storage = lazy Storage.load();

            storage.counter = 0;
            storage.save();
        }

        else => {
            assert (in.body.isEmpty()) throw 0xFFFF
        }
    }
}

fun onBouncedMessage(in: InMessageBounced) {
}

get fun currentCounter(): int {
    val storage = lazy Storage.load();
    return storage.counter;
}

get fun initialId(): int {
    val storage = lazy Storage.load();
    return storage.id;
}