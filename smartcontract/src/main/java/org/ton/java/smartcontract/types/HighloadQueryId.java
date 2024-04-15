package org.ton.java.smartcontract.types;

public class HighloadQueryId {
    static final int MAX_SHIFT = 8191;
    static final int MAX_BITNUMBER = 1022;
    static final int SHIFT_SIZE = 13;
    static final int BITNUMBER_SIZE = 10;

    int shift;
    int bitNumber;

    public HighloadQueryId() {
        shift = 0;
        bitNumber = 0;
    }

    public static HighloadQueryId fromShiftAndBitNumber(int shift, int bitNumber) throws IllegalArgumentException {
        if (shift < 0) {
            throw new IllegalArgumentException("Shift cannot be less than 0");
        }
        if (shift > MAX_SHIFT) {
            throw new IllegalArgumentException("Shift cannot be greater than " + MAX_SHIFT);
        }
        if (bitNumber < 0) {
            throw new IllegalArgumentException("BitNumber cannot be less than 0");
        }
        if (bitNumber > MAX_BITNUMBER) {
            throw new IllegalArgumentException("BitNumber cannot be greater than " + MAX_BITNUMBER);
        }
        HighloadQueryId qid = new HighloadQueryId();
        qid.shift = shift;
        qid.bitNumber = bitNumber;
        return qid;
    }

    public static HighloadQueryId fromSeqno(int seqno) {
        return fromShiftAndBitNumber(seqno / 1023, seqno % 1023);
    }

    public static HighloadQueryId fromQueryId(int queryId) {
        return fromShiftAndBitNumber(queryId >> BITNUMBER_SIZE, queryId & 1023);
    }

    public int getQueryId() {
        return (shift << BITNUMBER_SIZE) + bitNumber;
    }

    public boolean hasNext() {
        boolean isEnd = bitNumber >= (MAX_BITNUMBER - 1) && shift >= MAX_SHIFT; // last usable queryId is left for emergency withdrawal
        return !isEnd;
    }

    public HighloadQueryId getNext() throws IllegalStateException {
        int newBitnumber = bitNumber + 1;
        int newShift = shift;

        if (newShift >= MAX_SHIFT && newBitnumber > (MAX_BITNUMBER - 1)) {
            throw new IllegalStateException("Overload");
        }

        if (newBitnumber > MAX_BITNUMBER) {
            newBitnumber = 0;
            newShift += 1;
            if (newShift > MAX_SHIFT) {
                throw new IllegalStateException("Overload");
            }
        }

        return HighloadQueryId.fromShiftAndBitNumber(newShift, newBitnumber);
    }

    public int getShift() {
        return shift;
    }

    public int getBitNumber() {
        return bitNumber;
    }

    public int toSeqno() {
        return shift * 1023 + bitNumber;
    }
}
