import java.util.*;
/**
 * This class manages the application, does the calculations to get the probabilities, and checks how good the
 * classifier works
 */
public class Application {
    private ArrayList<String> file;
    private String[] file_header;
    private int feature_length;
    private int class_index;
    private ArrayList<ArrayList<String>> file_to_columns;
    private String missing_value_designation;
    public Application(ArrayList<String> file, String missing_value_designation) {
        //pop off header, make it a class variable
        this.missing_value_designation = missing_value_designation;
        //the first row of the file is the header, remove it and store it globally
        this.file_header = file.remove(0).split(",");
        this.file = file;
        //used to find the index the class is located, based on the header of the file
        this.class_index = getClassIndex(this.file_header);
        //Handle if class is not found
        if (this.class_index == -1) {
            System.out.println("Could not find class column, exiting");
            System.exit(1);
        }
        System.out.println("Non-Shuffled Version");
        System.out.println("----------------------------------");
        //run our modeling for a non-modified file
        runAlgorithmAndTests(file);
        //shuffle 10% of the features
        shuffleRandomTen();
        System.out.println("Shuffled Version");
        System.out.println("----------------------------------");
        //Run the algorithm and tests for the now shuffled file
        runAlgorithmAndTests(file);
    }
    private void runAlgorithmAndTests(ArrayList<String> file){
        //We have one class variable - so we can say our features are split -1, this is to manage shuffling features
        this.feature_length = this.file_header.length - 1;
        //get the file ot columns of the current file passed
        this.file_to_columns = fileToColumns(file);
        //impute the data that is missing using mean value of column
        ArrayList<ArrayList<String>> imputed_data = imputeMissing(this.file_to_columns);
        //bin the variables (if the header specifies a column needs to be binned)
        ArrayList<ArrayList<String>> binned = binContinuousValues(imputed_data,4);
        //swap the mapping back from column->row->value to row->column->value, easier access
        ArrayList<ArrayList<String>> backToRow = columnsToRow(binned);
        //separate the contents of the file into class distinctions
        HashMap<String,ArrayList<ArrayList<String>>> class_count_hash = processFileIntoMap(backToRow);
        //Outer->Inner, Array List containing rows of the file, containing values
        ArrayList<ArrayList<ArrayList<String>>> split_up = tenFoldSplit(class_count_hash);
        //using pure 0-1 loss, sum the total right and the total wrong we get when classifying
        int total_right = 0;
        int total_wrong = 0;
        //use the previous train on the next split
        HashMap<String,ArrayList<HashMap<String,Double>>> previous_train = null;
        for(ArrayList<ArrayList<String>> split: split_up){
            //if we have a previous train, check how well it classifies on the current split
            if(previous_train != null){
                int[] right_and_wrong = checkAccuracy(previous_train,split);
                //add up how good/bad it did
                total_right += right_and_wrong[0];
                total_wrong += right_and_wrong[1];
            }
            //set the new previous train to that of the current split
            previous_train = trainClassifier(split);
        }
        //do the final check, which would need training on the first split
        int[] right_and_wrong = checkAccuracy(previous_train,split_up.get(0));
        //add up how good/bad it did
        total_right += right_and_wrong[0];
        total_wrong += right_and_wrong[1];
        //communicate the values to the user
        System.out.println("Total Classified Right: " + total_right);
        System.out.println("Total Classified Wrong: " + total_wrong);
        System.out.println("0-1 Loss: " + String.format("%2.2f",((double)total_right/(total_right+total_wrong))*100.0) + "%");
    }
    /**
     * Split the data into a randomized grouping based on class
     * @param map map created by processFileIntoMap where the mapping is class->count
     * @return and ArrayList holding the split up groups
     */
    private ArrayList<ArrayList<ArrayList<String>>> tenFoldSplit(HashMap<String,ArrayList<ArrayList<String>>> map){
        //arraylist of arrays containing the split up groups
        ArrayList<ArrayList<ArrayList<String>>> split_groups = new ArrayList<>();
        //Initializing based on knowing we will have 10 groups
        int groups = 10;
        //initialize the data object
        for(int x = 0;x<groups;x++){
            split_groups.add(new ArrayList<>());
        }
        //set current group - used to loop around
        int current_group = 0;
        //we will be removing entries that are empty, due to this we can run the loop until our original map is empty
        while(!map.isEmpty()){
            //create a list of keys we need to remove
            ArrayList<String> remove_keys = new ArrayList<>();
            //create a set of keys we have available
            ArrayList<String> keys_available = new ArrayList<>(map.keySet());
            //pick a random key index in the available keys
            int random_class_pick = (int) (Math.random() * keys_available.size());
            //class pick is the actual class picked from the random generation
            String class_pick = keys_available.get(random_class_pick);
            //generate another random variable that specifies what line we will remove from the current class
            int random = (int) (Math.random() * map.get(class_pick).size());
            //add to the split groups
            split_groups.get(current_group).add(map.get(class_pick).remove(random));
            //if the class is now empty, add it to list of needed to be removed
            if (map.get(class_pick).isEmpty()) {
                remove_keys.add(class_pick);
            }
            //iterate the current group
            current_group++;
            //if we reached the maximum, loop back around
            if (current_group == groups) {
                current_group = 0;
            }
            //if we have keys we need to remove
            if(!remove_keys.isEmpty()){
                //iterate through them and remove them from the map
                for(String key:remove_keys){
                    map.remove(key);
                }
            }
        }
        return split_groups;
    }
    /**
     * Impute missing data using mean value imputation
     * @param column_map a map of the columns orientated column -> row -> value
     * @return the column map passed, with imputed data points filled in
     */
    private ArrayList<ArrayList<String>> imputeMissing(ArrayList<ArrayList<String>> column_map){
        //Hold our missing data values so we only have to iterate through once
        HashMap<Integer,HashSet<Integer>> missing_data_addresses = new HashMap<>();
        //Hold our mean values for each column so we can fill in missing_data_addresses
        HashMap<Integer,Integer> mean_values = new HashMap<>();
        //Iterate through the column map
        for(int x = 0;x<column_map.size();x++){
            //See if the header says we should impute
            boolean should_impute = hasAttribute(x,"impute");
            //If we are not imputing,skip this column
            if(!should_impute){
                continue;
            }
            //value used to store the sum of the column
            double total_value = 0;
            //value used to count total entries
            int double_count = 0;
            //For error checking so that we know when not to do calculation
            boolean column_is_not_double = false;
            //the current column we are in
            ArrayList<String> current_column = column_map.get(x);
            //iterate through the values of the column
            for(int y = 0;y<current_column.size();y++){
                try{
                    //get the value (which is a string)
                    String data = current_column.get(y);
                    //If the data has the value we are using to designate a missing value
                    if(data.equals(this.missing_value_designation)){
                        //see if we have an active hashset holding the address
                        HashSet<Integer> current = missing_data_addresses.get(x);
                        //If we don't have a count, init the hashset that stores it
                        if(current == null) {
                            //init the hashset, setting the x of it
                            missing_data_addresses.put(x,new HashSet<>());
                        }
                        //set the y of the address
                        missing_data_addresses.get(x).add(y);
                    //If the data is an actual value
                    }else{
                        //Add the double to the total
                        total_value += Double.parseDouble(data);
                        double_count++;
                    }
                //We tried to convert a double that could not be converted, break out and give warning
                }catch(NumberFormatException nfe){
                    System.out.println("When Imputing missing data value was unable to parse double");
                    column_is_not_double = true;
                    break;
                }
            }
            //If our column is valid then mean value can be calculated
            if(!column_is_not_double){
                int mean_value = (int)Math.round(total_value/double_count);
                //set the mean value of the column within the mean values hash
                mean_values.put(x,mean_value);
            }
        }
        //go through the tracked missing data addresses
        Set<Integer> keys = missing_data_addresses.keySet();
        for(Integer key: keys) {
            //go through the rows present in the missing data addresses
            for(int row : missing_data_addresses.get(key)){
                //in place change that address to the value of the mean for that column
                column_map.get(key).set(row,String.valueOf(mean_values.get(key)));
            }
        }
        return column_map;
    }
    /**
     * bin values based on the parameter present in the header of the data files into groups based on the range of
     * values present within the column - bins into equally sized factors
     * @param data the data that we want to group
     * @param bins the amount of bins we want to create
     * @return the data separated into the specified quantity of bins
     */
    private ArrayList<ArrayList<String>> binContinuousValues(ArrayList<ArrayList<String>> data,int bins){
        // holds [min of column, max of column, min of column...]
        ArrayList<Double> col_min_max = new ArrayList<>();
        //Create initial bins based on the value
        for(int column_index = 0;column_index < data.size();column_index++){
            //Only bin if the current column has the flag indicating it needs to be binned
            if(!hasAttribute(column_index,"bin")){
                continue;
            }
            if(hasAttribute(column_index,"bin-6")){
                bins = 6;
            }
            //set min and max to values that are higher than our datasets can reach
            double min = 999999;
            double max = -999999;
            //iterate through the values present, to calculate what the max and min for a column are
            for(String value : data.get(column_index)){
                try{
                    double value_double = Double.parseDouble(value);
                    if(value_double < min){
                        min = value_double;
                    }
                    if(value_double > max){
                        max = value_double;
                    }
                }catch(NumberFormatException nfe){
                    System.out.println("Key was not a double");
                    break;
                }
            }
            //add the min and max to the array
            col_min_max.add(min);
            col_min_max.add(max);
            //System.out.println("Our binning returned a max of " + max + " and a min of " + min + " for row " + column_index);
        }
        //Iterate through again, this time with the intention of changing the data such that it is binned
        for(int column_index = 0;column_index < data.size();column_index++){
            //Only bin if the current column has the flag indicating it needs to be binned
            if(!hasAttribute(column_index,"bin")){
                //System.out.println("bin flag not present");
                continue;
            }
            double[] bin_groups = new double[bins];
            //Iterate through the col_min_max to set values
            double min = col_min_max.get(column_index*2);
            double max = col_min_max.get((column_index*2) + 1);
            //calculate range
            double range = max - min;
            //System.out.println("Range was " + range);
            //get the intervals to break the range into
            double interval = range / bins;

            //create the bins
            for (int y = 0; y < bins; y++) {
                bin_groups[y] = (min + (interval * (y + 1)));
            }
            for(int row = 0;row<data.get(column_index).size();row++) {
                try {
                    //System.out.println("Unconverted: " + data.get(column_index).get(row));
                    double converted = new Double(data.get(column_index).get(row));
                   // System.out.println("Converted: " + converted);
                    for (int y = 0; y < bins; y++) {
                        //System.out.println(bin_groups[y] + "<=" + converted);
                        String bin_num = new String(String.valueOf(y));
                        if (converted <= bin_groups[y]) {
                         //   System.out.println("Setting bin group " + bin_num + " for row " + row);
                            data.get(column_index).set(row, bin_num);
                            break;
                        }
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("Could not parse double when binning");
                }
            }
        }
        return data;
    }
    /**
     * Turns the file into a map of class=>ArrayList of lines
     * @param data data that is orientated it columns->rows->value
     * @return a simple map of class->count of class within data
     */
    private HashMap<String, ArrayList<ArrayList<String>>> processFileIntoMap(ArrayList<ArrayList<String>> data){
        //result hash to store what we are returning
        HashMap<String,ArrayList<ArrayList<String>>> resultHash = new HashMap<>();
        //Go through each line
        for(ArrayList<String> row : data){
            String class_string = row.get(this.class_index);
            //Grab the current arraylist, could not be initialized!
            ArrayList<ArrayList<String>> current_arraylist = resultHash.get(class_string);
            //if it is not initialized
            if(current_arraylist == null){
                //make a new arraylist to go there
                ArrayList<ArrayList<String>> new_array = new ArrayList<>();
                //add the line
                new_array.add(row);
                //add it to the arraylist
                resultHash.put(class_string,new_array);
            }else{
                //the arraylist exists, just add to existing one
                current_arraylist.add(row);
            }
        }
        return resultHash;
    }
    /**
     * Could be used on a full set or during 10 fold cross validation
     * eventually returns P(X|Y) and P(X) where X is a class
     * @param data
     * @return returns a hashmap of class -> column -> value -> probability
     */
    private HashMap<String,ArrayList<HashMap<String,Double>>> trainClassifier(ArrayList<ArrayList<String>> data){
        HashMap<Integer,HashMap<String,Double>> hash_columns = new HashMap<>();
        for(int x = 0;x<this.file_header.length;x++){
            hash_columns.put(x,new HashMap<>());
        }
        //map that holds all of the probabilities of P(X|Y) and P(X) where X is a class
        HashMap<String,ArrayList<HashMap<String,Double>>> all_map = new HashMap<>();
        HashMap<String,Double> total = new HashMap<>();
        //go through the rows within the data
        for(ArrayList<String> row : data){
            //grab the classname from the row
            String class_name = row.get(class_index);
            //check if a total exists for the class
            Double current_total = total.get(class_name);
            //if it does not init a new double with value 1.0
            if(current_total  == null){
                total.put(class_name,1.0);
            }else{
                //else iterate occurrences and set the hash
                Double new_occurrence = current_total +1.0;
                total.put(class_name,new_occurrence);
            }
            //Iterate through the columns + values within the rows
            for(int x = 0;x<row.size();x++){
                //If the value is equal to the class name
                if(row.get(x).equals(class_name)){
                    //don't need it, have already counted class occurrences
                    continue;
                }
                //get the current location of where the hash would go
                ArrayList<HashMap<String,Double>> current_hash = all_map.get(class_name);
                boolean updated =  false;
                //if it is null, init it
                if(current_hash == null){
                    all_map.put(class_name, new ArrayList<>());
                    updated = true;
                }
                //grab the hash again if it has been updated
                if(updated){
                    current_hash = all_map.get(class_name);
                }
                //if the current hash is empty, initialize it
                if(current_hash.isEmpty()){
                    for(int y = 0;y<row.size();y++){
                        current_hash.add(new HashMap<>());
                    }
                }
                //get the location of the current occurrences
                Double column_value = current_hash.get(x).get(row.get(x));
                //if it is null, initialize it with 1
                if(column_value == null){
                    current_hash.get(x).put(row.get(x),1.0);
                }else{
                    //if it is not, add one to the current value
                    Double new_occurrence = column_value+ 1.0;
                    current_hash.get(x).put(row.get(x),new_occurrence);
                }
            }
        }
        //get the set of the hashes in all_map in order to iterate
        Set<String> hashes = all_map.keySet();
        //iterate through the hashes
        for(String hash_key : hashes){
            //get the hash that contains the string value + double of count
            ArrayList<HashMap<String,Double>> value_counts = all_map.get(hash_key);
            for(int x = 0;x<value_counts.size();x++){
                HashMap<String,Double> hash_counts = value_counts.get(x);
                int all_occurences = 0;
                Set<String> counts_set = hash_counts.keySet();
                for(String value : counts_set){
                    all_occurences += all_map.get(hash_key).get(x).get(value);
                }
                //second loop is to calculate the probability based on the current all_occurrences count
                for(String value : counts_set){
                    double probability = (all_map.get(hash_key).get(x).get(value) + 1)/(all_occurences+counts_set.size());
                    all_map.get(hash_key).get(x).put(value,probability);
                }
            }
        }
        //Calculating the probability of just the classes being present in relation to the whole dataset
        Set<String> total_keys = total.keySet();
        for(String key : total_keys){
            total.put(key,total.get(key)/data.size());
        }
        //Set the total map as having the name "Total"
        ArrayList<HashMap<String,Double>> total_array = new ArrayList<>();
        total_array.add(total);
        all_map.put("total",total_array);
        return all_map;
    }
    /**
     * Check accuracy of a classifier based on data passed by test_set
     * @param classifier the classifier holding the probabilities
     * @param test_set the set of data to use to test the classifier
     * @return and integer of [right,wrong] using 0-1 loss
     */
    private int[] checkAccuracy(HashMap<String,ArrayList<HashMap<String,Double>>> classifier, ArrayList<ArrayList<String>> test_set){
        //get the classes present in the data
        Set<String> class_designations = classifier.keySet();
        //a holder for the general probabilities based on the class
        HashMap<String,Double> class_probabilities = new HashMap<>();
        //count how many we get right and wrong 0-1
        int right = 0;
        int wrong = 0;
        //iterate through our test set rows
        for(int x = 0;x<test_set.size();x++){
            //for the classes in our training set
            for(String class_designation : class_designations){
                if(class_designation.equals("total")){
                    continue;
                }
                //get the row of the test set we want to look at
                ArrayList<String> row = test_set.get(x);
                //get the probability of the total class distribution
                Double current_probability = classifier.get("total").get(0).get(class_designation);
                //iterate through the columns
                for(int y = 0;y<row.size();y++){
                    //don't want to mess with the class row
                    if(y==class_index){
                        continue;
                    }
                    //get the local probability of the current column, the value tested being the one in test data
                    Double local_probability = classifier.get(class_designation).get(y).get(row.get(y));
                    //if our local probability doesn't exist (0 probability)
                    if(local_probability == null){
                        //set current probability equal to zero and break, since we know the result will be zero
                        current_probability = 0.0;
                        break;
                    }else{
                        //initialize or multiply the probabilities we have
                        if(current_probability != null) {
                            current_probability = local_probability * current_probability;
                        }else{
                            current_probability = local_probability;
                        }
                    }
                }
                //used to store the probability of it being a single class
                Double set_probability = class_probabilities.get(class_designation);
                //if it is null, it will be the current probability of that class
                if(set_probability == null){
                    class_probabilities.put(class_designation,current_probability);
                }else{
                    //if it isn't null, compare and select the greater value
                    if(set_probability<current_probability){
                        class_probabilities.put(class_designation,current_probability);
                    }
                }
            }
            //the class that has the current highest probability for selection
            String class_choice = "";
            //the current max probability
            Double current_max = -1.0;
            //iterate through class probabilities
            Set<String> classes_in_prob = class_probabilities.keySet();
            for(String class_designation: classes_in_prob){
                //get the probability
                Double class_probability = class_probabilities.get(class_designation);
                //if it is greater than the current_max, then we set the current choice to it
                if(class_probability > current_max){
                    current_max = class_probability;
                    class_choice = class_designation;
                }
            }
            //if our final choice was equal to the choice it was supposed to be
            if(class_choice.equals(test_set.get(x).get(this.class_index))){
                right++;//it were right
            }else{
                wrong++;//it were wrong
            }
        }
        //return an array representing the right and wrong counts
        return new int[]{right,wrong};
    }

    /**
     * Starter method to determine what features should be randomized
     */
    private void shuffleRandomTen(){
        //Do the ceil to get the features to scramble, so 4 features will scramble 1 feature
        Double f_shuffle_length = Math.ceil((this.feature_length+9)/10);
        Double f_shuffle_original = f_shuffle_length;
        //while we still need to shuffle
        while(f_shuffle_length > 0){
            //choose a row to shuffle
            int choice = (int)(Math.random() * this.feature_length);
            //do not shuffle classes
            if(choice == class_index){
                continue;
            }
            //shuffle good choice
            shuffle(choice);
            //take away from the amount of features we need to shuffle
            f_shuffle_length=f_shuffle_length-1;
        }
        //pretty print that the file has been shuffled
        System.out.println("-----------------------------");
        System.out.println("10% - " + f_shuffle_original.intValue() + " feature(s) were shuffled");
        System.out.println("-----------------------------");
    }

    /**
     * Shuffle a column within a file
     * @param column row index to
     */
    private void shuffle(int column){
        //get all possible values for column
        ArrayList<String> possible_values = new ArrayList<>();
        //iterate through, getting the column and passing it into possible values
        for(int x = 0;x<this.file.size();x++){
            String[] split = this.file.get(x).split(",");
            possible_values.add(split[column]);
        }
        //iterate through again, this time subbing in a random variable from the available values (no replacement)
        for(int x = 0;x<this.file.size();x++){
            String[] split = this.file.get(x).split(",");
            int choice = (int)(Math.random() * possible_values.size());
            split[column] = possible_values.get(choice);
            this.file.set(x,stringJoin(split,","));
        }
    }
    /**
     * Turn a csv file into a doubly nested ArrayList so that we don't have to split each line every time we come to it
     * @param file the raw file with unsplit csv
     * @return a double nested ArrayList that signifies columns -> row -> value
     */
    private ArrayList<ArrayList<String>> fileToColumns(ArrayList<String> file){
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        for(int x = 0;x<this.file_header.length;x++){
            result.add(new ArrayList<String>());
        }
        for(String line : file){
            String[] columns = line.split(",");
            for(int x = 0;x<columns.length;x++){
                result.get(x).add(columns[x]);
            }
        }
        return result;
    }
    /**
     * Turn an array list orientated of columns-rows into rows-columns
     * @param data the data we want to switch the orientation of
     * @return the data with switched orientation
     */
    private ArrayList<ArrayList<String>> columnsToRow(ArrayList<ArrayList<String>> data){
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        int size = data.get(0).size();
        for(int x=0;x<size;x++){
            result.add(new ArrayList<>());
        }
        int current_row_count = 0;
        while(current_row_count < size){
            for(ArrayList<String> columns : data){
                result.get(current_row_count).add(columns.get(current_row_count));
            }
            current_row_count++;
        }
        return result;
    }
    /**
     * Checks the header of the currently loaded file for an attribute
     * @param column the column of the header that we are searching
     * @param name the value we are searching for
     * @return if the value was found that we are looking for
     */
    private boolean hasAttribute(int column, String name){
        //if there is a match, string will be of length 2
        return (file_header[column] + " ").split("!" + name).length > 1;
    }
    /*
    Join a string by a token
     */
    private String stringJoin(String[] string, String insert){
        StringBuilder sb = new StringBuilder();
        for(int x = 0;x<string.length-1;x++){
            sb.append(string[x] + insert);
        }
        sb.append(string[string.length-1]);
        return sb.toString();
    }
    /**
     * Return the class index indicated by "class" in the csv header
     * @param header the header of the file
     * @return the index where the class was found
     */
    private int getClassIndex(String[] header){
        for(int x = 0;x<header.length;x++){
            if(header[x].substring(0,5).equalsIgnoreCase("class")){
                return x;
            }
        }
        //indicate class index was not found
        return -1;
    }
}
