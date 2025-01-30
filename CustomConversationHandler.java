package mts;

import java.io.BufferedWriter;// pentru scrierea in fisierul log
import java.io.FileWriter;//pentru scrierea in fisierul log
import java.io.IOException;// pentru cand scrierea in fisier nu se poate realiza din diverse motive
import java.util.ArrayList;//pentru lista de mesaje (invizibile in acest mod) 
import java.util.List;//chiar daca ele nu se vad in interfata, ele tot trebuie generate


 //codul pentru conversatiile din CustomMode, in care nu se tine cont de delay-uri in generarea conversatiilor.
 //Resursele utilizate sunt salvate in fisierul "custom.log" pentru a nu fi amestecate cu rezultatele obtinute prin analiza celorlalte conversatii

public class CustomConversationHandler implements Runnable {
    private final int conversationId;
    private final Server ui;
    private final List<String> messages = new ArrayList<>();
    private final boolean isVirtual; // var bool pentru a determina tipul de thread
    private final int messagesPerConversation; // modul custom permite alegerea nr de mesaje de catre utilizator
    
    // Constructorul clasei
    public CustomConversationHandler(int conversationId, Server ui, boolean isVirtual, int messagesPerConversation) {
        this.conversationId = conversationId;
        this.ui = ui;
        this.isVirtual = isVirtual;
        this.messagesPerConversation = messagesPerConversation;
    }

    @Override
    public void run() {
    	// similar cu clasele pentru celelalte tipuri de conversatii, avem nevoie de valorile de start si finish ale resurselor utilizate de thread-uri
        long startTime = System.currentTimeMillis();
        double startCpuLoad = ui.getResourceMonitor().getCpuLoad();
        long startMemory = ui.getResourceMonitor().getUsedMemory();

        // Adaugam instant nr de mesaje, fara delay-uri 
        List<String> users = MessageUtils.generateUsernames(); // generam cele doua username-uri
        for (int i = 0; i < messagesPerConversation; i++) { //cat timp nu e atinsa limita stabilita de mesaje per conversatie
            MessageUtils.addMessage(conversationId, ui, messages, users); // adaugam mesaje la conversatie 
        }

        long endTime = System.currentTimeMillis();
        double endCpuLoad = ui.getResourceMonitor().getCpuLoad();
        long endMemory = ui.getResourceMonitor().getUsedMemory();
        // aceste valori vor fi utilizate pentru calcularea resurselor; custom mode utilizeaza o metoda proprie pentru calcularea lor
        logCustomResourceUsage(startTime, endTime, startCpuLoad, endCpuLoad, startMemory, endMemory);
    }
    
    // functia pentru salvarea valorilor generate de custom mode in fisierul custom.log
    private void logCustomResourceUsage(long startTime, long endTime, double startCpuLoad, double endCpuLoad, long startMemory, long endMemory) {
        String threadType = isVirtual ? "Virtual Thread" : "Traditional Thread"; // in primul rand se determina tipul de thread utilizat de conversatie
        // calcularea si scrierea in fisier a valorilor corespunzatoare conversatiei
        String logEntry = String.format(
                "%s | ID: %d%nTotal Time: %d ms %nCPU Load Change: %.5f%% %nUsed Memory Change: %.3fMB%n %n",
                threadType,
                conversationId,
                // am folosit functia modul pentru ca valorile CPU si valorile memoriei sa nu fie negative (arata urat si imi tot da senzatia ca ceva
                // nu e corect in aplicatie; timpul nu avea neaparat nevoie de functia asta, dar am zis ca rau nu-i face s-o am acolo preventiv )
                Math.abs(endTime - startTime), // timpul cat a rulat thread-ul
                Math.abs(endCpuLoad - startCpuLoad), // cat este % de CPU utilizat in timpul rularii 
                Math.abs (endMemory / (1024.0 * 1024) - (startMemory / (1024.0 * 1024))) // memoria alocata pentru thread exprimata in MB 
        );
        // scriem rezultatele in custom.log ; Fisierul e suprascris de fiecare data cand se apasa butonul 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("custom.log", true))) {
            writer.write(logEntry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
