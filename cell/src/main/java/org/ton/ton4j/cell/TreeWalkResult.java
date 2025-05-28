package org.ton.ton4j.cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TreeWalkResult {

    List<TopologicalOrderArray> topologicalOrderArray;
    HashMap<String, Integer> indexHashmap;

    TreeWalkResult(List<TopologicalOrderArray> topologicalOrderArray, HashMap<String, Integer> indexHashmap) {
        this.topologicalOrderArray = new ArrayList<>(topologicalOrderArray);
        this.indexHashmap = new HashMap<>(indexHashmap);
    }
}
