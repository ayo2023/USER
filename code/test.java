package ca.pfv.spmf.algorithms.sequential_rules.husrm;

import java.io.IOException;
import java.util.*;

public class test {
    public void run() throws IOException {
        Database database = new Database();
        database.loadFile("BIBLE_sequence_utility_50.txt", 10000000);
        database.print();
        Sequence s = database.getSequence(2);
        Integer IntAlphaItem = 8;
        Integer IntBetaItem = 53;

        Map<Integer, ItemWithUtilityPosition> ItemMaxUtilityPosition = s.ItemMaxUtility;
        ItemWithUtilityPosition AlphaItem = ItemMaxUtilityPosition.get(IntAlphaItem);
        ItemWithUtilityPosition BetaItem = ItemMaxUtilityPosition.get(IntBetaItem);
        MaxRuleInformation maxUtilityInformation = RuleMaxUtilityInformation(AlphaItem, BetaItem, 0, s.size() - 1, s);
        //System.out.println(1);
         /*
        ItemWithUtilityPosition FirstAlphaItem=FindFirstAlphaItem(IntAlphaItem,s);
        ItemWithUtilityPosition LastBetaItem=FindLastBetaItem(IntBetaItem,s);
        System.out.println(FirstAlphaItem.position());
        System.out.println(LastBetaItem.position());

          */
    }

    public static double calcRuleMaxUtility(ItemWithUtilityPosition AlphaItem, ItemWithUtilityPosition BetaItem, int begin, int end, Sequence sequence) {
        /*
        if (AlphaItem.getItemName()==8 && BetaItem.getItemName()==10){
            System.out.println(1);
        }
        */

        if (AlphaItem.position() < BetaItem.position()) {
            return AlphaItem.utility() + BetaItem.utility();
        } else {
            // 当：beta取到最大值时的位置 < alpha取到最大值的位置
            ItemWithUtilityPosition FoundItem1 = PreItemMaxUtilityBeforeBeta(AlphaItem, begin, BetaItem.position(), sequence);
            ItemWithUtilityPosition FoundItem2 = NextItemMaxUtilityBehindAlpha(BetaItem, AlphaItem.position(), end, sequence);
            double u1=0;
            double u2=0;
            if ( FoundItem1.utility()!= -1) {
                u1 = FoundItem1.utility() + BetaItem.utility();
            }
            if (FoundItem2.utility() != -1) {
                u2 = FoundItem2.utility() + AlphaItem.utility();
            }
            ItemWithUtilityPosition MidAlphaItem = MaxUtilityItemBetweenAlphaAndBeta(AlphaItem, BetaItem.position()+1, AlphaItem.position()-1, sequence, 0);
            ItemWithUtilityPosition MidBetaItem = MaxUtilityItemBetweenAlphaAndBeta(BetaItem, BetaItem.position()+1, AlphaItem.position()-1, sequence, 1);

            double u3;
            if (MidAlphaItem.utility() == -1 || MidBetaItem.utility() == -1) {
                // 只要有一个找不到，那么中间部分就没必要进行递归
                u3 = 0;
            } else {
                u3 = calcRuleMaxUtility(MidAlphaItem, MidBetaItem, BetaItem.position(), AlphaItem.position(), sequence);
            }
            return Math.max(Math.max(u1, u2), u3);
        }
    }

