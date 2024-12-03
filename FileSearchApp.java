// File: FileSearchApp.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

public class FileSearchApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileSearchFrame::new);
    }
}

class FileSearchFrame extends JFrame {
    private final JTextField folderPathField;
    private final JTextField searchField;
    private final DefaultListModel<String> resultListModel;
    private final JList<String> resultList;
    private FileSearchTask searchTask;

    public FileSearchFrame() {
        setTitle("File Search Application");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout and components
        setLayout(new BorderLayout());

        // Top panel for input
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel folderPathPanel = new JPanel(new BorderLayout(5, 5));
        JLabel folderPathLabel = new JLabel("Folder Path:");
        folderPathField = new JTextField();
        JButton browseButton = new JButton("Browse");

        // ActionListener for Browse button
        browseButton.addActionListener(e -> chooseFolder());

        folderPathPanel.add(folderPathField, BorderLayout.CENTER);
        folderPathPanel.add(browseButton, BorderLayout.EAST);

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        JLabel searchLabel = new JLabel("Search Keyword (Name or Content):");
        searchField = new JTextField();

        searchPanel.add(searchField, BorderLayout.CENTER);

        topPanel.add(folderPathLabel, BorderLayout.NORTH);
        topPanel.add(folderPathPanel, BorderLayout.CENTER);
        topPanel.add(searchLabel, BorderLayout.SOUTH);
        topPanel.add(searchField, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Center panel for results
        resultListModel = new DefaultListModel<>();
        resultList = new JList<>(resultListModel);
        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));

        // Add double-click listener to open files
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Detect double-click
                    int index = resultList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String selectedFile = resultListModel.getElementAt(index);
                        openFile(selectedFile);
                    }
                }
            }
        });

        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel for buttons
        JPanel bottomPanel = new JPanel();
        JButton searchButton = new JButton("Search");
        JButton clearButton = new JButton("Clear");

        searchButton.addActionListener(new SearchButtonListener());
        clearButton.addActionListener(e -> clearFields());

        bottomPanel.add(searchButton);
        bottomPanel.add(clearButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void chooseFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            folderPathField.setText(selectedFolder.getAbsolutePath());
        }
    }

    private void clearFields() {
        folderPathField.setText("");
        searchField.setText("");
        resultListModel.clear();
        if (searchTask != null && !searchTask.isDone()) {
            searchTask.cancel(true);
        }
    }

    private void openFile(String filePath) {
        try {
            if (filePath.startsWith("Found in")) {
                filePath = filePath.substring(filePath.indexOf(":") + 2); // Extract actual file path
            }
            Desktop desktop = Desktop.getDesktop();
            desktop.open(new File(filePath)); // Open file with default application
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to open the file: " + filePath,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class SearchButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String folderPath = folderPathField.getText().trim();
            String keyword = searchField.getText().trim();

            if (folderPath.isEmpty() || keyword.isEmpty()) {
                JOptionPane.showMessageDialog(FileSearchFrame.this,
                        "Please provide both folder path and keyword.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File folder = new File(folderPath);
            if (!folder.isDirectory()) {
                JOptionPane.showMessageDialog(FileSearchFrame.this,
                        "Invalid folder path. Please enter a valid directory.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            resultListModel.clear(); // Clear previous results
            if (searchTask != null && !searchTask.isDone()) {
                searchTask.cancel(true); // Cancel any ongoing task
            }

            searchTask = new FileSearchTask(folder, keyword);
            searchTask.execute(); // Start the search
        }
    }

    private class FileSearchTask extends SwingWorker<Void, String> {
        private final File folder;
        private final String keyword;

        public FileSearchTask(File folder, String keyword) {
            this.folder = folder;
            this.keyword = keyword;
        }

        @Override
        protected Void doInBackground() throws Exception {
            searchFiles(folder);
            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String filePath : chunks) {
                resultListModel.addElement(filePath);
            }
        }

        private void searchFiles(File folder) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (isCancelled()) {
                        break; // Stop if the task is cancelled
                    }

                    if (file.isDirectory()) {
                        searchFiles(file);
                    } else {
                        if (file.getName().contains(keyword)) {
                            publish("Found in file name: " + file.getAbsolutePath());
                        } else if (containsKeyword(file, keyword)) {
                            publish("Found in file content: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }

        private boolean containsKeyword(File file, String keyword) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(keyword)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                // Ignore files that cannot be read
            }
            return false;
        }

        @Override
        protected void done() {
            if (isCancelled()) {
                resultListModel.addElement("Search cancelled.");
            } else {
                resultListModel.addElement("Search completed.");
            }
        }
    }
}
