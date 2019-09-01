import util.FileReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
/**
 * Menu that is used to launch the program and select options
 */
public class Menu {
    private String menu;
    private String[] options;

    /**
     * @param welcome - welcome message printed once by the menu
     * @param options - options that build the menu displayed
     * @param paths - file paths relating to the options
     */
    public Menu(String welcome,String[] options,String[] paths){
        this.menu = buildMenu();
        this.options = options;
        System.out.println(welcome);
        //if a valid choice is picked, it will be a number that is not zero
        ArrayList<String> file_read = new ArrayList<>();
        //Handling for if file reading fails, give error and then re-prompt for a new choice
        while(file_read.size() == 0){
            //choice from menu
            int choice = getOptionChoice();
            //resource related to the selection
            InputStream is = getClass().getClassLoader().getResourceAsStream(paths[choice-1]);
            //custom filereader
            FileReader fr = new FileReader(is);
            //get array list of contents
            file_read = fr.getFileContentsAsArrayList();
        }
        //TODO: add splitting + some form of ignoring for values that shouldn't be used and
        //TODO: Ability to weigh categorical values such as yes/no and some form of class identification
        //TODO: so that we can give what classification our classifier chooses when presented with a new situation
        //A readout of the file_read
        readArrayList(file_read);
    }

    /**
     * This method prints out the menu and also prompts for a valid input
     * @return int representing the valid choice selected from our available choices
     */
    private int getOptionChoice(){
        int valid_choice = 0;
        //while we don't have a valid choice
        while(valid_choice == 0){
            //Print out menu
            System.out.println(this.menu);
            //get choice
            Scanner choice_input = new Scanner(System.in);
            String choice = choice_input.next();
            try{
                //parse int (can throw NumberFormatException)
                int int_choice = Integer.parseInt(choice);
                //If a choice is in our valid range
                if(int_choice > 0 && int_choice <= this.options.length){
                    valid_choice = int_choice;
                    //If choice is exit
                }else if(int_choice == -1){
                    System.exit(0);
                    //Choice is not valid
                }else{
                    System.out.println(int_choice + " was not a valid choice");
                    System.out.println("1-" + this.options.length + " are valid choices");
                }
            }catch(NumberFormatException nfe){
                //Input was not a number, prompt user to enter in a number
                System.out.println("Please enter a valid number");
            }
        }
        //loop exited with a valid choice selection
        return valid_choice;
    }
    /**
     * Using the classes options, constructs a nice looking menu for option selection
     * @return String of the newly built menu
     */
    private String buildMenu(){
        //String builder to build displayed options
        StringBuilder selection_builder = new StringBuilder();
        selection_builder.append("Choose dataset to use (-1 to exit): \n");
        for(int x = 0;x<this.options.length;x++){
            //using tabs and \n to correctly format menu
            selection_builder.append("\t" + (x+1) + ": " + this.options[x] + "\n");
        }
        return selection_builder.toString();
    }

    /**
     * prints out the content of an array list
     * @param arr any array list composed of strings
     */
    private static void readArrayList(ArrayList<String> arr){
        for(String line: arr){
            System.out.println(line);
        }
    }
}
