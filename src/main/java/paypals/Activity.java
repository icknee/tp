package paypals;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Activity {
    private String description;
    private Person payer;
    private HashMap<String, Person> owed;

    public Activity(String description, Person payer, HashMap<String, Double> owedMap) {
        this.description = description;
        this.payer = payer;
        this.owed = new HashMap<>();
        for (Map.Entry<String, Double> entry : owedMap.entrySet()) {
            this.owed.put(entry.getKey(), new Person(entry.getKey(), entry.getValue(), false));
        }
    }

    public String getDescription(){
        return description;
    }

    public Person getPayer() {
        return payer;
    }

    public Person getFriend(String name) {
        return owed.get(name);
    }

    public Collection<Person> getAllFriends() {
        return owed.values();
    }

    @Override
    public String toString() {
        String spacing = "    ";
        String outputString = "Desc: " + description + "\n"
                + spacing + "Payer: " + payer.getName() + "\n"
                + spacing + "Owed by: ";
        int personCount = 0;
        for (String name : owed.keySet()) {
            outputString += name;
            if (personCount++ < owed.size() - 1) {
                outputString += ", ";
            }
        }
        return outputString;
    }

    public String toStorageString(String separator) {
        String data = this.description + separator;
        data += payer.getName() + separator + payer.getAmount() + separator + payer.hasPaid() + separator;
        for (Map.Entry<String, Person> entry : owed.entrySet()) {
            Person person = entry.getValue();
            data += person.getName() + separator + person.getAmount() + separator + person.hasPaid() + separator;
        }
        return data;
    }

    public void setOwed(HashMap<String, Person> owed) {
        this.owed = owed;
    }

}