    /*
    public static double[] RuleMaxUtilityInformation(ItemWithUtilityPosition AlphaItem, ItemWithUtilityPosition BetaItem, int begin, int end, Sequence sequence) {

        double[] MaxInformation={-1,-1,-1};

        if (AlphaItem.position() < BetaItem.position()) {
            MaxInformation[0]=AlphaItem.utility() + BetaItem.utility();
            MaxInformation[1]=AlphaItem.position();
            MaxInformation[2]=BetaItem.position();
            return MaxInformation;
        } else {
            // 当：beta取到最大值时的位置 < alpha取到最大值的位置
            ItemWithUtilityPosition FoundItem1 = PreItemMaxUtilityBeforeBeta(AlphaItem, begin, BetaItem.position(), sequence);
            ItemWithUtilityPosition FoundItem2 = NextItemMaxUtilityBehindAlpha(BetaItem, AlphaItem.position(), end, sequence);
            double u1=0;
            double u2=0;
            if ( FoundItem1.utility()!= -1) {
                u1 = FoundItem1.utility() + BetaItem.utility();
            }
            if (FoundItem2.utility() != -1) {
                u2 = FoundItem2.utility() + AlphaItem.utility();
            }
            ItemWithUtilityPosition MidAlphaItem = MaxUtilityItemBetweenAlphaAndBeta(AlphaItem, BetaItem.position() + 1, AlphaItem.position() - 1, sequence, 0);
            ItemWithUtilityPosition MidBetaItem = MaxUtilityItemBetweenAlphaAndBeta(BetaItem, BetaItem.position() + 1, AlphaItem.position() - 1, sequence, 1);

            double[] MiddleInformation = {-1, -1, -1};
            if (MidAlphaItem.utility() == -1 || MidBetaItem.utility() == -1) {
                // 只要有一个找不到，那么中间部分就没必要进行递归
                MiddleInformation[0] = 0;
            } else {
                MiddleInformation = RuleMaxUtilityInformation(MidAlphaItem, MidBetaItem, BetaItem.position(), AlphaItem.position(), sequence);
            }
            if (u1 >= u2 && u1 >= MiddleInformation[0]) {
                MaxInformation[0] = u1;
                MaxInformation[1]=FoundItem1.position();
                MaxInformation[2]=BetaItem.position();
            } else if (u2 > u1 && u2 > MiddleInformation[0]) {
                MaxInformation[0] = u2;
                MaxInformation[1]=AlphaItem.position();
                MaxInformation[2]=FoundItem2.position();
            } else {
                MaxInformation = MiddleInformation;
            }

            return MaxInformation;
        }
    }
    */
    public static MaxRuleInformation RuleMaxUtilityInformation(ItemWithUtilityPosition AlphaItem, ItemWithUtilityPosition BetaItem, int begin, int end, Sequence sequence) {

        /*
        if (AlphaItem.getItemName()==10 && BetaItem.getItemName()==46 &&sequence.id==512) {
            System.out.println(1);
        }

         */

        MaxRuleInformation maxRuleInformation=new MaxRuleInformation();

        if (AlphaItem.position() < BetaItem.position()) {
            maxRuleInformation.MaxUtility=AlphaItem.utility() + BetaItem.utility();
            maxRuleInformation.MaxUtilityXPosition.add(AlphaItem.position());
            maxRuleInformation.MaxUtilityYPosition.add(BetaItem.position());
            return maxRuleInformation;
        } else {
            // 当：beta取到最大值时的位置 < alpha取到最大值的位置
            ItemWithUtilityPosition FoundItem1 = PreItemMaxUtilityBeforeBeta(AlphaItem, begin, BetaItem.position(), sequence);
            ItemWithUtilityPosition FoundItem2 = NextItemMaxUtilityBehindAlpha(BetaItem, AlphaItem.position(), end, sequence);
            double u1=0;
            double u2=0;
            if ( FoundItem1.utility()!= -1) {
                u1 = FoundItem1.utility() + BetaItem.utility();
            }
            if (FoundItem2.utility() != -1) {
                u2 = FoundItem2.utility() + AlphaItem.utility();
            }
            ItemWithUtilityPosition MidAlphaItem = MaxUtilityItemBetweenAlphaAndBeta(AlphaItem, BetaItem.position() + 1, AlphaItem.position() - 1, sequence, 0);
            ItemWithUtilityPosition MidBetaItem = MaxUtilityItemBetweenAlphaAndBeta(BetaItem, BetaItem.position() + 1, AlphaItem.position() - 1, sequence, 1);

            MaxRuleInformation MiddleInformation = new MaxRuleInformation();
            if (MidAlphaItem.utility() == -1 || MidBetaItem.utility() == -1) {
                // 只要有一个找不到，那么中间部分就没必要进行递归
                MiddleInformation.MaxUtility=0;
            } else {
                MiddleInformation = RuleMaxUtilityInformation(MidAlphaItem, MidBetaItem, BetaItem.position(), AlphaItem.position(), sequence);
            }
            if (u1 > u2 && u1 > MiddleInformation.MaxUtility) {
                maxRuleInformation.MaxUtility = u1;
                maxRuleInformation.MaxUtilityXPosition.add(FoundItem1.position());
                maxRuleInformation.MaxUtilityYPosition.add(BetaItem.position());
            } else if (u2 > u1 && u2 > MiddleInformation.MaxUtility) {
                maxRuleInformation.MaxUtility = u2;
                maxRuleInformation.MaxUtilityXPosition.add(AlphaItem.position());
                maxRuleInformation.MaxUtilityYPosition.add(FoundItem2.position());
            }else if (u1==u2 && u1!=0 && u2!=0 && MiddleInformation.MaxUtility<=u1){
                maxRuleInformation.MaxUtility=u1;
                maxRuleInformation.MaxUtilityXPosition.add(FoundItem1.position());
                maxRuleInformation.MaxUtilityYPosition.add(BetaItem.position());
                maxRuleInformation.MaxUtilityXPosition.add(AlphaItem.position());
                maxRuleInformation.MaxUtilityYPosition.add(FoundItem2.position());
            } else {
                maxRuleInformation = MiddleInformation;
            }

            return maxRuleInformation;
        }
    }


