import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedTextEd extends JFrame {

    private JTextArea textArea;
    private JFileChooser fileChooser;
    private File currentFile;
    private JScrollPane scrollPane;
    private JCheckBoxMenuItem wordWrapItem;
    private boolean isDarkMode = false;
    private HashSet<String> dictionary; // for spell checker
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();

    public AdvancedTextEd() {
        setTitle("Advanced Text Editor");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        textArea = new JTextArea();
        fileChooser = new JFileChooser();

        scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        textArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        textArea.setMargin(new Insets(5, 5, 5, 5));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateUndoStack();
            }

            public void removeUpdate(DocumentEvent e) {
                updateUndoStack();
            }

            public void changedUpdate(DocumentEvent e) {
                updateUndoStack();
            }
        });

        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo");
        textArea.getActionMap().put("Undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                customUndo();
            }
        });

        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo");
        textArea.getActionMap().put("Redo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                customRedo();
            }
        });

        loadDictionary();
        createMenuBar();

        undoStack.push(textArea.getText());

        setVisible(true);
    }

    private void updateUndoStack() {
    String currentText = textArea.getText();
    if (undoStack.isEmpty() || !undoStack.peek().equals(currentText)) {
        undoStack.push(currentText);
        // Clear redo history since it's a new operation
        redoStack.clear();
    }
}


   private void customUndo() {
    if (undoStack.size() > 1) {
        String currentState = undoStack.pop();
        redoStack.push(currentState);
        textArea.setText(undoStack.peek()); // Peek at the new top
    }
}


    private void customRedo() {
        if (!redoStack.isEmpty()) {
            String nextState = redoStack.pop();
            textArea.setText(nextState);
            undoStack.push(nextState);
        }
    }

    private void loadDictionary() {
        dictionary = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader("dict.txt"))) {
            String word;
            while ((word = br.readLine()) != null) {
                dictionary.add(word.trim().toLowerCase());
            }
        } catch (IOException e) {
            System.out.println("Dictionary file not found. Spell checker will be disabled.");
        }
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newFile = new JMenuItem("New");
        JMenuItem open = new JMenuItem("Open");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem saveAs = new JMenuItem("Save As");
        JMenuItem exit = new JMenuItem("Exit");

        newFile.addActionListener(e -> newFile());
        open.addActionListener(e -> openFile());
        save.addActionListener(e -> saveFile());
        saveAs.addActionListener(e -> saveAsFile());
        exit.addActionListener(e -> System.exit(0));

        fileMenu.add(newFile);
        fileMenu.add(open);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.add(exit);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem cut = new JMenuItem("Cut");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem paste = new JMenuItem("Paste");
        JMenuItem undo = new JMenuItem("Undo");
        JMenuItem redo = new JMenuItem("Redo");

        cut.addActionListener(e -> textArea.cut());
        copy.addActionListener(e -> textArea.copy());
        paste.addActionListener(e -> textArea.paste());
        undo.addActionListener(e -> customUndo());
        redo.addActionListener(e -> customRedo());

        editMenu.add(cut);
        editMenu.add(copy);
        editMenu.add(paste);
        editMenu.add(undo);
        editMenu.add(redo);

        JMenu findMenu = new JMenu("Find");
        JMenuItem find = new JMenuItem("Find");
        JMenuItem replace = new JMenuItem("Replace");
        JMenuItem spellCheck = new JMenuItem("Spell Check");

        find.addActionListener(e -> findText());
        replace.addActionListener(e -> replaceText());
        spellCheck.addActionListener(e -> spellCheck());

        findMenu.add(find);
        findMenu.add(replace);
        findMenu.add(spellCheck);

        JMenu viewMenu = new JMenu("View");
        JMenuItem fontItem = new JMenuItem("Change Font");
        wordWrapItem = new JCheckBoxMenuItem("Word Wrap");
        JMenuItem themeToggle = new JMenuItem("Toggle Theme");

        fontItem.addActionListener(e -> changeFont());
        wordWrapItem.addActionListener(e -> toggleWordWrap());
        themeToggle.addActionListener(e -> toggleTheme());

        viewMenu.add(fontItem);
        viewMenu.add(wordWrapItem);
        viewMenu.add(themeToggle);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(findMenu);
        menuBar.add(viewMenu);

        setJMenuBar(menuBar);
    }

    private void spellCheck() {
        if (dictionary == null || dictionary.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Dictionary not loaded. Spell check unavailable.");
            return;
        }

        String text = textArea.getText();
        Set<String> misspelledWords = new TreeSet<>();

        Matcher matcher = Pattern.compile("\\b[a-zA-Z]+\\b").matcher(text);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase().trim();
            if (!dictionary.contains(word)) {
                misspelledWords.add(word);
            }
        }

        if (misspelledWords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No spelling errors found.");
        } else {
            JOptionPane.showMessageDialog(this, "Misspelled words:\n" + String.join(", ", misspelledWords));
        }
    }

    private void newFile() {
        textArea.setText("");
        currentFile = null;
        setTitle("Untitled - Advanced Text Editor");
        undoStack.clear();
        redoStack.clear();
        undoStack.push("");
    }

    private void openFile() {
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                textArea.read(reader, null);
                setTitle(currentFile.getName() + " - Advanced Text Editor");
                undoStack.clear();
                redoStack.clear();
                undoStack.push(textArea.getText());
            } catch (IOException ex) {
                showError("Failed to open file.");
            }
        }
    }

    private void saveFile() {
        if (currentFile != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                textArea.write(writer);
            } catch (IOException ex) {
                showError("Failed to save file.");
            }
        } else {
            saveAsFile();
        }
    }

    private void saveAsFile() {
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            saveFile();
            setTitle(currentFile.getName() + " - Advanced Text Editor");
        }
    }

    private void findText() {
        String input = JOptionPane.showInputDialog(this, "Find:");
        if (input != null && !input.isEmpty()) {
            String text = textArea.getText();
            List<Integer> indexes = new ArrayList<>();
            int index = text.indexOf(input);
            while (index >= 0) {
                indexes.add(index);
                index = text.indexOf(input, index + 1);
            }
            if (!indexes.isEmpty()) {
                textArea.select(indexes.get(0), indexes.get(0) + input.length());
                textArea.requestFocus();
                JOptionPane.showMessageDialog(this, "Found at positions: " + indexes);
            } else {
                JOptionPane.showMessageDialog(this, "Text not found.");
            }
        }
    }

    private void replaceText() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField findField = new JTextField();
        JTextField replaceField = new JTextField();
        panel.add(new JLabel("Find:"));
        panel.add(findField);
        panel.add(new JLabel("Replace with:"));
        panel.add(replaceField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Find & Replace", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String find = findField.getText();
            String replace = replaceField.getText();
            textArea.setText(textArea.getText().replace(find, replace));
        }
    }

    private void changeFont() {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String font = (String) JOptionPane.showInputDialog(this, "Choose Font", "Font",
                JOptionPane.PLAIN_MESSAGE, null, fonts, textArea.getFont().getFamily());
        if (font != null) {
            textArea.setFont(new Font(font, Font.PLAIN, 16));
        }
    }

    private void toggleWordWrap() {
        boolean wrap = wordWrapItem.isSelected();
        textArea.setLineWrap(wrap);
        textArea.setWrapStyleWord(wrap);
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            textArea.setBackground(Color.DARK_GRAY);
            textArea.setForeground(Color.WHITE);
            textArea.setCaretColor(Color.WHITE);
        } else {
            textArea.setBackground(Color.WHITE);
            textArea.setForeground(Color.BLACK);
            textArea.setCaretColor(Color.BLACK);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdvancedTextEd::new);
    }
}
