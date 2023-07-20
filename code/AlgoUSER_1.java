package ca.pfv.spmf.algorithms.sequential_rules.husrm;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;


public class AlgoUSER_1 {
    // for statistics //
    /**
     * start time of latest execution
     */
    long timeStart = 0;
    /**
     * end time of latest execution
     */
    long timeEnd = 0;
    /**
     * number of rules generated
     */
    int ruleCount;

    // parameters ***/
    /**
     * minimum confidence
     **/
    double minConfidence;

    /**
     * minimum support
     */
    double minutil;

    /**
     * this is the sequence database
     */
    Database database;

    /**
     * this buffered writer is used to write the output file
     */
    BufferedWriter writer = null;

    /**
     * this is a map where the KEY is an item and the VALUE is the list of sequences
     * /* containing the item.
     */
    private Map<Integer, ListSequenceIDs> mapItemSequences;

    /**
     * this variable is used to activate the debug mode.  When this mode is activated
     * /* some additional information about the algorithm will be shown in the console for
     * /* debugging
     **/
    final boolean DEBUG = false;

    /**
     * this is a contrainst on the maximum number of item that the left side of a rule should
     * /* contain
     */
    private int maxSizeAntecedent;

    /**
     * this is a contrainst on the maximum number of item that the right side of a rule should
     * /* contain
     */
    private int maxSizeConsequent;

    ////// ================ STRATEGIES ===============================
    // Various strategies have been used to improve the performance of HUSRM.
    // The following boolean values are used to deactivate these strategies.

    /**
     * Strategy 1: remove items with a sequence estimated utility < minutil
     */
    private boolean deactivateStrategy1 = false;

    /**
     * Strategy 2: remove rules contains two items a--> b with a sequence estimated utility < minutil
     */
    private boolean deactivateStrategy2 = false;

    /**
     * Strategy 3 use bitvectors instead of array list for quickly calculating the support of
     * /*  rule antecedent
     */
    private boolean deactivateStrategy3 = false;

    /**
     * Strategy 4 :  utilize the sum of the utility of lutil, lrutil and rutil
     * /* If deactivated, we use the same utility tables, but the upper bound will be calculated as
     * /*  lutil + lrutil + rutil instead of the better upper bounds described in the paper
     */
    private boolean deactivateStrategy4 = false;


    /**
     * Default constructor
     */
    public AlgoUSER_1() {
    }

    /**
     * This is a structure to store some estimated utility and a list of sequence ids.
     * It will be use in the code for storing the estimated utility of a rule and the list
     * of sequence ids where the rule appears.
     */
    public class EstimatedUtilityAndSequences {
        // an estimated profit value
        Double utility = 0d;
        // a list of sequence ids
        List<Integer> sequenceIds = new ArrayList<Integer>();
    }

    public class UtilityAndPostionAndSequenceId {
        double utility;
        int AlphaPosition;
        int BetaPosition;

        List<Integer> MaxUtilityXPosition = new ArrayList<>();
        List<Integer> MaxUtilityYPosition = new ArrayList<>();

        int LastBetaItemsetPosition;
        int SequenceId;
    }

    public class EstimatedUtilityAndInformationList {
        double EstimatedUtility;
        List<UtilityAndPostionAndSequenceId> ListRuleInformation;
    }


