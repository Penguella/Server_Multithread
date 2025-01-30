package mts;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("serial")
public class Server extends JFrame {

    private final DefaultListModel<String> conversationListModel;
    private final List<String> conversations;
    private final Map<Integer, List<String>> conversationMessages; // Stocheaza mesajele fiecarei conversatii in HashMap
    private final List<JDialog> openDialogs = new ArrayList<>();
    private final ResourceMonitor resourceMonitor = new ResourceMonitor();// obiect pt monitorizarea resurselor
    private final ScheduledExecutorService resourceExecutor = Executors.newScheduledThreadPool(1);



    // Executor global pentru gestionarea thread-urilor
    private final ExecutorService conversationExecutor = Executors.newFixedThreadPool(10000); 
    private int totalGeneratedConversations = 0; // Totalul conversatiilor generate pana acum

    public Server() {
        setTitle("Chat Server");
        setSize(900, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        conversationListModel = new DefaultListModel<>();
        conversations = new ArrayList<>();
        conversationMessages = new HashMap<>();
        //butoanele si listenerii aferenti
        JButton btn10Conversations = new JButton("Generate 10 Conversations");
        JButton btn1000Conversations = new JButton("Generate 1000 Conversations");
        btn10Conversations.addActionListener(e -> generateConversations(10));
        btn1000Conversations.addActionListener(e -> generateConversations(1000));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btn10Conversations);
        buttonPanel.add(btn1000Conversations);
        
        JButton btnViewResults = new JButton("View Resource Usage");
        btnViewResults.addActionListener(e -> viewResourceUsage());
        buttonPanel.add(btnViewResults);
        //
        JButton btnCustomMode = new JButton("Start Custom Mode");
        btnCustomMode.addActionListener(e -> new CustomMode(this).start());
        buttonPanel.add(btnCustomMode);


        //gestionarea conversatiilor si deschiderea lor prin dublu clic
        JList<String> conversationList = new JList<>(conversationListModel);
        conversationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = conversationList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        showConversationDetails(index + 1);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(conversationList);
        add(buttonPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void generateConversations(int count) {
        int startId = totalGeneratedConversations + 1; // incepe de la conversatia urmatoare
        int endId = totalGeneratedConversations + count;

        for (int i = startId; i <= endId; i++) {
            int conversationId = i;
            conversations.add("Conversation " + conversationId + " - Waiting");
            conversationListModel.addElement("Conversation " + conversationId + " - Waiting");

            conversationMessages.put(conversationId, new ArrayList<>());

            // folosim ConversationFactory pentru a crea handler-ul (daca e id par se foloseste thread traditional, altfel unul virtual) 
            Runnable task = ConversationFactory.createConversation(conversationId, this);

            conversationExecutor.submit(task); // executorul incepe executia task-ului
        }

        totalGeneratedConversations += count; // actualizeaza nr total de conversatii
    }

    //actualizare statut conversatie
    public void updateConversationStatus(int conversationId, String status) {
        SwingUtilities.invokeLater(() -> {
            conversations.set(conversationId - 1, "Conversation " + conversationId + " - " + status);
            conversationListModel.set(conversationId - 1, conversations.get(conversationId - 1));
            //daca s-a incheiat conversatia
            if (status.equals("Finished")) {
                for (JDialog dialog : openDialogs) {
                    if (dialog.getTitle().equals("Conversation " + conversationId + " Details")) {
                        JPanel panel = (JPanel) dialog.getContentPane().getComponent(1);
                        JButton saveButton = (JButton) panel.getComponent(0); //activam butonul de save odata ce conversatia e finalizata
                        JLabel messageLabel = (JLabel) panel.getComponent(1);//ascundem mesajul de atentionare
                        saveButton.setEnabled(true);
                        messageLabel.setVisible(false);
                        break;
                    }
                }
            }
        });
    }
    //actualizeaza live lista de mesaje a unei conversatii in fereastra de detalii
    public void updateConversationMessages(int conversationId, List<String> messages) {
        SwingUtilities.invokeLater(() -> {
            conversationMessages.put(conversationId, new ArrayList<>(messages));

            for (JDialog dialog : openDialogs) {
                if (dialog.getTitle().equals("Conversation " + conversationId + " Details")) {
                	//Actualizarea textului din fereastra de detalii
                    JTextArea textArea = (JTextArea) ((JScrollPane) dialog.getContentPane().getComponent(0)).getViewport().getView();
                    textArea.setText("");
                    for (String message : messages) {
                        textArea.append(message + "\n");
                    }
                    break;
                }
            }
        });
    }
    //afisare detalii conversatie
    private void showConversationDetails(int conversationId) {
        JDialog detailsDialog = new JDialog(this, "Conversation " + conversationId + " Details", true);
        detailsDialog.setSize(400, 300);
        detailsDialog.setLayout(new BorderLayout());

        JTextArea conversationArea = new JTextArea();
        conversationArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(conversationArea);

        List<String> messages = conversationMessages.getOrDefault(conversationId, new ArrayList<>());
        for (String message : messages) {
            conversationArea.append(message + "\n");
        }

        JButton saveButton = new JButton("Save Conversation");
        saveButton.setEnabled(messages.size() > 0 && conversations.get(conversationId - 1).contains("Finished"));
        saveButton.addActionListener(e -> saveConversationToFile(conversationId, conversationArea.getText()));

        JLabel messageLabel = new JLabel("Conversation must finish before saving.");
        messageLabel.setForeground(Color.RED);
        messageLabel.setVisible(!saveButton.isEnabled());

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(saveButton, BorderLayout.WEST);
        southPanel.add(messageLabel, BorderLayout.CENTER);

        detailsDialog.add(scrollPane, BorderLayout.CENTER);
        detailsDialog.add(southPanel, BorderLayout.SOUTH);
        detailsDialog.setVisible(true);
        openDialogs.add(detailsDialog);
    }
    //salvarea conversatiei in fisier txt
    private void saveConversationToFile(int conversationId, String content) {
        String fileName = "conversation_" + conversationId + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
            JOptionPane.showMessageDialog(this, "Conversation saved to " + fileName, "Save Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving conversation: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void clearLogFile() {
        File logFile = new File("resources.log");
        try {
            // Daca fisierul exista, il vom goli
            if (logFile.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
                    // Nu scriem nimic in fisier, il golim
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error clearing log file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {//  inchiderea ferestrei si oprirea thread-urilor
        super.dispose();// inchide fereastra Swing
        conversationExecutor.shutdown(); // inchide executorul principal pentru conversatii
        try {
            if (!conversationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                conversationExecutor.shutdownNow();// Forteaza oprirea thread-urilor ramase
            }
        } catch (InterruptedException e) {
            conversationExecutor.shutdownNow();// in cazul unei intreruperi, forteaza shutdown
        }
    }
    private void startResourceMonitoring() {
        // Suprascriem fisierul la inceputul monitorizarii resurselor
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("resources.log"))) {
            writer.write("Resource Monitoring Log 4.2.4 \n"); // versiunea aplicatiei
            writer.write("===== Note: For accurate results use custom mode ====\n"); //datele de aici includ delay-urile, deci nu sunt 100% accurate
            writer.write("========================\n");// modul acesta e pur pentru generarea conversatiilor, conteaza mai putin masurarea performantei  
        } catch (IOException e) {
            e.printStackTrace();
        }

        resourceExecutor.scheduleAtFixedRate(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("resources.log", true))) {
                double cpuLoad = resourceMonitor.getCpuLoad();
                long usedMemory = resourceMonitor.getUsedMemory();
                long committedMemory = resourceMonitor.getCommittedMemory();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS); // Log la fiecare secunda
    }
    
    public void disposeRes() { // similar cu dispose() dar opreste si monitorizarea resurselor pe langa inchiderea ferestrei si oprirea thread-urilor
        super.dispose();
        conversationExecutor.shutdown();
        resourceExecutor.shutdown(); // Se termina monitorizarea resurselor
        try {
            if (!conversationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                conversationExecutor.shutdownNow();
            }
            if (!resourceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                resourceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            conversationExecutor.shutdownNow();
            resourceExecutor.shutdownNow();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Server server = new Server();
            server.clearLogFile(); // curata fisierul log la inceputul rularii
            server.startResourceMonitoring(); // incepe monitorizarea resurselor
            server.setVisible(true);
        });
    }
    //functia pentru salvarea valorilor generate
    public void logResourceUsage(int conversationId, long startTime, long endTime, double startCpuLoad, double endCpuLoad, long startMemory, long endMemory) {
        String threadType = (conversationId % 2 == 0) ? "Traditional Thread" : "Virtual Thread"; // determina tipul de thread folosit
        // calcularea si scrierea in fisier a valorilor corespunzatoare conversatiei
        String logEntry = String.format(
                "%s | ID: %d%n %nTotal Time: %d ms%nCPU Load: %.5f%%%nUsed Memory: %.3fMB%n%n",
                threadType,
                conversationId,
             // am folosit functia modul pentru ca valorile CPU si valorile memoriei sa nu fie negative (arata urat si imi tot da senzatia ca ceva
                // nu e corect in aplicatie; timpul nu avea neaparat nevoie de functia asta, dar am zis ca rau nu-i face s-o am acolo preventiv )
                Math.abs(endTime - startTime),
               Math.abs(endCpuLoad - startCpuLoad),
               Math.abs( endMemory / (1024.0 * 1024)) - (startMemory / (1024.0 * 1024))
        );
        //scrierea in fisier a datelor obtinute; fisierul e suprascros de fiecare data
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("resources.log", true))) {
            writer.write(logEntry);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error writing to log file: " + e.getMessage(), "Logging Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    //functie pentru vizualizarea resurselor utilizate (in fisierul resources.log)
    private void viewResourceUsage() {
        try {
            Desktop.getDesktop().open(new File("resources.log"));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not open resources.log", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

}
