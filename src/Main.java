public class Main {
    public static void main(String[] args) {
        //welcome message for program entry
        String welcome = "Welcome to our naive bayes ML project";
        //Array of options to allow for dynamic option selection and insertion
        String[] options = new String[] {"Breast Cancer","Glass","Iris","Soybean","Voting-Records"};
        //Paths that relate to the options, so selection can occur in a dynamic-ish way
        String[] option_paths = new String[] {"data/breast-cancer/breast-cancer-wisconsin.csv", "data/glass/glass.csv",
                "data/iris/iris.csv", "data/soybean/soybean-small.csv", "data/voting-records/house-votes-84.csv"};
        //Build menu and handle selection
        Menu main = new Menu(welcome,options,option_paths);
    }
}
