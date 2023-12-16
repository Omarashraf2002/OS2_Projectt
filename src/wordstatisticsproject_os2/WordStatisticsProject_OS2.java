package wordstatisticsproject_os2;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class WordStatisticsProject_OS2 extends JFrame {
    private JTextField directoryField;
    private JCheckBox subdirectoryCheckBox;
    private JButton browseButton;
    private JButton processButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JTextArea directoryInfoTextArea;

    private String overallLongestWord = "";
    private String overallShortestWord = "";
    private int overallIsCount = 0;
    private int overallAreCount = 0;
    private int overallYouCount = 0;
    private final Object lock = new Object(); // Mutex lock object
    private final Semaphore semaphore = new Semaphore(5); // Adjust the permits as needed

    public WordStatisticsProject_OS2() {
        setTitle("Word Statistics App");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel inputPanel = new JPanel(new FlowLayout());
        JLabel directoryLabel = new JLabel("Directory:");
        directoryField = new JTextField(20);
        inputPanel.add(directoryLabel);
        inputPanel.add(directoryField);

        browseButton = new JButton("Browse");
        inputPanel.add(browseButton);

        subdirectoryCheckBox = new JCheckBox("Include Subdirectories");
        inputPanel.add(subdirectoryCheckBox);
        processButton = new JButton("Start Processing");
        inputPanel.add(processButton);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        tableModel.addColumn("File Name");
        tableModel.addColumn("Word Count");
        tableModel.addColumn("is Count");
        tableModel.addColumn("are Count");
        tableModel.addColumn("you Count");
        tableModel.addColumn("Longest Word");
        tableModel.addColumn("Shortest Word");

        JScrollPane tableScrollPane = new JScrollPane(resultTable);

        directoryInfoTextArea = new JTextArea(10, 30);
        directoryInfoTextArea.setEditable(false);
        JScrollPane infoScrollPane = new JScrollPane(directoryInfoTextArea);

        add(inputPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(infoScrollPane, BorderLayout.SOUTH);

        browseButton.addActionListener(e -> browseDirectory());
        processButton.addActionListener(e -> processDirectory());

        setVisible(true);
    }

    private void browseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            directoryField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void processDirectory() {
        String directoryPath = directoryField.getText();
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Invalid directory");
            return;
        }

        // Reset overall statistics for each directory processing
        overallLongestWord = "";
        overallShortestWord = "";
        overallIsCount = 0;
        overallAreCount = 0;
        overallYouCount = 0;

        // Use ExecutorService for concurrent file processing
        ExecutorService executorService = Executors.newFixedThreadPool(5); // Adjust the pool size as needed

        executorService.execute(() -> {
            try {
                semaphore.acquire(); // Acquire a permit

                processFiles(directory, subdirectoryCheckBox.isSelected());
                findLongestAndShortestWords(directory);

                // Update GUI with overall statistics
                synchronized (lock) {
                    directoryInfoTextArea.append("Overall is Count: " + overallIsCount + "\n");
                    directoryInfoTextArea.append("Overall are Count: " + overallAreCount + "\n");
                    directoryInfoTextArea.append("Overall you Count: " + overallYouCount + "\n");
                    directoryInfoTextArea.append("Overall Longest Word: " + overallLongestWord + "\n");
                    directoryInfoTextArea.append("Overall Shortest Word: " + overallShortestWord + "\n");
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release(); // Release the permit
            }

            // Shutdown the executor service
            executorService.shutdown();
        });
    }

    private void processFiles(File directory, boolean includeSubdirectories) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && includeSubdirectories) {
                    processFiles(file, true);
                } else if (file.isFile() && file.getName().endsWith(".txt")) {
                    String directoryName = directory.getName();
                    processFileAndRecordWords(file, directoryName);
                }
            }
        }
    }

    private void processFileAndRecordWords(File file, String directoryName) {
        int wordCount = 0;
        String longestWord = "";
        String shortestWord = "";
        int isCount = 0;
        int areCount = 0;
        int youCount = 0;

        try {
            List<String> lines;
            synchronized (lock) {
                lines = Files.readAllLines(file.toPath());
            }

            for (String line : lines) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    wordCount++;
                    if (word.length() > longestWord.length()) {
                        longestWord = word;
                    }
                    if (shortestWord.isEmpty() || word.length() < shortestWord.length()) {
                        shortestWord = word;
                    }
                    if (word.equalsIgnoreCase("is")) {
                        isCount++;
                    } else if (word.equalsIgnoreCase("are")) {
                        areCount++;
                    } else if (word.equalsIgnoreCase("you")) {
                        youCount++;
                    }
                }
            }

            synchronized (lock) {
                tableModel.addRow(new String[] { file.getName(), String.valueOf(wordCount), String.valueOf(isCount),
                        String.valueOf(areCount), String.valueOf(youCount), longestWord, shortestWord });
                updateOverallStatistics(isCount, areCount, youCount, longestWord, shortestWord);
            }

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error occurred while reading files");
        }
    }

    private void updateOverallStatistics(int isCount, int areCount, int youCount, String currentLongestWord,
            String currentShortestWord) {
        synchronized (lock) {
            // Update overall counts
            overallIsCount += isCount;
            overallAreCount += areCount;
            overallYouCount += youCount;

            // Update overall longest word
            if (overallLongestWord.isEmpty() || currentLongestWord.length() > overallLongestWord.length()) {
                overallLongestWord = currentLongestWord;
            }

            // Update overall shortest word
            if (overallShortestWord.isEmpty() || currentShortestWord.length() < overallShortestWord.length()) {
                overallShortestWord = currentShortestWord;
            }
        }
    }

    private void findLongestAndShortestWords(File directory) {
        // Similar changes for concurrent processing if needed
        // ...
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WordStatisticsProject_OS2::new);
    }
}
