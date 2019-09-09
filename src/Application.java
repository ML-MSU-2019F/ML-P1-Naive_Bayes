import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

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
        System.out.println("This data set has: " + this.feature_length + " features");
        shuffleRandomTen();
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
    private void trainModel(ArrayList<String> data){

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
