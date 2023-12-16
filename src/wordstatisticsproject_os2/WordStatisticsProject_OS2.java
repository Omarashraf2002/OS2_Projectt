package wordstatisticsproject_os2;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordStatisticsProject_OS2 extends JFrame {
    private JTextField directoryField;
    private JCheckBox subdirectoryCheckBox;
    private JButton browseButton;
    private JButton processButton;
//    private JButton showLongestShortestButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JTextArea directoryInfoTextArea;

    private String overallLongestWord = "";
    private String overallShortestWord = "";

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

//        showLongestShortestButton = new JButton("Show Longest and Shortest Words Per Directory");
//        inputPanel.add(showLongestShortestButton);

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

//        showLongestShortestButton.addActionListener(e -> showLongestAndShortestWordsPerDirectory());

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

        // Reset overall longest and shortest words for each directory processing
        overallLongestWord = "";
        overallShortestWord = "";

        processFiles(directory, subdirectoryCheckBox.isSelected());
        findLongestAndShortestWords(directory);

        // Update GUI with overall longest and shortest words
        directoryInfoTextArea.append("Overall Longest Word: " + overallLongestWord + "\n");
        directoryInfoTextArea.append("Overall Shortest Word: " + overallShortestWord + "\n");
    }

    private void showLongestAndShortestWordsPerDirectory() {
        String directoryPath = directoryField.getText();
        File rootDirectory = new File(directoryPath);

        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Invalid directory");
            return;
        }

        Map<String, String> longestWordsMap = new HashMap<>();
        Map<String, String> shortestWordsMap = new HashMap<>();

        processFilesAndRecordWords(rootDirectory, longestWordsMap, shortestWordsMap, subdirectoryCheckBox.isSelected());

        StringBuilder infoBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : longestWordsMap.entrySet()) {
            String directoryName = entry.getKey();
            String longestWord = entry.getValue();
            String shortestWord = shortestWordsMap.get(directoryName);

            infoBuilder.append("Directory: ").append(directoryName).append("\n");
            infoBuilder.append("Longest word: ").append(longestWord).append("\n");
            infoBuilder.append("Shortest word: ").append(shortestWord).append("\n\n");
        }

        directoryInfoTextArea.setText(infoBuilder.toString());
    }

    private void processFilesAndRecordWords(File directory, Map<String, String> longestWords, Map<String, String> shortestWords, boolean includeSubdirectories) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && includeSubdirectories) {
                    processFilesAndRecordWords(file, longestWords, shortestWords, true);
                } else if (file.isFile() && file.getName().endsWith(".txt")) {
                    String directoryName = directory.getName();
                    processFileAndRecordWords(file, directoryName, longestWords, shortestWords);
                }
            }
        }
    }

    private void processFileAndRecordWords(File file, String directoryName, Map<String, String> longestWords, Map<String, String> shortestWords) {
        int wordCount = 0;
        String longestWord = "";
        String shortestWord = "";

        try {
            List<String> lines = Files.readAllLines(file.toPath());
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
                }
            }

            longestWords.put(directoryName, longestWord);
            shortestWords.put(directoryName, shortestWord);

            // Update overall longest and shortest words
            updateOverallLongestAndShortest(longestWord, shortestWord);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error occurred while reading files");
        }
    }

    private void updateOverallLongestAndShortest(String currentLongestWord, String currentShortestWord) {
        // Update overall longest word
        if (overallLongestWord.isEmpty() || currentLongestWord.length() > overallLongestWord.length()) {
            overallLongestWord = currentLongestWord;
        }

        // Update overall shortest word
        if (overallShortestWord.isEmpty() || currentShortestWord.length() < overallShortestWord.length()) {
            overallShortestWord = currentShortestWord;
        }
    }

    private void processFiles(File directory, boolean includeSubdirectories) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && includeSubdirectories) {
                    processFiles(file, true);
                } else if (file.isFile() && file.getName().endsWith(".txt")) {
                    processFile(file, file.getName());
                }
            }
        }
    }

    private void processFile(File file, String filename) {
        int wordCount = 0;
        String longestWord = "";
        String shortestWord = "";
        int isCount = 0;
        int areCount = 0;
        int youCount = 0;

        try {
            List<String> lines = Files.readAllLines(file.toPath());
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

            tableModel.addRow(new String[]{filename, String.valueOf(wordCount), String.valueOf(isCount), String.valueOf(areCount), String.valueOf(youCount), longestWord, shortestWord});

            // Update overall longest and shortest words
            updateOverallLongestAndShortest(longestWord, shortestWord);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error occurred while reading files");
        }
    }

    private void findLongestAndShortestWords(File directory) {
        String longestWordInDirectory = "";
        String shortestWordInDirectory = "";

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    try {
                        List<String> lines = Files.readAllLines(file.toPath());
                        for (String line : lines) {
                            String[] words = line.split("\\s+");
                            for (String word : words) {
                                if (longestWordInDirectory.isEmpty() || word.length() > longestWordInDirectory.length()) {
                                    longestWordInDirectory = word;
                                }
                                if (shortestWordInDirectory.isEmpty() || word.length() < shortestWordInDirectory.length()) {
                                    shortestWordInDirectory = word;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (file.isDirectory()) {
                    findLongestAndShortestWords(file);
                }
            }
        }

        System.out.println("Longest word in directory: " + longestWordInDirectory);
        System.out.println("Shortest word in directory: " + shortestWordInDirectory);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WordStatisticsProject_OS2::new);
    }
    
}