    /**
     * The main method to run the algorithm
     *
     * @param input                    an input file
     * @param output                   an output file
     * @param minConfidence            the minimum confidence threshold
     * @param minutil                  the minimum utility threshold
     * @param maxConsequentSize        a constraint on the maximum number of items that the right side of a rule should contain
     * @param maxAntecedentSize        a constraint on the maximum number of items that the left side of a rule should contain
     * @param maximumNumberOfSequences the maximum number of sequences to be used
     * @throws IOException if error reading/writing files
     */
    //@SuppressWarnings("unused")
    public void runAlgorithm(String input, String output,
                             double minConfidence, double minutil, int maxAntecedentSize, int maxConsequentSize,
                             int maximumNumberOfSequences) throws IOException {

        // save the minimum confidence parameter
        this.minConfidence = minConfidence;

        // save the constraints on the maximum size of left/right side of the rules
        this.maxSizeAntecedent = maxAntecedentSize;
        this.maxSizeConsequent = maxConsequentSize;

        // reinitialize the number of rules found
        ruleCount = 0;
        this.minutil = minutil;

        // if the database was not loaded, then load it.
        if (database == null) {
            try {
                database = new Database();
                database.loadFile(input, maximumNumberOfSequences);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // if in debug mode, we print the database to the console
        if (DEBUG) {
            database.print();
        }

        // We reset the tool for calculating the maximum memory usage
        MemoryLogger.getInstance().reset();

        // we prepare the object for writing the output file
        writer = new BufferedWriter(new FileWriter(output));

        // if minutil is 0, set it to 1 to avoid generating
        // all rules
        this.minutil = minutil;
        if (this.minutil == 0) {
            this.minutil = 0.001;
        }

        // save the start time
        timeStart = System.currentTimeMillis(); // for stats

        // FIRST STEP: We will calculate the estimated profit of each single item

        // if this strategy has not been deactivated
        if (deactivateStrategy1 == false) {
            // This map will store pairs of (key: item   value: estimated profit of the item)
            Map<Integer, Double> mapItemEstimatedUtility = new HashMap<Integer, Double>();

            // We read the database.
            // For each sequence
            for (Sequence sequence : database.getSequences()) {


                // for each itemset in that sequence
                for (List<ItemWithUtilityPosition> itemset : sequence.getItemsets()) {

                    // for each item
                    for (ItemWithUtilityPosition item : itemset) {

                        if (item.PrePosition != -1) {
                            continue;
                        }
                        // get the current sequence estimated utility of that item
                        Double estimatedUtility = mapItemEstimatedUtility.get(item.getItemName());

                        // if we did not see that item yet
                        if (estimatedUtility == null) {
                            // then its estimated utility of that item until now is the
                            // utility of that sequence
                            // estimatedUtility = sequence.ItemMaxUtility.get(item.getItemName()).utility();
                            estimatedUtility = sequence.SequenceUtility;

                        } else {
                            // otherwise, it is not the first time that we saw that item
                            // so we add the utility of that sequence to the sequence
                            // estimated utility f that item
                            //estimatedUtility = estimatedUtility + sequence.ItemMaxUtility.get(item.getItemName()).utility();

                            estimatedUtility = estimatedUtility + sequence.SequenceUtility;
                        }

                        // update the estimated utility of that item in the map
                        mapItemEstimatedUtility.put(item.getItemName(), estimatedUtility);

                    }
                }
            }

            // if we are in debug mode, we will print the calculated estimated utility
            // of all items to the console for easy debugging
            if (DEBUG) {
                System.out
                        .println("==============================================================================");
                System.out
                        .println("--------------------ESTIMATED UTILITY OF ITEMS -----------------------------------");
                System.out
                        .println("==============================================================================");
                System.out.println(" ");

                // for each entry in the map
                for (Entry<Integer, Double> entreeMap : mapItemEstimatedUtility.entrySet()) {
                    // we print the item and its estimated utility
                    System.out.println("item : " + entreeMap.getKey()
                            + " profit estime: " + entreeMap.getValue());
                }


                // NEXT STEP: WE WILL REMOVE THE UNPROMISING ITEMS

                System.out
                        .println("==============================================================================");
                System.out
                        .println("-------------------ESTIMATED UTILITY OF PROMISING ITEMS      ----------------");
                System.out
                        .println("==============================================================================");
            }


            // we create an iterator to loop over all items
            Iterator<Entry<Integer, Double>> iterator = mapItemEstimatedUtility.entrySet().iterator();
            // for each item
            while (iterator.hasNext()) {

                // we obtain the entry in the map
                Entry<Integer, Double> entryMapItemEstimatedUtility
                        = (Entry<Integer, Double>) iterator.next();
                Double estimatedUtility = entryMapItemEstimatedUtility.getValue();

                // if the estimated utility of the current item is less than minutil
                if (estimatedUtility < minutil) {

                    // we remove the item from the map
                    iterator.remove();
                }
            }


            // if the debug mode is activated
            if (DEBUG) {
                // we will print all the promising items

                // we loop over the entries of the map
                for (Entry<Integer, Double> entreeMap : mapItemEstimatedUtility.entrySet()) {
                    // we print the item and its estimated utility
                    System.out.println("item : " + entreeMap.getKey()
                            + " profit estime: " + entreeMap.getValue());
                }

                System.out
                        .println("==============================================================================");
                System.out
                        .println("-------------- DATABASE WITH ONLY ITEMS HAVING ESTIMATED UTILITY >= miinutil-------------");
                System.out
                        .println("==============================================================================");

            }

            // NEXT STEP: WE REMOVE UNPROMISING ITEMS FROM THE SEQUENCES
            // (PREVIOUSLY WE HAD ONLY REMOVED THEM FROM THE MAP).


            // So we scan the database again.
            // For each sequence
            Iterator<Sequence> iteratorSequence = database.getSequences().iterator();
            while (iteratorSequence.hasNext()) {
                boolean flag = true;
                int position = 0;
                List<Integer> RemoveItemsetPosition = new ArrayList<>();
                Sequence sequence = iteratorSequence.next();

                //For each itemset
                Iterator<List<ItemWithUtilityPosition>> iteratorItemset = sequence.getItemsets().iterator();
                // Iterator<List<Double>> iteratorItemsetUtilities = sequence.getSequenceUtility().iterator();
                while (iteratorItemset.hasNext()) {
                    // the items in that itemset
                    List<ItemWithUtilityPosition> itemset = iteratorItemset.next();
                    // the utility values in that itemset
                    // List<Double> itemsetUtilities = iteratorItemsetUtilities.next();

                    // Create an iterator over each item in that itemset
                    Iterator<ItemWithUtilityPosition> iteratorItem = itemset.iterator();
                    // Create an iterator over utility values in that itemset
                    // Iterator<Double> iteratorItemUtility = itemsetUtilities.iterator();

                    // For each item
                    while (iteratorItem.hasNext()) {
                        // get the item
                        Integer item = iteratorItem.next().getItemName();
                        // get its utility value
                        // Double utility = iteratorItemUtility.next();

                        // if the item is unpromising
                        if (mapItemEstimatedUtility.get(item) == null) {

                            // remove the item
                            iteratorItem.remove();
                            // remove its utility value
                            // iteratorItemUtility.remove();
                            // subtract the item utility value from the sequence utility.
                            if (flag) {
                                sequence.SequenceUtility -= sequence.ItemMaxUtility.get(item).utility();
                                flag = false;
                            }
                        }
                    }

                    // If the itemset has become empty, we remove it from the sequence
                    if (itemset.isEmpty()) {
                        iteratorItemset.remove();
                        // iteratorItemsetUtilities.remove();
                        RemoveItemsetPosition.add(position);
                    }
                    position++;
                }

                // If the sequence has become empty, we remove the sequences from the database
                if (sequence.size() == 0) {
                    iteratorSequence.remove();
                } else if (!RemoveItemsetPosition.isEmpty()) {

                    iteratorItemset = sequence.getItemsets().iterator();
                    while (iteratorItemset.hasNext()) {
                        List<ItemWithUtilityPosition> itemset = iteratorItemset.next();
                        Iterator<ItemWithUtilityPosition> iteratorItem = itemset.iterator();
                        while (iteratorItem.hasNext()) {
                            ItemWithUtilityPosition item = iteratorItem.next();
                            if (item.position() <= RemoveItemsetPosition.get(0)) {
                                if (item.NextPosition > RemoveItemsetPosition.get(0)) {
                                    int RemoveNextPositionNum = 0;
                                    for (int RemovePosition : RemoveItemsetPosition) {
                                        if (item.NextPosition > RemovePosition) {
                                            RemoveNextPositionNum++;
                                        }
                                    }
                                    item.NextPosition -= RemoveNextPositionNum;
                                }
                            } else {
                                int RemovePositionNum = 0, RemovePrePositionNum = 0, RemoveNextPositionNum = 0;

                                for (int RemovePosition : RemoveItemsetPosition) {
                                    if (item.position() > RemovePosition) {
                                        //item.itemPosition -= 1;
                                        RemovePositionNum++;
                                    }
                                    if (item.PrePosition > RemovePosition) {
                                        //item.PrePosition -= 1;
                                        RemovePrePositionNum++;
                                    }
                                    if (item.NextPosition > RemovePosition) {
                                        //item.NextPosition -= 1;
                                        RemoveNextPositionNum++;
                                    }
                                }
                                item.itemPosition -= RemovePositionNum;
                                item.PrePosition -= RemovePrePositionNum;
                                item.NextPosition -= RemoveNextPositionNum;

                                //ItemWithUtilityPosition tmpItem =sequence.ItemMaxUtility.get(item.getItemName());
                                //sequence.ItemMaxUtility.get(item.getItemName()).itemPosition-=RemovePositionNum;

                                //int tmpPosition=sequence.ItemFirstPosition.get(item.getItemName());
                                //tmpPosition-=RemovePositionNum;
                                if (sequence.ItemMaxUtility.get(item.getItemName()).utility() == item.utility()) {
                                    sequence.ItemMaxUtility.put(item.getItemName(), item);
                                }
                                if (item.PrePosition == -1) {
                                    sequence.ItemFirstPosition.put(item.getItemName(), item.itemPosition);
                                }
                            }
                        }
                    }
                }
            }
        }


        // if we are in debug mode
        if (DEBUG) {
            // print the database without the unpromising items
            database.print();

            System.out
                    .println("==============================================================================");
            System.out
                    .println("----- FOR EACH ITEM, REMEMBER THE IDS OF SEQUENCES CONTAINING THE ITEM  -------");
            System.out
                    .println("==============================================================================");

        }

        // We create a map to store for each item, the list of sequences containing the item
        // Key: an item   Value:  the list of sequences containing the item
        mapItemSequences = new HashMap<Integer, ListSequenceIDs>();

        // For each sequence
        for (int i = 0; i < database.getSequences().size(); i++) {
            Sequence sequence = database.getSequences().get(i);

            // For each itemset
            for (List<ItemWithUtilityPosition> itemset : sequence.getItemsets()) {

                // For each item
                for (ItemWithUtilityPosition item : itemset) {
                    // Get the list of identifiers of sequence containing that item
                    ListSequenceIDs numerosSequenceItem = mapItemSequences.get(item.getItemName());

                    // If the list does not exist, we will create it
                    if (numerosSequenceItem == null) {
                        // if the user desactivated strategy 3, we will use an arraylist implementation
                        if (deactivateStrategy3) {
                            numerosSequenceItem = new ListSequenceIDsArrayList();
                        } else {
                            // otherwise we use a bitvector implementation, which is more efficient
                            numerosSequenceItem = new ListSequenceIDsBitVector();
                        }
                        // we add the list in the map for that item
                        mapItemSequences.put(item.getItemName(), numerosSequenceItem);
                    }
                    // finally we add the current sequence ids to the list of sequences ids of the current
                    // item
                    numerosSequenceItem.addSequenceID(i);
                }
            }
        }

        // if we are in debug mode
        if (DEBUG) {
            // We will print the map which will show the list of sequence identifiers
            // for each item.
            for (Entry<Integer, ListSequenceIDs> entree : mapItemSequences.entrySet()) {
                System.out.println("Item : " + entree.getKey() + " Sequences : " + entree.getValue());
            }

            System.out
                    .println("==============================================================================");
            System.out
                    .println("----- CALCULATE SEQUENCE ESTIMATED UTILITY OF EACH RULE OF SIZE 2 -------------");
            System.out
                    .println("==============================================================================");
        }

        // We create a map of map to store the estimated utility and list of sequences ids for
        // each rule of two items (e.g. a -> b  ).
        // The key of the first map: the item "a" in the left side of the rule
        // The key of the second map:  the item "b" in the right side of the rule
        // The value in the second map:  the estimated utility of the rule and sequence ids for that rule
        Map<Integer, Map<Integer, EstimatedUtilityAndSequences>> mapItemItemEstimatedUtility = new HashMap<Integer, Map<Integer, EstimatedUtilityAndSequences>>();

        Map<Integer, Map<Integer, List<UtilityAndPostionAndSequenceId>>> mapAlphaBeta = new HashMap<>();
        Map<Integer, Map<Integer, Double>> AlphaBetaEstimatedUtility = new HashMap<>();
        //Map<Integer, Map<Integer, EstimatedUtilityAndInformationList>> mapAlphaBeta = new HashMap<>();

        //Map<Integer,List<UtilityAndPostionAndSequenceId>> mapBeta=new HashMap<>();


        // For each sequence
        for (int z = 0; z < database.getSequences().size(); z++) {
            Map<Integer, Map<Integer, Boolean>> mapXY = new HashMap<>();
            Sequence sequence = database.getSequences().get(z);

            // For each itemset I
            for (int i = 0; i < sequence.getItemsets().size(); i++) {

                // get the itemset
                List<ItemWithUtilityPosition> itemset = sequence.getItemsets().get(i);


                // For each item  X
                for (int j = 0; j < itemset.size(); j++) {
                    ItemWithUtilityPosition itemX = itemset.get(j);
                    // SI X N'A PAS DEJA ETE VU
                    //List<UtilityAndPostionAndSequenceId> ListRuleInformation = new ArrayList<>();
                    //EstimatedUtilityAndInformationList RuleInformation=new EstimatedUtilityAndInformationList();

                    // For each item Y occuring after X,
                    // that is in the itemsets I+1, I+2 ....
                    for (int k = i + 1; k < sequence.getItemsets().size(); k++) {
                        //  for a given itemset K
                        List<ItemWithUtilityPosition> itemsetK = sequence.getItemsets().get(k);
                        // for an item Y
                        for (ItemWithUtilityPosition itemY : itemsetK) {
                            if (itemX.getItemName().equals(itemY.getItemName())) {
                                continue;
                            }

                            boolean XYHasBeenSeem = false;
                            if (mapXY.containsKey(itemX.getItemName())) {
                                Map<Integer, Boolean> Y = mapXY.get(itemX.getItemName());
                                if (Y.containsKey(itemY.getItemName())) {
                                    XYHasBeenSeem = true;
                                } else {
                                    Y.put(itemY.getItemName(), true);
                                }
                            } else {
                                Map<Integer, Boolean> Y = new HashMap<>();
                                Y.put(itemY.getItemName(), true);
                                mapXY.put(itemX.getItemName(), Y);
                            }


                            if (XYHasBeenSeem) {
                                continue;
                            }


                            Map<Integer, EstimatedUtilityAndSequences> mapXItemUtility = mapItemItemEstimatedUtility.get(itemX.getItemName());

                            Map<Integer, List<UtilityAndPostionAndSequenceId>> mapBeta = mapAlphaBeta.get(itemX.getItemName());

                            Map<Integer, Double> BetaEstimatedUtility = AlphaBetaEstimatedUtility.get(itemX.getItemName());

                            // Map<Integer, EstimatedUtilityAndInformationList> mapBeta = mapAlphaBeta.get(itemX.getItemName());
                            List<UtilityAndPostionAndSequenceId> InformationList = new ArrayList<>();
                            UtilityAndPostionAndSequenceId RuleInformation;
                            if (mapBeta == null) {
                                mapBeta = new HashMap<>();
                                mapAlphaBeta.put(itemX.getItemName(), mapBeta);
                            } else {
                                InformationList = mapBeta.get(itemY.getItemName());
                                if (InformationList == null) {
                                    InformationList = new ArrayList<>();
                                }
                            }


                            RuleInformation = new UtilityAndPostionAndSequenceId();

                            ItemWithUtilityPosition MaxUtilityItemX = sequence.ItemMaxUtility.get(itemX.getItemName());
                            ItemWithUtilityPosition MaxUtilityItemY = sequence.ItemMaxUtility.get(itemY.getItemName());
                            //RuleInformation.utility

                            ItemWithUtilityPosition tmpMaxUtilityItemY = MaxUtilityItemY;
                            while (tmpMaxUtilityItemY.NextPosition != -1) {
                                int NextPosition = tmpMaxUtilityItemY.NextPosition;
                                List<ItemWithUtilityPosition> itemsetY = sequence.getItemset(NextPosition);
                                for (ItemWithUtilityPosition item : itemsetY) {
                                    if (item.getItemName().equals(tmpMaxUtilityItemY.getItemName())) {
                                        tmpMaxUtilityItemY = item;
                                        if (item.utility() == MaxUtilityItemY.utility()) {
                                            MaxUtilityItemY = item;
                                        }
                                    }
                                }
                            }


                            test.MaxRuleInformation maxRuleInformation = new test.MaxRuleInformation();

                            // ROOR Pruning Strategy
                            maxRuleInformation = test.RuleMaxUtilityInformation(MaxUtilityItemX, MaxUtilityItemY, 0, sequence.size() - 1, sequence);

                            //System.out.println(1);
                            RuleInformation.utility = maxRuleInformation.MaxUtility;
                            RuleInformation.MaxUtilityXPosition = maxRuleInformation.MaxUtilityXPosition;
                            RuleInformation.MaxUtilityYPosition = maxRuleInformation.MaxUtilityYPosition;


                            while (itemY.NextPosition != -1) {
                                int NextPosition = itemY.NextPosition;
                                List<ItemWithUtilityPosition> ItemSet = sequence.getItemset(NextPosition);
                                for (ItemWithUtilityPosition Item : ItemSet) {
                                    if (Item.getItemName().equals(itemY.getItemName())) {
                                        itemY = Item;
                                    }
                                }
                            }
                            RuleInformation.LastBetaItemsetPosition = itemY.position();

                            RuleInformation.SequenceId = z;
                            InformationList.add(RuleInformation);
                            mapBeta.put(itemY.getItemName(), InformationList);

                            if (BetaEstimatedUtility == null) {
                                BetaEstimatedUtility = new HashMap<>();
                                BetaEstimatedUtility.put(itemY.getItemName(), sequence.SequenceUtility);

                            } else {
                                Double EstimatedUtility = BetaEstimatedUtility.get(itemY.getItemName());
                                if (EstimatedUtility == null) {
                                    EstimatedUtility = sequence.SequenceUtility;

                                } else {
                                    EstimatedUtility = EstimatedUtility + sequence.SequenceUtility;
                                }
                                BetaEstimatedUtility.put(itemY.getItemName(), EstimatedUtility);
                            }
                            AlphaBetaEstimatedUtility.put(itemX.getItemName(), BetaEstimatedUtility);

                        }
                    }
                }
            }
        }

        // if in debuging mode
        if (DEBUG) {
            // we will print the estimated utility and list of sequences ids of all rules containing two items
            // e.g.   "a" -> "b"

            for (Entry<Integer, Map<Integer, List<UtilityAndPostionAndSequenceId>>> entreeX : mapAlphaBeta.entrySet()) {
                int itemX = entreeX.getKey();
                for (Entry<Integer, List<UtilityAndPostionAndSequenceId>> entryYInformation : entreeX.getValue().entrySet()) {
                    int itemY = entryYInformation.getKey();
                    System.out.print("Rule: {" + itemX + "} => {" + itemY + "}");
                    System.out.print("  EstimatedUtility: " + AlphaBetaEstimatedUtility.get(itemX).get(itemY));
                    List<UtilityAndPostionAndSequenceId> ListStructureXY = entryYInformation.getValue();
                    Map<Integer, Integer> SameSequenceId = new HashMap<>();

                    for (UtilityAndPostionAndSequenceId StructureXY : ListStructureXY) {
                        if (SameSequenceId.get(StructureXY.SequenceId) == null) {
                            SameSequenceId.put(StructureXY.SequenceId, 1);
                            System.out.print("\n    sequence id: " + StructureXY.SequenceId + "  (alpha: " + StructureXY.MaxUtilityXPosition + ", beta: " + StructureXY.MaxUtilityYPosition + ")");
                        } else {
                            System.out.print("  (alpha: " + StructureXY.MaxUtilityXPosition + ", beta: " + StructureXY.MaxUtilityYPosition + ")");
                        }
                    }
                    System.out.print("\n");
                }
            }


            System.out
                    .println("==============================================================================");
            System.out
                    .println("-------------- RULES OF SIZE 2 WITH ESTIMATED UTILITY >= minutil -------------");
            System.out
                    .println("==============================================================================");
        }


        for (Entry<Integer, Map<Integer, List<UtilityAndPostionAndSequenceId>>> mapI : mapAlphaBeta.entrySet()) {
            Iterator<Entry<Integer, List<UtilityAndPostionAndSequenceId>>> iterEntry = mapI.getValue().entrySet().iterator();
            while (iterEntry.hasNext()) {
                Entry<Integer, List<UtilityAndPostionAndSequenceId>> entry = iterEntry.next();
                Integer itemX = mapI.getKey();
                Integer itemY = entry.getKey();
                if (AlphaBetaEstimatedUtility.get(itemX).get(itemY) < minutil) {
                    if (deactivateStrategy2 == false) {
                        iterEntry.remove();
                    }
                }
            }
        }

        // If in debug mode
        if (DEBUG) {
            // We will print the remaining rules

            // we will print the estimated utility and list of sequences ids of all rules containing two items
            // e.g.   "a" -> "b"

            for (Entry<Integer, Map<Integer, List<UtilityAndPostionAndSequenceId>>> entreeX : mapAlphaBeta.entrySet()) {
                int itemX = entreeX.getKey();
                for (Entry<Integer, List<UtilityAndPostionAndSequenceId>> entryYInformation : entreeX.getValue().entrySet()) {
                    int itemY = entryYInformation.getKey();
                    System.out.print("Rule: {" + itemX + "} => {" + itemY + "}");
                    System.out.print("  EstimatedUtility: " + AlphaBetaEstimatedUtility.get(itemX).get(itemY));
                    List<UtilityAndPostionAndSequenceId> ListStructureXY = entryYInformation.getValue();
                    Map<Integer, Integer> SameSequenceId = new HashMap<>();

                    for (UtilityAndPostionAndSequenceId StructureXY : ListStructureXY) {
                        if (SameSequenceId.get(StructureXY.SequenceId) == null) {
                            SameSequenceId.put(StructureXY.SequenceId, 1);
                            System.out.print("\n    sequence id: " + StructureXY.SequenceId + "  (alpha: " + StructureXY.MaxUtilityXPosition + ", beta: " + StructureXY.MaxUtilityYPosition + ")");
                        } else {
                            System.out.print("  (alpha: " + StructureXY.MaxUtilityXPosition + ", beta: " + StructureXY.MaxUtilityYPosition + ")");
                        }
                    }
                    System.out.print("\n");
                }
            }

            System.out
                    .println("==============================================================================");
            System.out
                    .println("-------------- RULES OF SIZE 2 WITH UTILITY >= minutil -------------");
            System.out
                    .println("==============================================================================");
        }


        for (Entry<Integer, Map<Integer, List<UtilityAndPostionAndSequenceId>>> entryX : mapAlphaBeta.entrySet()) {
            // Get the item X
            Integer ItemX = entryX.getKey();

            // Get the list of sequence ids containing the item X
            ListSequenceIDs sequenceIDsX = mapItemSequences.get(ItemX);

            List<Integer> ListMaxUtilityXPosition = new ArrayList<>();
            List<Integer> ListMaxUtilityYPosition = new ArrayList<>();

            // Get the support of item X
            double supportX = sequenceIDsX.getSize();

            double supportXY = 0;
            Map<Integer, Integer> mapSequenceIDItemXPosition = new HashMap<>();

            // For each Y
            for (Entry<Integer, List<UtilityAndPostionAndSequenceId>> entryYUtility : entryX.getValue().entrySet()) {
                Integer ItemY = entryYUtility.getKey();
                List<UtilityAndPostionAndSequenceId> ListStructureXY = entryYUtility.getValue();

                UtilityTable table = new UtilityTable();

                for (UtilityAndPostionAndSequenceId StructureXY : ListStructureXY) {

                    int SequenceId = StructureXY.SequenceId;


                    Sequence sequence = database.getSequences().get(SequenceId);
                    // Create a new element in the table
                    ElementOfTable element = new ElementOfTable(SequenceId);

                    // int ItemXPosition = StructureXY.AlphaPosition;

                    int ItemXPosition = sequence.ItemFirstPosition.get(ItemX);

                    int ItemYPosition = StructureXY.LastBetaItemsetPosition;


                    element.positionAlphaItemset = ItemXPosition;
                    element.positionBetaItemset = ItemYPosition;

                    element.ListMaxUtilityXPosition = StructureXY.MaxUtilityXPosition;
                    element.ListMaxUtilityYPosition = StructureXY.MaxUtilityYPosition;


                    element.utility = StructureXY.utility;

                    // Before Alpha itemset
                    HashMap<Integer, Double> LeftExpandItemMaxUtility = new HashMap<>();
                    for (int i = 0; i < ItemXPosition; i++) {

                        List<ItemWithUtilityPosition> Itemset = sequence.getItemset(i);
                        for (int j = Itemset.size() - 1; j >= 0; j--) {
                            ItemWithUtilityPosition Item = Itemset.get(j);
                            if (Item.getItemName() <= ItemX) {
                                break;
                            }
                            if (!Item.getItemName().equals(ItemY)) {
                                if (Item.PrePosition == -1 && (Item.NextPosition >= ItemYPosition || Item.NextPosition == -1)) {
                                    element.utilityLeft += Item.utility();
                                } else {
                                    if (!LeftExpandItemMaxUtility.containsKey(Item.getItemName())) {
                                        LeftExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    } else if (LeftExpandItemMaxUtility.get(Item.getItemName()) < Item.utility()) {
                                        LeftExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    }
                                }
                            }
                        }
                    }

                    // in Alpha itemset
                    List<ItemWithUtilityPosition> XItemset = sequence.getItemset(ItemXPosition);
                    for (int i = XItemset.size() - 1; i >= 0; i--) {
                        ItemWithUtilityPosition Item = XItemset.get(i);
                        if (Item.getItemName().equals(ItemX)) {
                            break;
                        }
                        if (!Item.getItemName().equals(ItemY)) {
                            if (Item.PrePosition == -1 && (Item.NextPosition >= ItemYPosition || Item.NextPosition == -1)) {
                                element.utilityLeft += Item.utility();
                            } else {
                                if (!LeftExpandItemMaxUtility.containsKey(Item.getItemName())) {
                                    LeftExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                } else if (LeftExpandItemMaxUtility.get(Item.getItemName()) < Item.utility()) {
                                    LeftExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                }
                            }
                        }
                    }

                    // After the Beta itemset
                    HashMap<Integer, Double> RightExpandItemMaxUtility = new HashMap<>();

                    for (int i = sequence.size() - 1; i > ItemYPosition; i--) {
                        List<ItemWithUtilityPosition> Itemset = sequence.getItemset(i);
                        for (int j = Itemset.size() - 1; j >= 0; j--) {
                            ItemWithUtilityPosition Item = Itemset.get(j);
                            if (Item.getItemName() <= ItemY) {
                                break;
                            }
                            if (!Item.getItemName().equals(ItemX)) {
                                if (Item.PrePosition <= ItemXPosition && Item.NextPosition == -1) {
                                    element.utilityRight += Item.utility();
                                } else {
                                    if (!RightExpandItemMaxUtility.containsKey(Item.getItemName())) {
                                        RightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    } else if (RightExpandItemMaxUtility.get(Item.getItemName()) < Item.utility()) {
                                        RightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    }
                                }
                            }
                        }
                    }


                    // In the Beta itemset
                    List<ItemWithUtilityPosition> YItemset = sequence.getItemset(ItemYPosition);
                    for (int i = YItemset.size() - 1; i >= 0; i--) {
                        ItemWithUtilityPosition Item = YItemset.get(i);
                        if (Item.getItemName().equals(ItemY)) {
                            break;
                        }
                        if (!Item.getItemName().equals(ItemX)) {
                            if (Item.PrePosition <= ItemXPosition && Item.NextPosition == -1) {
                                element.utilityRight += Item.utility();
                            } else {
                                if (!RightExpandItemMaxUtility.containsKey(Item.getItemName())) {
                                    RightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                } else if (RightExpandItemMaxUtility.get(Item.getItemName()) < Item.utility()) {
                                    RightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                }
                            }
                        }

                    }


                    // area II
                    HashMap<Integer, Double> LeftRightExpandItemMaxUtility = new HashMap<>();

                    for (int i = ItemXPosition + 1; i < ItemYPosition; i++) {
                        List<ItemWithUtilityPosition> Itemset = sequence.getItemset(i);
                        for (ItemWithUtilityPosition Item : Itemset) {
                            if (Item.getItemName() > ItemX && Item.getItemName() > ItemY) {
                                if (Item.PrePosition < ItemXPosition && (Item.NextPosition == -1 || Item.NextPosition >= ItemYPosition)) {
                                    element.utilityLeftRight += Item.utility();
                                } else {
                                    if (!LeftRightExpandItemMaxUtility.containsKey(Item.getItemName())) {
                                        LeftRightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    } else if (LeftRightExpandItemMaxUtility.get(Item.getItemName()) < Item.utility()) {
                                        LeftRightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    }
                                }


                            } else if (Item.getItemName() > ItemX && !Item.getItemName().equals(ItemY)) {

                                if (Item.PrePosition == -1 && (Item.NextPosition >= ItemYPosition || Item.NextPosition == -1)) {
                                    element.utilityLeft += Item.utility();
                                } else {
                                    if (!LeftExpandItemMaxUtility.containsKey(Item.getItemName())) {
                                        LeftExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    } else if (LeftExpandItemMaxUtility.get(Item.getItemName()) < Item.utility()) {
                                        LeftExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    }
                                }

                            } else if (Item.getItemName() > ItemY && !Item.getItemName().equals(ItemX)) {
                                if (Item.NextPosition == -1 && Item.PrePosition <= ItemXPosition) {
                                    element.utilityRight += Item.utility();
                                } else {
                                    if (!RightExpandItemMaxUtility.containsKey(Item.getItemName())) {
                                        RightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    } else if (RightExpandItemMaxUtility.get(Item.getItemName()) < Item.utility()) {
                                        RightExpandItemMaxUtility.put(Item.getItemName(), Item.utility());
                                    }
                                }

                            }
                        }
                    }


                    for (Entry<Integer, Double> entry : LeftRightExpandItemMaxUtility.entrySet()) {
                        element.utilityLeftRight += entry.getValue();
                    }
                    for (Entry<Integer, Double> entry : LeftExpandItemMaxUtility.entrySet()) {
                        element.utilityLeft += entry.getValue();
                    }
                    for (Entry<Integer, Double> entry : RightExpandItemMaxUtility.entrySet()) {
                        element.utilityRight += entry.getValue();
                    }
                    table.addElement(element);
                }
                table.CalculateUtility();

                //supportXY= table.MapSequenceMaxUtilityList.size();
                supportXY = table.elements.size();

                // We calculate the confidence of X -> Y
                double confidence = supportXY / supportX;

                double conditionExpandLeft;
                double conditionExpandRight;

                // if strategy 4 is deactivated
                // we use a worse upper bound
                if (deactivateStrategy4) {
                    conditionExpandLeft = table.totalUtility + table.totalUtilityLeft + table.totalUtilityLeftRight
                            + table.totalUtilityRight;
                    conditionExpandRight = conditionExpandLeft;
                } else {
                    // otherwise we use a better upper bound
                    conditionExpandLeft = table.totalUtility + table.totalUtilityLeft + table.totalUtilityLeftRight;
                    conditionExpandRight = table.totalUtility + table.totalUtilityRight + table.totalUtilityLeftRight
                            + table.totalUtilityLeft;
                }


                // if in debug mode
                if (DEBUG) {

                    //We will print the rule and its profit and whether it is a high utility rule or not
                    String isInteresting = (table.totalUtility >= minutil) ? " *** HIGH UTILITY RULE! ***" : " ";
                    System.out.println("\n  RULE: " + ItemX + " --> " + ItemY + "   utility " + table.totalUtility
                            + " frequence : " + supportXY
                            + " confidence : " + confidence + isInteresting);

                    // we will print the utility table of the rule
                    for (ElementOfTable element : table.elements) {
                        System.out.println("      SEQ:" + element.numeroSequence + " \t utility: " + element.utility
                                + " \t lutil: " + element.utilityLeft
                                + " \t lrutil: " + element.utilityLeftRight + " \t rutil: " + element.utilityRight
                                + " \t (alpha: " + element.positionAlphaItemset
                                + ", beta: " + element.positionBetaItemset + ")");
                    }

                    System.out.println("      TOTAL: " + " \t utility: " + table.totalUtility + " \t lutil: " + table.totalUtilityLeft
                            + " \t lrutil: " + table.totalUtilityLeftRight + " \t rutil: " + table.totalUtilityRight);
                    System.out.println("      Should we explore larger rules by left expansions ? " + (conditionExpandLeft >= minutil)
                            + " (" + conditionExpandLeft + " )");
                    System.out.println("       Should we explore larger rules by right expansions ? " + (conditionExpandRight >= minutil)
                            + " (" + conditionExpandRight + " )");
                }

                // create the rule antecedent and consequence
                int[] antecedent = new int[]{ItemX};
                int[] consequent = new int[]{ItemY};

                // if high utility with ENOUGH  confidence
                if ((table.totalUtility >= minutil) && confidence >= minConfidence) {
                    // we output the rule
                    saveRule(antecedent, consequent, table.totalUtility, supportXY, confidence);
                }


                // if the right side size is less than the maximum size, we will try to expand the rule
                if (conditionExpandRight >= minutil && maxConsequentSize > 1) {


                    expandRight(table, antecedent, consequent, sequenceIDsX);


                }


                // if the left side size is less than the maximum size, we will try to expand the rule
                if (conditionExpandLeft >= minutil && maxAntecedentSize > 1) {
                    expandFirstLeft(table, antecedent, consequent, sequenceIDsX);
                }


            }
        }


        //We will check the current memory usage
        MemoryLogger.getInstance().checkMemory();

        // save end time
        timeEnd = System.currentTimeMillis();

        // close the file
        writer.close();

        // after the algorithm ends, we don't need a reference to the database
        // anymore.
        database = null;
    }

    /*
    /**
     * This method save a rule to the output file
     *
     * @param antecedent the left side of the rule
     * @param consequent the right side of the rule
     * @param utility    the rule utility
     * @param support    the rule support
     * @param confidence the rule confidence
     * @throws IOException if an error occurs when writing to file
     */
    private void saveRule(int[] antecedent, int[] consequent,
                          double utility, double support, double confidence) throws IOException {

        // increase the number of rule found
        ruleCount++;

        // create a string buffer
        StringBuilder buffer = new StringBuilder();

        // write the left side of the rule (the antecedent)
        for (int i = 0; i < antecedent.length; i++) {
            buffer.append(antecedent[i]);
            if (i != antecedent.length - 1) {
                buffer.append(",");
            }
        }

        // write separator
        buffer.append("	==> ");

        // write the right side of the rule (the consequent)
        for (int i = 0; i < consequent.length; i++) {
            buffer.append(consequent[i]);
            if (i != consequent.length - 1) {
                buffer.append(",");
            }
        }
        // write support
        buffer.append("\t#SUP: ");
        buffer.append(support);
        // write confidence
        buffer.append("\t#CONF: ");
        buffer.append(confidence);
        buffer.append("\t#UTIL: ");
        buffer.append(utility);
        writer.write(buffer.toString());
        writer.newLine();

        /*
        //if we are in debug mode, we will automatically check that the utility, confidence and support
        // are correct to ensure that there is no bug.
        if (DEBUG) {
            //We will check if the rule utility support and confidence is ok
            checkMeasuresForARule(antecedent, consequent, utility, support, confidence);

        }
         */
    }

    /**
     * This method is used for debugging. It scan a database to check if the measures
     * (confidence, utility, support) of a given rule have been correctly calculated.
     *
     * @param antecedent the left isde
     * @param antecedent the left side of the rule
     * @param consequent the right side of the rule
     * @param utility    the rule utility
     * @param support    the rule support
     * @param confidence the rule confidence
     */
    private void checkMeasuresForARule(int[] antecedent, int[] consequent,
                                       double utility, double support, double confidence) {

        // We will calculate again the utility, support and confidence by
        // scanning the database.
        double supportOfAntecedent = 0;
        double supportOfTheRule = 0;
        double utilityOfTheRule = 0;

        // for each sequence
        for (Sequence sequence : database.getSequences()) {

            // Count the number of items already seen from the antecedent in that sequence
            int numberOfAntecedentItemsAlreadySeen = 0;

            double ruleUtilityInSequence = 0;

            //=========================================
            // For each itemset in that sequence
            int i = 0;
            loop1:
            for (; i < sequence.getItemsets().size(); i++) {
                List<ItemWithUtilityPosition> itemset = sequence.getItemsets().get(i);

                // For each item
                for (int j = 0; j < itemset.size(); j++) {
                    ItemWithUtilityPosition item = itemset.get(j);

                    // if the item appear in the left side of a rule
                    if (Arrays.binarySearch(antecedent, item.getItemName()) >= 0) {
                        // add the profit of that item to the rule utility
                        double utilityItem = item.utility();
                        ruleUtilityInSequence += utilityItem;

                        // increase the number of items from the antecedent that we have seen
                        numberOfAntecedentItemsAlreadySeen++;

                        // if we have completely found the antecedent X
                        if (numberOfAntecedentItemsAlreadySeen == antecedent.length) {
                            // increase the support of the antecedent
                            supportOfAntecedent++;
                            // and stop searching for items in the antecedent
                            break loop1;
                        }

                    }
                }
            }

            //=========================================
            // Now we will search for the consequent of the rule
            // starting from the next itemset in that sequence
            i++;

            // This variable will count the number of items of the consequent
            // that we have already seen
            int numberOfConsequentItemsAlreadySeen = 0;


            // for each itemset after the antecedent
            boucle2:
            for (; i < sequence.getItemsets().size(); i++) {
                List<ItemWithUtilityPosition> itemset = sequence.getItemsets().get(i);

                // for each item
                for (int j = 0; j < itemset.size(); j++) {
                    ItemWithUtilityPosition item = itemset.get(j);

                    // if the item appear in the consequent of the rule
                    if (Arrays.binarySearch(consequent, item.getItemName()) >= 0) {
                        // add the utility of that item
                        double utilityItem = item.utility();
                        ruleUtilityInSequence += utilityItem;

                        // increase the number of items from the consequent that we have seen
                        numberOfConsequentItemsAlreadySeen++;

                        // if we have completely found the consequent Y
                        if (numberOfConsequentItemsAlreadySeen == consequent.length) {
                            // increase the support of the rule
                            supportOfTheRule++;
                            // increase the global utility of the rule in the database
                            utilityOfTheRule += ruleUtilityInSequence;
                            // and stop searching for items in the antecedent
                            break boucle2;
                        }

                    }
                }
            }
        }

        // We now check if the support is the same as the support calculated by HUSRM
        if (support != supportOfTheRule) {
            throw new RuntimeException(" The support is incorrect for the rule : "
                    + Arrays.toString(antecedent) + " ==>" + Arrays.toString(consequent)
                    + "   support : " + support + " recalculated support: " + supportOfTheRule);
        }

        // We now check  if the confidence is the same as the confidence calculated by HUSRM
        double recalculatedConfidence = supportOfTheRule / supportOfAntecedent;

        if (confidence != recalculatedConfidence) {
            throw new RuntimeException(" The confidence is incorrect for the rule :"
                    + Arrays.toString(antecedent) + " ==>" + Arrays.toString(consequent)
                    + "   confidence : " + confidence + " recalculated confidence: " + recalculatedConfidence);
        }

        // We now check  if the utility is the same as the utility calculated by HUSRM
        if (utility != utilityOfTheRule) {
            throw new RuntimeException(" The utility is incorrect for the rule :"
                    + Arrays.toString(antecedent) + " ==>" + Arrays.toString(consequent)
                    + "   utility : " + utility + " recalculated utility " + utilityOfTheRule);
        }
    }


    /**
     * This method is used to create new rule(s) by adding items to the right side of a rule
     *
     * @param table                 the utility-table of the rule
     * @param antecedent            the rule antecedent
     * @param consequent            the rule consequent
     * @param sequenceIdsAntecedent the list of ids of sequences containing the left side of the rule
     * @throws IOException if an error occurs while writing to file
     */

    private void expandRight(UtilityTable table, int[] antecedent,
                             int[] consequent, ListSequenceIDs sequenceIdsAntecedent) throws IOException {

        // We first find the largest item on the left side and right side of the rule
        int largestItemInAntecedent = antecedent[antecedent.length - 1];
        int largestItemInConsequent = consequent[consequent.length - 1];


        // We create a new map where we will build the utility table for the new rules that
        // will be created by adding an item to the current rule.
        // Key: an item appended to the rule     Value: the utility-table of the corresponding new rule
        Map<Integer, UtilityTable> mapItemsTables = new HashMap<Integer, UtilityTable>();

//		// for each sequence containing the original rule (according to its utility table)
        for (ElementOfTable element : table.elements) {

            // Optimisation: if the "rutil" is 0 for that rule in that sequence,
            // we do not need to scan this sequence.
            if (element.utilityLeft + element.utilityRight + element.utilityLeftRight == 0) {
                continue;
            }

            // Get the sequence
            Sequence sequence = database.getSequences().get(element.numeroSequence);

            //============================================================
            // Case 1: for each itemset in BETA or AFTER BETA.....
            Map<Integer, Boolean> ItemJHasBeenExpanded = new HashMap<>();
            // For each itemset after beta:
            for (int i = element.positionBetaItemset; i < sequence.size(); i++) {
                // get the itemset
                List<ItemWithUtilityPosition> itemsetI = sequence.getItemsets().get(i);

                // For each item
                for (int j = 0; j < itemsetI.size(); j++) {
                    ItemWithUtilityPosition itemJ = itemsetI.get(j);

                    // Check if the item is greater than items in the consequent of the rule
                    // according to the lexicographical order
                    if (itemJ.getItemName() <= largestItemInConsequent) {
                        // if not, then we continue because that item cannot be added to the rule
                        continue;
                    }

                    // Determine if itemJ has duplicates in Alpha
                    boolean Duplicate = false;
                    for (int value : antecedent) {
                        if (itemJ.getItemName().equals(value)) {
                            Duplicate = true;
                            break;
                        }
                    }
                    if (Duplicate) {
                        continue;
                    }

                    // ======= Otherwise, we need to update the utility table of the item ====================

                    // Get the utility table of the item
                    UtilityTable tableItemJ = mapItemsTables.get(itemJ.getItemName());
                    if (tableItemJ == null) {
                        // if no utility table, we create one
                        tableItemJ = new UtilityTable();
                        mapItemsTables.put(itemJ.getItemName(), tableItemJ);
                    }



                    //==========

                    // We will add a new element (line) in the utility table
                    ElementOfTable newElement = new ElementOfTable(element.numeroSequence);
                    ItemJHasBeenExpanded.put(itemJ.getItemName(), true);


                    //newElement.utility = element.utility + Math.max(MaxUtilityItemJ, itemJ.utility());
                    newElement.utility=element.utility+itemJ.utility();

                    // we will copy the "lutil" value from the original rule
                    newElement.utilityLeft = element.utilityLeft;

                    // we will copy the "lrutil" value from the original rule
                    newElement.utilityLeftRight = element.utilityLeftRight;

                    // we will copy the "rutil" value from the original rule
                    // but we will subtract the utility of the item J
                    if (itemJ.getItemName() < largestItemInAntecedent) {
                        double MaxProfitItemJ1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, itemJ.position(), sequence.size() - 1, sequence, 1).utility();
                        double MaxProfitItemJ2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, element.positionAlphaItemset + 1, itemJ.position(), sequence, 0).utility();
                        newElement.utilityRight = element.utilityRight - Math.max(itemJ.utility(), Math.max(MaxProfitItemJ1, MaxProfitItemJ2));
                    } else {
                        double MaxProfitItemJ1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, itemJ.position(), sequence.size() - 1, sequence, 1).utility();
                        double MaxProfitItemJ2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, element.positionBetaItemset, itemJ.position(), sequence, 0).utility();
                        newElement.utilityRight = element.utilityRight - Math.max(itemJ.utility(), Math.max(MaxProfitItemJ1, MaxProfitItemJ2));
                    }

                    // we will copy the position of Alpha and Beta in that sequences because it
                    // does not change
                    newElement.positionBetaItemset = element.positionBetaItemset;
                    newElement.positionAlphaItemset = element.positionAlphaItemset;

                    newElement.ListMaxUtilityXPosition = element.ListMaxUtilityXPosition;
                    newElement.ListMaxUtilityYPosition = element.ListMaxUtilityYPosition;


                    // Then, we will scan itemsets after the beta position in the sequence
                    // We will subtract the utility of items that are smaller than item J
                    // according to the lexicographical order from "rutil" because they
                    // cannot be added anymore to the new rule.

                    // for each such itemset
                    // area I
                    Map<Integer, Boolean> ItemHasBeenSeem = new HashMap<>();
                    for (int z = element.positionBetaItemset; z < sequence.size(); z++) {
                        List<ItemWithUtilityPosition> itemsetZ = sequence.getItemset(z);

                        // for each item W
                        for (int w = itemsetZ.size() - 1; w >= 0; w--) {
                            // Optimisation:
                            // if the item is smaller than the larger item in the right side of the rule
                            ItemWithUtilityPosition itemW = itemsetZ.get(w);

                            if (itemW.getItemName() <= largestItemInConsequent) {
                                // we break;
                                break;
                            }

                            // Determine if itemW has duplicates in Alpha
                            Duplicate = false;
                            for (int value : antecedent) {
                                if (itemW.getItemName().equals(value)) {
                                    Duplicate = true;
                                    break;
                                }
                            }
                            if (Duplicate) {
                                continue;
                            }

                            if (!ItemHasBeenSeem.containsKey(itemW.getItemName())) {
                                ItemHasBeenSeem.put(itemW.getItemName(), true);
                            } else {
                                continue;
                            }

                            // otherwise, if item W is smaller than item J
                            if (itemW.getItemName() < itemJ.getItemName()) {
                                // RightExpandable to unExpandable
                                // We will subtract the utility of W from "rutil"
                                if (itemW.getItemName() < largestItemInAntecedent) {
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), sequence.size() - 1, sequence, 1).utility();
                                    double MaxProfitItemW2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                    newElement.utilityRight -= Math.max(itemW.utility(), Math.max(MaxProfitItemW1, MaxProfitItemW2));
                                } else {
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), sequence.size() - 1, sequence, 1).utility();
                                    newElement.utilityRight -= Math.max(itemW.utility(), MaxProfitItemW1);
                                }
                            }
                        }
                    }
                    // end


                    // area II
                    ItemHasBeenSeem = new HashMap<>();
                    for (int z = element.positionBetaItemset - 1; z > element.positionAlphaItemset; z--) {
                        List<ItemWithUtilityPosition> itemsetZ = sequence.getItemsets().get(z);
                        for (int w = itemsetZ.size() - 1; w >= 0; w--) {
                            ItemWithUtilityPosition itemW = itemsetZ.get(w);
                            if (!ItemHasBeenSeem.containsKey(itemW.getItemName())) {
                                ItemHasBeenSeem.put(itemW.getItemName(), true);
                            } else {
                                continue;
                            }

                            if (itemW.getItemName() <= largestItemInConsequent) {
                                // we break;
                                break;
                            }

                            // Determine if itemW has duplicates in Alpha
                            Duplicate = false;
                            for (int value : antecedent) {
                                if (itemW.getItemName().equals(value)) {
                                    Duplicate = true;
                                    break;
                                }
                            }
                            if (Duplicate) {
                                continue;
                            }

                            boolean wIsLeftRight = itemW.getItemName() > largestItemInAntecedent && itemW.getItemName() > largestItemInConsequent;
                            boolean wIsRight = itemW.getItemName() > largestItemInConsequent && itemW.getItemName() < largestItemInAntecedent;

                            if (wIsLeftRight && itemW.getItemName() <= itemJ.getItemName()) {
                                // LeftRightExpandable to LeftExpandable
                                double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                newElement.utilityLeftRight -= Math.max(itemW.utility(), MaxProfitItemW1);
                                if (!itemW.getItemName().equals(itemJ.getItemName())) {
                                    ItemWithUtilityPosition itemZ = itemW;
                                    while (itemZ.PrePosition != -1) {
                                        int PrePosition = itemZ.PrePosition;
                                        List<ItemWithUtilityPosition> itemset = sequence.getItemset(PrePosition);
                                        for (ItemWithUtilityPosition item : itemset) {
                                            if (item.getItemName().equals(itemZ.getItemName())) {
                                                itemZ = item;
                                            }
                                        }
                                    }

                                    if (itemZ.position() <= element.positionAlphaItemset) {
                                        double MaxProfitItemW2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemZ, itemZ.position(), element.positionAlphaItemset, sequence, 1).utility();
                                        double MaxProfitItemW3 = test.MaxUtilityItemBetweenAlphaAndBeta(itemZ, itemZ.position(), element.positionBetaItemset - 1, sequence, 1).utility();
                                        newElement.utilityLeft -= Math.max(itemZ.utility(), MaxProfitItemW2);
                                        newElement.utilityLeft += Math.max(itemZ.utility(), MaxProfitItemW3);
                                    } else {
                                        double MaxProfitItemW3 = test.MaxUtilityItemBetweenAlphaAndBeta(itemZ, itemZ.position(), element.positionBetaItemset - 1, sequence, 1).utility();
                                        newElement.utilityLeft += Math.max(itemZ.utility(), MaxProfitItemW3);
                                    }
                                }
                            } else if (wIsRight && itemW.getItemName() < itemJ.getItemName()) {
                                // RightExpandable to unExpandable
                                if (itemW.NextPosition == -1) {
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                    newElement.utilityRight -= Math.max(itemW.utility(), MaxProfitItemW1);
                                }
                            }
                        }
                    }


                    // Now that we have created the element for that sequence and that new rule
                    // , we will add the utility table of that new rule
                    tableItemJ.addElement(newElement, element.numeroSequence);
                }
            }


            //============================================================
            // CAS 2 : For each itemset from itemset BETA - 1 to itemset ALPHA + 1
            // in the sequence
            //.....
            // For each itemset before the BETA itemset, we will scan the sequence

            // We will look here for the case where an item J is added to the right side of a rule
            // but it is an item found between the left side and right side of the rule in the sequence.
            // In that case, the position beta will change to a new position that we will call beta prime.

            // These two variable will be used to sum the utility of lrutil and lutil
            // after beta has changed


            // For each itemset from itemset BETA - 1 to itemset ALPHA + 1
            for (int i = element.positionBetaItemset - 1; i > element.positionAlphaItemset; i--) {
                // Get the itemset
                List<ItemWithUtilityPosition> itemsetI = sequence.getItemsets().get(i);

                // Get the item
                for (int j = 0; j < itemsetI.size(); j++) {
                    ItemWithUtilityPosition itemJ = itemsetI.get(j);


                    // Check if the item is greater than items in the consequent of the rule
                    // according to the lexicographical order
                    if (itemJ.getItemName() <= largestItemInConsequent) {
                        // if not, then we continue because that item cannot be added to the rule
                        continue;
                    }

                    // Determine if itemJ has duplicates in Alpha
                    boolean Duplicate = false;
                    for (int value : antecedent) {
                        if (itemJ.getItemName().equals(value)) {
                            Duplicate = true;
                            break;
                        }
                    }
                    if (Duplicate) {
                        continue;
                    }



                    //Check if the item could be added to the left side,
                    // right side, or left and right side of the rule according to the lexicographical order
                    // boolean isLeft = itemJ.getItemName() > largestItemInAntecedent && itemJ.getItemName() < largestItemInConsequent;
                    boolean isLeftRight = itemJ.getItemName() > largestItemInAntecedent && itemJ.getItemName() > largestItemInConsequent;
                    boolean isRight = itemJ.getItemName() > largestItemInConsequent && itemJ.getItemName() < largestItemInAntecedent;

                    UtilityTable tableItemJ = mapItemsTables.get(itemJ.getItemName());
                    if (tableItemJ == null) {
                        // if it does not exist, create a new utility table
                        tableItemJ = new UtilityTable();
                        mapItemsTables.put(itemJ.getItemName(), tableItemJ);
                    }

                    // Create a new element (line) in the utility table for that sequence
                    ElementOfTable newElement = new ElementOfTable(element.numeroSequence);


                    //  Add the utility of the item to the utility of the new rule
                    double profitItemJ = sequence.getItem(i, j).utility();

                    List<Integer> MaxUtilityXPosition = element.ListMaxUtilityXPosition;
                    List<Integer> MaxUtilityYPosition = element.ListMaxUtilityYPosition;
                    int num = 0;
                    for (Integer XPosition : MaxUtilityXPosition) {
                        if (XPosition >= itemJ.position()) {
                            num++;
                        }
                    }


                    double XDeltaUtility = 0;
                    double YDeltaUtility = 0;

                    if (num == 0) {
                        newElement.ListMaxUtilityXPosition = element.ListMaxUtilityXPosition;
                        newElement.ListMaxUtilityYPosition = element.ListMaxUtilityYPosition;
                    }
                    if (num == 1 && MaxUtilityXPosition.size() == 1) {

                        ItemWithUtilityPosition ItemX = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> XItemset = sequence.getItemset(MaxUtilityXPosition.get(0));
                        for (ItemWithUtilityPosition item : XItemset) {
                            if (item.getItemName().equals(antecedent[0])) {
                                ItemX = item;
                                break;
                            }
                        }
                        ItemWithUtilityPosition XSecondMaxUtility = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpX = ItemX;

                        while (tmpX.PrePosition != -1) {
                            int PrePosition = tmpX.PrePosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(PrePosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpX.getItemName())) {
                                    tmpX = item;
                                    if (item.position() < itemJ.position() && XSecondMaxUtility.utility() <= item.utility()) {
                                        XSecondMaxUtility = item;
                                    }
                                }
                            }
                        }
                        XDeltaUtility = ItemX.utility() - XSecondMaxUtility.utility();
                        newElement.ListMaxUtilityXPosition.add(XSecondMaxUtility.position());

                        ItemWithUtilityPosition ItemY = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> YItemset = sequence.getItemset(MaxUtilityYPosition.get(0));
                        for (ItemWithUtilityPosition item : YItemset) {
                            if (item.getItemName().equals(consequent[0])) {
                                ItemY = item;
                                break;
                            }
                        }
                        ItemWithUtilityPosition tmpY = ItemY;
                        ItemWithUtilityPosition FoundY = ItemY;
                        //double tmpYUtility=tmpY.utility();
                        while (tmpY.PrePosition != -1 && tmpY.PrePosition > XSecondMaxUtility.position() && tmpY.PrePosition > element.positionAlphaItemset) {
                            int PrePosition = tmpY.PrePosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(PrePosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpY.getItemName())) {
                                    tmpY = item;
                                    if (FoundY.utility() < item.utility()) {
                                        FoundY = item;
                                    }
                                }
                            }
                        }
                        YDeltaUtility = FoundY.utility() - ItemY.utility();
                        newElement.ListMaxUtilityYPosition.add(FoundY.position());

                        //newElement.MaxUtilityXPosition=XSecondMaxUtility.position();
                        //MaxUtilityXPosition.set(num,)
                    }

                    if (num == 1 && MaxUtilityXPosition.size() == 2) {
                        int px1 = MaxUtilityXPosition.get(0);
                        int px2 = MaxUtilityXPosition.get(1);

                        int py1 = MaxUtilityYPosition.get(0);
                        int py2 = MaxUtilityYPosition.get(1);
                        if (px1 < itemJ.position()) {
                            newElement.ListMaxUtilityXPosition.add(px1);
                            newElement.ListMaxUtilityYPosition.add(py1);
                        } else {
                            newElement.ListMaxUtilityXPosition.add(px2);
                            newElement.ListMaxUtilityYPosition.add(py2);
                        }
                    }

                    if (num == 2) {
                        int px1 = MaxUtilityXPosition.get(0);
                        int px2 = MaxUtilityXPosition.get(1);

                        int py1 = MaxUtilityYPosition.get(0);
                        int py2 = MaxUtilityYPosition.get(1);
                        ItemWithUtilityPosition ItemX1 = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> X1Itemset = sequence.getItemset(px1);
                        for (ItemWithUtilityPosition item : X1Itemset) {
                            if (item.getItemName().equals(antecedent[0])) {
                                ItemX1 = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition ItemX2 = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> X2Itemset = sequence.getItemset(px2);
                        for (ItemWithUtilityPosition item : X2Itemset) {
                            if (item.getItemName().equals(antecedent[0])) {
                                ItemX2 = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition XSecondMaxUtility1 = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpX1 = ItemX1;
                        //double tmpUtility1 = tmpX1.utility();
                        while (tmpX1.PrePosition != -1) {
                            int PrePosition = tmpX1.PrePosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(PrePosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpX1.getItemName())) {
                                    tmpX1 = item;
                                    if (item.position() < itemJ.position() && XSecondMaxUtility1.utility() < item.utility()) {
                                        XSecondMaxUtility1 = item;
                                    }
                                }
                            }
                        }
                        double XDeltaUtility1 = ItemX1.utility() - XSecondMaxUtility1.utility();

                        ItemWithUtilityPosition XSecondMaxUtility2 = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpX2 = ItemX2;
                        //double tmpUtility2 = tmpX2.utility();
                        while (tmpX2.PrePosition != -1) {
                            int PrePosition = tmpX2.PrePosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(PrePosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpX2.getItemName())) {
                                    tmpX2 = item;
                                    if (item.position() < itemJ.position() && XSecondMaxUtility2.utility() < item.utility()) {
                                        XSecondMaxUtility2 = item;
                                    }
                                }
                            }
                        }
                        double XDeltaUtility2 = ItemX2.utility() - XSecondMaxUtility2.utility();

                        if (XDeltaUtility1 < XDeltaUtility2) {
                            XDeltaUtility = XDeltaUtility1;
                            newElement.ListMaxUtilityXPosition.add(XSecondMaxUtility1.position());
                            newElement.ListMaxUtilityYPosition.add(py1);
                        } else if (XDeltaUtility1 > XDeltaUtility2) {
                            XDeltaUtility = XDeltaUtility2;
                            newElement.ListMaxUtilityXPosition.add(XSecondMaxUtility2.position());
                            newElement.ListMaxUtilityYPosition.add(py2);
                        } else {
                            XDeltaUtility = XDeltaUtility1;
                            if (XSecondMaxUtility1.position() <= XSecondMaxUtility2.position()) {
                                newElement.ListMaxUtilityXPosition.add(XSecondMaxUtility1.position());
                                newElement.ListMaxUtilityYPosition.add(py1);
                            } else {
                                newElement.ListMaxUtilityXPosition.add(XSecondMaxUtility2.position());
                                newElement.ListMaxUtilityYPosition.add(py2);
                            }
                        }
                    }

                    newElement.utility = element.utility + profitItemJ - XDeltaUtility + YDeltaUtility;


                    newElement.utilityLeft = element.utilityLeft;


                    if (isLeftRight) {
                        double MaxProfitItemJ1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, itemJ.position(), element.positionBetaItemset - 1, sequence, 1).utility();
                        double MaxProfitItemJ2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, element.positionAlphaItemset + 1, itemJ.position(), sequence, 0).utility();
                        newElement.utilityLeftRight = element.utilityLeftRight - Math.max(itemJ.utility(), Math.max(MaxProfitItemJ1, MaxProfitItemJ2));
                        newElement.utilityRight = element.utilityRight;

                    } else if (isRight) {
                        double MaxProfitItemJ1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, itemJ.position(), sequence.size() - 1, sequence, 1).utility();
                        double MaxProfitItemJ2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, element.positionAlphaItemset + 1, itemJ.position(), sequence, 0).utility();
                        newElement.utilityRight = element.utilityRight - Math.max(itemJ.utility(), Math.max(MaxProfitItemJ1, MaxProfitItemJ2));
                        newElement.utilityLeftRight = element.utilityLeftRight;
                    } else {
                        continue;
                    }

                    // area I
                    Map<Integer, Boolean> ItemHasBeenSeem = new HashMap<>();
                    for (int z = element.positionBetaItemset; z < sequence.size(); z++) {
                        List<ItemWithUtilityPosition> itemsetZ = sequence.getItemsets().get(z);
                        // for each item W
                        for (int w = itemsetZ.size() - 1; w >= 0; w--) {
                            // Optimisation:
                            // if the item is smaller than the larger item on the right side of the rule
                            ItemWithUtilityPosition itemW = itemsetZ.get(w);


                            if (itemW.getItemName() <= largestItemInConsequent) {
                                // we break;
                                break;
                            }

                            // Determine if itemW has duplicates in Alpha
                            Duplicate = false;
                            for (int value : antecedent) {
                                if (itemW.getItemName().equals(value)) {
                                    Duplicate = true;
                                    break;
                                }
                            }
                            if (Duplicate) {
                                continue;
                            }

                            if (!ItemHasBeenSeem.containsKey(itemW.getItemName())) {
                                ItemHasBeenSeem.put(itemW.getItemName(), true);
                            } else {
                                continue;
                            }

                            // otherwise, if item W is smaller than item J
                            if (itemW.getItemName() <= itemJ.getItemName()) {

                                // We will subtract the utility of W from "rutil"
                                if (itemW.getItemName() < largestItemInAntecedent) {
                                    if (itemW.getItemName().equals(itemJ.getItemName())) {
                                        continue;
                                    }
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), sequence.size() - 1, sequence, 1).utility();
                                    double MaxProfitItemW2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                    newElement.utilityRight -= Math.max(itemW.utility(), Math.max(MaxProfitItemW1, MaxProfitItemW2));
                                } else {
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), sequence.size() - 1, sequence, 1).utility();
                                    newElement.utilityRight -= Math.max(itemW.utility(), MaxProfitItemW1);
                                }
                            }
                        }
                    }

                    // area II''
                    ItemHasBeenSeem = new HashMap<>();
                    for (int z = element.positionBetaItemset - 1; z >= i; z--) {

                        List<ItemWithUtilityPosition> itemsetZ = sequence.getItemsets().get(z);

                        // for each item W
                        for (int w = itemsetZ.size() - 1; w >= 0; w--) {
                            ItemWithUtilityPosition itemW = itemsetZ.get(w);


                            // Determine if itemW has duplicates in Alpha
                            Duplicate = false;
                            for (int value : antecedent) {
                                if (itemW.getItemName().equals(value)) {
                                    Duplicate = true;
                                    break;
                                }
                            }
                            if (Duplicate) {
                                continue;
                            }

                            if (!ItemHasBeenSeem.containsKey(itemW.getItemName())) {
                                ItemHasBeenSeem.put(itemW.getItemName(), true);
                            } else {
                                continue;
                            }

                            // check if the item can be appended to the left or right side of the rule
                            boolean wIsLeftRight = itemW.getItemName() > largestItemInAntecedent && itemW.getItemName() > largestItemInConsequent;
                            // check if the item can only be appended to the right side of the rule
                            boolean wIsRight = itemW.getItemName() > largestItemInConsequent && itemW.getItemName() < largestItemInAntecedent;

                            boolean wIsLeft = itemW.getItemName() > largestItemInAntecedent && itemW.getItemName() < largestItemInConsequent;
                            // if the item can only be appended to the right side of the original rule
                            // but is smaller than item W that is appended to the right side of the
                            // new rule
                            if (wIsRight && itemW.getItemName() < itemJ.getItemName()) {
                                // RightExpandable to unExpandable
                                if (itemW.NextPosition == -1) {
                                    //double MaxProfitItemW1=test.MaxUtilityItemBetweenAlphaAndBeta(itemW,itemW.position(),element.positionBetaItemset-1,sequence,1).utility();
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, i, itemW.position(), sequence, 0).utility();
                                    newElement.utilityRight -= Math.max(itemW.utility(), MaxProfitItemW1);
                                }
                            } else if (wIsLeftRight && itemW.getItemName() > itemJ.getItemName()) {
                                // LeftRightExpandable to RightExpandable
                                ItemWithUtilityPosition itemZ = itemW;
                                boolean flag = false;
                                while (itemZ.PrePosition != -1) {
                                    int PrePosition = itemZ.PrePosition;
                                    List<ItemWithUtilityPosition> itemset = sequence.getItemset(PrePosition);
                                    for (ItemWithUtilityPosition item : itemset) {
                                        if (item.getItemName().equals(itemW.getItemName())) {
                                            itemZ = item;
                                            if (item.position() < i && item.position() > element.positionAlphaItemset) {
                                                flag = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (flag && itemW.NextPosition == -1) {
                                    // in area II'
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, i, itemW.position(), sequence, 0).utility();
                                    double MaxProfitItemW2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                    newElement.utilityRight += Math.max(MaxProfitItemW1, itemW.utility());
                                    if (MaxProfitItemW2 >= MaxProfitItemW1 && MaxProfitItemW2 >= itemW.utility()) {
                                        continue;
                                    }

                                    double MaxProfitItemW3 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemZ.position(), sequence, 0).utility();
                                    newElement.utilityLeftRight += Math.max(itemZ.utility(), MaxProfitItemW3) - Math.max(MaxProfitItemW1, itemW.utility());
                                }

                                if (!flag && itemW.NextPosition != -1) {
                                    // also in area I
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, i, itemW.position(), sequence, 0).utility();
                                    double MaxProfitItemW2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), sequence.size() - 1, sequence, 1).utility();
                                    newElement.utilityLeftRight -= Math.max(MaxProfitItemW1, itemW.utility());
                                    if (MaxProfitItemW2 >= MaxProfitItemW1 && MaxProfitItemW2 >= itemW.utility()) {
                                        continue;
                                    }
                                    newElement.utilityRight += Math.max(MaxProfitItemW1, itemW.utility()) - MaxProfitItemW2;
                                }

                                if (flag && itemW.NextPosition != -1) {
                                    // in area I + area II' + area II''
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, i, itemW.position(), sequence, 0).utility();
                                    double MaxProfitItemW2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                    double MaxProfitItemW3 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), sequence.size() - 1, sequence, 1).utility();
                                    if (MaxProfitItemW2 >= MaxProfitItemW1 && MaxProfitItemW2 >= itemW.utility()) {
                                        if (MaxProfitItemW3 >= MaxProfitItemW1 && MaxProfitItemW3 >= itemW.utility()) {
                                            continue;
                                        } else {
                                            newElement.utilityRight += Math.max(MaxProfitItemW1, itemW.utility()) - MaxProfitItemW3;
                                        }
                                    } else if (MaxProfitItemW3 >= MaxProfitItemW1 && MaxProfitItemW3 >= itemW.utility()) {
                                        newElement.utilityLeftRight -= Math.max(MaxProfitItemW1, itemW.utility());
                                    } else {
                                        double MaxProfitItemW4 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemZ.position(), sequence, 0).utility();
                                        newElement.utilityRight += Math.max(MaxProfitItemW1, itemW.utility()) - MaxProfitItemW3;
                                        newElement.utilityLeftRight += Math.max(itemZ.utility(), MaxProfitItemW4) - Math.max(MaxProfitItemW1, itemW.utility());
                                    }
                                }

                                if (!flag && itemW.NextPosition == -1) {
                                    // in area II''
                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, i, itemW.position(), sequence, 0).utility();
                                    newElement.utilityLeftRight -= Math.max(MaxProfitItemW1, itemW.utility());
                                    newElement.utilityRight += Math.max(MaxProfitItemW1, itemW.utility());
                                }
                            } else if (wIsLeftRight && itemW.getItemName() < itemJ.getItemName()) {
                                // LeftRightExpandable to unExpandable
                                double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                newElement.utilityLeftRight -= Math.max(itemW.utility(), MaxProfitItemW1);
                            } else if (wIsLeft) {
                                // LeftExpandable to unExpandable
                                double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, 0, itemW.position(), sequence, 0).utility();
                                double MaxProfitItemW2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, i, itemW.position(), sequence, 0).utility();
                                if (MaxProfitItemW1 > MaxProfitItemW2 && MaxProfitItemW1 > itemW.utility()) {
                                    continue;
                                }
                                ItemWithUtilityPosition itemZ = itemW;
                                while (itemZ.PrePosition >= i) {
                                    int PrePosition = itemZ.PrePosition;
                                    List<ItemWithUtilityPosition> itemset = sequence.getItemset(PrePosition);
                                    for (ItemWithUtilityPosition item : itemset) {
                                        if (item.getItemName().equals(itemZ.getItemName())) {
                                            itemZ = item;
                                        }
                                    }
                                }
                                double MaxProfitItemW3 = test.MaxUtilityItemBetweenAlphaAndBeta(itemZ, 0, itemZ.position(), sequence, 0).utility();
                                newElement.utilityLeft -= Math.max(itemW.utility(), MaxProfitItemW1);
                                newElement.utilityLeft += Math.max(itemZ.utility(), MaxProfitItemW3);
                            }
                        }
                    }

                    // area II'
                    ItemHasBeenSeem = new HashMap<>();
                    for (int z = i - 1; z > element.positionAlphaItemset; z--) {
                        List<ItemWithUtilityPosition> itemsetZ = sequence.getItemsets().get(z);
                        // for each item W
                        for (int w = itemsetZ.size() - 1; w >= 0; w--) {
                            ItemWithUtilityPosition itemW = itemsetZ.get(w);

                            if (!ItemHasBeenSeem.containsKey(itemW.getItemName())) {
                                ItemHasBeenSeem.put(itemW.getItemName(), true);
                            } else {
                                continue;
                            }

                            if (itemW.getItemName() <= largestItemInConsequent) {
                                // we break;
                                break;
                            }

                            // check if the item can be appended to the left or right side of the rule
                            boolean wIsLeftRight = itemW.getItemName() > largestItemInAntecedent && itemW.getItemName() > largestItemInConsequent;
                            // check if the item can only be appended to the right side of the rule
                            boolean wIsRight = itemW.getItemName() > largestItemInConsequent && itemW.getItemName() < largestItemInAntecedent;


                            if (wIsLeftRight && itemW.getItemName() < itemJ.getItemName()) {
                                // LeftRightExpandable becomes LeftExpandable

                                double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();

                                if (!itemW.getItemName().equals(itemJ.getItemName())) {
                                    ItemWithUtilityPosition itemZ=itemW;
                                    while (itemZ.PrePosition!=-1){
                                        int PrePosition=itemZ.PrePosition;
                                        List<ItemWithUtilityPosition> itemset = sequence.getItemset(PrePosition);
                                        for (ItemWithUtilityPosition item : itemset) {
                                            if(item.getItemName().equals(itemZ.getItemName())){
                                                itemZ=item;
                                            }
                                        }
                                    }


                                    if (itemZ.position()<=element.positionAlphaItemset){
                                        double MaxProfitItemW2=test.MaxUtilityItemBetweenAlphaAndBeta(itemZ,itemZ.position(),element.positionAlphaItemset,sequence,1).utility();
                                        double MaxProfitItemW3=test.MaxUtilityItemBetweenAlphaAndBeta(itemZ,itemZ.position(),element.positionBetaItemset-1,sequence,1).utility();
                                        newElement.utilityLeft -= Math.max(itemZ.utility(), MaxProfitItemW2);
                                        newElement.utilityLeft += Math.max(itemZ.utility(), MaxProfitItemW3);
                                    }else {
                                        double MaxProfitItemW3=test.MaxUtilityItemBetweenAlphaAndBeta(itemZ,itemZ.position(),element.positionBetaItemset-1,sequence,1).utility();
                                        newElement.utilityLeft += Math.max(itemZ.utility(), MaxProfitItemW3);
                                    }
                                }

                                if (itemW.NextPosition >= i && itemW.NextPosition < element.positionBetaItemset) {
                                    continue;
                                }
                                newElement.utilityLeftRight -= Math.max(itemW.utility(), MaxProfitItemW1);


                            } else if (wIsRight && itemW.getItemName() < itemJ.getItemName()) {
                                // RightExpandable becomes unExpandable
                                if (itemW.NextPosition == -1) {

                                    double MaxProfitItemW1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, element.positionAlphaItemset + 1, itemW.position(), sequence, 0).utility();
                                    newElement.utilityRight -= Math.max(itemW.utility(), MaxProfitItemW1);
                                }
                            }
                        }
                    }

                    // The first itemset of the right side of the rule has now changed.
                    // We thus set beta to the new value "i"
                    newElement.positionBetaItemset = i;
                    // The left side of the rule has not changed, so Alpha stay the same.
                    newElement.positionAlphaItemset = element.positionAlphaItemset;


                    // Finally, we add the element that we just created to the utility-table
                    // of the new rule.
                    tableItemJ.addElement(newElement, element.numeroSequence);
                    //===========
                }
            }

        }


        // For each new rule
        for (Entry<Integer, UtilityTable> entryItemTable : mapItemsTables.entrySet()) {
            // We get the item and its utility table
            Integer item = entryItemTable.getKey();
            UtilityTable utilityTable = entryItemTable.getValue();
            utilityTable.CalculateUtility();

            // We check if we should try to expand its left side
            boolean shouldExpandLeftSide;
            // We check if we should try to expand its right side
            boolean shouldExpandRightSide;

            // If the user deactivate strategy 4, we use a worst upper bound to check that
            if (deactivateStrategy4) {
                shouldExpandLeftSide = utilityTable.totalUtility + utilityTable.totalUtilityLeft
                        + utilityTable.totalUtilityLeftRight + utilityTable.totalUtilityRight >= minutil
                        && antecedent.length + 1 < maxSizeAntecedent;
                shouldExpandRightSide = utilityTable.totalUtility + utilityTable.totalUtilityRight
                        + utilityTable.totalUtilityLeftRight + utilityTable.totalUtilityLeft >= minutil
                        && consequent.length + 1 < maxSizeConsequent;
            } else {
                // Otherwise, we use the best upper bound.
                shouldExpandLeftSide = utilityTable.totalUtility + utilityTable.totalUtilityLeft
                        + utilityTable.totalUtilityLeftRight >= minutil
                        && antecedent.length + 1 < maxSizeAntecedent;
                shouldExpandRightSide = utilityTable.totalUtility + utilityTable.totalUtilityRight
                        + utilityTable.totalUtilityLeftRight + utilityTable.totalUtilityLeft >= minutil
                        && consequent.length + 1 < maxSizeConsequent;

            }

            // check if the rule is high utility
            boolean isHighUtility = utilityTable.totalUtility >= minutil;

            // We create the consequent for the new rule by appending the new item
            int[] newConsequent = new int[consequent.length + 1];
            System.arraycopy(consequent, 0, newConsequent, 0, consequent.length);
            newConsequent[consequent.length] = item;

            // We calculate the confidence
            double confidence = (double) utilityTable.MapSequenceMaxUtilityList.size() / (double) sequenceIdsAntecedent.getSize();

            // If the rule is high utility and high confidence
            if (isHighUtility && confidence >= minConfidence) {
                // We save the rule to file
                saveRule(antecedent, newConsequent, utilityTable.totalUtility, utilityTable.MapSequenceMaxUtilityList.size(), confidence);


                // If we are in debugging mode, we will show the rule in the console
                if (DEBUG) {
                    System.out.println("\n\t  HIGH UTILITY SEQ. RULE: " + Arrays.toString(antecedent) +
                            " --> " + Arrays.toString(consequent) + "," + item + "   utility " + utilityTable.totalUtility
                            + " support : " + utilityTable.MapSequenceMaxUtilityList.size()
                            + " confidence : " + confidence);

                    for (ElementOfTable element : utilityTable.elements) {
                        if (element.utilityRight < 0 || element.utilityLeft < 0 || element.utilityLeftRight < 0) {
                            System.out.println("error!");
                        }

                        System.out.println("\t      SEQ:" + element.numeroSequence + " \t utility: " + element.utility
                                + " \t lutil: " + element.utilityLeft
                                + " \t lrutil: " + element.utilityLeftRight + " \t rutil: " + element.utilityRight
                                + " \t (alpha: " + element.positionAlphaItemset
                                + ", beta: " + element.positionBetaItemset + ")");
                    }
                }

            } else {
                // If we are in debugging mode and the rule is not high utility and high confidence,
                // we will still show it in the console for debugging
                if (DEBUG) {

                    System.out.println("\n\t  LOW UTILITY RULE: " + Arrays.toString(antecedent) +
                            " --> " + Arrays.toString(consequent) + "," + item + "   utility " + utilityTable.totalUtility
                            + " support : " + utilityTable.MapSequenceMaxUtilityList.size()
                            + " confidence : " + confidence);

                    for (ElementOfTable element : utilityTable.elements) {
                        System.out.println("\t      SEQ:" + element.numeroSequence + " \t utility: " + element.utility
                                + " \t lutil: " + element.utilityLeft
                                + " \t lrutil: " + element.utilityLeftRight + " \t rutil: " + element.utilityRight
                                + " \t (alpha: " + element.positionAlphaItemset
                                + ", beta: " + element.positionBetaItemset + ")");
                    }
                }
            }


            // If we should try to expand the left side of this rule
            if (shouldExpandLeftSide) {
                expandFirstLeft(utilityTable, antecedent, newConsequent, sequenceIdsAntecedent);
            }


            // If we should try to expand the right side of this rule
            if (shouldExpandRightSide) {
                expandRight(utilityTable, antecedent, newConsequent, sequenceIdsAntecedent);
            }


        }

        // Check the maximum memory usage
        MemoryLogger.getInstance().checkMemory();
    }


    /**
     * This method will recursively try to append items to the left side of a rule to generate
     * rules containing one more item on the left side.  This method is only called for rules
     * of size 1*1, thus containing two items (e.g. a -> b)
     *
     * @param utilityTable          the rule utility table
     * @param antecedent            the rule antecedent
     * @param consequent            the rule consequent
     * @param sequenceIDsConsequent the list of sequences ids of sequences containing the rule antecedent
     * @throws IOException if error while writting to file
     */

    private void expandFirstLeft(UtilityTable utilityTable, int[] antecedent,
                                 int[] consequent, ListSequenceIDs sequenceIDsConsequent) throws IOException {

        // We first find the largest item on the left side of the rule
        int largestItemInAntecedent = antecedent[antecedent.length - 1];

        // We create a new map where we will build the utility table for the new rules that
        // will be created by adding an item to the current rule.
        // Key: an item appended to the rule     Value: the utility-table of the corresponding new rule
        Map<Integer, UtilityTableLeft> mapItemUtilityTable = new HashMap<Integer, UtilityTableLeft>();


        // for each sequence containing the rule (a line in the utility table of the original rule)
        for (ElementOfTable element : utilityTable.elements) {
            // Optimisation: if the "lutil" is 0 for that rule in that sequence,
            // we do not need to scan this sequence.
            if (element.utilityLeft + element.utilityLeftRight == 0) {
                continue;
            }


            // Get the sequence
            Sequence sequence = database.getSequence(element.numeroSequence);

            // For each itemset before beta
            for (int i = 0; i < element.positionBetaItemset; i++) {
                List<ItemWithUtilityPosition> itemsetI = sequence.getItemset(i);

                // For each item
                for (int j = 0; j < itemsetI.size(); j++) {
                    ItemWithUtilityPosition itemJ = itemsetI.get(j);

                    // Check if the item is greater than items in the antecedent of the rule
                    // according to the lexicographical order
                    if (itemJ.getItemName() <= largestItemInAntecedent) {
                        continue;
                    }

                    // Determine if itemJ has duplicates in Beta
                    boolean Duplicate = false;
                    for (int value : consequent) {
                        if (itemJ.getItemName().equals(value)) {
                            Duplicate = true;
                            break;
                        }
                    }
                    if (Duplicate) {
                        continue;
                    }

                    // ======= Otherwise, we need to update the utility table of the item ====================
                    // Get the utility table of the item
                    UtilityTableLeft tableItemJ = mapItemUtilityTable.get(itemJ.getItemName());
                    if (tableItemJ == null) {
                        // if no utility table, we create one
                        tableItemJ = new UtilityTableLeft();
                        mapItemUtilityTable.put(itemJ.getItemName(), tableItemJ);
                    }


                    // We will add a new element (line) in the utility table
                    ElementTableLeft newElement = new ElementTableLeft(element.numeroSequence);


                    // we will update the utility vlaue of that rule by adding the utility of the item
                    // in that sequence

                    List<Integer> MaxUtilityXPosition = element.ListMaxUtilityXPosition;
                    List<Integer> MaxUtilityYPosition = element.ListMaxUtilityYPosition;
                    int num = 0;
                    for (Integer YPosition : MaxUtilityYPosition) {
                        if (YPosition <= itemJ.position()) {
                            num++;
                        }
                    }

                    double YDeltaUtility = 0;
                    double XDeltaUtility = 0;

                    if (num == 0) {
                        newElement.ListMaxUtilityXPosition = element.ListMaxUtilityXPosition;
                        newElement.ListMaxUtilityYPosition = element.ListMaxUtilityYPosition;
                    }

                    if (num == 1 && MaxUtilityYPosition.size() == 1) {

                        ItemWithUtilityPosition ItemY = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> YItemset = sequence.getItemset(MaxUtilityYPosition.get(0));
                        for (ItemWithUtilityPosition item : YItemset) {
                            if (item.getItemName().equals(consequent[0])) {
                                ItemY = item;
                                break;
                            }
                        }
                        ItemWithUtilityPosition YSecondMaxUtility = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpY = ItemY;

                        while (tmpY.NextPosition != -1) {
                            int NextPosition = tmpY.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpY.getItemName())) {
                                    tmpY = item;
                                    if (item.position() > itemJ.position() && YSecondMaxUtility.utility() <= item.utility()) {
                                        YSecondMaxUtility = item;
                                    }
                                }
                            }
                        }
                        YDeltaUtility = ItemY.utility() - YSecondMaxUtility.utility();
                        newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility.position());

                        ItemWithUtilityPosition ItemX = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> XItemset = sequence.getItemset(MaxUtilityXPosition.get(0));
                        for (ItemWithUtilityPosition item : XItemset) {
                            if (item.getItemName().equals(antecedent[0])) {
                                ItemX = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition tmpX = ItemX;
                        ItemWithUtilityPosition FoundX = ItemX;

                        while (tmpX.NextPosition != -1 && tmpX.NextPosition < YSecondMaxUtility.position() && tmpX.NextPosition < element.positionBetaItemset) {
                            int NextPosition = tmpX.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(ItemX.getItemName())) {
                                    tmpX = item;
                                    if (item.utility() > FoundX.utility()) {
                                        FoundX = item;
                                    }
                                }
                            }
                        }
                        XDeltaUtility = FoundX.utility() - ItemX.utility();
                        newElement.ListMaxUtilityXPosition.add(FoundX.position());
                    }

                    if (num == 1 && MaxUtilityYPosition.size() == 2) {
                        int px1 = MaxUtilityXPosition.get(0);
                        int px2 = MaxUtilityXPosition.get(1);

                        int py1 = MaxUtilityYPosition.get(0);
                        int py2 = MaxUtilityYPosition.get(1);
                        if (py1 > itemJ.position()) {
                            newElement.ListMaxUtilityXPosition.add(px1);
                            newElement.ListMaxUtilityYPosition.add(py1);
                        } else {
                            newElement.ListMaxUtilityXPosition.add(px2);
                            newElement.ListMaxUtilityYPosition.add(py2);
                        }
                    }

                    if (num == 2) {
                        int px1 = MaxUtilityXPosition.get(0);
                        int px2 = MaxUtilityXPosition.get(1);

                        int py1 = MaxUtilityYPosition.get(0);
                        int py2 = MaxUtilityYPosition.get(1);


                        ItemWithUtilityPosition ItemY1 = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> Y1Itemset = sequence.getItemset(py1);
                        for (ItemWithUtilityPosition item : Y1Itemset) {
                            if (item.getItemName().equals(consequent[0])) {
                                ItemY1 = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition ItemY2 = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> Y2Itemset = sequence.getItemset(py2);
                        for (ItemWithUtilityPosition item : Y2Itemset) {
                            if (item.getItemName().equals(consequent[0])) {
                                ItemY2 = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition YSecondMaxUtility1 = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpY1 = ItemY1;
                        double tmpUtility1 = tmpY1.utility();

                        while (tmpY1.NextPosition != -1) {
                            int NextPosition = tmpY1.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpY1.getItemName())) {
                                    tmpY1 = item;
                                    if (item.position() > itemJ.position() && YSecondMaxUtility1.utility() <= item.utility()) {
                                        YSecondMaxUtility1 = item;
                                    }
                                }
                            }
                        }
                        double YDeltaUtility1 = tmpUtility1 - YSecondMaxUtility1.utility();

                        ItemWithUtilityPosition YSecondMaxUtility2 = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpY2 = ItemY2;
                        double tmpUtility2 = tmpY2.utility();

                        while (tmpY2.NextPosition != -1) {
                            int NextPosition = tmpY2.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpY2.getItemName())) {
                                    tmpY2 = item;
                                    if (item.position() > itemJ.position() && YSecondMaxUtility2.utility() <= item.utility()) {
                                        YSecondMaxUtility2 = item;
                                    }
                                }
                            }
                        }
                        double YDeltaUtility2 = tmpUtility2 - YSecondMaxUtility2.utility();

                        if (YDeltaUtility1 < YDeltaUtility2) {
                            YDeltaUtility = YDeltaUtility1;
                            newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility2.position());
                            newElement.ListMaxUtilityXPosition.add(px1);
                        } else if (YDeltaUtility1 > YDeltaUtility2) {
                            YDeltaUtility = YDeltaUtility2;
                            newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility2.position());
                            newElement.ListMaxUtilityXPosition.add(px2);
                        } else {
                            YDeltaUtility = YDeltaUtility1;
                            if (YSecondMaxUtility1.position() <= YSecondMaxUtility2.position()) {
                                newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility1.position());
                                newElement.ListMaxUtilityXPosition.add(px1);
                            } else {
                                newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility2.position());
                                newElement.ListMaxUtilityXPosition.add(px2);
                            }
                        }
                    }


                    newElement.utility = element.utility + itemJ.utility() - YDeltaUtility + XDeltaUtility;

                    double MaxProfitItemJ1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, 0, itemJ.position(), sequence, 0).utility();
                    double MaxProfitItemJ2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, itemJ.position(), element.positionBetaItemset - 1, sequence, 1).utility();


                    // If the user deactivate strategy 4, we will store the lrutil in the column
                    // called lutil
                    if (deactivateStrategy4) {
                        newElement.utilityLeft = element.utilityLeft + element.utilityLeftRight
                                + element.utilityRight - Math.max(itemJ.utility(), Math.max(MaxProfitItemJ1, MaxProfitItemJ2));
                    } else {
                        // otherwise we really calculate the lutil
                        newElement.utilityLeft = element.utilityLeft + element.utilityLeftRight - Math.max(itemJ.utility(), Math.max(MaxProfitItemJ1, MaxProfitItemJ2));
                    }


                    // Then, we will scan itemsets from the first one until the beta -1  itemset
                    // in the sequence.
                    // We will subtract the utility of items that are smaller than item J
                    // according to the lexicographical order from "lutil" because they
                    // cannot be added anymore to the new rule.

                    // For each itemset before the beta itemset
                    Map<Integer, Boolean> ItemHasBeenSeem = new HashMap<>();
                    for (int z = 0; z < element.positionBetaItemset; z++) {
                        List<ItemWithUtilityPosition> itemsetZ = sequence.getItemsets().get(z);

                        // For each item W in that itemset
                        for (int w = itemsetZ.size() - 1; w >= 0; w--) {
                            ItemWithUtilityPosition itemW = itemsetZ.get(w);

                            // if the item is smaller than the larger item in the left side of the rule
                            if (itemW.getItemName() <= largestItemInAntecedent) {
                                // we break;
                                break;
                            }

                            // Determine if itemW has duplicates in Beta
                            Duplicate = false;
                            for (int value : consequent) {
                                if (itemW.getItemName().equals(value)) {
                                    Duplicate = true;
                                    break;
                                }
                            }
                            if (Duplicate) {
                                continue;
                            }

                            if (!ItemHasBeenSeem.containsKey(itemW.getItemName())) {
                                ItemHasBeenSeem.put(itemW.getItemName(), true);
                            } else {
                                continue;
                            }

                            // otherwise, if item W is smaller than item J
                            // LeftExpandable becomes unExpandable
                            if (itemJ.getItemName() > itemW.getItemName()) {
                                // We will subtract the utility of W from "rutil"
                                double MaxProfitItem1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), element.positionBetaItemset - 1, sequence, 1).utility();
                                newElement.utilityLeft -= Math.max(itemW.utility(), MaxProfitItem1);
                            }
                        }
                    }
                    // end

                    newElement.positionBetaItemset = element.positionBetaItemset;


                    // Now that we have created the element for that sequence and that new rule
                    // we will add the utility table of that new rule
                    tableItemJ.addElement(newElement, element.numeroSequence);

                }
            }
        }

        // After that for each new rule, we create a table to store the beta values
        // for each sequence where the new rule appears.
        // The reason is that the "beta" column of any new rules that will be generated
        // by recursively adding to the left, will staty the same. So we don't store it in the
        // utility tble of the rule directly but in a separated structure.

        // Beta is a map where the key is a sequence id
        //   and the key is the position of an itemset in the sequence.
        Map<Integer, Integer> tableBeta = null;


        // For each new rule
        for (Entry<Integer, UtilityTableLeft> entryItemTable : mapItemUtilityTable.entrySet()) {
            // We get the item that was added to create the new rule
            Integer item = entryItemTable.getKey();
            // We get the utility table of the new rule
            UtilityTableLeft tableItem = entryItemTable.getValue();
            tableItem.CalculateUtility();


            // We check if we should try to expand its left side
            boolean shouldExpandLeftSide = tableItem.utility + tableItem.utilityLeft >= minutil
                    && antecedent.length + 1 < maxSizeAntecedent;

            // We need to calculate the list of sequences ids containing the antecedent of the new
            // rule since the antecedent has changed
            ListSequenceIDs sequenceIdentifiersNewAntecedent = null;

            // To calculate the confidence
            double confidence = 0;

            // If we should try to expand the left side of the rule
            // or if the rule is high utility, we recalculate the sequences ids containing
            // the antecedent
            if (shouldExpandLeftSide || tableItem.utility >= minutil) {
                // We obtain the list of sequence ids for the item
                ListSequenceIDs sequencesIdsItem = mapItemSequences.get(item);

                // We perform the intersection of the sequences ids of the antecedent
                // with those of the item to obtain the sequence ids of the new antecedent.
                sequenceIdentifiersNewAntecedent = sequenceIDsConsequent.intersection(sequencesIdsItem);

                // we calculate the confidence
                confidence = (double) tableItem.MapSequenceMaxUtilityList.size() / (double) sequenceIdentifiersNewAntecedent.getSize();
            }

            // if the new rule is high utility and has a high confidence
            boolean isHighUtilityAndHighConfidence = tableItem.utility >= minutil && confidence >= minConfidence;
            if (isHighUtilityAndHighConfidence) {

                // We create the antecedent for the new rule by appending the new item
                int[] nouvelAntecedent = new int[antecedent.length + 1];
                System.arraycopy(antecedent, 0, nouvelAntecedent, 0, antecedent.length);
                nouvelAntecedent[antecedent.length] = item;


                // We save the rule to file
                saveRule(nouvelAntecedent, consequent, tableItem.utility, tableItem.MapSequenceMaxUtilityList.size(), confidence);

                // If we are in debugging mode, we will show the rule in the console
                if (DEBUG) {
                    System.out.println("\n\t  HIGH UTILITY SEQ. RULE: " + Arrays.toString(antecedent) + "," + item +
                            " --> " + Arrays.toString(consequent) + "   utility " + tableItem.utility
                            + " support : " + utilityTable.MapSequenceMaxUtilityList.size()
                            + " confidence : " + confidence);

                    for (ElementTableLeft element : tableItem.elements) {
                        System.out.println("\t      SEQ:" + element.sequenceID + " \t utility: " + element.utility
                                + " \t lutil: " + element.utilityLeft);
                    }
                }

            } else {
                // if we are in debuging mode
                if (DEBUG) {
                    System.out.println("\n\t  LOW UTILITY SEQ. RULE: " + Arrays.toString(antecedent) + "," + item +
                            " --> " + Arrays.toString(consequent) + "   utility " + tableItem.utility
                            + " support : " + utilityTable.MapSequenceMaxUtilityList.size()
                            + " confidence : " + confidence);

                    for (ElementTableLeft element : tableItem.elements) {
                        System.out.println("\t      SEQ:" + element.sequenceID + " \t utility: " + element.utility
                                + " \t lutil: " + element.utilityLeft);
                    }
                }
            }
            // If we should try to expand the left side of this rule
            if (shouldExpandLeftSide) {
                // We create the antecedent for the new rule by appending the new item
                int[] newAntecedent = new int[antecedent.length + 1];
                System.arraycopy(antecedent, 0, newAntecedent, 0, antecedent.length);
                newAntecedent[antecedent.length] = item;

                // We create the table for storing the beta position in each sequence
                if (tableBeta == null) {
                    tableBeta = new HashMap<Integer, Integer>();
                    // We loop over each line from the original utility table and copy the
                    // beta value for each line

                    // For each element of the utility of the original rule
                    for (ElementOfTable element : utilityTable.elements) {
                        // copy the beta position
                        tableBeta.put(element.numeroSequence, element.positionBetaItemset);
                    }
                }

                // we recursively try to expand this rule
                expandSecondLeft(tableItem, newAntecedent, consequent, sequenceIdentifiersNewAntecedent, tableBeta);

            }
        }
        // We check the memory usage for statistics
        MemoryLogger.getInstance().checkMemory();
    }


    /**
     * This method will recursively try to append items to the left side of a rule to generate
     * rules containing one more item on the left side.  This method is called for rules
     * containing at least 2 items on their left side already. For rules having 1 item on their left side
     * another method is used instead.
     *
     * @param utilityTable          the rule utility table
     * @param antecedent            the rule antecedent
     * @param consequent            the rule consequent
     * @param sequenceIDsConsequent the list of sequences ids of sequences containing the rule antecedent
     * @throws IOException if error while writting to file
     */

    private void expandSecondLeft(
            UtilityTableLeft utilityTable,
            int[] antecedent, int[] consequent,
            ListSequenceIDs sequenceIDsConsequent,
            Map<Integer, Integer> tableBeta) throws IOException {


        // We first find the largest item in the left side aof the rule
        int largestItemInAntecedent = antecedent[antecedent.length - 1];

        // We create a new map where we will build the utility table for the new rules that
        // will be created by adding an item to the current rule.
        // Key: an item appended to the rule     Value: the utility-table of the corresponding new rule
        Map<Integer, UtilityTableLeft> mapItemUtilityTable = new HashMap<Integer, UtilityTableLeft>();

        // for each sequence containing the rule (a line in the utility table of the original rule)
        for (ElementTableLeft element : utilityTable.elements) {
            // Optimisation: if the "lutil" is 0 for that rule in that sequence,
            // we do not need to scan this sequence.
            if (element.utilityLeft == 0) {
                continue;
            }

            // Get the sequence
            Sequence sequence = database.getSequences().get(element.sequenceID);

            // Get the beta position in that sequence
            Integer positionBetaItemset = element.positionBetaItemset;

            // For each itemset before beta
            for (int i = 0; i < positionBetaItemset; i++) {
                List<ItemWithUtilityPosition> itemsetI = sequence.getItemsets().get(i);

                //for each  item
                for (int j = 0; j < itemsetI.size(); j++) {
                    ItemWithUtilityPosition itemJ = itemsetI.get(j);


                    // Check if the item is greater than items in the antecedent of the rule
                    // according to the lexicographical order
                    if (itemJ.getItemName() <= largestItemInAntecedent) {
                        continue;
                    }

                    // Determine if itemJ has duplicates in Beta
                    boolean Duplicate = false;
                    for (int value : consequent) {
                        if (itemJ.getItemName().equals(value)) {
                            Duplicate = true;
                            break;
                        }
                    }
                    if (Duplicate) {
                        continue;
                    }


                    // ======= Otherwise, we need to update the utility table of the item ====================
                    // Get the utility table of the item
                    UtilityTableLeft tableItemJ = mapItemUtilityTable.get(itemJ.getItemName());
                    if (tableItemJ == null) {
                        // if no utility table, we create one
                        tableItemJ = new UtilityTableLeft();
                        mapItemUtilityTable.put(itemJ.getItemName(), tableItemJ);
                    }


                    // We will add a new element (line) in the utility table
                    ElementTableLeft newElement = new ElementTableLeft(element.sequenceID);
                    //newElement.MaxUtilityYPosition=element.MaxUtilityYPosition;


                    // we will update the utility vlaue of that rule by adding the utility of the item
                    // in that sequence
                    // double utilityItemJ = sequence.getUtilities().get(i).get(j);


                    List<Integer> MaxUtilityXPosition = element.ListMaxUtilityXPosition;
                    List<Integer> MaxUtilityYPosition = element.ListMaxUtilityYPosition;
                    int num = 0;
                    for (Integer YPosition : MaxUtilityYPosition) {
                        if (YPosition <= itemJ.position()) {
                            num++;
                        }
                    }


                    double YDeltaUtility = 0;
                    double XDeltaUtility = 0;

                    if (num == 0) {
                        newElement.ListMaxUtilityXPosition = element.ListMaxUtilityXPosition;
                        newElement.ListMaxUtilityYPosition = element.ListMaxUtilityYPosition;
                    }

                    if (num == 1 && MaxUtilityYPosition.size() == 1) {

                        ItemWithUtilityPosition ItemY = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> YItemset = sequence.getItemset(MaxUtilityYPosition.get(0));
                        for (ItemWithUtilityPosition item : YItemset) {
                            if (item.getItemName().equals(consequent[0])) {
                                ItemY = item;
                                break;
                            }
                        }
                        ItemWithUtilityPosition YSecondMaxUtility = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpY = ItemY;

                        while (tmpY.NextPosition != -1) {
                            int NextPosition = tmpY.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpY.getItemName())) {
                                    tmpY = item;
                                    if (item.position() > itemJ.position() && YSecondMaxUtility.utility() <= item.utility()) {
                                        YSecondMaxUtility = item;
                                    }
                                }
                            }
                        }
                        YDeltaUtility = ItemY.utility() - YSecondMaxUtility.utility();
                        newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility.position());

                        ItemWithUtilityPosition ItemX = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> XItemset = sequence.getItemset(MaxUtilityXPosition.get(0));
                        for (ItemWithUtilityPosition item : XItemset) {
                            if (item.getItemName().equals(antecedent[0])) {
                                ItemX = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition tmpX = ItemX;
                        ItemWithUtilityPosition FoundX = ItemX;
                        //double tmpXUtility = ItemX.utility();
                        while (tmpX.NextPosition != -1 && tmpX.NextPosition < YSecondMaxUtility.position() && tmpX.NextPosition < element.positionBetaItemset) {
                            int NextPosition = tmpX.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(ItemX.getItemName())) {
                                    tmpX = item;
                                    if (item.utility() > FoundX.utility()) {
                                        FoundX = item;
                                    }
                                }
                            }
                        }
                        XDeltaUtility = FoundX.utility() - ItemX.utility();
                        newElement.ListMaxUtilityXPosition.add(FoundX.position());
                    }

                    if (num == 1 && MaxUtilityYPosition.size() == 2) {
                        int px1 = MaxUtilityXPosition.get(0);
                        int px2 = MaxUtilityXPosition.get(1);

                        int py1 = MaxUtilityYPosition.get(0);
                        int py2 = MaxUtilityYPosition.get(1);
                        if (py1 > itemJ.position()) {
                            newElement.ListMaxUtilityXPosition.add(px1);
                            newElement.ListMaxUtilityYPosition.add(py1);
                        } else {
                            newElement.ListMaxUtilityXPosition.add(px2);
                            newElement.ListMaxUtilityYPosition.add(py2);
                        }
                    }

                    if (num == 2) {
                        int px1 = MaxUtilityXPosition.get(0);
                        int px2 = MaxUtilityXPosition.get(1);

                        int py1 = MaxUtilityYPosition.get(0);
                        int py2 = MaxUtilityYPosition.get(1);


                        ItemWithUtilityPosition ItemY1 = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> Y1Itemset = sequence.getItemset(py1);
                        for (ItemWithUtilityPosition item : Y1Itemset) {
                            if (item.getItemName().equals(consequent[0])) {
                                ItemY1 = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition ItemY2 = new ItemWithUtilityPosition(-1, -1, -1);
                        List<ItemWithUtilityPosition> Y2Itemset = sequence.getItemset(py2);
                        for (ItemWithUtilityPosition item : Y2Itemset) {
                            if (item.getItemName().equals(consequent[0])) {
                                ItemY2 = item;
                                break;
                            }
                        }

                        ItemWithUtilityPosition YSecondMaxUtility1 = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpY1 = ItemY1;
                        //double tmpUtility1 = tmpY1.utility();

                        while (tmpY1.NextPosition != -1) {
                            int NextPosition = tmpY1.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpY1.getItemName())) {
                                    tmpY1 = item;
                                    if (item.position() > itemJ.position() && YSecondMaxUtility1.utility() < item.utility()) {
                                        YSecondMaxUtility1 = item;
                                    }
                                }
                            }
                        }
                        double YDeltaUtility1 = ItemY1.utility() - YSecondMaxUtility1.utility();

                        ItemWithUtilityPosition YSecondMaxUtility2 = new ItemWithUtilityPosition(-1, -1, -1);
                        ItemWithUtilityPosition tmpY2 = ItemY2;
                        //double tmpUtility2 = tmpY2.utility();

                        while (tmpY2.NextPosition != -1) {
                            int NextPosition = tmpY2.NextPosition;
                            List<ItemWithUtilityPosition> Itemset = sequence.getItemset(NextPosition);
                            for (ItemWithUtilityPosition item : Itemset) {
                                if (item.getItemName().equals(tmpY2.getItemName())) {
                                    tmpY2 = item;
                                    if (item.position() > itemJ.position() && YSecondMaxUtility2.utility() < item.utility()) {
                                        YSecondMaxUtility2 = item;
                                    }
                                }
                            }
                        }
                        double YDeltaUtility2 = ItemY2.utility() - YSecondMaxUtility2.utility();

                        if (YDeltaUtility1 < YDeltaUtility2) {
                            YDeltaUtility = YDeltaUtility1;
                            newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility2.position());
                            newElement.ListMaxUtilityXPosition.add(px1);
                        } else if (YDeltaUtility1 > YDeltaUtility2) {
                            YDeltaUtility = YDeltaUtility2;
                            newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility2.position());
                            newElement.ListMaxUtilityXPosition.add(px2);
                        } else {
                            YDeltaUtility = YDeltaUtility1;
                            if (YSecondMaxUtility1.position() <= YSecondMaxUtility2.position()) {
                                newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility1.position());
                                newElement.ListMaxUtilityXPosition.add(px1);
                            } else {
                                newElement.ListMaxUtilityYPosition.add(YSecondMaxUtility2.position());
                                newElement.ListMaxUtilityXPosition.add(px2);
                            }
                        }
                    }


                    newElement.utility = element.utility + itemJ.utility() - YDeltaUtility + XDeltaUtility;


                    // The lutil value is updated by subtracting the utility of the item
                    double MaxProfitItemJ1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, 0, itemJ.position(), sequence, 0).utility();
                    double MaxProfitItemJ2 = test.MaxUtilityItemBetweenAlphaAndBeta(itemJ, itemJ.position(), positionBetaItemset - 1, sequence, 1).utility();
                    newElement.utilityLeft = element.utilityLeft - Math.max(itemJ.utility(), Math.max(MaxProfitItemJ1, MaxProfitItemJ2));

                    // Then, we will scan itemsets from the first one until the beta -1  itemset
                    // in the sequence.
                    // We will subtract the utility of items that are smaller than item J
                    // according to the lexicographical order from "lutil" because they
                    // cannot be added anymore to the new rule.

                    // for each itemset
                    Map<Integer, Boolean> ItemHasBeenSeem = new HashMap<>();
                    for (int z = 0; z < positionBetaItemset; z++) {
                        List<ItemWithUtilityPosition> itemsetZ = sequence.getItemset(z);

                        // for each item
                        for (int w = itemsetZ.size() - 1; w >= 0; w--) {
                            ItemWithUtilityPosition itemW = itemsetZ.get(w);
                            // if the item is smaller than the larger item in the left side of the rule
                            if (itemW.getItemName() <= largestItemInAntecedent) {
                                break;
                            }

                            // Determine if itemW has duplicates in Beta
                            Duplicate = false;
                            for (int value : consequent) {
                                if (itemW.getItemName().equals(value)) {
                                    Duplicate = true;
                                    break;
                                }
                            }
                            if (Duplicate) {
                                continue;
                            }

                            if (!ItemHasBeenSeem.containsKey(itemW.getItemName())) {
                                ItemHasBeenSeem.put(itemW.getItemName(), true);
                            } else {
                                continue;
                            }

                            // otherwise, if item W is smaller than item J
                            if (itemW.getItemName() < itemJ.getItemName()) {
                                // We will subtract the utility of W from "lutil"
                                double MaxProfitItem1 = test.MaxUtilityItemBetweenAlphaAndBeta(itemW, itemW.position(), positionBetaItemset - 1, sequence, 1).utility();
                                newElement.utilityLeft -= Math.max(itemW.utility(), MaxProfitItem1);
                            }
                        }
                    }
                    // end

                    newElement.positionBetaItemset = element.positionBetaItemset;


                    // Now that we have created the element for that sequence and that new rule
                    // we will add that element to tthe utility table of that new rule
                    tableItemJ.addElement(newElement, element.sequenceID);

                }
            }
        }

        // For each new rule
        for (Entry<Integer, UtilityTableLeft> entryItemTable : mapItemUtilityTable.entrySet()) {
            // We get the item that was added to create the new rule
            Integer item = entryItemTable.getKey();
            // We get the utility table of the new rule
            UtilityTableLeft tableItem = entryItemTable.getValue();
            tableItem.CalculateUtility();

            // We check if we should try to expand its left side
            boolean shouldExpandLeft = tableItem.utility + tableItem.utilityLeft >= minutil
                    && antecedent.length + 1 < maxSizeAntecedent;

            // We check if the rule is high utility
            boolean isHighUtility = tableItem.utility >= minutil;

            double confidence = 0;

            // We need to calculate the list of sequences ids containing the antecedent of the new
            // rule since the antecedent has changed
            ListSequenceIDs sequenceIdentifiersNewAntecedent = null;

            // If we should try to expand the left side of the rule
            // or if the rule is high utility, we recalculate the sequences ids containing
            // the antecedent
            if (shouldExpandLeft || isHighUtility) {
                // We obtain the list of sequence ids for the item
                ListSequenceIDs numerosequencesItem = mapItemSequences.get(item);

                // We perform the intersection of the sequences ids of the antecedent
                // with those of the item to obtain the sequence ids of the new antecedent.
                sequenceIdentifiersNewAntecedent = sequenceIDsConsequent.intersection(numerosequencesItem);

                // we calculate the confidence
                confidence = (double) tableItem.MapSequenceMaxUtilityList.size() / (double) sequenceIdentifiersNewAntecedent.getSize();
            }

            // if the new rule is high utility and has a high confidence
            if (isHighUtility && confidence >= minConfidence) {

                // We create the antecedent for the new rule by appending the new item
                int[] newAntecedent = new int[antecedent.length + 1];
                System.arraycopy(antecedent, 0, newAntecedent, 0, antecedent.length);
                newAntecedent[antecedent.length] = item;

                // We save the rule to file
                saveRule(newAntecedent, consequent, tableItem.utility, tableItem.MapSequenceMaxUtilityList.size(), confidence);

                // If we are in debugging mode, we will show the rule in the console
                if (DEBUG) {
                    // print the rule
                    System.out.println("\n\t  HIGH UTILITY SEQ. RULE: " + Arrays.toString(antecedent) + "," + item +
                            " --> " + Arrays.toString(consequent) + "   utility " + tableItem.utility
                            + " support : " + utilityTable.MapSequenceMaxUtilityList.size()
                            + " confidence : " + confidence);

                    for (ElementTableLeft element : tableItem.elements) {
                        System.out.println("\t      SEQ:" + element.sequenceID + " \t utility: " + element.utility
                                + " \t lutil: " + element.utilityLeft);
                    }
                }
            } else {
                // if we are in debuging mode
                if (DEBUG) {
                    // print the rule
                    System.out.println("\n\t  LOW UTILITY SEQ. RULE: " + Arrays.toString(antecedent) + "," + item +
                            " --> " + Arrays.toString(consequent) + "   utility " + tableItem.utility
                            + " support : " + utilityTable.MapSequenceMaxUtilityList.size()
                            + " confidence : " + confidence);

                    for (ElementTableLeft element : tableItem.elements) {
                        System.out.println("\t      SEQ:" + element.sequenceID + " \t utility: " + element.utility
                                + " \t lutil: " + element.utilityLeft);
                    }
                }
            }

            // If we should try to expand the left side of this rule
            if (shouldExpandLeft) {
                // We create the antecedent for the new rule by appending the new item
                int[] nouvelAntecedent = new int[antecedent.length + 1];
                System.arraycopy(antecedent, 0, nouvelAntecedent, 0, antecedent.length);
                nouvelAntecedent[antecedent.length] = item;


                // we recursively call this method
                expandSecondLeft(tableItem, nouvelAntecedent, consequent, sequenceIdentifiersNewAntecedent, tableBeta);
            }
        }
        // We check the memory usage
        MemoryLogger.getInstance().checkMemory();
    }


    /**
     * Print statistics about the last algorithm execution to System.out.
     *
     * @param input
     */
    public void printStats(String input) {
        System.out.println("=============== USER algorithm pruning 1 ===================");
//		System.out.println(" minutil: " + minutil);
        System.out.println(" Sequential rules count: " + ruleCount);
        System.out.println(" Total time : " + (timeEnd - timeStart) + " ms");
        System.out.println(" Max memory (mb) : "
                + MemoryLogger.getInstance().getMaxMemory());
        System.out.println("============================================================");
    }


