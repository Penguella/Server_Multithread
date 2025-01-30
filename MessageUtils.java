package mts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

//clasa dedicata crearii de conversatii. Exista o lista de posibile „intrebari”, fiecare avand o lista de posibile raspunsuri. Aplicatia va alege aleatoriu
// intrebarea, dupa care va alege in mod similar un raspuns din lista asignata de potentiale raspunsuri. Totodata, ne asiguram ca o intrebare nu va fi aleasa
// de doua ori consecutiv (twice in a row) in cadrul aceleasi conversatii. 
// De asemenea, tot aici se genereaza aleatoriu doua username-uri pentru cei doi participanti la conversatie (presupunem ca o conversatie e realizata intre
// doi utilizatori) 

public class MessageUtils {
    private static final Map<String, String[]> PHRASE_ANSWERS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final ThreadLocal<String> LAST_ASKED_QUESTION = ThreadLocal.withInitial(() -> null);

    static {
        PHRASE_ANSWERS.put("Ai vazut meciul de aseara?", new String[]{
            "Da, a fost incredibil!",
            "Nu, am fost ocupat.",
            "A castigat echipa cealalta, am plans",
            "O pierdere de timp",
            "Macar am egalat in ultimul minut"
        });
        PHRASE_ANSWERS.put("Crezi ca maine o sa ploua?", new String[]{
            "Probabil",
            "Nu cred",
            "La meteo a zis ca va ploua pana luni",
            "Sper ca nu"
        });
        PHRASE_ANSWERS.put("[A trimis 10 imagini]", new String[]{
                "Ce papagal dragut!",
                "Foarte frumoase peisajele",
                "Nu cred asa ceva :)",
                "Preferata mea e cea verde"
            });
        PHRASE_ANSWERS.put("Ti-am trimis raportul pe mail", new String[]{
                "Mersi",
                "Am vazut",
                "Raman dator",
                "[atasament indisponibil]"
            });
        PHRASE_ANSWERS.put("Mi-am luat o vioara noua", new String[]{
                "Bravo!",
                "Iar? Abia ai schimbat-o",
                "Imi poti recomanda si mie ceva potrivit unui incepator?",
                "Daca nu stii ce sa faci cu cea veche, stiu pe cineva interesat"
            });
        PHRASE_ANSWERS.put("Ai vazut cumva unde am pus cheile de la masina?", new String[]{
                "Le-ai lasat pe masa in sufragerie",
                "Nu cred",
                "Am vazut. Cinci lei si iti zic si unde :)"
            });
        PHRASE_ANSWERS.put("Unde esti?", new String[]{
                "Acum plec de acasa",
                "Sunt pe drum",
                "Am ajuns de 5 minute. Unde esti TU?",
                "Am pierdut tramvaiul, vin pe jos",
                "Uita-te in spate"
            });
    }

    public static String addMessage(int conversationId, Server ui, List<String> messages, List<String> users) {
        String user = users.get(messages.size() % 2);
        String message;

        if (messages.size() % 2 == 0) {
            message = getRandomStartingPhrase();
        } else {
            String lastPhrase = getLastStartingPhrase(messages);
            String[] potentialAnswers = PHRASE_ANSWERS.get(lastPhrase);
            message = (potentialAnswers != null) ? potentialAnswers[RANDOM.nextInt(potentialAnswers.length)] : "Hmm...";
        }

        String fullMessage = user + ": " + message;
        messages.add(fullMessage);
        ui.updateConversationMessages(conversationId, messages);
        return message;  // Returneaza doar mesajul text (fara username) pt calcul delay
    }

    private static String getRandomStartingPhrase() {
        List<String> startingPhrases = new ArrayList<>(PHRASE_ANSWERS.keySet());
        String lastAskedQuestion = LAST_ASKED_QUESTION.get();
        String chosenPhrase;

        do {
            chosenPhrase = startingPhrases.get(RANDOM.nextInt(startingPhrases.size()));
        } while (chosenPhrase.equals(lastAskedQuestion));

        LAST_ASKED_QUESTION.set(chosenPhrase);
        return chosenPhrase;
    }

    private static String getLastStartingPhrase(List<String> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            String message = messages.get(i);
            String phrase = message.substring(message.indexOf(":") + 2);
            if (PHRASE_ANSWERS.containsKey(phrase)) {
                return phrase;
            }
        }
        return null;
    }

    public static List<String> generateUsernames() {
        String user1 = generateRandomUsername();
        String user2;
        //in cazul in care username-ul generat pt al doilea utilizator este identic cu cel generat pentru primul
        do {
            user2 = generateRandomUsername();
        } while (user1.equals(user2));
        // returneaza lista de utilizatori
        return List.of(user1, user2);
    }

    // functia pt a genera username-uri pentru utilizatori
    private static String generateRandomUsername() {
        int length = 8 + RANDOM.nextInt(5);
        StringBuilder username = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int choice = RANDOM.nextInt(3);
            if (choice == 0) username.append((char) ('a' + RANDOM.nextInt(26)));
            else if (choice == 1) username.append((char) ('A' + RANDOM.nextInt(26)));
            else username.append(RANDOM.nextInt(10));
        }
        return username.toString();
    }
}
