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

    }

    public static double calcRuleMaxUtility(ItemWithUtilityPosition AlphaItem, ItemWithUtilityPosition BetaItem, int begin, int end, Sequence sequence) {


        if (AlphaItem.position() < BetaItem.position()) {
            return AlphaItem.utility() + BetaItem.utility();
        } else {
            
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
                
                u3 = 0;
            } else {
                u3 = calcRuleMaxUtility(MidAlphaItem, MidBetaItem, BetaItem.position(), AlphaItem.position(), sequence);
            }
            return Math.max(Math.max(u1, u2), u3);
        }
    }

    
    public static MaxRuleInformation RuleMaxUtilityInformation(ItemWithUtilityPosition AlphaItem, ItemWithUtilityPosition BetaItem, int begin, int end, Sequence sequence) {


        MaxRuleInformation maxRuleInformation=new MaxRuleInformation();

        if (AlphaItem.position() < BetaItem.position()) {
            maxRuleInformation.MaxUtility=AlphaItem.utility() + BetaItem.utility();
            maxRuleInformation.MaxUtilityXPosition.add(AlphaItem.position());
            maxRuleInformation.MaxUtilityYPosition.add(BetaItem.position());
            return maxRuleInformation;
        } else {
            
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


    
    public static ItemWithUtilityPosition MaxUtilityItemBetweenAlphaAndBeta(ItemWithUtilityPosition Item, int begin, int end, Sequence sequence, int direction) {
        ItemWithUtilityPosition FoundItem = new ItemWithUtilityPosition(Item.getItemName(), -1, -1);
        
        if (direction == 1) {
            while (Item.NextPosition <= end && Item.NextPosition!=-1) {   
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
        
        if (direction == 0) {
            
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