//============================================================================================================================
// =========================================== CLASSES FOR STORING LISTS OF SEQUENCE IDs===================
//============================================================================================================================

    /**
     * This interface represents a list of sequences ids
     *
     * @author Souleymane Zida, Philippe Fournier-Viger
     */
    public interface ListSequenceIDs {

        /**
         * This method adds a sequence id to this list
         */
        public abstract void addSequenceID(int noSequence);

        /**
         * Get the number of sequence ids
         *
         * @return the number of sequence ids
         */
        public abstract int getSize();

        /**
         * Method to intersect two lists of sequences ids
         *
         * @return the intersection of this list and the other list.
         */
        public abstract ListSequenceIDs intersection(ListSequenceIDs vector2);
    }

    /**
     * This class represents a list of sequences ids implemented by a bit vector
     *
     * @author Souleymane Zida, Philippe Fournier-Viger
     */
    public class ListSequenceIDsBitVector implements ListSequenceIDs {
        // the internal bitset
        private BitSet bitset = new BitSet();
        // the number of bit set to 1 in the bitset
        private int size = -1;

        /**
         * Constructor
         */
        public ListSequenceIDsBitVector() {
        }

        @Override
        /**
         * This method adds a sequence id to this list
         * @param int the sequence id
         */
        public void addSequenceID(int bit) {
            bitset.set(bit);
        }

        /**
         * Get the number of sequence ids
         *
         * @return the number of sequence ids
         */
        public int getSize() {
            // if we don't know the size
            if (size == -1) {
                // we calculate it but remember it in variable "size" for future use.
                size = bitset.cardinality();
            }
            // return the size
            return size;
        }

        /**
         * Method to intersect two lists of sequences ids
         *
         * @return the intersection of this list and the other list.
         */
        public ListSequenceIDs intersection(ListSequenceIDs vector2) {
            //  we get the first vector
            ListSequenceIDsBitVector bitVector2 = (ListSequenceIDsBitVector) vector2;

            // we create a new vector for the result
            ListSequenceIDsBitVector result = new ListSequenceIDsBitVector();
            // we clone the first bit vecotr
            result.bitset = (BitSet) bitset.clone();
            // we intersect both bit vector
            result.bitset.and(bitVector2.bitset);
            // Return the result
            return result;
        }

        /**
         * Get a string representation of this list
         *
         * @return a string
         */
        public String toString() {
            return bitset.toString();
        }
    }

