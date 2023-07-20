package ca.pfv.spmf.algorithms.sequential_rules.husrm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sequence {
    private List<List<ItemWithUtilityPosition>> Itemsets = new ArrayList<>();
    //private List<ItemWithUtilityPosition> Itemsets = new ArrayList<>();
    double SequenceUtility;
    int id;
    Map<Integer, Integer> ItemFirstPosition = new HashMap<>();
    Map<Integer, ItemWithUtilityPosition> ItemMaxUtility = new HashMap<>();

    public Sequence() {
    }

    public void addItemset(List<ItemWithUtilityPosition> Itemset) {
        Itemsets.add(Itemset);

    }

    public List<List<ItemWithUtilityPosition>> getItemsets() {
        return Itemsets;
    }

    public List<ItemWithUtilityPosition> getItemset(int ItemSetPosition) {
        return Itemsets.get(ItemSetPosition);
    }

    public ItemWithUtilityPosition getItem(int ItemSetPosition, int ItemPosition) {
        return Itemsets.get(ItemSetPosition).get(ItemPosition);
    }

    public int getId() {
        return id;
    }

    public double getSequenceUtility() {
        return SequenceUtility;
    }

    public Map<Integer, Integer> getItemFirstPosition() {
        return ItemFirstPosition;
    }

    public int size() {
        return Itemsets.size();
    }

    public void print() {
        for (List<ItemWithUtilityPosition> Itemset : Itemsets) {
            System.out.print("{");
            for (ItemWithUtilityPosition item : Itemset) {
                item.print();
            }
            System.out.print("}");
        }
    }
}