    // 修改为：包括begin和end的项集中的项的判断
    public static ItemWithUtilityPosition MaxUtilityItemBetweenAlphaAndBeta(ItemWithUtilityPosition Item, int begin, int end, Sequence sequence, int direction) {
        ItemWithUtilityPosition FoundItem = new ItemWithUtilityPosition(Item.getItemName(), -1, -1);
        // direction=1代表向后找
        if (direction == 1) {
            //往后找，直到end位置
            while (Item.NextPosition <= end && Item.NextPosition!=-1) {   // 修改：增加了等号
                int NextPosition = Item.NextPosition;
                List<ItemWithUtilityPosition> itemset = sequence.getItemset(NextPosition);
                for (ItemWithUtilityPosition item : itemset) {
                    if (item.getItemName().equals(Item.getItemName())) {
                        Item = item;
                        if (FoundItem.utility() < item.utility()) {
                            FoundItem = item;
                        }
                    }
                }
            }
        }
        // direction=0代表向前找
        if (direction == 0) {
            //往前找，直到begin位置
            while (Item.PrePosition >= begin && Item.PrePosition!=-1) {
                int PrePosition = Item.PrePosition;
                List<ItemWithUtilityPosition> itemset = sequence.getItemset(PrePosition);
                for (ItemWithUtilityPosition item : itemset) {
                    if (item.getItemName().equals(Item.getItemName())) {
                        Item = item;
                        if (FoundItem.utility() < item.utility()) {
                            FoundItem = item;
                        }
                    }
                }
            }
        }
        // 如果找不到，效用为-1
        return FoundItem;
    }


    public static ItemWithUtilityPosition PreItemMaxUtilityBeforeBeta(ItemWithUtilityPosition IntItem, int begin, int end, Sequence sequence) {
        ItemWithUtilityPosition FoundItem = new ItemWithUtilityPosition(IntItem.getItemName(), -1, -1);
        while (IntItem.PrePosition != -1 && IntItem.position() >= begin) {
            int PrePosition = IntItem.PrePosition;
            List<ItemWithUtilityPosition> itemset = sequence.getItemset(PrePosition);
            for (ItemWithUtilityPosition item : itemset) {
                if (item.getItemName().equals(IntItem.getItemName())) {
                    IntItem = item;
                    if (item.position() >= end) {
                        continue;
                    }
                    if (FoundItem.utility() < item.utility()) {
                        FoundItem = item;
                    }
                }
            }
        }
        return FoundItem;
    }


    public static ItemWithUtilityPosition NextItemMaxUtilityBehindAlpha(ItemWithUtilityPosition IntItem, int begin, int end, Sequence sequence) {
        ItemWithUtilityPosition FoundItem = new ItemWithUtilityPosition(IntItem.getItemName(), -1, -1);
        while (IntItem.NextPosition != -1 && IntItem.position() <= end) {
            int NextPosition = IntItem.NextPosition;
            List<ItemWithUtilityPosition> itemset = sequence.getItemset(NextPosition);
            for (ItemWithUtilityPosition item : itemset) {
                if (item.getItemName().equals(IntItem.getItemName())) {
                    IntItem = item;
                    if (item.position() <= begin) {
                        continue;
                    }
                    if (FoundItem.utility() < item.utility()) {
                        FoundItem = item;
                    }
                }
            }
        }
        return FoundItem;
    }

    public ItemWithUtilityPosition FindFirstAlphaItem(Integer IntItem, Sequence sequence) {
        ItemWithUtilityPosition FindFirstAlpha=null;
        Integer FirstPosition = sequence.ItemFirstPosition.get(IntItem);
        if(FirstPosition==null){
            return null;
        }
        List<ItemWithUtilityPosition> ItemSet = sequence.getItemset(FirstPosition);
        for (ItemWithUtilityPosition Item : ItemSet) {
            if (Item.getItemName().equals(IntItem)) {
                FindFirstAlpha= Item;
            }
        }
        return FindFirstAlpha;
    }

    public ItemWithUtilityPosition FindLastBetaItem(Integer IntItem, Sequence sequence) {
        ItemWithUtilityPosition LastBetaItem = null;
        Integer Position=sequence.ItemFirstPosition.get(IntItem);
        if (Position==null){
            return null;
        }
        while (Position!=-1) {
            List<ItemWithUtilityPosition> ItemSet = sequence.getItemset(Position);
            for (ItemWithUtilityPosition Item : ItemSet) {
                if(Item.getItemName().equals(IntItem)){
                    if(Item.NextPosition==-1) {
                        LastBetaItem=Item;
                    }
                    Position = Item.NextPosition;
                }
            }
        }
        return LastBetaItem;
    }

    public static void main(String[] arg) throws IOException {
        test test = new test();
        test.run();
    }

    public static class MaxRuleInformation{
        double MaxUtility=0;
        List<Integer> MaxUtilityXPosition=new ArrayList<>();
        List<Integer> MaxUtilityYPosition=new ArrayList<>();
    }
}


