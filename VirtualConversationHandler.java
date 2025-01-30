package mts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VirtualConversationHandler implements Runnable {
    private static final int MAX_MESSAGES = 10;
    private final int conversationId;
    private final Server ui;
    private final List<String> messages = new ArrayList<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private boolean isFinished = false;
    private final List<String> users;

    // variabile monitorizare resurse
    private long startTime;
    private double startCpuLoad;
    private long startMemory;

    public VirtualConversationHandler(int conversationId, Server ui) {
        this.conversationId = conversationId;
        this.ui = ui;
        this.users = MessageUtils.generateUsernames();
    }

    @Override
    public void run() {
    	  // Variabile pentru monitorizarea resurselor
        startTime = System.currentTimeMillis();
        startCpuLoad = ui.getResourceMonitor().getCpuLoad();
        startMemory = ui.getResourceMonitor().getUsedMemory();

        ui.updateConversationStatus(conversationId, "Virtual Thread - Ongoing");
        executor.schedule(this::sendNextMessage, 0, TimeUnit.MILLISECONDS);
    }

    private void sendNextMessage() {
        if (isFinished || messages.size() >= MAX_MESSAGES) {
            isFinished = true;
            executor.shutdown();
            ui.updateConversationStatus(conversationId, "Virtual Thread - Finished");

            // Capture resource usage at end
            long endTime = System.currentTimeMillis();
            double endCpuLoad = ui.getResourceMonitor().getCpuLoad();
            long endMemory = ui.getResourceMonitor().getUsedMemory();

            // Log resource usage
            ui.logResourceUsage(conversationId, startTime, endTime, startCpuLoad, endCpuLoad, startMemory, endMemory);
        } else {
            String message = MessageUtils.addMessage(conversationId, ui, messages, users);
            long delay = (long) (message.length() * 0.01 * 1000); // 0.01 sec per caracter
            executor.schedule(this::sendNextMessage, delay, TimeUnit.MILLISECONDS);
        }
    }
}
