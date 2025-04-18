package paypals.commands;

import paypals.Activity;
import paypals.ActivityManager;
import paypals.Person;
import paypals.exception.ExceptionMessage;
import paypals.exception.PayPalsException;
import paypals.util.Logging;
import paypals.util.UI;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the command to add a new activity in PayPals.
 * Parses and validates user input, then creates and stores a new {@link Activity} instance.
 */
public class AddCommand extends Command {

    private static final String WRONG_ADD_FORMAT =
            "add d/DESCRIPTION n/PAYER f/FRIEND1 a/AMOUNT_OWED_1 f/FRIEND2 a/AMOUNT_OWED_2...";
    private static final double LARGE_AMOUNT_LIMIT = 10000.0;
    private static final String MONEY_FORMAT = "^-?\\d+(\\.\\d{1,2})?$";

    /**
     * Constructs an AddCommand with the given raw user input.
     *
     * @param command The user command input string.
     */
    public AddCommand(String command) {
        super(command);
    }

    /**
     * Extracts the value of a given prefix (e.g., d/, n/) from the user input.
     *
     * @param key               The prefix to look for.
     * @param exceptionMessage  The exception message to throw if the prefix is missing.
     * @return The extracted and trimmed value corresponding to the prefix.
     * @throws PayPalsException If the key is not found in the input.
     */
    String extractValue(String key, ExceptionMessage exceptionMessage) throws PayPalsException {
        String regex = key + "(?<!\\S[a-zA-Z]\\/)(?<=[a-zA-Z]\\/)([^\\/]+?)(?=\\s+[a-zA-Z]+\\/|$)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            Logging.logWarning("Invalid input format detected");
            System.out.println(WRONG_ADD_FORMAT);
            throw new PayPalsException(exceptionMessage);
        }
    }

    /**
     * Validates that a friend entry is unique and not the same as the payer.
     *
     * @param payer    Name of the payer.
     * @param oweName  Name of the friend who owes money.
     * @param names  Map of names of payer/friends currently in the activity.
     * @throws PayPalsException If validation fails (e.g., duplicate friend or payer owes themselves).
     */
    void validateFriend(String payer, String oweName, HashMap<String, String> names) throws PayPalsException {
        assert payer != null : "Payer name should not be null";
        assert oweName != null : "OweName should not be null";
        assert names != null : "Names map should not be null";

        String lowercasePayer = payer.toLowerCase();
        String lowercaseOweName = oweName.toLowerCase();

        if (lowercasePayer.equals(lowercaseOweName)) {
            Logging.logWarning("Payer tried to owe themselves.");
            throw new PayPalsException(ExceptionMessage.PAYER_OWES, oweName);
        }
        if (names.containsKey(lowercaseOweName)) {
            Logging.logWarning("Duplicate friend entry detected: {0} already exists.");
            System.out.println(WRONG_ADD_FORMAT);
            throw new PayPalsException(ExceptionMessage.DUPLICATE_FRIEND, oweName);
        }
    }

    /**
     * Executes the add command: parses input, validates it, creates a new Activity,
     * and adds it to the {@link ActivityManager}.
     *
     * @param activityManager The activity manager managing all activities.
     * @param enablePrint     Whether to enable printing UI messages.
     * @throws PayPalsException If the command input is invalid or logic constraints fail.
     */
    @Override
    public void execute(ActivityManager activityManager, boolean enablePrint) throws PayPalsException {
        assert activityManager != null : "ActivityManager should not be null";

        UI ui = new UI(enablePrint);
        if (activityManager.getSize() >= 1000) {
            throw new PayPalsException(ExceptionMessage.MORE_THAN_1000_ACTIVITIES);
        }
        HashMap<String, String> names = new HashMap<>();
        HashMap<String, Double> owed = new HashMap<>();
        validatePrefixOrder();

        // Step 1: Extract description and payer name
        String description = extractValue("d/", ExceptionMessage.NO_DESCRIPTION);
        String name = extractValue("n/", ExceptionMessage.NO_PAYER);

        if (name.matches(".*\\d.*")) {
            throw new PayPalsException(ExceptionMessage.NUMBERS_IN_NAME);
        }

        if (name.contains("/")) {
            throw new PayPalsException(ExceptionMessage.SLASH_IN_NAME);
        }

        assert !description.isEmpty() : "Description should not be null or empty";
        assert !name.isEmpty() : "Payer name should not be null or empty";

        // Step 2: Capture all (f/... a/...) pairs
        String[] pairs = command.split("\\s+(?i)f/");
        double totalOwed = 0.0;
        for (int i = 1; i < pairs.length; i++) {
            String[] parameters = pairs[i].split("\\s+(?i)a/");
            if (parameters.length == 2) {
                String oweName = parameters[0].trim();
                if (oweName.matches(".*\\d.*")) {
                    throw new PayPalsException(ExceptionMessage.NUMBERS_IN_NAME);
                }
                String amountString = parameters[1].trim();
                if (amountString.split("\\s+").length > 1) {
                    throw new PayPalsException(ExceptionMessage.INVALID_FORMAT, WRONG_ADD_FORMAT);
                }
                double oweAmount;

                if (oweName.contains("/")) {
                    throw new PayPalsException(ExceptionMessage.SLASH_IN_NAME);
                }

                try {
                    oweAmount = Double.parseDouble(parameters[1]);
                } catch (Exception e) {
                    Logging.logWarning("Invalid amount entered for friend");
                    throw new PayPalsException(ExceptionMessage.INVALID_AMOUNT);
                }

                if (oweName.isEmpty()){
                    throw new PayPalsException(ExceptionMessage.INVALID_FRIEND);
                }
                if (!isValidAmount(parameters[1].trim())) {
                    throw new PayPalsException(ExceptionMessage.NOT_MONEY_FORMAT);
                }
                if (oweAmount <= 0.0) {
                    Logging.logWarning("Amount entered for friend out of bounds");
                    throw new PayPalsException(ExceptionMessage.AMOUNT_OUT_OF_BOUNDS);
                }
                if (oweAmount > LARGE_AMOUNT_LIMIT) {
                    Logging.logWarning("Amount entered for friend out of bounds");
                    throw new PayPalsException(ExceptionMessage.LARGE_AMOUNT);
                }

                validateFriend(name, oweName, names);
                names.put(oweName.toLowerCase(), oweName);
                owed.put(oweName, oweAmount);
                totalOwed += oweAmount;

                Logging.logInfo("Friend added successfully");
            } else {
                Logging.logWarning("Incorrect number of parameters detected: {0}");
                throw new PayPalsException(parameters.length < 2
                        ? ExceptionMessage.NO_AMOUNT_ENTERED : ExceptionMessage.MULTIPLE_AMOUNTS_ENTERED);
            }
        }

        assert totalOwed >= 0 : "Total owed amount should not be negative";

        ui.print("Desc: " + description);
        ui.print("Name of payer: " + name);
        ui.print("Number of friends who owe " + name + ": " + owed.size());

        Activity newActivity = new Activity(description, new Person(name, -totalOwed, false), owed);
        activityManager.addActivity(newActivity);

        Logging.logInfo("Activity added successfully");
    }

    /**
     * Checks if a given string is a valid monetary amount (up to 2 decimal places).
     *
     * @param amountStr The string to validate.
     * @return true if the format is valid; false otherwise.
     */
    public boolean isValidAmount(String amountStr) {
        return amountStr.matches(MONEY_FORMAT);
    }

    public void validatePrefixOrder() throws PayPalsException {
        // Get positions of d/ flag
        int dIndex = command.indexOf("d/");
        if (dIndex == -1) {
            dIndex = command.indexOf("D/");
        }
        // Get positions of n/ flag
        int nIndex = command.indexOf("n/");
        if (nIndex == -1) {
            nIndex = command.indexOf("N/");
        }
        // Get positions of f/ flag
        int fIndex = command.indexOf("f/");
        if (fIndex == -1) {
            fIndex = command.indexOf("F/");
        }
        // Get positions of a/ flag
        int aIndex = command.indexOf("a/");
        if (aIndex == -1) {
            aIndex = command.indexOf("A/");
        }

        // Check for incorrect flags, with 2 or more characters before the '/' character
        String regex = "(?<=\\S[a-zA-Z]\\/)([^\\/]+?)(?=\\s+[a-zA-Z]+\\/|$)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            throw new PayPalsException(ExceptionMessage.INVALID_FORMAT, WRONG_ADD_FORMAT);
        }
      
        if (dIndex == -1){
            throw new PayPalsException(ExceptionMessage.NO_DESCRIPTION);
        }

        if (nIndex == -1){
            throw new PayPalsException(ExceptionMessage.NO_PAYER);
        }

        if (fIndex == -1){
            throw new PayPalsException(ExceptionMessage.NO_FRIENDS);
        }

        if (aIndex == -1){
            throw new PayPalsException(ExceptionMessage.NO_AMOUNT_ENTERED);
        }

        if (!(dIndex < nIndex && nIndex < fIndex && fIndex < aIndex)) {
            throw new PayPalsException(ExceptionMessage.INVALID_FORMAT, WRONG_ADD_FORMAT);
        }
        if (command.indexOf("d/",dIndex+1) != -1 || command.indexOf("n/",nIndex+1) != -1){
            throw new PayPalsException(ExceptionMessage.INVALID_FORMAT, WRONG_ADD_FORMAT);
        }
    }
}
