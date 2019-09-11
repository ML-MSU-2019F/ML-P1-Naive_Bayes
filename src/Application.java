import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Application {
    private ArrayList<String> file;
    private String file_header;
    private int feature_length;
    private int class_index;
    //String identifies the class, the array list contains all rows with the class
    private HashMap<String,ArrayList<String>> hash_by_class;
    public Application(ArrayList<String> file) {
        //pop off header, make it a class variable
        this.file_header = file.remove(0);
        String[] header_split = this.file_header.split(",");
        this.class_index = getClassIndex(header_split);
        //Handle if class is not found
        if(this.class_index == -1){
            System.out.println("Could not find class column, exiting");
            System.exit(1);
        }
        //readArrayList(file);
        this.hash_by_class = processFileIntoMap(file);
        System.out.println("Class variable is at index " + class_index);
        //We have one class variable - so we can say our features are split -1
        this.feature_length = header_split.length -1;
        trainModel(file);
        binContinuousValues(4);
        System.out.println("This data set has: " + this.feature_length + " features");
        shuffleRandomTen();
    }
    private void binContinuousValues(int bins){
        ArrayList<Double> col_min_max = new ArrayList<>();
        Set<Integer> column_keys = hash_to_column.keySet();
        for(Integer column_key : column_keys){
            col_min_max.add(new Double(column_key));
            double min = 999999;
            double max = -999999;
            Set<String> occurrence_keys = hash_to_column.get(column_key).keySet();
            for(String occurrence_key : occurrence_keys){
                try{
                    double value = Double.parseDouble(occurrence_key);
                    if(value < min){
                        min = value;
                    }
                    if(value > max){
                        max = value;
                    }
                }catch(NumberFormatException nfe){
                    System.out.println("Key was not a double");
                    break;
                }
            }
            col_min_max.add(min);
            col_min_max.add(max);
            System.out.println("Our binning returned a max of " + max + " and a min of " + min + " for row " + column_key);
        }
        for(Integer column_key : column_keys){
            HashMap<String,Integer> occurrence_hash = hash_to_column.get(column_key);
            Set<String> occurrence_keys = occurrence_hash.keySet();
            for(int x = 0;x<col_min_max.size();x=x+3){
                double min =  col_min_max.get(x+1);
                double max =  col_min_max.get(x+2);
                double range = max-min;
                System.out.println("Range was " + range);
                double interval = range/bins;
                double[] bin_groups = new double[bins];
                for(int y = 0;y<bins;y++){
                    bin_groups[y] = (min + (interval*(y+1)));
                }
                HashMap<String,Integer> replacement_map = new HashMap<>();
                for(String occurrence_key : occurrence_keys){
                    try{
                        double converted = Double.parseDouble(occurrence_key);
                        for(int y = 0;y<bins;y++){
                            System.out.println(bin_groups[y] + "<=" +  converted);
                            String bin_num = String.valueOf(y);
                            if(converted <= bin_groups[y]){
                                System.out.println("Getting put in bin: " + bin_num);
                                Integer occurrence_count = replacement_map.get(bin_num);
                                if(occurrence_count == null){
                                    replacement_map.put(bin_num,1);
                                }else{
                                    int new_occurrence = occurrence_count + 1;
                                    replacement_map.put(bin_num,new_occurrence);
                                }
                            }
                        }
                    }catch(NumberFormatException nfe){
                        System.out.println("Could not parse double when binning");
                        break;
                    }
                }
                hash_to_column.put(column_key,replacement_map);
            }
        }
        System.out.println();
    }
    /**
     * Turns the file into a map of class=>arraylist of lines
     * @param file
     * @return
     */
    private HashMap<String, ArrayList<String>> processFileIntoMap(ArrayList<String> file){
        //result hash to store what we are returning
        HashMap<String,ArrayList<String>> resultHash = new HashMap<>();
        //Go through each line
        for(String line : file){
            //Split it to get individual values
            String[] split = line.split(",");
            //Grab the class string
            String class_string = split[this.class_index];
            //Grab the current arraylist, could not be initialized!
            ArrayList<String> current_arraylist = resultHash.get(class_string);
            //if it is not initialized
            if(current_arraylist == null){
                //make a new arraylist to go there
                ArrayList<String> new_array = new ArrayList<>();
                //add the line
                new_array.add(line);
                //add it to the arraylist
                resultHash.put(class_string,new_array);
            }else{
                //the arraylist exists, just add to existing one
                current_arraylist.add(line);
            }
        }
        return resultHash;
    }
    private int getClassIndex(String[] header){
        for(int x = 0;x<header.length;x++){
            if(header[x].substring(0,5).equalsIgnoreCase("class")){
                return x;
            }
        }
        //indicate class index was not found
        return -1;
    }
    /**
     * Could be used on a full set or during 10 fold cross validation
     * eventually will return the data we need to analyze a new situation
     * @param data
     */
    private HashMap<Integer,HashMap<String,Integer>> hash_to_column = new HashMap<>();
    private void trainModel(ArrayList<String> data){
        String[] headersplit = this.file_header.split(",");
        for(int x = 0;x<headersplit.length;x++){
            hash_to_column.put(x,new HashMap<String,Integer>());
        }
        for(String row : data){
            String[] columns = row.split(",");
            for(int x = 0;x<columns.length;x++){
                HashMap<String,Integer> current_map = hash_to_column.get(x);
                Integer occurrences = current_map.get(columns[x]);
                if(occurrences == null){
                    current_map.put(columns[x],1);
                }else{
                    int new_occurrence = occurrences+ 1;
                    current_map.put(columns[x],new_occurrence);
                }
            }
        }
        System.out.println();
    }
    private void shuffleRandomTen(){
        //Do the ceil to get the features to scramble, so 4 features will scramble 1 feature
        Double f_shuffle_length = Math.ceil((this.feature_length+9)/10);
        System.out.println("We are shuffling: " + f_shuffle_length.intValue() + " features");
    }
    /**
     * prints out the content of an array list
     * @param arr any array list composed of strings
     */
    private void readArrayList(ArrayList<String> arr){
        for(String line: arr){
            System.out.println(line);
        }
    }
}
