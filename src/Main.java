import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        //welcome message for program entry
        String welcome = "Welcome to our naive bayes ML project";
        //Array of options to allow for dynamic option selection and insertion
        String[] options = new String[] {"Breast Cancer","Glass","Iris","Soybean","Voting-Records"};
        //Paths that relate to the options, so selection can occur in a dynamic-ish way
        String[] option_paths = new String[] {"data/breast-cancer/breast-cancer-wisconsin.csv", "data/glass/glass.csv",
               "data/iris/iris.csv", "data/soybean/soybean-small.csv", "data/voting-records/house-votes-84.csv"};
        String[] missing_value_designations = new String[] {"?","?","?","?",""};
        //Build menu and handle selection
        Menu choice_menu = new Menu(welcome,options,option_paths);
        //get the choice and file from the menu, and use it to initialize the application
        int choice = choice_menu.getChoice();
        ArrayList<String> file = choice_menu.getFile();
        Application app = new Application(file,missing_value_designations[choice-1]);
    }
}
