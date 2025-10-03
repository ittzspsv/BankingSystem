package banking_modal;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ConsoleFrame extends JFrame {
    private final JTextPane consolePane;
    private final StyledDocument doc;
    private int inputStart = 0;
    private final PipedOutputStream pipedOut;
    private final PrintStream guiOut;
    private final PrintWriter inputWriter;
    private final PrintStream originalErr;

    public ConsoleFrame() {
        super("MyBank Console");

        consolePane = new JTextPane();
        consolePane.setEditable(false);
        consolePane.setBackground(Color.WHITE);
        consolePane.setForeground(Color.BLACK);
        consolePane.setFont(new Font("Consolas", Font.PLAIN, 14));

        doc = consolePane.getStyledDocument();

        JScrollPane scrollPane = new JScrollPane(consolePane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);


        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin;
        try {
            pin = new PipedInputStream(pout);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        pipedOut = pout;
        guiOut = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                appendToConsole(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                appendToConsole(new String(b, off, len));
            }
        }, true);

        originalErr = System.err;
        System.setOut(guiOut);
        System.setErr(originalErr);


        PipedOutputStream inputPout = new PipedOutputStream();
        PipedInputStream inputPin;
        try {
            inputPin = new PipedInputStream(inputPout);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.setIn(inputPin);
        inputWriter = new PrintWriter(inputPout, true);


        consolePane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == KeyEvent.CHAR_UNDEFINED) return;

                if (c == '\b') { // backspace
                    if (getCaretPosition() > inputStart) {
                        try {
                            doc.remove(getCaretPosition() - 1, 1);
                        } catch (BadLocationException ex) {
                            ex.printStackTrace(originalErr);
                        }
                    }
                } else if (c == '\n') { // enter
                    String input = getCurrentInput();
                    appendNewline();
                    inputWriter.println(input);
                } else if (!Character.isISOControl(c)) {
                    try {
                        doc.insertString(getCaretPosition(), String.valueOf(c), null);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace(originalErr);
                    }
                }
                e.consume();
            }
        });

        setVisible(true);

        new Thread(() -> {
            try {
                Service.main(new String[0]);
            } catch (Exception ex) {
                ex.printStackTrace(originalErr);
            }
        }).start();
    }

    private void appendToConsole(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Insert text at the end always
                doc.insertString(doc.getLength(), text, null);
                consolePane.setCaretPosition(doc.getLength());
                inputStart = doc.getLength();
            } catch (BadLocationException e) {
                e.printStackTrace(originalErr);
            }
        });
    }

    private void appendNewline() {
        try {
            doc.insertString(doc.getLength(), "\n", null);
            consolePane.setCaretPosition(doc.getLength());
            inputStart = doc.getLength();
        } catch (BadLocationException e) {
            e.printStackTrace(originalErr);
        }
    }

    private String getCurrentInput() {
        try {
            return doc.getText(inputStart, doc.getLength() - inputStart);
        } catch (BadLocationException e) {
            e.printStackTrace(originalErr);
            return "";
        }
    }

    private int getCaretPosition() {
        return consolePane.getCaretPosition();
    }

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        SwingUtilities.invokeLater(ConsoleFrame::new);
    }
}