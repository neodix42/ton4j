package org.ton.java.tl.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

import java.util.Arrays;


@Builder
@Getter
@Setter
@ToString
public class Text {

    private static final int MaxTextChunkSize = 127 - 2;

    int maxFirstChunkSize;
    String value;

    @ToString.Exclude
    private byte[] val;
    @ToString.Exclude
    private int chunksNum = 1;

    public Cell toCell() {
        if (value.length() == 0) {
            return CellBuilder.beginCell().storeUint(0, 8).endCell();
        }
        if (maxFirstChunkSize > MaxTextChunkSize) {
            throw new Error("too big first chunk size");
        }
        if (maxFirstChunkSize == 0) {
            throw new Error("first chunk size should be greater than 0");
        }

        val = value.getBytes();

        int leftSize = val.length - maxFirstChunkSize;
        chunksNum = 1;

        if (leftSize > 0) {
            chunksNum += leftSize / MaxTextChunkSize;
            if (leftSize % MaxTextChunkSize > 0) {
                chunksNum++;
            }
        }

        if (chunksNum > 255) {
            throw new Error("too big data");
        }

        return CellBuilder.beginCell()
                .storeUint(chunksNum, 8)
                .storeSlice(CellSlice.beginParse(f(0).endCell()))
                .endCell();
    }

    private CellBuilder f(int depth) {
        CellBuilder c = CellBuilder.beginCell();
        int sz = MaxTextChunkSize;
        if (depth == 0) {
            sz = maxFirstChunkSize;
        }
        if (sz > val.length) {
            sz = val.length;
        }
        c.storeUint(sz, 8);
        byte[] textPiece = Arrays.copyOfRange(val, 0, sz);
        c.storeBytes(textPiece, sz * 8);
        val = Arrays.copyOfRange(val, sz, val.length);

        if (depth != chunksNum - 1) {
            c.storeRef(f(depth + 1).endCell());
        }
        return c;
    }

    public static Text deserialize(CellSlice cs) {
        int chunksNum = cs.loadUint(8).intValue();
        int firstSize = 0;
        int lengthOfChunk = 0;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < chunksNum; i++) {
            lengthOfChunk = cs.loadUint(8).intValue();
            if (i == 0) {
                firstSize = lengthOfChunk;
            }
            int[] dataOfChunk = cs.loadBytes(lengthOfChunk * 8);
            result.append(new String(Utils.unsignedBytesToSigned(dataOfChunk)));

            if (i < chunksNum - 1) {
                cs = CellSlice.beginParse(cs.loadRef());
            }
        }
        return Text.builder()
                .maxFirstChunkSize(firstSize)
                .value(result.toString())
                .build();
    }
}
