package util;
import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class FileReader {
    private InputStream input_stream;
    /**
     * @param input_stream input stream relating to file data
     */
    public FileReader(InputStream input_stream){
        //Notify if bad input stream is passed
        if(input_stream == null){
            System.out.println("Invalid input stream passed to filereader constructor");
        }else{
            this.input_stream = input_stream;
        }
    }

    /**
     * Turns the input stream within the class to an ArrayList of Strings
     * @return an array list representation of a file
     */
    public ArrayList<String> getFileContentsAsArrayList(){
        ArrayList<String> lines = new ArrayList<>();
        if(this.input_stream == null){
            System.out.println("Couldn't get input stream from file");
            return lines;
        }else{
            InputStreamReader isr = new InputStreamReader(this.input_stream);
            BufferedReader br = new BufferedReader(isr);
            lines.addAll(br.lines().collect(Collectors.toList()));
            return lines;
        }
    }
}