//==================================

    /**
     * This class represents a list of sequences ids implemented by an array list
     *
     * @author Souleymane Zida, Philippe Fournier-Viger
     */
    public class ListSequenceIDsArrayList implements ListSequenceIDs {
        // the internal array list representation
        List<Integer> list = new ArrayList<Integer>();

        /**
         * Constructor
         */
        public ListSequenceIDsArrayList() {
        }

        /**
         * This method adds a sequence id to this list
         */
        public void addSequenceID(int noSequence) {
            list.add(noSequence);
        }


        /**
         * Get the number of sequence ids
         *
         * @return the number of sequence ids
         */
        public int getSize() {
            return list.size();
        }

        /**
         * Method to intersect two lists of sequences ids
         *
         * @return the intersection of this list and the other list.
         */
        public ListSequenceIDs intersection(ListSequenceIDs list2) {
            // Get the second list
            ListSequenceIDsArrayList arrayList2 = (ListSequenceIDsArrayList) list2;
            // Create a new list for the result
            ListSequenceIDs result = new ListSequenceIDsArrayList();

            // for each sequence id in this list
            for (Integer no : list) {
                // if it appear in the second list
                boolean appearInSecondList = Collections.binarySearch(arrayList2.list, no) >= 0;
                if (appearInSecondList) {
                    // then we add it to the new list
                    result.addSequenceID(no);
                }
            }
            // return the result
            return result;
        }

        /**
         * Get a string representation of this list
         *
         * @return a string
         */
        public String toString() {
            return list.toString();
        }
    }

