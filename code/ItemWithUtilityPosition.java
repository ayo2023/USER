package ca.pfv.spmf.algorithms.sequential_rules.husrm;

public class ItemWithUtilityPosition {
    private int itemName;
    private double utility;
    int itemPosition;
    int PrePosition;
    int NextPosition;

    public ItemWithUtilityPosition(int item, int position, double utility) {
        this.itemName = item;
        this.itemPosition = position;
        this.utility = utility;
    }

    public double utility() {
        return utility;
    }

    public int position() {
        return itemPosition;
    }

    public Integer getItemName() {
        return itemName;
    }

    public int PrePosition() {
        return PrePosition;
    }

    public int NextPosition() {
        return NextPosition;
    }



    public void print() {
        System.out.print("(" + itemName + ", " + utility + ", " + itemPosition + ")");
    }

    @Override
    public String toString() {
        return "itemInformation ( item: " + itemName + ", utility: " + utility + ", position:" + itemPosition + ")";
    }
}
