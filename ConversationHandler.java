package mts;

import java.util.ArrayList; //necesar pentru lista de mesaje si lista de participanti la conversatie
import java.util.List;//necesar pentru lista de mesaje si lista de participanti la conversatie
import java.util.concurrent.Executors;//avem nevoie pentru lucrul cu thread-uri 
import java.util.concurrent.ScheduledExecutorService;//avem nevoie pentru lucrul cu thread-uri
import java.util.concurrent.TimeUnit; // pentru masurarea timpului necesar indeplinirii task-urilor asignate thread-ului

// clasa dedicata conversatiilor ce folosesc thread-uri traditionale
// !!! NU se aplica conversatiilor din Custom Mode !!!

public class ConversationHandler implements Runnable {
    private static final int MAX_MESSAGES = 10; // nr max de mesaje dintr-o conversatie
    private final int conversationId; // id-ul conversatiei curente
    private final Server ui; // apel catre metodele clasei main
    private final List<String> messages = new ArrayList<>(); //lista mesajelor continute de catre conversatia curenta
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1); // alocarea unui thread per conversatie
    private boolean isFinished = false;// var pt a determina daca conversatia va genera mesaje noi sau nu
    private final List<String> users; // lista participantilor la conversatia actuala

    // Variabile pentru monitorizarea resurselor
    private long startTime; // pentru calcularea timpului in care thread-ul a fost activ
    private double startCpuLoad; // pentru calcularea procentului de CPU utilizat 
    private long startMemory; // pentru calcularea memoriei in MB folosite de thread
    
    //Constructor
    public ConversationHandler(int conversationId, Server ui) {
        this.conversationId = conversationId;
        this.ui = ui;
        this.users = MessageUtils.generateUsernames(); // generam username-uri aleatoare pentru participantii la conversatie
    }

    @Override
    public void run() {
        // incepem monitorizarea resurselor utiliate la inceputul conversatiei
        startTime = System.currentTimeMillis();
        startCpuLoad = ui.getResourceMonitor().getCpuLoad();
        startMemory = ui.getResourceMonitor().getUsedMemory();
        // actualizam statutul conversatiei la „Ongoing” ; conversatia este in desfasurare
        ui.updateConversationStatus(conversationId, "Traditional Thread - Ongoing");
        // trimitem urmatorul mesaj
        executor.schedule(this::sendNextMessage, 0, TimeUnit.MILLISECONDS);
    }
    // fct pentru trimiterea urmatorului mesaj 
    private void sendNextMessage() {
    	// daca conversatia a ajuns la final (s-a atins nr max de mesaje permis unei conversatii)
        if (isFinished || messages.size() >= MAX_MESSAGES) {
            isFinished = true;
            executor.shutdown();// se inchide executorul, thread-ul si-a indeplinit task-urile
            ui.updateConversationStatus(conversationId, "Traditional Thread - Finished"); // actualizam statutul conversatiei drept finalizata

            // monitorizarea resurselor utiliate la finalul conversatiei
            long endTime = System.currentTimeMillis();
            double endCpuLoad = ui.getResourceMonitor().getCpuLoad();
            long endMemory = ui.getResourceMonitor().getUsedMemory();

            // resurse utilizate. Scrierea in fisier a resurselor necesita parsarea lor catre functia din Server care se ocupa cu asta
            ui.logResourceUsage(conversationId, startTime, endTime, startCpuLoad, endCpuLoad, startMemory, endMemory);
        } else { // conversatia nu a ajuns la final
            String message = MessageUtils.addMessage(conversationId, ui, messages, users);
            long delay = (long) (message.length() * 0.01 * 1000); // delay de 0.01 sec per caracter
            executor.schedule(this::sendNextMessage, delay, TimeUnit.MILLISECONDS); // trimitem mesajul dupa delay
        }
    }
}
