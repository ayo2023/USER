package ca.pfv.spmf.algorithms.sequential_rules.husrm;

import java.io.*;
import java.util.*;

public class Database {
    private List<Sequence> Sequences = new ArrayList<>();
    private int sequenceID;


    public void loadFile(String path, int maximumNumberOfSequences) throws IOException {
        String thisLine;    // variable to read each line.
        BufferedReader myInput = null;
        try {
            FileInputStream fin = new FileInputStream(new File(path));
            myInput = new BufferedReader(new InputStreamReader(fin));
            // for each line until the end of the file
            int i = 0;
            while ((thisLine = myInput.readLine()) != null) {
                // if the line is not a comment, is not empty or is not other
                // kind of metadata
                if (thisLine.isEmpty() == false &&
                        thisLine.charAt(0) != '#' && thisLine.charAt(0) != '%'
                        && thisLine.charAt(0) != '@') {
                    // split this line according to spaces and process the line
                    String[] split = thisLine.split(" ");
                    addSequence(split, i);
                    i++;
                    // if we reached the maximum number of lines, we stop reading
                    if (i == maximumNumberOfSequences) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                // close the file
                myInput.close();
            }
        }
    }

    public void addSequence(String[] tokens, int id) {
        // This set is used to remember items that we have seen already
        //Set<Integer> alreadySeenItems = new HashSet<Integer>();
        HashMap<Integer, ItemWithUtilityPosition> alreadySeenItems = new HashMap<>();
        Map<Integer, Integer> ItemFirstPosition = new HashMap<>();

        // This variable is to count the utility of items that appear twice in the sequence.
        // The reason why we count this, is that HUSRM does not allow items to appear twice or more in the same
        // sequence. Thus, we have to count the extra utility of items appearing more than once and
        // subtract this utility from the utility of the sequence and remove these extra occurrences
        // of items.
        int profitExtraItemOccurrences = 0;

        // create a new Sequence to store the sequence
        Sequence sequence = new Sequence();
        sequence.id = id;


        // create a list of integers to store the items of the current  itemset.
        List<ItemWithUtilityPosition> itemset = new ArrayList<ItemWithUtilityPosition>();
        // create a list of double values to store the utility of each item in the current itemset
        List<Double> itemsetProfit = new ArrayList<Double>();

        int itemsetCount = 0;
        //HashMap<Integer, Double> ItemMaxUtility = new HashMap<>();
        HashMap<Integer, ItemWithUtilityPosition> ItemMaxUtility = new HashMap<>();
        // for each token in this line
        for (String token : tokens) {

            // if this token is not empty
            if (token.isEmpty()) {
                continue;
            }
            // if the token is -1, it means that we reached the end of an itemset.
            if (token.charAt(0) == 'S') {
                String[] strings = token.split(":");  //
                String exactUtility = strings[1];
                sequence.SequenceUtility = Double.parseDouble(exactUtility) - profitExtraItemOccurrences;
            }
            // if it is the end of an itemset
            else if (token.equals("-1")) {
                itemsetCount++;
                // add the current itemset to the sequence
                sequence.addItemset(itemset);
                // create a new itemset
                itemset = new ArrayList<ItemWithUtilityPosition>();
            }
            // if the token is -2, it means that we reached the end of
            // the sequence.
            else if (token.equals("-2")) {
                sequence.ItemFirstPosition = ItemFirstPosition;
                sequence.ItemMaxUtility = ItemMaxUtility;
                // we add it to the list of sequences
                Sequences.add(sequence);
            } else {
                // otherwise it is an item.
                // we parse it as an integer and add it to
                // the current itemset.

                String[] strings = token.split("\\[");
                String itemString = strings[0];
                int itemInt = Integer.parseInt(itemString);
                String profit = strings[1];
                String profitWithoutBrackets = profit.substring(0, profit.length() - 1);
                double utility = Double.parseDouble(profitWithoutBrackets);
                ItemWithUtilityPosition item = new ItemWithUtilityPosition(itemInt, itemsetCount, utility);

                if (alreadySeenItems.get(itemInt) == null) {
                    //alreadySeenItems.put(itemInt, item);
                    item.PrePosition = -1;
                    ItemFirstPosition.put(itemInt, itemsetCount);
                    ItemMaxUtility.put(itemInt, item);
                } else {
                    alreadySeenItems.get(itemInt).NextPosition = itemsetCount;
                    item.PrePosition = alreadySeenItems.get(itemInt).position();
                    //alreadySeenItems.put(itemInt,item);
                    if (ItemMaxUtility.get(itemInt).utility() < utility) {
                        profitExtraItemOccurrences += ItemMaxUtility.get(itemInt).utility();
                        ItemMaxUtility.put(itemInt, item);
                    } else {
                        profitExtraItemOccurrences += utility;
                    }
                }
                item.NextPosition = -1;
                alreadySeenItems.put(itemInt, item);
                itemset.add(item);
            }
        }
    }

    public List<Sequence> getSequences() {
        return Sequences;
    }
    public Sequence getSequence(int sequenceID){
        return Sequences.get(sequenceID);
    }


    public void print() {
        for (Sequence sequence : Sequences) {
            System.out.print(sequence.getId() + ":  ");
            System.out.print("<");
            sequence.print();
            System.out.print(">");
            System.out.print(" Utility: " + sequence.SequenceUtility);
            System.out.print("\n");
        }
    }


}