//============================================================================================================================
// =========================================== CLASS FOR LEFT-UTILITY-TABLES ===========================================
//============================================================================================================================

    /**
     * This class represents a utility-table used for left expansions (what we call a left-utility table)
     *
     * @author Souleymane Zida, Philippe Fournier-Viger
     */
    public class UtilityTableLeft {
        // the list of elements (lines) in that utility table
        List<ElementTableLeft> elements = new ArrayList<ElementTableLeft>();
        // the total utility in that table
        int utility = 0;
        // the toal lutil values of elements in that table
        int utilityLeft = 0;


        Map<Integer, List<Double>> MapSequenceMaxUtilityList = new HashMap<>();

        /**
         * Constructor
         */
        public UtilityTableLeft() {
        }

        /**
         * Add a new element (line) to that table
         *
         * @param element the new element
         */
        public void addElement(ElementTableLeft element) {
            // add the element
            elements.add(element);
            // add the utility of this element to the total utility of that table
            utility += element.utility;
            // add the "lutil" utilit of this element to the total for that table
            utilityLeft += element.utilityLeft;
        }

        public void addElement(ElementTableLeft element, int SequenceId) {
            elements.add(element);
            if (!MapSequenceMaxUtilityList.containsKey(SequenceId)) {
                List<Double> SequenceMaxUtilityList = new ArrayList<>();
                SequenceMaxUtilityList.add(element.utility);
                SequenceMaxUtilityList.add(element.utilityLeft);
                MapSequenceMaxUtilityList.put(SequenceId, SequenceMaxUtilityList);
            } else {
                List<Double> SequenceMaxUtilityList = MapSequenceMaxUtilityList.get(SequenceId);
                if (SequenceMaxUtilityList.get(0) < element.utility) {
                    SequenceMaxUtilityList.set(0, element.utility);
                }
                if (SequenceMaxUtilityList.get(1) < element.utilityLeft) {
                    SequenceMaxUtilityList.set(1, element.utilityLeft);
                }
            }
        }

        public void CalculateUtility() {
            for (Entry<Integer, List<Double>> entry : MapSequenceMaxUtilityList.entrySet()) {
                List<Double> MaxUtilityList = entry.getValue();
                utility += MaxUtilityList.get(0);
                utilityLeft += MaxUtilityList.get(1);
            }
        }

    }

    /**
     * This class represents a element(line) of a utility-table used for left expansions
     *
     * @author Souleymane Zida, Philippe Fournier-Viger
     */
    public class ElementTableLeft {
        // the corresponding sequence id
        int sequenceID;
        // the utility
        double utility;
        // the "lutil" value
        double utilityLeft;

        int positionBetaItemset = -1;

        List<Integer> ListMaxUtilityXPosition = new ArrayList<>();
        List<Integer> ListMaxUtilityYPosition = new ArrayList<>();

        /**
         * Constructor
         *
         * @param sequenceID the sequence id
         */
        public ElementTableLeft(int sequenceID) {
            this.sequenceID = sequenceID;
            this.utility = 0;
            this.utilityLeft = 0;
        }

        /**
         * Constructor
         *
         * @param sequenceID  a sequence id
         * @param utility     the utility
         * @param utilityLeft the lutil value
         */
        public ElementTableLeft(int sequenceID, int utility, int utilityLeft) {
            this.sequenceID = sequenceID;
            this.utility = utility;
            this.utilityLeft = utilityLeft;
        }
    }


