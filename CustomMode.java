package mts;

import javax.swing.*; // pentru lucrul cu interfata (necesar pt introducerea parametrilor)
import java.io.BufferedWriter;// pentru scrierea in fisierul log
import java.io.FileWriter;// pentru scrierea in fisierul log
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService; // pentru generarea thread-urilor traditionale si virtuale
import java.util.concurrent.Executors;// pentru generarea thread-urilor traditionale si virtuale
import java.awt.Desktop;//
import java.io.File;// pentru scrierea in fisierul log


 // CustomMode genereaza un set de conversatii controlate (fara delay, nr fix de mesaje per conversatie ales de utilizator) 
 // pentru monitorizarea resurselor cu acuratete ridicata
 // Utilizatorul specifica nr de mesaje pe care fiecare conversatie ar trebui sa o aiba
 // Rezultatele sunt salvate in custom.log

public class CustomMode {
    private final Server ui;
    private final ExecutorService traditionalExecutor = Executors.newFixedThreadPool(50); // generam 50 thread-uri traditionale
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor(); // restul sunt virtuale
    private final int messagesPerConversation; // nr ales de mesaje per conversatie

    
    //constructor
    public CustomMode(Server ui) {
        this.ui = ui;
        this.messagesPerConversation = promptForMessageCount();
    }
    
    //popup pentru selectarea nr de mesaje per conversatie
    private int promptForMessageCount() {
        while (true) {
            String input = JOptionPane.showInputDialog(
                ui,
                "Enter the number of messages per conversation:",
                "Custom Mode Setup",
                JOptionPane.QUESTION_MESSAGE
            );

            if (input == null) return -1; // Utilizatorul a dat cancel
            try {
                int num = Integer.parseInt(input);
                if (num > 0) return num;
                // ne asiguram ca utilizatorul introduce un nr intreg nenul pozitiv de mesaje. 
                // sa nu ne aflam in situatia de a incerca sa generam o conversatie cu „-3” , „5.74” sau „xD” mesaje
                JOptionPane.showMessageDialog(ui, "Please enter a positive number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(ui, "Please enter a valid integer.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void start() {
        if (messagesPerConversation == -1) return; // Utilizatorul a dat cancel

        logCustomModeStart(); // incepe rularea modului custom

        List<Runnable> conversations = new ArrayList<>();

        // Creaza 50 conversatii pt thread-uri traditionale
        for (int i = 1; i <= 50; i++) {
            conversations.add(new CustomConversationHandler(i, ui, false, messagesPerConversation));
        }

     // Creaza 50 conversatii pt thread-uri virtuale
        for (int i = 51; i <= 100; i++) {
            conversations.add(new CustomConversationHandler(i, ui, true, messagesPerConversation));
        }

        //asignam tipul de thread in functie de id-ul conversatiei. id < 50 = traditional, altfel foloseste thread virtual 
        for (int i = 0; i < 50; i++) {
            traditionalExecutor.submit(conversations.get(i));  
            virtualExecutor.submit(conversations.get(i + 50)); 
        }
        //inchidem executoarele, thread-urile si-au indeplinit task-urile
        traditionalExecutor.shutdown();
        virtualExecutor.shutdown();

        // arata popup si deschide fisierul log dupa ce utilizatorul apasa "OK"
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                ui,
                "Custom Mode has completed.\nLog saved to custom.log.",
                "Custom Mode Finished",
                JOptionPane.INFORMATION_MESSAGE
            );

            openLogFile("custom.log"); // Deschide automat fisierul log
        });
    }

    private void openLogFile(String filename) {
        File logFile = new File(filename);
        if (logFile.exists()) {
            try {
                Desktop.getDesktop().open(logFile); // deschide fisierul
            } catch (IOException e) {
                JOptionPane.showMessageDialog(ui, "Could not open " + filename, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(ui, "Log file not found!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logCustomModeStart() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("custom.log", false))) {
            writer.write("Custom Mode Resource Monitoring\n"); // modul dedicat pentru monitorizarea resurselor
            writer.write("Messages per conversation: " + messagesPerConversation + "\n");// afisam si nr de mesaje
            writer.write("=============================\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
