import java.util.ArrayList;

public class Application {
    private ArrayList<String> file;
    private String file_header;
    private int feature_length;
    public Application(ArrayList<String> file) {
        //pop off header
        this.file_header = file.remove(0);
        String[] header_split = this.file_header.split(",");
        //We have one class variable - so we can say our features are split -1
        this.feature_length = header_split.length -1;
        readArrayList(file);
        System.out.println("This data set has: " + this.feature_length + " features");
        shuffleRandomTen();

    }
    private void shuffleRandomTen(){
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