//============================================================================================================================
// ===========================================  CLASS FOR LEFT-RIGHT UTILITY-TABLES===========================================
//============================================================================================================================


    /**
     * This class represents a utility-table used for left or right expansions (what we call a left-right utility table)
     *
     * @author Souleymane Zida, Philippe Fournier-Viger
     */
    public class UtilityTable {
        // the list of elements (lines) in that utility table
        List<ElementOfTable> elements = new ArrayList<ElementOfTable>();
        // the total utility in that table
        double totalUtility = 0;
        // the total lutil values of elements in that table
        double totalUtilityLeft = 0;
        // the total lrutil values of elements in that table
        double totalUtilityLeftRight = 0;
        // the total rutil values of elements in that table
        double totalUtilityRight = 0;

        Map<Integer, List<Double>> MapSequenceMaxUtilityList = new HashMap<>();

        /**
         * Constructor
         */
        public UtilityTable() {

        }

        /**
         * Add a new element (line) to that table
         *
         * @param element the new element
         */
        public void addElement(ElementOfTable element) {
            // add the element
            elements.add(element);
            // make the sum of the utility, lutil, rutil and lrutil values
            totalUtility += element.utility;
            totalUtilityLeft += element.utilityLeft;
            totalUtilityLeftRight += element.utilityLeftRight;
            totalUtilityRight += element.utilityRight;
        }

        public void addElement(ElementOfTable element, int SequenceId) {
            elements.add(element);
            if (!MapSequenceMaxUtilityList.containsKey(SequenceId)) {
                List<Double> SequenceMaxUtilityList = new ArrayList<>();
                SequenceMaxUtilityList.add(element.utility);
                SequenceMaxUtilityList.add(element.utilityLeft);
                SequenceMaxUtilityList.add(element.utilityRight);
                SequenceMaxUtilityList.add(element.utilityLeftRight);
                MapSequenceMaxUtilityList.put(SequenceId, SequenceMaxUtilityList);
            } else {
                List<Double> SequenceMaxUtilityList = MapSequenceMaxUtilityList.get(SequenceId);
                if (SequenceMaxUtilityList.get(0) < element.utility) {
                    SequenceMaxUtilityList.set(0, element.utility);
                }
                if (SequenceMaxUtilityList.get(1) < element.utilityLeft) {
                    SequenceMaxUtilityList.set(1, element.utilityLeft);
                }
                if (SequenceMaxUtilityList.get(2) < element.utilityRight) {
                    SequenceMaxUtilityList.set(2, element.utilityRight);
                }
                if (SequenceMaxUtilityList.get(3) < element.utilityLeftRight) {
                    SequenceMaxUtilityList.set(3, element.utilityLeftRight);
                }
            }
        }

        public void CalculateUtility() {
            for (Entry<Integer, List<Double>> entry : MapSequenceMaxUtilityList.entrySet()) {
                List<Double> MaxUtilityList = entry.getValue();
                totalUtility += MaxUtilityList.get(0);
                totalUtilityLeft += MaxUtilityList.get(1);
                totalUtilityRight += MaxUtilityList.get(2);
                totalUtilityLeftRight += MaxUtilityList.get(3);
            }
        }

    }

    /**
     * This class represents a element(line) of a utility-table used for left or right expansions
     *
     * @author Souleymane Zida, Philippe Fournier-Viger
     */
    public class ElementOfTable {
        // the corresponding sequence id
        int numeroSequence;
        // the utility
        double utility;
        // the lutil value
        double utilityLeft;
        // the lrutil value
        double utilityLeftRight;
        // the rutil value
        double utilityRight;
        // the alpha and beta values
        int positionAlphaItemset = -1;
        int positionBetaItemset = -1;

        List<Integer> ListMaxUtilityXPosition = new ArrayList<>();
        List<Integer> ListMaxUtilityYPosition = new ArrayList<>();

        /**
         * Constructor
         *
         * @param sequenceID the sequence id
         */
        public ElementOfTable(int sequenceID) {
            this.numeroSequence = sequenceID;
            this.utility = 0;
            this.utilityLeft = 0;
            this.utilityLeftRight = 0;
            this.utilityRight = 0;
        }

        /**
         * Constructor
         *
         * @param sequenceID       a sequence id
         * @param utility          the utility
         * @param utilityLeft      the lutil value
         * @param utilityLeftRight the lrutil value
         * @param utilityRight     the rutil value
         */
        public ElementOfTable(int sequenceID,
                              double utility,
                              double utilityLeft,
                              double utilityLeftRight,
                              double utilityRight) {
            this.numeroSequence = sequenceID;
            this.utility = utility;
            this.utilityLeft = utilityLeft;
            this.utilityLeftRight = utilityLeftRight;
            this.utilityRight = utilityRight;
        }
    }

    public class MaxRuleInformation {
        double MaxUtility = 0;
        List<Integer> MaxUtilityXPosition = new ArrayList<>();
        List<Integer> MaxUtilityYPosition = new ArrayList<>();
    }


}
